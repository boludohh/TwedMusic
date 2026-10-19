package com.twedmusic.app.feature.tabs.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.ArtistImageRepository
import com.twedmusic.app.core.data.repository.AudioRepository
import com.twedmusic.app.core.data.repository.CoverRepository
import com.twedmusic.app.core.data.repository.PlaybackHistoryRepository
import com.twedmusic.app.core.database.TotalStats
import com.twedmusic.app.core.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** Álbum reciente para la sección de álbumes de Home. */
data class HomeAlbumData(
    val title: String,
    val artist: String,
    val coverPath: String?,
    val dateAdded: Long
)

/** Estado completo de la pantalla de inicio. */
data class HomeUiState(
    /** Total de canciones de la biblioteca. */
    val songCount: Int = 0,
    val totalStats: TotalStats = TotalStats(0, 0, 0L),
    /** Últimas canciones reproducidas (la primera alimenta "Seguir escuchando"). */
    val recentlyPlayed: List<Song> = emptyList(),
    /** Últimas canciones agregadas a la biblioteca. */
    val recentlyAdded: List<Song> = emptyList(),
    /** Artistas únicos para la sección "Tus artistas". */
    val uniqueArtists: List<String> = emptyList(),
    /** Álbumes más recientes (agrupados por álbum + artista). */
    val recentAlbums: List<HomeAlbumData> = emptyList()
)

/**
 * ViewModel de la pestaña Inicio.
 *
 * Combina la biblioteca y el historial de reproducción en un único
 * [HomeUiState]. Todo el cálculo (agrupar, ordenar, recortar) se hace
 * en un hilo de fondo y no en el composable.
 *
 * Las portadas y las imágenes de artista siguen consultándose por
 * archivo (con caché) desde la UI mediante [coverFile] y
 * [artistImageFile], igual que antes de introducir el ViewModel.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    audioRepository: AudioRepository,
    playbackHistoryRepository: PlaybackHistoryRepository,
    private val coverRepository: CoverRepository,
    private val artistImageRepository: ArtistImageRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        audioRepository.songs,
        playbackHistoryRepository.totalStats,
        playbackHistoryRepository.getRecentlyPlayed(RECENTLY_PLAYED_LIMIT)
    ) { songs, totalStats, recentlyPlayed ->
        HomeUiState(
            songCount = songs.size,
            totalStats = totalStats,
            recentlyPlayed = recentlyPlayed,
            recentlyAdded = songs.sortedByDescending { it.dateAdded }.take(SECTION_LIMIT),
            uniqueArtists = songs
                .mapNotNull { it.artist.takeIf { name -> name.isNotBlank() } }
                .distinct()
                .take(SECTION_LIMIT),
            recentAlbums = buildRecentAlbums(songs)
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    /** Archivo de portada en disco (consulta con caché); `null` si no hay. */
    fun coverFile(path: String): File? = coverRepository.coverFile(path)

    /** Imagen del artista en disco (consulta con caché); `null` si no hay. */
    fun artistImageFile(name: String): File? = artistImageRepository.artistImageFile(name)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val RECENTLY_PLAYED_LIMIT = 5
        const val SECTION_LIMIT = 10
    }
}

/** Agrupa por álbum + artista y devuelve los más recientes primero. */
private fun buildRecentAlbums(songs: List<Song>): List<HomeAlbumData> =
    songs
        .groupBy { song ->
            val albumName = song.album?.takeIf { it.isNotBlank() }
            val artistName = song.albumArtist?.takeIf { it.isNotBlank() }
                ?: song.artist.takeIf { it.isNotBlank() }
                ?: "Unknown Artist"
            if (albumName != null) "$albumName|$artistName" else "|$artistName"
        }
        .map { (_, groupSongs) ->
            val firstSong = groupSongs.first()
            val title = firstSong.album?.takeIf { it.isNotBlank() } ?: firstSong.artist
            val artist = firstSong.albumArtist?.takeIf { it.isNotBlank() } ?: firstSong.artist
            val coverPath = groupSongs.firstOrNull { it.path.isNotBlank() }?.path
            val dateAdded = groupSongs.maxOf { it.dateAdded }
            HomeAlbumData(title, artist, coverPath, dateAdded)
        }
        .sortedByDescending { it.dateAdded }
        .take(10)
