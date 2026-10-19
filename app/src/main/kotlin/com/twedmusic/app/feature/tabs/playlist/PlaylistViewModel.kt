package com.twedmusic.app.feature.tabs.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.PlaylistRepository
import com.twedmusic.app.core.model.Playlist
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel de la pestaña Listas: expone las playlists del usuario con
 * su contador de canciones.
 */
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    playlistRepository: PlaylistRepository
) : ViewModel() {

    val playlists: StateFlow<List<Playlist>> = playlistRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
