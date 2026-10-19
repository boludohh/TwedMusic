package com.twedmusic.app.feature.onboarding

import androidx.lifecycle.ViewModel
import com.twedmusic.app.core.datastore.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel de [SetupScreen]: acceso a las preferencias de idioma.
 *
 * Sustituye al `AppPreferences(context)` que la pantalla creaba por su
 * cuenta: ahora se usa el singleton de Hilt.
 */
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    /**
     * Idioma guardado. Lectura bloqueante: solo se usa al componer la
     * pantalla (y tras `recreate()` de la Activity al cambiar idioma),
     * donde se necesita el valor antes de dibujar.
     */
    fun currentLanguage(): String = preferences.getLanguageBlocking()

    /** Guarda el idioma elegido por el usuario. */
    suspend fun setLanguage(code: String) = preferences.setLanguage(code)
}
