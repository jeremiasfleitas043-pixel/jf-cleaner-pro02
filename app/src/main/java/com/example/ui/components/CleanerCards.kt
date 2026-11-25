package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shizuku.ShizukuConnectionState
import com.example.shizuku.ShizukuStatus
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.CardDarkGray
import com.example.ui.theme.CardElevatedGray
import com.example.ui.theme.CyberMint
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

@Composable
fun FuturisticFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    accentColor: Color = TextPrimary,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = CardDarkGray,
        border = BorderStroke(1.dp, CardBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
                trailingContent?.invoke()
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NeonProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    heightDp: Int = 5
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(CircleShape)
            .background(CardElevatedGray)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(heightDp.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(EmeraldDeep, EmeraldPrimary, CyberMint)
                        )
                    )
            )
        }
    }
}

@Composable
fun ShizukuStatusBadge(
    status: ShizukuStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (dotColor, label) = when (status.state) {
        ShizukuConnectionState.READY -> NeonEmerald to "Shizuku"
        ShizukuConnectionState.PERMISSION_REQUIRED -> WarningAmber to "Authorize"
        ShizukuConnectionState.NOT_RUNNING -> TextSecondary to "Shizuku Off"
        ShizukuConnectionState.NOT_INSTALLED -> TextMuted to "Standard"
    }

    Surface(
        modifier = modifier
            .testTag("shizuku_status_badge")
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = CardDarkGray,
        border = BorderStroke(1.dp, if (status.isReady) EmeraldDeep else CardBorderSubtle)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (status.isReady) CyberMint else TextSecondary,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
