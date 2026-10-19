package com.twedmusic.app.feature.tabs.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.AudioRepository
import com.twedmusic.app.core.data.repository.CoverRepository
import com.twedmusic.app.core.model.Album
import com.twedmusic.app.core.model.Song
import com.twedmusic.app.feature.playback.PlaybackController
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Estado del detalle de un álbum: sus canciones ordenadas (disco y
 * número de pista) y la duración total.
 */
data class AlbumDetailUiState(
    val songs: List<Song> = emptyList(),
    val totalDurationMs: Long = 0L
)

/**
 * ViewModel de la pestaña Álbumes (lista + detalle).
 *
 * - [albums]: canciones agrupadas por álbum, ordenadas por título.
 * - [selectedAlbum]: álbum abierto en el detalle (`null` = lista).
 *   Vive en el ViewModel, por lo que sobrevive a una rotación.
 * - [albumDetail]: canciones del álbum seleccionado.
 *
 * Lo comparten [AlbumsScreen] y [AlbumDetailScreen] (misma instancia).
 */
@HiltViewModel
class AlbumsViewModel @Inject constructor(
    audioRepository: AudioRepository,
    private val playbackController: PlaybackController,
    private val coverRepository: CoverRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = audioRepository.songs
        .map { songs -> groupSongsByAlbum(songs) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private val selectedAlbumKey = MutableStateFlow<String?>(null)

    val selectedAlbum: StateFlow<Album?> = combine(albums, selectedAlbumKey) { list, key ->
        if (key == null) null else list.firstOrNull { it.key == key }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val albumDetail: StateFlow<AlbumDetailUiState> =
        combine(audioRepository.songs, selectedAlbumKey) { allSongs, key ->
            if (key == null) {
                AlbumDetailUiState()
            } else {
                val songs = allSongs
                    .filter { albumKeyOf(it) == key }
                    .sortedWith(
                        compareBy<Song> { it.discNumber ?: 0 }
                            .thenBy { it.trackNumber ?: Int.MAX_VALUE }
                            .thenBy { it.title.lowercase() }
                    )
                AlbumDetailUiState(songs = songs, totalDurationMs = songs.sumOf { it.duration })
            }
        }
            .flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AlbumDetailUiState())

    val currentMediaId: StateFlow<String?> = playbackController.currentMediaId

    fun selectAlbum(album: Album) {
        selectedAlbumKey.value = album.key
    }

    fun clearSelection() {
        selectedAlbumKey.value = null
    }

    /** Archivo de portada en disco (consulta con caché); `null` si no hay. */
    fun coverFile(path: String): File? = coverRepository.coverFile(path)

    /** Reproduce el álbum desde su primera canción. */
    fun playAlbum() {
        val songs = albumDetail.value.songs
        val first = songs.firstOrNull() ?: return
        playbackController.playSong(song = first, queueId = QUEUE_ALBUM, queueSongs = songs)
    }

    /** Reproduce [song] con todo el álbum como cola. */
    fun playSong(song: Song) {
        playbackController.playSong(
            song = song,
            queueId = QUEUE_ALBUM,
            queueSongs = albumDetail.value.songs
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /**
         * Queue ID del detalle de álbum: permite al adapter distinguir
         * la cola global de biblioteca de la cola de un álbum.
         */
        const val QUEUE_ALBUM = "album"
    }
}

/** Duración para considerar un álbum como "Nuevo" (badge en la portada). */
private val NEW_ALBUM_THRESHOLD_DAYS = 30.days

/**
 * Clave de agrupación de una canción: `"${album}|${albumArtist}"`.
 * Para canciones sin álbum se usa el artista como fallback
 * (`"|${artist}"`), de modo que las sueltas se agrupan por artista.
 */
private fun albumKeyOf(song: Song): String {
    val albumName = song.album?.takeIf { it.isNotBlank() }
    val artistName = song.albumArtist?.takeIf { it.isNotBlank() }
        ?: song.artist.takeIf { it.isNotBlank() }
        ?: "Unknown Artist"
    return if (albumName != null) "$albumName|$artistName" else "|$artistName"
}

/**
 * Agrupa canciones por álbum y devuelve una lista de [Album] ordenada
 * alfabéticamente por título.
 *
 * - Carátula: se usa el `path` de la primera canción del grupo con
 *   ruta no vacía; si ninguna la tiene, [Album.coverPath] es `null`.
 * - Badge "Nuevo": [Album.isNew] es `true` si la canción más reciente
 *   del grupo se agregó en los últimos 30 días.
 */
private fun groupSongsByAlbum(songs: List<Song>): List<Album> {
    if (songs.isEmpty()) return emptyList()

    val newThresholdMs = System.currentTimeMillis() - NEW_ALBUM_THRESHOLD_DAYS.inWholeMilliseconds

    return songs.groupBy { albumKeyOf(it) }.map { (key, groupSongs) ->
        val firstSong = groupSongs.first()

        val title = firstSong.album?.takeIf { it.isNotBlank() }
            ?: firstSong.artist

        val artist = firstSong.albumArtist?.takeIf { it.isNotBlank() }
            ?: firstSong.artist

        val year = groupSongs.firstNotNullOfOrNull { it.year }

        val coverPath = groupSongs
            .filter { it.path.isNotBlank() }
            .firstOrNull()?.path

        val maxDateAdded = groupSongs.maxOfOrNull { it.dateAdded } ?: 0L
        val isNew = maxDateAdded > newThresholdMs

        Album(
            key = key,
            title = title,
            artist = artist,
            year = year,
            trackCount = groupSongs.size,
            coverPath = coverPath,
            isNew = isNew
        )
    }.sortedBy { it.title.lowercase() }
}
