package com.example.ui.components

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
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.CallGreen
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Navy950
import com.example.ui.theme.SpamRed
import com.example.ui.viewmodel.ActiveCallState

@Composable
fun ActiveCallDialog(
    callState: ActiveCallState,
    onEndCall: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onToggleHold: () -> Unit
) {
    var showInCallKeypad by remember { mutableStateOf(false) }
    var inCallDtmfText by remember { mutableStateOf("") }

    val formattedDuration = remember(callState.durationSeconds) {
        val min = callState.durationSeconds / 60
        val sec = callState.durationSeconds % 60
        String.format("%02d:%02d", min, sec)
    }

    Dialog(
        onDismissRequest = { /* Must click end call */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
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
                // Top status
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = if (callState.isOnHold) "ON HOLD" else "ACTIVE CALL",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (callState.isOnHold) Color(0xFFF59E0B) else CallGreen,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formattedDuration,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                // Avatar / Callee details
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = callState.callerName.take(1).uppercase(),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = callState.callerName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = callState.number,
                        fontSize = 16.sp,
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
                            .fillMaxWidth(0.85f)
                            .padding(bottom = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DTMF Tones: $inCallDtmfText",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            KeypadMatrix(
                                onDigitClick = { inCallDtmfText += it },
                                onLongDigitClick = { inCallDtmfText += it }
                            )
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
                            label = if (callState.isMuted) "Unmute" else "Mute",
                            isActive = callState.isMuted,
                            onClick = onToggleMute,
                            testTag = "incall_mute"
                        )
                        InCallControlButton(
                            icon = Icons.Default.Dialpad,
                            label = "Keypad",
                            isActive = showInCallKeypad,
                            onClick = { showInCallKeypad = !showInCallKeypad },
                            testTag = "incall_keypad"
                        )
                        InCallControlButton(
                            icon = Icons.Default.VolumeUp,
                            label = "Speaker",
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
                            label = if (callState.isOnHold) "Resume" else "Hold",
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

                Spacer(modifier = Modifier.height(28.dp))

                // Big Red End Call Button
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
