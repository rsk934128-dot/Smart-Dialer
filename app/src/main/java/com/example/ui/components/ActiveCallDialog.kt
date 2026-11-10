package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CallGreen
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.SpamRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.ActiveCallState
import com.example.ui.viewmodel.AppLanguage

private fun toBengaliDigits(input: String): String {
    val en = "0123456789"
    val bn = "০১২৩৪৫৬৭৮৯"
    var res = input
    for (i in en.indices) {
        res = res.replace(en[i], bn[i])
    }
    return res
}

@Composable
fun ActiveCallDialog(
    callState: ActiveCallState,
    language: AppLanguage = AppLanguage.ENGLISH,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit
) {
    var showInCallKeypad by remember { mutableStateOf(false) }
    var inCallDtmfText by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val formattedDuration = remember(callState.durationSeconds, language) {
        val min = callState.durationSeconds / 60
        val sec = callState.durationSeconds % 60
        val standard = String.format("%02d:%02d", min, sec)
        if (language == AppLanguage.BENGALI) toBengaliDigits(standard) else standard
    }

    BackHandler(enabled = true) {
        // Prevent accidental dismissal via back button during active call; user must press End Call
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
                .background(Navy950)
                .padding(24.dp)
                .testTag("active_call_dialog"),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top status bar (Call state + Counting Seconds Timer)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when {
                            callState.isOnHold -> WarningAmber.copy(alpha = 0.2f)
                            callState.isCallAnswered -> CallGreen.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            // Pulsing dot indicator
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .scale(if (callState.isCallAnswered && !callState.isOnHold) pulseScale else 1f)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            callState.isOnHold -> WarningAmber
                                            callState.isCallAnswered -> CallGreen
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when {
                                    callState.isOnHold -> if (language == AppLanguage.BENGALI) "হোল্ডে আছে (ON HOLD)" else "ON HOLD"
                                    callState.isCallAnswered -> if (language == AppLanguage.BENGALI) "কল রিসিভ হয়েছে • কথা চলছে" else "CONNECTED • CALL IN PROGRESS"
                                    else -> if (language == AppLanguage.BENGALI) "রিং হচ্ছে / সংযোগ হচ্ছে..." else "CALLING / RINGING..."
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    callState.isOnHold -> WarningAmber
                                    callState.isCallAnswered -> CallGreen
                                    else -> MaterialTheme.colorScheme.primary
                                },
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Prominent Timer Display: starts counting seconds once the call is answered/received!
                    if (callState.isCallAnswered) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = formattedDuration,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                letterSpacing = 2.sp,
                                modifier = Modifier.testTag("active_call_duration_text")
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (language == AppLanguage.BENGALI)
                                    "⏱️ মোট সময়: ${toBengaliDigits(callState.durationSeconds.toString())} সেকেন্ড"
                                else
                                    "⏱️ Duration: ${callState.durationSeconds}s",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    } else {
                        // Before answer / while dialing
                        Text(
                            text = if (language == AppLanguage.BENGALI) "রিং হচ্ছে..." else "Ringing...",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFCBD5E1),
                            modifier = Modifier.testTag("active_call_ringing_text")
                        )
                    }
                }

                // Avatar / Callee details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = callState.callerName.take(1).uppercase(),
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = callState.callerName,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = callState.number,
                        fontSize = 15.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )

                    if (callState.carrier.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Navy900
                        ) {
                            Text(
                                text = "${callState.carrier} • HD Voice",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                // In-Call Keypad display if toggled
                if (showInCallKeypad) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Navy900,
                        modifier = Modifier
                            .fillMaxWidth(0.95f)
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = inCallDtmfText.ifEmpty { if (language == AppLanguage.BENGALI) "ডায়াল করুন" else "Dial numbers" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            val keypadKeys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("*", "0", "#")
                            )
                            keypadKeys.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    row.forEach { digit ->
                                        FilledIconButton(
                                            onClick = { inCallDtmfText += digit },
                                            shape = CircleShape,
                                            colors = IconButtonDefaults.filledIconButtonColors(
                                                containerColor = Navy800,
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier
                                                .padding(3.dp)
                                                .size(46.dp)
                                        ) {
                                            Text(digit, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // In-Call Controls Matrix (2 rows x 3 buttons)
                Column(
                    modifier = Modifier.fillMaxWidth(0.9f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = if (callState.isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = if (language == AppLanguage.BENGALI) {
                                if (callState.isMuted) "আনমিউট" else "মিউট"
                            } else {
                                if (callState.isMuted) "Unmute" else "Mute"
                            },
                            isActive = callState.isMuted,
                            onClick = onToggleMute,
                            testTag = "incall_mute"
                        )
                        InCallControlButton(
                            icon = Icons.Default.Dialpad,
                            label = if (language == AppLanguage.BENGALI) "কীপ্যাড" else "Keypad",
                            isActive = showInCallKeypad,
                            onClick = { showInCallKeypad = !showInCallKeypad },
                            testTag = "incall_keypad"
                        )
                        InCallControlButton(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            label = if (language == AppLanguage.BENGALI) "স্পিকার" else "Speaker",
                            isActive = callState.isSpeakerOn,
                            onClick = onToggleSpeaker,
                            testTag = "incall_speaker"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        InCallControlButton(
                            icon = if (callState.isOnHold) Icons.Default.PlayArrow else Icons.Default.Pause,
                            label = if (language == AppLanguage.BENGALI) {
                                if (callState.isOnHold) "চালু করুন" else "হোল্ড"
                            } else {
                                if (callState.isOnHold) "Resume" else "Hold"
                            },
                            isActive = callState.isOnHold,
                            onClick = onToggleHold,
                            testTag = "incall_hold"
                        )
                        InCallControlButton(
                            icon = Icons.Default.RecordVoiceOver,
                            label = "HD Voice",
                            isActive = true,
                            onClick = { /* Informational */ },
                            testTag = "incall_hd"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Big Red End Call Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FilledIconButton(
                        onClick = onEndCall,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = SpamRed,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .size(76.dp)
                            .testTag("incall_end_call")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (language == AppLanguage.BENGALI) "কল কাটুন" else "End Call",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        }
}

@Composable
fun InCallControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) Color.White else Navy800,
                contentColor = if (isActive) Navy950 else Color.White
            ),
            modifier = Modifier
                .size(56.dp)
                .testTag(testTag)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = if (isActive) Color.White else Color(0xFF94A3B8)
        )
    }
}
