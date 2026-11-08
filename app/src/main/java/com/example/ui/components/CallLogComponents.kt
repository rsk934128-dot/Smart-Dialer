package com.example.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallMissed
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.CallGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.RecentsFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CallLogCard(
    callLog: CallLogEntity,
    onCallClick: () -> Unit,
    onDetailsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val typeColor = when (callLog.type) {
        CallType.INCOMING -> CallGreen
        CallType.OUTGOING -> AccentBlue
        CallType.MISSED -> SpamRed
        CallType.BLOCKED -> SpamRed
        CallType.REJECTED -> WarningAmber
    }

    val typeIcon = when (callLog.type) {
        CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived
        CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade
        CallType.MISSED -> Icons.AutoMirrored.Filled.CallMissed
        CallType.BLOCKED -> Icons.Default.Block
        CallType.REJECTED -> Icons.Default.Shield
    }

    val displayName = if (callLog.cachedName.isNotBlank()) callLog.cachedName else callLog.number
    val initials = if (callLog.cachedName.isNotBlank()) {
        callLog.cachedName.take(1).uppercase()
    } else {
        "#"
    }
    val avatarBg = AvatarColors[Math.abs(displayName.hashCode()) % AvatarColors.size]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onDetailsClick)
            .testTag("call_log_item_${callLog.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (callLog.isSpam) {
                SpamRed.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (callLog.isSpam) SpamRed else avatarBg),
                contentAlignment = Alignment.Center
            ) {
                if (callLog.isSpam) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Spam Caller",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (callLog.type == CallType.MISSED || callLog.isSpam) {
                            SpamRed
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (callLog.isSpam) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SpamRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (callLog.spamCategory.isNotEmpty()) callLog.spamCategory else "SPAM",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SpamRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = callLog.type.name,
                        tint = typeColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = formatTimestamp(callLog.timestamp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (callLog.durationSeconds > 0) {
                        Text(
                            text = "• ${formatDuration(callLog.durationSeconds)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (callLog.carrier.isNotBlank()) {
                        Text(
                            text = "• ${callLog.carrier}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Call Action
            IconButton(
                onClick = onCallClick,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CallGreen.copy(alpha = 0.12f))
                    .testTag("call_log_call_${callLog.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = CallGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun FilterChipRow(
    selectedFilter: RecentsFilter,
    onFilterSelected: (RecentsFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        RecentsFilter.entries.forEach { filter ->
            val label = when (filter) {
                RecentsFilter.ALL -> "All"
                RecentsFilter.MISSED -> "Missed"
                RecentsFilter.INCOMING -> "Incoming"
                RecentsFilter.OUTGOING -> "Outgoing"
                RecentsFilter.BLOCKED -> "Blocked/Spam"
            }
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(text = label, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                ),
                modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
            )
        }
    }
}

private fun formatTimestamp(timeMs: Long): String {
    val diff = System.currentTimeMillis() - timeMs
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 48 * 60 * 60 * 1000 -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timeMs))
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return if (m > 0) "${m}m ${s}s" else "${s}s"
}
