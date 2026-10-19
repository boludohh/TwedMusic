package com.twedmusic.app.feature.tabs.artists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.ArtistImageRepository
import com.twedmusic.app.core.data.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel de la pestaña Artistas.
 *
 * - [artists]: nombres de artista únicos, ordenados alfabéticamente.
 * - [artistImages]: mapa reactivo nombre → imagen en disco (solo las
 *   ya resueltas).
 *
 * Al crearse lanza el enriquecido remoto cache-first: cada vez que
 * cambia la lista de artistas se cancela la corrida anterior y se
 * inicia otra. El Mutex interno de [ArtistImageRepository] descarta
 * corridas solapadas y los artistas ya resueltos no generan peticiones.
 */
@HiltViewModel
class ArtistsViewModel @Inject constructor(
    audioRepository: AudioRepository,
    private val artistImageRepository: ArtistImageRepository
) : ViewModel() {

    val artists: StateFlow<List<String>> = audioRepository.songs
        .map { songs -> songs.map { it.artist }.distinct().sorted() }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val artistImages: StateFlow<Map<String, File>> = artistImageRepository.artistImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyMap())

    init {
        viewModelScope.launch {
            artists.collectLatest { names -> artistImageRepository.enrichArtists(names) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
