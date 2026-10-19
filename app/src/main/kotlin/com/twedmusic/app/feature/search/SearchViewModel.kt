package com.twedmusic.app.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.AudioRepository
import com.twedmusic.app.core.data.repository.CoverRepository
import com.twedmusic.app.core.data.repository.PlaylistRepository
import com.twedmusic.app.core.model.Playlist
import com.twedmusic.app.core.model.Song
import com.twedmusic.app.feature.playback.PlaybackController
import com.twedmusic.app.feature.playback.engine.BassPlayerAdapter
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * Resultado de la búsqueda: el texto que ya pasó el debounce y los
 * resultados de canciones y playlists para ese texto.
 */
data class SearchUiState(
    val debouncedQuery: String = "",
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList()
)

/**
 * ViewModel de la búsqueda global.
 *
 * - [query]: texto actual del campo (se actualiza en cada tecla).
 * - [uiState]: resultados reactivos. La consulta a Room se hace
 *   [SEARCH_DEBOUNCE_MS] ms después del último cambio de texto para
 *   evitar consultas excesivas mientras el usuario escribe.
 */
@HiltViewModel
class SearchViewModel @Inject constructor(
    audioRepository: AudioRepository,
    playlistRepository: PlaylistRepository,
    private val playbackController: PlaybackController,
    private val coverRepository: CoverRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<SearchUiState> = _query
        .debounce(SEARCH_DEBOUNCE_MS)
        .flatMapLatest { debounced ->
            combine(
                audioRepository.searchSongs(debounced),
                playlistRepository.searchPlaylists(debounced)
            ) { songs, playlists ->
                SearchUiState(debouncedQuery = debounced, songs = songs, playlists = playlists)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState())

    val currentMediaId: StateFlow<String?> = playbackController.currentMediaId

    fun onQueryChange(value: String) {
        _query.value = value
    }

    /** Archivo de portada en disco (consulta con caché); `null` si no hay. */
    fun coverFile(path: String): File? = coverRepository.coverFile(path)

    /** Reproduce [song] con los resultados de canciones como cola. */
    fun onSongClick(song: Song, results: List<Song>) {
        playbackController.playSong(
            song = song,
            queueId = BassPlayerAdapter.QUEUE_SEARCH,
            queueSongs = results
        )
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /**
         * Delay del debounce (ms) antes de ejecutar la búsqueda tras el
         * último cambio en el campo de texto.
         */
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
