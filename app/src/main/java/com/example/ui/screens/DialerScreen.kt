package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddContactDialog
import com.example.ui.components.KeypadBottomBar
import com.example.ui.components.KeypadMatrix
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.CallGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.VerifiedTeal
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CarrierLookupEngine

@Composable
fun DialerScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val inputNumber by viewModel.inputNumber.collectAsState()
    val isKeypadVisible by viewModel.isKeypadVisible.collectAsState()
    val t9Matches by viewModel.t9Matches.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()

    var showAddContactDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val carrierInfo = remember(inputNumber) {
        if (inputNumber.isNotEmpty()) CarrierLookupEngine.lookup(inputNumber) else null
    }

    if (showAddContactDialog) {
        AddContactDialog(
            initialNumber = inputNumber,
            onDismiss = { showAddContactDialog = false },
            onSave = { name, num, email, company ->
                viewModel.createContact(name, num, email, company)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("dialer_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Simulation Quick Banner for Caller ID testing
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Caller ID Demo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (language == AppLanguage.BENGALI) "কলার আইডি টেস্ট:" else "Test Caller ID:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.simulateSafeIncomingCall() },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("btn_test_verified_call")
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "ভেরিফাইড কল" else "Verified",
                            fontSize = 10.sp,
                            color = VerifiedTeal
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.simulateIncomingCall(
                                number = "+880 1899-001122",
                                name = "Fake Bank KYC Agent",
                                isSpam = true,
                                category = "Scammer (1,420 reports)",
                                reportsCount = 1420
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpamRed),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("btn_test_spam_call")
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "স্প্যাম কল" else "Spam Call",
                            fontSize = 10.sp,
                            color = SpamRed
                        )
                    }
                }
            }
        }

        // T9 matches suggestions / Number display area
        if (inputNumber.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Typed Digits
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = inputNumber,
                        fontSize = if (inputNumber.length > 12) 28.sp else 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .testTag("dialer_display_number")
                    )

                    // Paste button if clipboard has text
                    val clip = clipboardManager.getText()?.text
                    if (!clip.isNullOrBlank() && clip != inputNumber) {
                        IconButton(
                            onClick = { viewModel.setInputNumber(clip) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Operator & Circle badge
                carrierInfo?.let { carrier ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(carrier.primaryColorHex).copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneInTalk,
                                contentDescription = "Carrier",
                                tint = Color(carrier.primaryColorHex),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "${carrier.name} • ${carrier.circle}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(carrier.primaryColorHex)
                            )
                        }
                    }
                }
            }
        }

        // T9 Matching Contacts Preview list
        if (inputNumber.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (t9Matches.isNotEmpty()) {
                    items(t9Matches.take(4)) { match ->
                        val contact = match.contact
                        val avatarBg = AvatarColors[contact.avatarColorIndex % AvatarColors.size]
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setInputNumber(contact.phoneNumber)
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = contact.name.take(1).uppercase(),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = contact.phoneNumber,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.startCall(contact.phoneNumber) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = CallGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "কোন পরিচিতি মেলেনি" else "No matching contact in phonebook",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // Retractable Keypad Matrix & Bottom Bar
        AnimatedVisibility(
            visible = isKeypadVisible,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                KeypadMatrix(
                    onDigitClick = { digit -> viewModel.appendDigit(digit) },
                    onLongDigitClick = { digit -> viewModel.appendDigit(digit) }
                )

                KeypadBottomBar(
                    hasInput = inputNumber.isNotEmpty(),
                    onCallClick = {
                        if (inputNumber.isNotEmpty()) {
                            viewModel.startCall(inputNumber)
                        }
                    },
                    onBackspaceClick = { viewModel.deleteDigit() },
                    onClearAllClick = { viewModel.clearInput() },
                    onAddContactClick = { showAddContactDialog = true },
                    onToggleKeypad = { viewModel.toggleKeypadVisibility() }
                )
            }
        }

        if (!isKeypadVisible) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedButton(
                    onClick = { viewModel.setKeypadVisible(true) },
                    modifier = Modifier.testTag("btn_show_keypad")
                ) {
                    Text(if (language == AppLanguage.BENGALI) "কীপ্যাড খুলুন" else "Show Keypad")
                }
            }
        }
    }
}
