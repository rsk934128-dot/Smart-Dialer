package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContactEntity
import com.example.ui.components.AddContactDialog
import com.example.ui.components.ContactCard
import com.example.ui.components.FavoriteContactBubble
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.theme.CallGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.PermissionHelper

@Composable
fun ContactsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val favorites by viewModel.favorites.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val permissionStatus by viewModel.permissionStatus.collectAsState()
    val context = LocalContext.current

    var searchContactQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedContactDetails by remember { mutableStateOf<ContactEntity?>(null) }
    var showContactsRationaleDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }

    val contactsPermissions = remember { PermissionHelper.contactsPermissionsArray() }
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshSimsAndSyncHistory(showFeedback = false)
        viewModel.syncContactsNow(showFeedback = true)
    }

    fun requestContactsPermission() {
        val missing = PermissionHelper.getMissingPermissions(context, contactsPermissions)
        if (missing.isEmpty()) {
            viewModel.syncContactsNow(showFeedback = true)
            return
        }
        val permDeniedForever = PermissionHelper.isPermanentlyDenied(context, missing)
        if (permDeniedForever) {
            isPermanentlyDenied = true
            showContactsRationaleDialog = true
        } else {
            PermissionHelper.markPermissionsPrompted(context, missing)
            contactsPermissionLauncher.launch(missing.toTypedArray())
        }
    }

    val filteredContacts = remember(contacts, searchContactQuery) {
        if (searchContactQuery.isBlank()) {
            contacts
        } else {
            contacts.filter {
                it.name.contains(searchContactQuery, ignoreCase = true) ||
                        it.phoneNumber.contains(searchContactQuery) ||
                        it.company.contains(searchContactQuery, ignoreCase = true)
            }
        }
    }

    if (showAddDialog) {
        AddContactDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, num, email, company ->
                viewModel.createContact(name, num, email, company)
            }
        )
    }

    Box(modifier = modifier.fillMaxSize().testTag("contacts_screen")) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Search & Add Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchContactQuery,
                    onValueChange = { searchContactQuery = it },
                    placeholder = {
                        Text(
                            if (language == AppLanguage.BENGALI) "কন্টাক্ট খুঁজুন..." else "Search contacts...",
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("search_contacts_input")
                )

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = { requestContactsPermission() },
                    modifier = Modifier.testTag("btn_sync_contacts")
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Sync Contacts",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier.testTag("btn_add_contact_top")
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = "Add Contact",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Contacts Permission Prompt Card if READ_CONTACTS not yet granted
            if (!permissionStatus.hasContacts) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("card_contacts_permission"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
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
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = "Contacts Permission",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.BENGALI)
                                    "কন্টাক্ট পারমিশন প্রয়োজন"
                                else
                                    "Sync Device Contacts",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (language == AppLanguage.BENGALI)
                                    "ফোনবুক থেকে কন্টাক্ট আনতে এবং T9 সার্চ করতে অনুমতি দিন"
                                else
                                    "Allow Contacts access to import phonebook & power T9 search",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { requestContactsPermission() },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("btn_grant_contacts_permission")
                        ) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "অনুমতি দিন" else "Allow",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Favorites horizontal section
            if (favorites.isNotEmpty() && searchContactQuery.isBlank()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "প্রিয় কন্টাক্টসমূহ" else "FAVORITES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(favorites, key = { it.id }) { fav ->
                            FavoriteContactBubble(
                                contact = fav,
                                onClick = { viewModel.startCall(fav.phoneNumber) }
                            )
                        }
                    }
                }
            }

            // Contacts List
            Text(
                text = if (language == AppLanguage.BENGALI) "সকল কন্টাক্ট (${filteredContacts.size})" else "ALL CONTACTS (${filteredContacts.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredContacts, key = { it.id }) { contact ->
                    ContactCard(
                        contact = contact,
                        onCallClick = { viewModel.startCall(contact.phoneNumber) },
                        onSmsClick = { viewModel.sendSms(context, contact.phoneNumber) },
                        onFavoriteToggle = { viewModel.toggleFavorite(contact) },
                        onClick = { selectedContactDetails = contact }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Contact Details Modal
    selectedContactDetails?.let { contact ->
        AlertDialog(
            onDismissRequest = { selectedContactDetails = null },
            title = {
                Text(text = contact.name, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Phone: ${contact.phoneNumber} (${contact.label})", fontSize = 14.sp)
                    if (contact.email.isNotBlank()) {
                        Text("Email: ${contact.email}", fontSize = 13.sp)
                    }
                    if (contact.company.isNotBlank()) {
                        Text("Company: ${contact.company}", fontSize = 13.sp)
                    }
                    if (contact.notes.isNotBlank()) {
                        Text("Notes: ${contact.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.startCall(contact.phoneNumber)
                            selectedContactDetails = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CallGreen)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call")
                    }
                    OutlinedButton(
                        onClick = {
                            viewModel.sendSms(context, contact.phoneNumber)
                            selectedContactDetails = null
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = "SMS", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SMS")
                    }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = {
                            viewModel.toggleFavorite(contact)
                            selectedContactDetails = contact.copy(isFavorite = !contact.isFavorite)
                        }
                    ) {
                        Icon(
                            imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Favorite",
                            tint = if (contact.isFavorite) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.deleteContact(contact)
                            selectedContactDetails = null
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SpamRed)
                    }
                }
            }
        )
    }

    if (showContactsRationaleDialog) {
        PermissionRationaleDialog(
            language = language,
            status = permissionStatus.copy(hasCallLog = true, hasPhoneState = true, hasCallPhone = true),
            isPermanentlyDenied = isPermanentlyDenied,
            onGrantPermissions = {
                showContactsRationaleDialog = false
                val missing = PermissionHelper.getMissingPermissions(context, contactsPermissions)
                if (missing.isNotEmpty()) {
                    PermissionHelper.markPermissionsPrompted(context, missing)
                    contactsPermissionLauncher.launch(missing.toTypedArray())
                }
            },
            onOpenSettings = {
                showContactsRationaleDialog = false
                context.startActivity(PermissionHelper.createAppSettingsIntent(context))
            },
            onDismiss = { showContactsRationaleDialog = false }
        )
    }
}
