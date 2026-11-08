package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallType
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.CallGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun AnalyticsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val callLogs by viewModel.callLogs.collectAsState()
    val blockedNumbers by viewModel.blockedNumbers.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()

    val totalCalls = callLogs.size
    val incomingCalls = callLogs.count { it.type == CallType.INCOMING }
    val outgoingCalls = callLogs.count { it.type == CallType.OUTGOING }
    val missedCalls = callLogs.count { it.type == CallType.MISSED }
    val blockedCalls = callLogs.count { it.type == CallType.BLOCKED || it.isSpam }

    val totalDurationSeconds = callLogs.sumOf { it.durationSeconds }
    val talkHours = totalDurationSeconds / 3600
    val talkMinutes = (totalDurationSeconds % 3600) / 60

    // Top Frequent Callers
    val topCallers = remember(callLogs) {
        callLogs
            .groupBy { if (it.cachedName.isNotBlank()) it.cachedName else it.number }
            .map { (nameOrNum, logs) ->
                val totalDuration = logs.sumOf { it.durationSeconds }
                val number = logs.first().number
                Triple(nameOrNum, logs.size, totalDuration)
            }
            .sortedByDescending { it.second }
            .take(5)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("analytics_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(AccentBlue.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = "Analytics",
                            tint = AccentBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "কল অ্যানালিটিক্স ও ইনসাইটস" else "Call Telemetry & Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language == AppLanguage.BENGALI) "আপনার কল প্যাটার্ন ও সুরক্ষার সারসংক্ষেপ" else "Comprehensive statistics of your call patterns",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 4 Grid KPI Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (language == AppLanguage.BENGALI) "মোট কল" else "Total Calls",
                        value = totalCalls.toString(),
                        subtitle = "Logs recorded",
                        icon = Icons.Default.Call,
                        tint = AccentBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (language == AppLanguage.BENGALI) "মোট কথা বলার সময়" else "Total Talk Time",
                        value = "${talkHours}h ${talkMinutes}m",
                        subtitle = "${totalDurationSeconds}s total",
                        icon = Icons.Default.Timer,
                        tint = CallGreen,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = if (language == AppLanguage.BENGALI) "মিসড কল" else "Missed Calls",
                        value = missedCalls.toString(),
                        subtitle = "Unanswered calls",
                        icon = Icons.Default.CallMissed,
                        tint = WarningAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = if (language == AppLanguage.BENGALI) "স্প্যাম প্রতিহত" else "Spam Prevented",
                        value = blockedCalls.toString(),
                        subtitle = "${blockedNumbers.size} blocked rules",
                        icon = Icons.Default.Shield,
                        tint = SpamRed,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Call Ratio Progress Bar
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "ইনকামিং বনাম আউটগোয়িং অনুপাত" else "Call Distribution Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    val incomingRatio = if (totalCalls > 0) incomingCalls.toFloat() / totalCalls else 0.5f
                    LinearProgressIndicator(
                        progress = { incomingRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = CallGreen,
                        trackColor = AccentBlue
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CallGreen))
                            Text("Incoming: $incomingCalls (${if (totalCalls > 0) (incomingRatio * 100).toInt() else 0}%)", fontSize = 12.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AccentBlue))
                            Text("Outgoing: $outgoingCalls (${if (totalCalls > 0) ((1f - incomingRatio) * 100).toInt() else 0}%)", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Top Frequent Callers Section
        item {
            Text(
                text = if (language == AppLanguage.BENGALI) "সর্বাধিক কথা বলা কন্টাক্ট" else "TOP FREQUENT CALLERS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }

        items(topCallers) { (name, count, duration) ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val avatarBg = AvatarColors[Math.abs(name.hashCode()) % AvatarColors.size]
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name.take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            text = "$count calls • ${duration / 60}m ${duration % 60}s talk time",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(18.dp))
            }
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
