package com.twedmusic.app.feature.tabs.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twedmusic.app.R
import com.twedmusic.app.core.model.Playlist
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor
import com.twedmusic.app.core.designsystem.theme.LocalScreenTitleColor
import com.twedmusic.app.core.designsystem.theme.LocalTracksCountTextColor
import com.twedmusic.app.core.designsystem.theme.screenTitle
import com.twedmusic.app.core.designsystem.components.PlaylistRow
import androidx.lifecycle.viewmodel.compose.viewModel
/**
 * Altura del desvanecido superior en dp.
 * El fade ocupa esta distancia desde el tope del LazyColumn hacia abajo,
 * dibujando un gradiente vertical del color de fondo principal (opaco en
 * el tope) a transparente. El extremo opaco oculta la línea de recorte
 * dura del viewport, y las filas que suben hacia el panel fijo se
 * desvanecen suavemente dentro de esta franja.
 */
private val TopFadeHeight = 64.dp

/**
 * Distancia de scroll (en dp) necesaria para que el fade alcance su
 * alfa máximo (1.0). El fade se enciende progresivamente desde 0 hasta
 * este umbral, evitando un encendido brusco de un frame a otro.
 */
private val ScrollFadeThreshold = 48.dp



/**
 * Pantalla de listas de reproducción de TwedMusic.
 *
 * Muestra las playlists creadas por el usuario ordenadas por fecha de
 * creación descendente (más recientes primero), con el nombre y el
 * contador de canciones en cada fila. El header (título + conteo)
 * forma parte del scroll junto con las filas.
 *
 * **Cascarón conectado**: esta pantalla ya consume el Flow reactivo
 * de [PlaylistRepository.playlists] mediante [collectAsState]. El
 * pulido visual (iconos personalizados, botón "Crear playlist",
 * diálogos de creación/edición, pantalla de detalle de playlist con
 * canciones) se implementará en la fase de UI posterior.
 *
 * **Efecto fade superior condicional**: idéntico al de TracksScreen y
 * ArtistsScreen; solo se dibuja cuando hay scroll activo.
 *
 * **Estado vacío**: si el usuario no ha creado ninguna playlist, se
 * muestra el string [R.string.playlist_empty_state] centrado.
 *
 * **Geometría óptica idéntica a TracksScreen y ArtistsScreen**: los
 * paddings del header (start=24dp, top=8dp, end=15dp) y las separaciones
 * (8dp título/conteo, 24dp conteo/primera fila) coinciden con las
 * otras pestañas, garantizando alineación visual consistente.
 *
 * @param modifier Modificador de Compose opcional.
 */
@Composable
fun PlaylistScreen(
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = viewModel()
) {
    val playlists by viewModel.playlists.collectAsState()
    val screenTitleColor = LocalScreenTitleColor.current
    val tracksCountTextColor = LocalTracksCountTextColor.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    val thresholdPx = with(density) { ScrollFadeThreshold.toPx() }

    val fadeAlpha by remember {
        derivedStateOf {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset

            if (firstVisibleItemIndex == 0 && firstVisibleItemScrollOffset == 0) {
                0f
            } else {
                val scrollDistance = if (firstVisibleItemIndex > 0) {
                    thresholdPx
                } else {
                    firstVisibleItemScrollOffset.toFloat()
                }
                (scrollDistance / thresholdPx).coerceIn(0f, 1f)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        if (playlists.isEmpty()) {
            // Estado vacío: título + mensaje centrado
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
                // Header fijo (no scrolleable porque no hay contenido)
                Text(
                    text = stringResource(R.string.playlist_screen_placeholder),
                    style = screenTitle(),
                    color = screenTitleColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = stringResource(R.string.main_playlists_count, 0),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tracksCountTextColor,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(top = 8.dp)
                )
                // Mensaje de estado vacío centrado en el espacio restante
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.playlist_empty_state),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tracksCountTextColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Hay playlists: LazyColumn con fade condicional
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithContent {
                        drawContent()
                        if (fadeAlpha > 0f) {
                            val fadeHeightPx = TopFadeHeight.toPx()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        backgroundColor.copy(alpha = fadeAlpha),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = fadeHeightPx
                                ),
                                topLeft = Offset.Zero,
                                size = Size(size.width, fadeHeightPx)
                            )
                        }
                    }
            ) {
                item(key = "playlists_header") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 8.dp, end = 15.dp, bottom = 24.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.playlist_screen_placeholder),
                            style = screenTitle(),
                            color = screenTitleColor,
                            textAlign = TextAlign.Start
                        )
                        Text(
                            text = stringResource(R.string.main_playlists_count, playlists.size),
                            style = MaterialTheme.typography.bodyLarge,
                            color = tracksCountTextColor,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                items(
                    items = playlists,
                    key = { it.id }
                ) { playlist ->
                    PlaylistRow(playlist = playlist)
                }
            }
        }
    }
}
