# TwedMusic

Reproductor de música local para Android, rápido y con soporte para formatos de alta calidad. *(Antes llamado OpenPlayer.)*

> 🚧 **Estado: en desarrollo activo.** La app ya funciona, pero algunas funciones y el diseño pueden cambiar.

## Características

- **Biblioteca local**: escanea la música del dispositivo y se mantiene actualizada cuando cambian los archivos.
- **Cinco secciones**: Inicio, Pistas, Álbumes, Artistas y Listas de reproducción.
- **Búsqueda** de canciones desde cualquier sección.
- **Reproducción en segundo plano** con notificación multimedia y controles del sistema.
- **Reproducción aleatoria** de toda la biblioteca.
- **Historial y estadísticas** de reproducción, con acceso rápido a lo escuchado recientemente.
- **Portadas** leídas de las etiquetas de los archivos e **imágenes de artistas** obtenidas en línea.
- **Motor de audio BASS** con soporte para FLAC, ALAC, AAC, Opus, APE, WavPack y DSD, además de los formatos habituales.
- **Tema claro y oscuro**.
- **Idiomas**: español, inglés y árabe (con soporte de escritura de derecha a izquierda).
- **Tipografía** IBM Plex Sans.

## Requisitos

- Android 8.1 (API 27) o superior.
- Arquitecturas: `arm64-v8a` y `armeabi-v7a`.
- Permisos: acceso al audio del dispositivo, notificaciones e internet (para las imágenes de artistas).

## Tecnologías

- Kotlin y Jetpack Compose con Material 3
- AndroidX Media3 (sesión y notificación multimedia)
- [BASS](https://www.un4seen.com/) y sus plugins (motor de audio nativo)
- Room 3 y DataStore
- Coil 3
- KTagLib (lectura de etiquetas)

### Librerías BASS

BASS pertenece a un4seen developments y tiene su propia licencia; revisa sus condiciones en [un4seen.com](https://www.un4seen.com/).

## Licencia

Distribuido bajo la licencia **GPL-3.0**. Consulta el archivo [LICENSE](LICENSE).