package com.twedmusic.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application de TwedMusic.
 *
 * Punto de entrada de Hilt (`@HiltAndroidApp`): crea el grafo de
 * dependencias de toda la aplicación. La base de datos, las
 * preferencias, los repositorios y el `PlaybackController` son
 * singletons gestionados por Hilt (ver `di/` y las anotaciones
 * `@Singleton` / `@Inject` de cada clase), por lo que ya no hay que
 * pedirlos a esta clase: se inyectan por constructor o por campo.
 *
 * Esta clase no guarda estado.
 */
@HiltAndroidApp
class TwedMusicApp : Application()
