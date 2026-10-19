package com.twedmusic.app.feature.tabs.albums

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.twedmusic.app.R
import com.twedmusic.app.core.model.Album
import com.twedmusic.app.core.model.Song
import com.twedmusic.app.feature.tabs.albums.components.AlbumCard
import com.twedmusic.app.core.designsystem.theme.LocalScreenTitleColor
import com.twedmusic.app.core.designsystem.theme.LocalTracksCountTextColor
import com.twedmusic.app.core.designsystem.theme.screenTitle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
/**
 * Altura del desvanecido superior en dp.
 * El fade ocupa esta distancia desde el tope del LazyVerticalGrid hacia abajo,
 * dibujando un gradiente vertical del color de fondo principal (opaco en
 * el tope) a transparente. El extremo opaco oculta la línea de recorte
 * dura del viewport, y las tarjetas que suben hacia el panel fijo se
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
 * Padding inferior del grid de álbumes. Garantiza que al hacer scroll
 * al final, la última fila de cards tenga al menos esta separación
 * respecto a la barra de navegación inferior, evitando que las cards
 * queden dibujadas al filo del panel.
 */
private val GridBottomPadding = 8.dp

/**
 * Pantalla de álbumes de TwedMusic.
 *
 * Gestiona internamente la navegación entre dos estados:
 * - **Lista de álbumes** (default): grid de 2 columnas con todas las
 *   tarjetas.
 * - **Detalle de álbum**: pantalla completa con hero + lista de pistas.
 *
 * La transición entre ambos estados usa `AnimatedContent` con fade
 * de 300ms, igual que la transición entre pestañas de MainScreen.
 *
 * ## Flujo de navegación
 * 1. Usuario toca un AlbumCard → `selectedAlbum` se establece.
 * 2. AnimatedContent muestra AlbumDetailScreen con fade in.
 * 3. Usuario toca "Volver" → `selectedAlbum` vuelve a null.
 * 4. AnimatedContent muestra la lista con fade in.
 *
 * El estado de scroll de la lista se preserva gracias a que el
 * `LazyVerticalGrid` se mantiene vivo en memoria (no se destruye
 * al navegar al detalle, solo se oculta bajo AnimatedContent).
 *
 * @param modifier Modificador de Compose opcional.
 * @param viewModel ViewModel de la pestaña (lista agrupada y álbum seleccionado).
 */
@Composable
fun AlbumsScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumsViewModel = viewModel()
) {
    // La lista ya viene agrupada y ordenada por el ViewModel.
    val albums by viewModel.albums.collectAsState()

    // Estado de navegación interna: null = lista, Album = detalle.
    // Vive en el ViewModel, por lo que sobrevive a una rotación.
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()

    AnimatedContent(
        targetState = selectedAlbum,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) togetherWith
                    fadeOut(animationSpec = tween(300))
        },
        label = "albumsNavigation",
        modifier = modifier.fillMaxSize()
    ) { album ->
        if (album == null) {
            AlbumsListContent(
                albums = albums,
                coverFileOf = viewModel::coverFile,
                onAlbumClick = viewModel::selectAlbum
            )
        } else {
            AlbumDetailScreen(
                album = album,
                onBack = viewModel::clearSelection
            )
        }
    }
}

/**
 * Contenido de la lista de álbumes: header + grid de 2 columnas.
 */
@Composable
private fun AlbumsListContent(
    albums: List<Album>,
    coverFileOf: (String) -> File?,
    onAlbumClick: (Album) -> Unit
) {
    val screenTitleColor = LocalScreenTitleColor.current
    val tracksCountTextColor = LocalTracksCountTextColor.current
    val backgroundColor = MaterialTheme.colorScheme.background
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current

    val thresholdPx = with(density) { ScrollFadeThreshold.toPx() }

    val fadeAlpha by remember {
        derivedStateOf {
            val firstVisibleItemIndex = gridState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = gridState.firstVisibleItemScrollOffset

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
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
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
                },
            contentPadding = PaddingValues(
                start = 18.dp,
                end = 18.dp,
                bottom = GridBottomPadding
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(
                key = "albums_header",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 8.dp, end = 3.dp, bottom = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.albums_screen_placeholder),
                        style = screenTitle(),
                        color = screenTitleColor,
                        textAlign = TextAlign.Start
                    )

                    Text(
                        text = stringResource(R.string.main_albums_count, albums.size),
                        style = MaterialTheme.typography.bodyLarge,
                        color = tracksCountTextColor,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            items(
                items = albums,
                key = { it.key }
            ) { album ->
                val coverFile = album.coverPath?.let { coverFileOf(it) }
                AlbumCard(
                    title = album.title,
                    artist = album.artist,
                    year = album.year,
                    trackCount = album.trackCount,
                    isNew = album.isNew,
                    coverFile = coverFile,
                    onClick = { onAlbumClick(album) }
                )
            }
        }
    }
}

