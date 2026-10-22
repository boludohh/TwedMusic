package com.twedmusic.app.feature.tabs.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import com.twedmusic.app.R
import com.twedmusic.app.core.database.TotalStats
import com.twedmusic.app.core.designsystem.theme.LocalCardL2Color
import com.twedmusic.app.core.designsystem.theme.LocalCoverPlaceholderIconColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor
import com.twedmusic.app.core.model.Song
import com.twedmusic.app.feature.tabs.home.components.HomeArtistCard
import com.twedmusic.app.feature.tabs.home.components.HomeRecentTrackRow
import com.twedmusic.app.feature.tabs.home.components.HomeSectionHeader
import com.twedmusic.app.feature.tabs.home.components.StatsPanel
import java.io.File

/**
 * Pantalla de inicio (boceto definitivo).
 *
 * 1. Título "Inicio"
 * 2. Tu actividad (3 stats con iconos)
 * 3. Reproducidas recientemente (3 filas)
 * 4. Tus artistas (5 círculos, más recientes)
 * 5. Álbumes (4 cards, 2×2)
 *
 * Sin playlists, sin atribución Deezer, sin saludo / shuffle / resume.
 */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val backgroundColor = MaterialTheme.colorScheme.background
    val titleColor = LocalListItemTitleColor.current

    val formattedTime = formatPlayedTime(uiState.totalStats.totalPlayedMs)
    val playsLabel = stringResource(R.string.home_total_plays)
    val completedLabel = stringResource(R.string.home_completed_plays)
    val timeLabel = stringResource(R.string.home_hours_listened)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            item(key = "title") {
                Text(
                    text = stringResource(R.string.home_screen_placeholder),
                    style = MaterialTheme.typography.headlineMedium,
                    color = titleColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Tu actividad: siempre visible (0 si no hay datos)
            item(key = "stats") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.home_your_activity),
                        style = MaterialTheme.typography.titleMedium,
                        color = titleColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    StatsPanel(
                        stats = uiState.totalStats,
                        formattedTime = formattedTime,
                        totalPlaysLabel = playsLabel,
                        completedLabel = completedLabel,
                        timeLabel = timeLabel
                    )
                }
            }

            if (uiState.recentlyPlayed.isNotEmpty()) {
                item(key = "recent") {
                    RecentlyPlayedSection(
                        songs = uiState.recentlyPlayed,
                        coverFileOf = viewModel::coverFile
                    )
                }
            }

            if (uiState.recentArtists.isNotEmpty()) {
                item(key = "artists") {
                    ArtistsSection(
                        artists = uiState.recentArtists,
                        artistImageFileOf = viewModel::artistImageFile
                    )
                }
            }

            if (uiState.recentAlbums.isNotEmpty()) {
                item(key = "albums") {
                    AlbumsGridSection(
                        albums = uiState.recentAlbums,
                        coverFileOf = viewModel::coverFile
                    )
                }
            }

            if (uiState.songCount == 0 && uiState.totalStats.totalPlays == 0) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.home_no_stats),
                        style = MaterialTheme.typography.bodyMedium,
                        color = LocalListItemSubtitleColor.current,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    songs: List<Song>,
    coverFileOf: (String) -> File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_recently_played),
            showSeeAll = false
        )
        songs.forEach { song ->
            HomeRecentTrackRow(
                title = song.title,
                artist = song.artist,
                coverFile = song.path.takeIf { it.isNotBlank() }?.let { coverFileOf(it) },
                onClick = { /* TODO: play */ },
                onMoreClick = { /* TODO: menú */ }
            )
        }
    }
}

@Composable
private fun ArtistsSection(
    artists: List<String>,
    artistImageFileOf: (String) -> File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_your_artists),
            showSeeAll = true,
            onSeeAllClick = { /* TODO: Artistas */ }
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(
                items = artists,
                key = { "artist-$it" }
            ) { name ->
                HomeArtistCard(
                    artistName = name,
                    imageFile = artistImageFileOf(name),
                    onClick = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
private fun AlbumsGridSection(
    albums: List<HomeAlbumData>,
    coverFileOf: (String) -> File?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeSectionHeader(
            title = stringResource(R.string.home_albums),
            showSeeAll = true,
            onSeeAllClick = { /* TODO: Álbumes */ }
        )
        // 2 columnas × 2 filas (máx. 4)
        val rows = albums.chunked(2)
        rows.forEach { rowAlbums ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowAlbums.forEach { album ->
                    HomeAlbumGridCard(
                        title = album.title,
                        artist = album.artist,
                        coverFile = album.coverPath?.let { coverFileOf(it) },
                        modifier = Modifier.weight(1f),
                        onClick = { /* TODO */ }
                    )
                }
                if (rowAlbums.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            if (rowAlbums !== rows.last()) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/** Card de álbum a ancho de columna (estilo grid AlbumsScreen). */
@Composable
private fun HomeAlbumGridCard(
    title: String,
    artist: String,
    coverFile: File?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val surfaceL2 = LocalCardL2Color.current
    val titleColor = LocalListItemTitleColor.current
    val subtitleColor = LocalListItemSubtitleColor.current
    val placeholderIcon = LocalCoverPlaceholderIconColor.current
    val density = LocalDensity.current

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceL2),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_album_filled),
                contentDescription = null,
                tint = placeholderIcon,
                modifier = Modifier.size(40.dp)
            )
            if (coverFile != null) {
                val sidePx = with(density) { 160.dp.roundToPx() }
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverFile)
                        .size(Size(sidePx, sidePx))
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = artist,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            ),
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun formatPlayedTime(playedMs: Long): String {
    val totalMinutes = playedMs / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) {
        stringResource(R.string.home_hours_minutes_format, hours.toInt(), minutes.toInt())
    } else {
        stringResource(R.string.home_only_minutes_format, minutes.toInt())
    }
}