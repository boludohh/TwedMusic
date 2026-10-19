package com.twedmusic.app.di

import android.content.Context
import com.twedmusic.app.core.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Módulo de Hilt para lo que no se puede anotar con `@Inject`:
 * la base de datos Room (se construye con un builder).
 *
 * El resto de clases (repositorios, preferencias, PlaybackController)
 * son `@Singleton` con `@Inject constructor` y no necesitan módulo.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.build(context)
}
