package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlockedNumberEntity
import com.example.ui.theme.CallGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CarrierLookupEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallBlockingScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null
) {
    val blockedNumbers by viewModel.blockedNumbers.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedReasonFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var numberToUnblock by remember { mutableStateOf<BlockedNumberEntity?>(null) }

    // Intercept back button if onNavigateBack is provided
    BackHandler(enabled = onNavigateBack != null) {
        onNavigateBack?.invoke()
    }

    val filterOptions = listOf(
        "All" to if (language == AppLanguage.BENGALI) "সব" else "All",
        "Scammer" to if (language == AppLanguage.BENGALI) "প্রতারক" else "Scammer",
        "Telemarketer" to if (language == AppLanguage.BENGALI) "মার্কেটিং" else "Telemarketing",
        "Robocall" to if (language == AppLanguage.BENGALI) "রোবোকল" else "Robocall",
        "Harassment" to if (language == AppLanguage.BENGALI) "হয়রানি" else "Harassment",
        "Manual" to if (language == AppLanguage.BENGALI) "কাস্টম" else "Manual"
    )

    // Filter blocked numbers by search query and category
    val filteredBlockedNumbers = remember(blockedNumbers, searchQuery, selectedReasonFilter) {
        blockedNumbers.filter { blocked ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                blocked.number.contains(searchQuery, ignoreCase = true) ||
                        blocked.name.contains(searchQuery, ignoreCase = true) ||
                        blocked.reason.contains(searchQuery, ignoreCase = true)
            }
            val matchesFilter = when (selectedReasonFilter) {
                "All" -> true
                "Scammer" -> blocked.reason.contains("Scam", ignoreCase = true) || blocked.reason.contains("Fraud", ignoreCase = true)
                "Telemarketer" -> blocked.reason.contains("Telemarket", ignoreCase = true) || blocked.reason.contains("Sales", ignoreCase = true)
                "Robocall" -> blocked.reason.contains("Robo", ignoreCase = true) || blocked.reason.contains("Automated", ignoreCase = true)
                "Harassment" -> blocked.reason.contains("Harass", ignoreCase = true)
                "Manual" -> blocked.reason.contains("Manual", ignoreCase = true) || (!blocked.reason.contains("Scam", ignoreCase = true) && !blocked.reason.contains("Telemarket", ignoreCase = true))
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SpamRed.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = "Blocklist",
                                tint = SpamRed,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "কল ব্লকলিস্ট ও ব্লকিং" else "Call Blacklist & Blocking",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == AppLanguage.BENGALI) "${blockedNumbers.size}টি নম্বর লোকাল ডাটাবেসে ব্লকড" else "${blockedNumbers.size} numbers blocked in Room database",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("btn_back_from_call_blocking")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("btn_top_add_blacklist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add to Blacklist",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("fab_add_blocked_number")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Number",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BENGALI) "নম্বর ব্লক করুন" else "Block Number",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .testTag("call_blocking_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Search Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "ব্লক করা নম্বর বা নাম খুঁজুন..." else "Search blocked numbers or names...",
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
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_blocked_input")
                )
            }

            // Category Filter Chips
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.take(3).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedReasonFilter == key,
                            onClick = { selectedReasonFilter = key },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filterOptions.drop(3).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedReasonFilter == key,
                            onClick = { selectedReasonFilter = key },
                            label = { Text(label, fontSize = 12.sp) },
                            modifier = Modifier.testTag("filter_chip_$key")
                        )
                    }
                }
            }

            // Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(SpamRed.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Shield",
                                tint = SpamRed,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "জিরো-টলারেন্স কল ব্লকার" else "Zero-Tolerance Call Blocker",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (language == AppLanguage.BENGALI) "তালিকাভুক্ত নম্বর থেকে আসা সমস্ত ইনকামিং কল তাৎক্ষণিক ড্রপ হবে" else "All calls from listed numbers will be immediately rejected and muted silently",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // List Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "ব্লকলিস্টে সংরক্ষিত নম্বর (${filteredBlockedNumbers.size})" else "BLACKLISTED NUMBERS (${filteredBlockedNumbers.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // Empty State
            if (filteredBlockedNumbers.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(CallGreen.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "All Clear",
                                    tint = CallGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) {
                                    if (language == AppLanguage.BENGALI) "কোন মেলানো নম্বর পাওয়া যায়নি" else "No matching blocked numbers"
                                } else {
                                    if (language == AppLanguage.BENGALI) "ব্লকলিস্ট খালি" else "Your Blacklist is Empty"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (language == AppLanguage.BENGALI) "অবাঞ্ছিত কলার বা স্প্যামারদের ব্লক করতে নিচের বাটনে চাপুন" else "Add spam, telemarketing, or nuisance numbers to block them permanently",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showAddDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("btn_empty_add_blacklist")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (language == AppLanguage.BENGALI) "প্রথম নম্বরটি যোগ করুন" else "Add First Number")
                            }
                        }
                    }
                }
            } else {
                items(filteredBlockedNumbers, key = { it.id }) { blocked ->
                    BlockedNumberItemCard(
                        item = blocked,
                        language = language,
                        onUnblockClick = { numberToUnblock = blocked }
                    )
                }
            }
        }
    }

    // Add to Blacklist Dialog
    if (showAddDialog) {
        AddBlockedNumberDialog(
            language = language,
            onDismiss = { showAddDialog = false },
            onConfirm = { number, name, reason ->
                viewModel.blockNumber(number = number, name = name, reason = reason)
                showAddDialog = false
            }
        )
    }

    // Unblock Confirmation Dialog
    numberToUnblock?.let { item ->
        AlertDialog(
            onDismissRequest = { numberToUnblock = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = WarningAmber
                )
            },
            title = {
                Text(
                    text = if (language == AppLanguage.BENGALI) "নম্বরটি আনব্লক করতে চান?" else "Unblock this number?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (language == AppLanguage.BENGALI)
                        "${item.number} নম্বরটি আপনার ব্লকলিস্ট থেকে সরানো হবে এবং তারা আবার কল করতে পারবে।"
                    else
                        "${item.number} will be removed from your local blacklist and will be able to call you again."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.unblockNumber(item.number)
                        numberToUnblock = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("btn_confirm_unblock")
                ) {
                    Text(if (language == AppLanguage.BENGALI) "হ্যাঁ, আনব্লক করুন" else "Unblock")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { numberToUnblock = null },
                    modifier = Modifier.testTag("btn_cancel_unblock")
                ) {
                    Text(if (language == AppLanguage.BENGALI) "বাতিল" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun BlockedNumberItemCard(
    item: BlockedNumberEntity,
    language: AppLanguage,
    onUnblockClick: () -> Unit
) {
    val carrierInfo = remember(item.number) { CarrierLookupEngine.lookup(item.number) }
    val dateString = remember(item.blockedAt) {
        val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
        sdf.format(Date(item.blockedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("blocked_card_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(SpamRed.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Blocked",
                    tint = SpamRed,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (item.name.isNotBlank()) item.name else item.number,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (item.name.isNotBlank()) {
                    Text(
                        text = item.number,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Reason Badge
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when {
                            item.reason.contains("Scam", ignoreCase = true) || item.reason.contains("Fraud", ignoreCase = true) -> SpamRed.copy(alpha = 0.12f)
                            item.reason.contains("Telemarket", ignoreCase = true) -> WarningAmber.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ) {
                        Text(
                            text = item.reason,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                item.reason.contains("Scam", ignoreCase = true) || item.reason.contains("Fraud", ignoreCase = true) -> SpamRed
                                item.reason.contains("Telemarket", ignoreCase = true) -> WarningAmber
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Carrier Tag
                    if (carrierInfo.name != "Unknown Carrier") {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = carrierInfo.name,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${if (language == AppLanguage.BENGALI) "ব্লক করা হয়েছে: " else "Blocked: "}$dateString",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onUnblockClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("btn_unblock_${item.id}")
            ) {
                Text(
                    text = if (language == AppLanguage.BENGALI) "আনব্লক" else "Unblock",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun AddBlockedNumberDialog(
    language: AppLanguage,
    onDismiss: () -> Unit,
    onConfirm: (number: String, name: String, reason: String) -> Unit
) {
    var inputNumber by remember { mutableStateOf("") }
    var inputName by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("Scammer / Fraud") }
    var customReason by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val presetReasons = listOf(
        "Scammer / Fraud",
        "Telemarketer / Sales",
        "Robocall / Automation",
        "Harassment / Nuisance",
        "Fake Courier / Phishing",
        "Personal / Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Block,
                    contentDescription = "Block Icon",
                    tint = SpamRed,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (language == AppLanguage.BENGALI) "ব্লকলিস্টে নতুন নম্বর যোগ করুন" else "Add Number to Blacklist",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (language == AppLanguage.BENGALI) "এই নম্বরটি সরাসরি লোকাল Room ডাটাবেসে সেভ হবে।" else "This number will be stored in your device Room database and blocked automatically.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputNumber,
                    onValueChange = {
                        inputNumber = it
                        if (it.isNotBlank()) isError = false
                    },
                    label = { Text(if (language == AppLanguage.BENGALI) "ফোন নম্বর *" else "Phone Number *") },
                    placeholder = { Text("e.g. +880 1899-001122 or 01712345678") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text(if (language == AppLanguage.BENGALI) "নম্বর প্রদান করা আবশ্যক" else "Phone number is required", color = MaterialTheme.colorScheme.error) }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_block_number")
                )

                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text(if (language == AppLanguage.BENGALI) "কলার নাম বা বিবরণ (ঐচ্ছিক)" else "Caller Name / Label (Optional)") },
                    placeholder = { Text("e.g. Fake Bank Caller") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_block_name")
                )

                Text(
                    text = if (language == AppLanguage.BENGALI) "ব্লক করার কারণ নির্বাচন করুন:" else "Select Block Reason:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Chips for quick reasons
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetReasons.take(2).forEach { reason ->
                            FilterChip(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                label = { Text(reason, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetReasons.subList(2, 4).forEach { reason ->
                            FilterChip(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                label = { Text(reason, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetReasons.takeLast(2).forEach { reason ->
                            FilterChip(
                                selected = selectedReason == reason,
                                onClick = { selectedReason = reason },
                                label = { Text(reason, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                if (selectedReason == "Personal / Other") {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Specific reason") },
                        placeholder = { Text("Enter custom reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (inputNumber.isBlank()) {
                        isError = true
                    } else {
                        val finalReason = if (selectedReason == "Personal / Other" && customReason.isNotBlank()) {
                            customReason.trim()
                        } else {
                            selectedReason
                        }
                        onConfirm(inputNumber.trim(), inputName.trim(), finalReason)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SpamRed),
                modifier = Modifier.testTag("btn_confirm_add_blacklist")
            ) {
                Text(if (language == AppLanguage.BENGALI) "ব্লকলিস্টে যোগ করুন" else "Add to Blacklist")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("btn_cancel_add_blacklist")
            ) {
                Text(if (language == AppLanguage.BENGALI) "বাতিল" else "Cancel")
            }
        }
    )
}
