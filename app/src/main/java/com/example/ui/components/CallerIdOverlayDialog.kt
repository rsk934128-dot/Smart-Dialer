package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.SpamRiskLevel
import com.example.ui.theme.CallGreen
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.SpamRed
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ActiveCallState

@Composable
fun CallerIdOverlayDialog(
    callState: ActiveCallState,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onBlockAndReport: () -> Unit,
    onQuickSms: (String) -> Unit
) {
    var showSmsOptions by remember { mutableStateOf(false) }

    // Pulsing animation for incoming call ring
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Dialog(
        onDismissRequest = { /* Don't dismiss by outside touch */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (callState.isSpam) {
                            listOf(Color(0xFF450A0A), Navy950, Navy950)
                        } else {
                            listOf(Color(0xFF0F2B48), Navy950, Navy950)
                        }
                    )
                )
                .padding(24.dp)
                .testTag("caller_id_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header / Caller ID badge
                Surface(
                    shape = RoundedCornerShape(30.dp),
                    color = if (callState.isSpam) {
                        SpamRed.copy(alpha = 0.25f)
                    } else {
                        VerifiedTeal.copy(alpha = 0.2f)
                    },
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (callState.isSpam) SpamRed else VerifiedTeal
                    ),
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (callState.isSpam) Icons.Default.Warning else Icons.Default.Shield,
                            contentDescription = "Security Status",
                            tint = if (callState.isSpam) SpamRed else VerifiedTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (callState.isSpam) {
                                "SPAM WARNING • SMART CALLER ID"
                            } else {
                                "VERIFIED CALLER • SMART CALLER ID"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (callState.isSpam) SpamRed else VerifiedTeal,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Pulsing Center Avatar
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .scale(pulseScale),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer glow ring
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .background(
                                if (callState.isSpam) {
                                    SpamRed.copy(alpha = 0.2f)
                                } else {
                                    VerifiedTeal.copy(alpha = 0.2f)
                                }
                            )
                    )
                    // Inner avatar
                    Box(
                        modifier = Modifier
                            .size(105.dp)
                            .clip(CircleShape)
                            .background(if (callState.isSpam) SpamRed else MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        if (callState.isSpam) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Spam",
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        } else {
                            Text(
                                text = callState.callerName.take(1).uppercase(),
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Caller Name & Details
                Text(
                    text = callState.callerName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = callState.number,
                    fontSize = 18.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Telecom Operator & Location tag
                if (callState.carrier.isNotBlank() || callState.location.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Navy900.copy(alpha = 0.85f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "${callState.carrier} • ${callState.location}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }

                // If Spam, show community report stats
                if (callState.isSpam) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SpamRed.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SpamRed.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Category: ${callState.spamCategory}",
                                fontWeight = FontWeight.Bold,
                                color = SpamRed,
                                fontSize = 14.sp
                            )
                            if (callState.spamReportsCount > 0) {
                                Text(
                                    text = "Reported by ${callState.spamReportsCount} users as fraud/telemarketer",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Quick SMS template selector dialog/row
                if (showSmsOptions) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Navy900,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Send Quick Response:",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            listOf(
                                "Can't talk now. What's up?",
                                "I'll call you right back.",
                                "In a meeting, please text me.",
                                "Wrong number, stop calling."
                            ).forEach { msg ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    onClick = {
                                        onQuickSms(msg)
                                        showSmsOptions = false
                                        onDecline()
                                    }
                                ) {
                                    Text(
                                        text = msg,
                                        fontSize = 12.sp,
                                        color = Color(0xFFE2E8F0),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons Bar
                Row(
                    modifier = Modifier.fillMaxWidth(0.95f),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline Button (Red)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onDecline,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = SpamRed,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("caller_id_decline")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Decline Call",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Decline", fontSize = 12.sp, color = Color.White)
                    }

                    // Quick SMS Button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = { showSmsOptions = !showSmsOptions },
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Navy900,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("caller_id_sms")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Message,
                                contentDescription = "Quick SMS",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Message", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }

                    // Block & Report Button (If Spam)
                    if (callState.isSpam) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FilledIconButton(
                                onClick = onBlockAndReport,
                                shape = CircleShape,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFF7F1D1D),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .size(54.dp)
                                    .testTag("caller_id_block_report")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Block,
                                    contentDescription = "Block and Report",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Block & Report", fontSize = 11.sp, color = SpamRed)
                        }
                    }

                    // Accept Button (Green)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = onAccept,
                            shape = CircleShape,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = CallGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(68.dp)
                                .testTag("caller_id_accept")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Answer Call",
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Answer", fontSize = 12.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
