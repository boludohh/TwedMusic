package com.twedmusic.app.core.model

import com.twedmusic.app.core.database.PlaylistEntity
/**
 * Modelo de dominio de una playlist.
 *
 * Une los datos persistidos en [PlaylistEntity] con la lista de
 * [Song] resuelta desde [com.twedmusic.app.core.database.SongDao]
 * (mediante las relaciones de "playlist_songs").
 *
 * El mapeo de [PlaylistEntity] a este modelo lo realiza
 * [com.twedmusic.app.core.data.repository.PlaylistRepository], manteniendo
 * el DAO libre de lógica de dominio.
 *
 * @param songCount Contador de canciones en la playlist. No depende
 *                  de que la lista [songs] esté cargada (útil para la
 *                  UI cuando se muestran contadores sin cargar todas
 *                  las canciones de cada playlist).
 */
data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val songCount: Int,
    val songs: List<Song> = emptyList()
) {
    /** Mapea una entidad de base de datos al modelo de dominio. */
    companion object {
        fun fromEntity(entity: PlaylistEntity, songCount: Int): Playlist = Playlist(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            songCount = songCount
        )
    }
}