package com.twedmusic.app.feature.tabs.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.ArtistImageRepository
import com.twedmusic.app.core.data.repository.AudioRepository
import com.twedmusic.app.core.data.repository.CoverRepository
import com.twedmusic.app.core.data.repository.PlaybackHistoryRepository
import com.twedmusic.app.core.database.TotalStats
import com.twedmusic.app.core.model.Album
import com.twedmusic.app.core.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val songCount: Int = 0,
    val totalStats: TotalStats = TotalStats(0, 0, 0L),
    val recentlyPlayed: List<Song> = emptyList(),
    /** 5 artistas más recientes por dateAdded de biblioteca (no por play). */
    val recentArtists: List<String> = emptyList(),
    /** 4 álbumes más recientes por dateAdded de biblioteca. */
    val recentAlbums: List<Album> = emptyList()
)

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
            recentArtists = buildRecentArtists(songs),
            recentAlbums = buildRecentAlbums(songs)
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    fun coverFile(path: String): File? = coverRepository.coverFile(path)

    fun artistImageFile(name: String): File? = artistImageRepository.artistImageFile(name)

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        const val RECENTLY_PLAYED_LIMIT = 3
        const val ARTISTS_LIMIT = 5
        const val ALBUMS_LIMIT = 4
    }
}

/**
 * 5 artistas cuyo material se agregó más recientemente a la biblioteca.
 * Criterio: max(dateAdded) de las pistas de cada artista.
 * [Song.dateAdded] está en segundos (MediaStore DATE_ADDED).
 * No usa historial de reproducción.
 */
private fun buildRecentArtists(songs: List<Song>): List<String> {
    if (songs.isEmpty()) return emptyList()
    return songs
        .asSequence()
        .filter { it.artist.isNotBlank() }
        .groupBy { it.artist }
        .mapValues { (_, list) -> list.maxOf { it.dateAdded } }
        .entries
        .sortedByDescending { it.value }
        .take(ARTISTS_LIMIT)
        .map { it.key }
}

/**
 * 4 álbumes más recientes por max(dateAdded) del grupo.
 * dateAdded en segundos; isNew compara contra epoch en segundos.
 */
private fun buildRecentAlbums(songs: List<Song>): List<Album> {
    if (songs.isEmpty()) return emptyList()

    // dateAdded es epoch en SEGUNDOS → umbral también en segundos
    val thresholdSec =
        (System.currentTimeMillis() / 1000L) - NEW_ALBUM_THRESHOLD_DAYS.inWholeSeconds

    return songs
        .groupBy { albumKeyOf(it) }
        .map { (key, groupSongs) ->
            val firstSong = groupSongs.first()
            val title = firstSong.album?.takeIf { it.isNotBlank() } ?: firstSong.artist
            val artist = firstSong.albumArtist?.takeIf { it.isNotBlank() }
                ?: firstSong.artist
            val year = groupSongs.firstNotNullOfOrNull { it.year }
            val trackCount = groupSongs.size
            val coverPath = groupSongs.firstOrNull { it.path.isNotBlank() }?.path
            val maxDateAdded = groupSongs.maxOfOrNull { it.dateAdded } ?: 0L
            Album(
                key = key,
                title = title,
                artist = artist,
                year = year,
                trackCount = trackCount,
                coverPath = coverPath,
                isNew = maxDateAdded >= thresholdSec
            ) to maxDateAdded
        }
        .sortedByDescending { it.second }
        .take(ALBUMS_LIMIT)
        .map { it.first }
}

private fun albumKeyOf(song: Song): String {
    val albumName = song.album?.takeIf { it.isNotBlank() }
    val artistName = song.albumArtist?.takeIf { it.isNotBlank() }
        ?: song.artist.takeIf { it.isNotBlank() }
        ?: "Unknown Artist"
    return if (albumName != null) "$albumName|$artistName" else "|$artistName"
}

private val NEW_ALBUM_THRESHOLD_DAYS = 30.days

private const val ARTISTS_LIMIT = 5
private const val ALBUMS_LIMIT = 4