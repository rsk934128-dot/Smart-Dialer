package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppLanguage
import com.example.util.DialerPermissionsStatus

@Composable
fun PermissionRationaleDialog(
    language: AppLanguage,
    status: DialerPermissionsStatus,
    isPermanentlyDenied: Boolean,
    onGrantPermissions: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPermanentlyDenied) Icons.Default.Settings else Icons.Default.Security,
                        contentDescription = "Permissions",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isPermanentlyDenied) {
                        if (language == AppLanguage.BENGALI) "সেটিংস থেকে অনুমতি দিন" else "Enable in App Settings"
                    } else {
                        if (language == AppLanguage.BENGALI) "প্রয়োজনীয় অনুমতি দিন" else "Permissions Required"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isPermanentlyDenied) {
                        if (language == AppLanguage.BENGALI)
                            "কল লগ, সিম ও কন্টাক্ট ফিচার ব্যবহারের জন্য অনুগ্রহ করে অ্যাপ সেটিংস থেকে নিচের পারমিশনগুলো চালু করুন:"
                        else
                            "Some permissions were permanently denied. Please enable them in Android App Settings for full Smart Dialer features:"
                    } else {
                        if (language == AppLanguage.BENGALI)
                            "স্মার্ট ডায়ালার সঠিকভাবে কাজ করার জন্য নিচের পারমিশনগুলো প্রয়োজন:"
                        else
                            "Smart Dialer needs access to these features to manage your calls and caller ID:"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!status.hasCallLog) {
                    PermissionReasonRow(
                        icon = Icons.Default.History,
                        tint = AccentBlue,
                        title = if (language == AppLanguage.BENGALI) "কল লগ (Call Logs)" else "Call Logs",
                        description = if (language == AppLanguage.BENGALI)
                            "ফোনের রিয়েল কল হিস্টরি দেখানো, ব্যাকআপ নেওয়া ও স্প্যাম কল শনাক্ত করার জন্য"
                        else
                            "Sync recent calls, show SIM 1/SIM 2 history, and flag spam callers"
                    )
                }

                if (!status.hasPhoneState || !status.hasCallPhone) {
                    PermissionReasonRow(
                        icon = Icons.Default.PhoneInTalk,
                        tint = VerifiedTeal,
                        title = if (language == AppLanguage.BENGALI) "ফোন ও কলিং (Phone State)" else "Phone State & Calling",
                        description = if (language == AppLanguage.BENGALI)
                            "ডুয়াল-সিম (SIM 1 ও SIM 2) শনাক্ত করা, কল করা এবং লাইভ ইনকামিং কলার আইডি দেখানোর জন্য"
                        else
                            "Detect SIM 1 / SIM 2, place calls directly, and identify incoming calls"
                    )
                }

                if (!status.hasContacts) {
                    PermissionReasonRow(
                        icon = Icons.Default.Contacts,
                        tint = WarningAmber,
                        title = if (language == AppLanguage.BENGALI) "কন্টাক্ট (Contacts)" else "Contacts",
                        description = if (language == AppLanguage.BENGALI)
                            "সেভ করা কন্টাক্ট ইমপোর্ট করা এবং কীপ্যাডে নাম দিয়ে T9 সার্চ করার জন্য"
                        else
                            "Display caller names and power instant T9 keypad contact search"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isPermanentlyDenied) onOpenSettings() else onGrantPermissions()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag(if (isPermanentlyDenied) "btn_open_permission_settings" else "btn_confirm_permission_grant")
            ) {
                Text(
                    text = if (isPermanentlyDenied) {
                        if (language == AppLanguage.BENGALI) "সেটিংস খুলুন" else "Open Settings"
                    } else {
                        if (language == AppLanguage.BENGALI) "অনুমতি দিন" else "Allow Permissions"
                    },
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .height(48.dp)
                    .testTag("btn_dismiss_permission_dialog")
            ) {
                Text(if (language == AppLanguage.BENGALI) "পরে" else "Not Now")
            }
        }
    )
}

@Composable
private fun PermissionReasonRow(
    icon: ImageVector,
    tint: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun MissingPermissionsBanner(
    language: AppLanguage,
    status: DialerPermissionsStatus,
    onRequestPermissions: () -> Unit,
    onDismiss: () -> Unit
) {
    val missingLabels = if (language == AppLanguage.BENGALI) {
        status.missingCoreLabelsBn.joinToString(", ")
    } else {
        status.missingCoreLabelsEn.joinToString(", ")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .testTag("banner_missing_permissions"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Permissions Needed",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BENGALI)
                        "পারমিশন প্রয়োজন ($missingLabels)"
                    else
                        "Permissions Needed: $missingLabels",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = if (language == AppLanguage.BENGALI)
                        "রিয়েল কল লগ, সিম ও কন্টাক্ট সিঙ্ক করতে অনুমতি দিন"
                    else
                        "Allow access to sync call history, SIM slots, and contacts",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onRequestPermissions,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_grant_missing_permissions")
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "অনুমতি দিন" else "Grant Access",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("btn_dismiss_missing_permissions")
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "পরে" else "Later",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
