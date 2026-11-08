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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactEntity
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.CallGreen
import com.example.ui.theme.WarningAmber

@Composable
fun ContactCard(
    contact: ContactEntity,
    onCallClick: () -> Unit,
    onSmsClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarBg = AvatarColors[contact.avatarColorIndex % AvatarColors.size]
    val initials = contact.name.trim().take(1).uppercase()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("contact_card_${contact.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    .background(avatarBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${contact.phoneNumber} • ${contact.label}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (contact.company.isNotBlank()) {
                    Text(
                        text = contact.company,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Favorite star
            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("contact_fav_${contact.id}")
            ) {
                Icon(
                    imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                    contentDescription = "Favorite",
                    tint = if (contact.isFavorite) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(22.dp)
                )
            }

            // Message button
            IconButton(
                onClick = onSmsClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(AccentBlue.copy(alpha = 0.1f))
                    .testTag("contact_sms_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Message,
                    contentDescription = "SMS",
                    tint = AccentBlue,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Call button
            IconButton(
                onClick = onCallClick,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(CallGreen.copy(alpha = 0.15f))
                    .testTag("contact_call_${contact.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Call",
                    tint = CallGreen,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
fun FavoriteContactBubble(
    contact: ContactEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val avatarBg = AvatarColors[contact.avatarColorIndex % AvatarColors.size]
    val initials = contact.name.trim().take(1).uppercase()

    Column(
        modifier = modifier
            .width(64.dp)
            .clickable(onClick = onClick)
            .testTag("fav_bubble_${contact.id}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(avatarBg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = contact.name.split(" ").firstOrNull() ?: contact.name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AddContactDialog(
    initialNumber: String = "",
    onDismiss: () -> Unit,
    onSave: (name: String, number: String, email: String, company: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var number by remember { mutableStateOf(initialNumber) }
    var email by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Add New Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_name")
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone Number *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_contact_number")
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company / Title (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && number.isNotBlank()) {
                        onSave(name, number, email, company)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank() && number.isNotBlank(),
                modifier = Modifier.testTag("btn_save_contact")
            ) {
                Text("Save Contact")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
