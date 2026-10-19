package com.twedmusic.app.core.locale

import com.twedmusic.app.core.datastore.AppPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Punto de acceso a Hilt para [LocaleManager].
 *
 * `attachBaseContext` de una Activity se ejecuta ANTES de que Hilt
 * inyecte los campos `@Inject`, por lo que ahí no se puede usar
 * inyección normal. Este EntryPoint permite pedirle a Hilt el
 * [AppPreferences] singleton directamente. Es el único EntryPoint de
 * la app.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LocaleEntryPoint {
    fun appPreferences(): AppPreferences
}
