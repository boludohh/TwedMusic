package com.twedmusic.app.feature.tabs.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.twedmusic.app.R
import com.twedmusic.app.core.designsystem.theme.LocalListItemSubtitleColor
import com.twedmusic.app.core.designsystem.theme.LocalListItemTitleColor

@Composable
fun HomeSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    showSeeAll: Boolean = true,
    onSeeAllClick: (() -> Unit)? = null
) {
    val titleColor = LocalListItemTitleColor.current
    val subtitleColor = LocalListItemSubtitleColor.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = titleColor
        )

        if (showSeeAll && onSeeAllClick != null) {
            Row(
                modifier = Modifier.clickable(onClick = onSeeAllClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.home_see_all),
                    style = MaterialTheme.typography.labelMedium,
                    color = subtitleColor,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_right),
                    contentDescription = null,
                    tint = subtitleColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}