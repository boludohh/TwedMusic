package com.twedmusic.app.feature.tabs.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twedmusic.app.R
import com.twedmusic.app.core.database.TotalStats
import com.twedmusic.app.core.designsystem.theme.LocalCardL2Color
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor

/**
 * Panel único "Tu actividad": tres stats en una sola superficie,
 * iconos sobre círculo (card L2) con padding limpio respecto al borde.
 */
@Composable
fun StatsPanel(
    stats: TotalStats,
    formattedTime: String,
    totalPlaysLabel: String,
    completedLabel: String,
    timeLabel: String,
    modifier: Modifier = Modifier
) {
    val panelColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(panelColor)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            iconRes = R.drawable.ic_play_arrow_filled,
            value = formatStatNumber(stats.totalPlays),
            label = totalPlaysLabel,
            modifier = Modifier.weight(1f)
        )
        StatItem(
            iconRes = R.drawable.ic_check,
            value = formatStatNumber(stats.totalCompleted),
            label = completedLabel,
            modifier = Modifier.weight(1f)
        )
        StatItem(
            iconRes = R.drawable.ic_timer_filled,
            value = formattedTime,
            label = timeLabel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatItem(
    iconRes: Int,
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val titleColor = LocalListItemTitleColor.current
    val subtitleColor = LocalListItemSubtitleColor.current
    val circleColor = LocalCardL2Color.current

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(circleColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = titleColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = titleColor,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Light
            ),
            color = subtitleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatStatNumber(value: Int): String {
    if (value < 1000) return value.toString()
    val str = value.toString()
    val result = StringBuilder()
    for (i in str.indices) {
        if (i > 0 && (str.length - i) % 3 == 0) result.append('.')
        result.append(str[i])
    }
    return result.toString()
}