package com.twedmusic.app.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.twedmusic.app.core.data.repository.AudioRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de [LoadingScreen]: lanza el escaneo inicial de la
 * biblioteca y expone cuándo terminó.
 *
 * El escaneo vive en `viewModelScope`, por lo que sobrevive a una
 * rotación de pantalla en lugar de reiniciarse. `syncIfNeeded()`
 * ejecuta el escaneo completo solo si Room está vacío.
 */
@HiltViewModel
class LoadingViewModel @Inject constructor(
    audioRepository: AudioRepository
) : ViewModel() {

    private val _libraryLoaded = MutableStateFlow(false)

    /** `true` cuando la biblioteca terminó de cargarse. */
    val libraryLoaded: StateFlow<Boolean> = _libraryLoaded.asStateFlow()

    init {
        viewModelScope.launch {
            audioRepository.syncIfNeeded().collect {
                // El flow emite lotes mientras se insertan; al completarse
                // (sin más emisiones) se marca la biblioteca como cargada.
            }
            _libraryLoaded.value = true
        }
    }
}
