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

data class HomeAlbumData(
    val title: String,
    val artist: String,
    val coverPath: String?,
    val dateAdded: Long
)

data class HomeUiState(
    val songCount: Int = 0,
    val totalStats: TotalStats = TotalStats(0, 0, 0L),
    val recentlyPlayed: List<Song> = emptyList(),
    val recentArtists: List<String> = emptyList(),
    val recentAlbums: List<HomeAlbumData> = emptyList()
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
            recentArtists = buildRecentArtists(songs, recentlyPlayed),
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
 * Artistas más recientes: primero los de reproducciones recientes,
 * luego el resto por fecha de adición de sus pistas.
 */
private fun buildRecentArtists(
    songs: List<Song>,
    recentlyPlayed: List<Song>
): List<String> {
    val ordered = LinkedHashSet<String>()
    recentlyPlayed.forEach { song ->
        song.artist.takeIf { it.isNotBlank() }?.let { ordered.add(it) }
    }
    songs
        .sortedByDescending { it.dateAdded }
        .forEach { song ->
            song.artist.takeIf { it.isNotBlank() }?.let { ordered.add(it) }
        }
    return ordered.take(5).toList()
}

/** Agrupa por álbum + artista; los más recientes primero (máx. 4). */
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
        .take(4)