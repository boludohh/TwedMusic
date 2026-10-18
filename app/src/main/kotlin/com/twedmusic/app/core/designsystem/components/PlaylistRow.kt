package com.twedmusic.app.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twedmusic.app.R
import com.twedmusic.app.core.model.Playlist
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor
/** Altura de cada fila de playlist en dp. */
private val PlaylistRowHeight = 64.dp

/** Tamaño del icono de playlist en dp. */
private val PlaylistIconSize = 40.dp

/**
 * Fila de una playlist individual (cascarón).
 *
 * Muestra el nombre de la playlist y su contador de canciones. La
 * geometría es idéntica a TrackRow (64dp de alto, paddings
 * consistentes) para mantener coherencia visual entre pestañas.
 *
 * **Visibilidad internal**: permite reutilización desde SearchScreen
 * para mostrar resultados de búsqueda de playlists sin duplicar código.
 *
 * ## Próximamente (fase de UI pulida)
 * - Icono personalizado (mosaico de carátulas de las primeras
 *   canciones, o icono temático).
 * - Botón de acción more_vert para editar/eliminar.
 * - Navegación a la pantalla de detalle de la playlist.
 */
@Composable
internal fun PlaylistRow(
    playlist: Playlist,
    modifier: Modifier = Modifier
) {
    val titleColor = LocalListItemTitleColor.current
    val subtitleColor = LocalListItemSubtitleColor.current
    val placeholderBg = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(PlaylistRowHeight)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder del icono de playlist (cascarón; en fase de UI
        // será un mosaico de carátulas o un icono temático)
        Box(
            modifier = Modifier
                .size(PlaylistIconSize)
                .background(placeholderBg, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_playlist_play),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = playlist.name,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.main_songs_count, playlist.songCount),
                style = MaterialTheme.typography.bodyMedium,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
