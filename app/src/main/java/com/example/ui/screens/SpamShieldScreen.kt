package com.example.ui.screens

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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.data.model.SpamRiskLevel
import com.example.ui.theme.CallGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CarrierLookupEngine
import com.example.util.DefaultDialerManager

@Composable
fun SpamShieldScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToCallBlocking: (() -> Unit)? = null
) {
    val spamReports by viewModel.spamReports.collectAsState()
    val blockedNumbers by viewModel.blockedNumbers.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()

    var lookupQuery by remember { mutableStateOf("") }
    var lookupResult by remember { mutableStateOf<String?>(null) }
    var isLookupSpam by remember { mutableStateOf(false) }

    var autoBlockHighRisk by remember { mutableStateOf(true) }
    var blockHiddenNumbers by remember { mutableStateOf(false) }
    var showCallerIdOverlay by remember { mutableStateOf(true) }

    var showReportModal by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var isDefaultDialer by remember { mutableStateOf(DefaultDialerManager.isDefaultDialer(context)) }
    var hasOverlayPermission by remember { mutableStateOf(DefaultDialerManager.hasOverlayPermission(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultDialer = DefaultDialerManager.isDefaultDialer(context)
        hasOverlayPermission = DefaultDialerManager.hasOverlayPermission(context)
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val granted = DefaultDialerManager.isDefaultDialer(context)
        isDefaultDialer = granted
        if (granted) {
            viewModel.showSnackbar(
                if (language == AppLanguage.BENGALI)
                    "স্মার্ট ডায়ালার ডিফল্ট ফোন অ্যাপ হিসেবে সেট হয়েছে!"
                else
                    "Smart Dialer is now your default phone app!"
            )
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val granted = DefaultDialerManager.hasOverlayPermission(context)
        hasOverlayPermission = granted
        if (granted) {
            viewModel.showSnackbar(
                if (language == AppLanguage.BENGALI)
                    "অন্য অ্যাপের ওপর প্রদর্শন অনুমতি প্রদান করা হয়েছে!"
                else
                    "Display Over Other Apps permission granted!"
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("spam_shield_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Hero Protection Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(CallGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Shield",
                            tint = CallGreen,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "স্প্যাম শিল্ড সক্রিয়" else "Spam Shield Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (language == AppLanguage.BENGALI) "রিয়েল-টাইম কলার আইডি ও ফ্রড প্রতিরোধ প্রস্তুত" else "Real-time AI Caller ID & Community Fraud Protection enabled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // System Permissions & Role Card (Default Phone App & Overlay Caller ID)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("card_system_permissions"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "কলিং অনুমতি ও সিস্টেম রোল" else "Call Roles & System Permissions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (language == AppLanguage.BENGALI)
                            "ইনকামিং কল রিসিভ এবং স্প্যাম নম্বর স্বয়ংক্রিয়ভাবে ব্লক করতে স্মার্ট ডায়ালারকে ডিফল্ট ফোন অ্যাপ হিসেবে সেট করুন।"
                        else
                            "Set Smart Dialer as your default phone app to receive calls and automatically block spam before ringing.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Default Phone App Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isDefaultDialer) CallGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Default Phone App",
                                tint = if (isDefaultDialer) CallGreen else WarningAmber,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "ডিফল্ট ফোন অ্যাপ" else "Default Phone App",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isDefaultDialer) {
                                    if (language == AppLanguage.BENGALI) "ডিফল্ট অ্যাপ হিসেবে সক্রিয়" else "Active as primary dialer"
                                } else {
                                    if (language == AppLanguage.BENGALI) "সেট করা নেই" else "Not set as default"
                                },
                                fontSize = 11.sp,
                                color = if (isDefaultDialer) CallGreen else WarningAmber
                            )
                        }

                        if (isDefaultDialer) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CallGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CallGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.BENGALI) "সক্রিয়" else "Active",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CallGreen
                                    )
                                }
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = {
                                        val intent = DefaultDialerManager.createDefaultDialerIntent(context)
                                        defaultDialerLauncher.launch(intent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_set_default_dialer_screen")
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BENGALI) "ডিফল্ট করুন" else "Set Default",
                                        fontSize = 11.sp
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        val intent = DefaultDialerManager.createDefaultAppsSettingsIntent(context)
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("btn_open_default_apps_settings")
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BENGALI) "সেটিংস" else "Settings",
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    // Overlay / Display Over Other Apps Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (hasOverlayPermission) CallGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Overlay Permission",
                                tint = if (hasOverlayPermission) CallGreen else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "অন্য অ্যাপের ওপর প্রদর্শন" else "Display Over Other Apps",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (hasOverlayPermission) {
                                    if (language == AppLanguage.BENGALI) "ফ্লোটিং কলার আইডি অনুমোদিত" else "Floating Caller ID enabled"
                                } else {
                                    if (language == AppLanguage.BENGALI) "ইনকামিং কলার আইডি পপআপের জন্য প্রয়োজন" else "Required for live incoming popups"
                                },
                                fontSize = 11.sp,
                                color = if (hasOverlayPermission) CallGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (hasOverlayPermission) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CallGreen.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = CallGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (language == AppLanguage.BENGALI) "অনুমোদিত" else "Granted",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CallGreen
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    val intent = DefaultDialerManager.createOverlayPermissionIntent(context)
                                    overlayPermissionLauncher.launch(intent)
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_request_overlay_permission")
                            ) {
                                Text(
                                    text = if (language == AppLanguage.BENGALI) "অনুমতি দিন" else "Enable",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live Number Lookup Tool
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "নম্বর সার্চ ও স্প্যাম টেস্ট" else "Check Any Number (Spam Lookup)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = lookupQuery,
                            onValueChange = { lookupQuery = it },
                            placeholder = { Text("Enter number e.g. 01899001122", fontSize = 12.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("spam_lookup_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (lookupQuery.isNotBlank()) {
                                    val match = spamReports.find {
                                        it.number.replace("[^0-9]".toRegex(), "").contains(lookupQuery.replace("[^0-9]".toRegex(), ""))
                                    }
                                    val carrier = CarrierLookupEngine.lookup(lookupQuery)
                                    if (match != null) {
                                        isLookupSpam = true
                                        lookupResult = "🚨 SPAM: ${match.reportedName} (${match.category})\nReports: ${match.reportsCount} users\nOperator: ${carrier.name}"
                                    } else {
                                        isLookupSpam = false
                                        lookupResult = "✅ SAFE: No community spam reports found.\nOperator: ${carrier.name} (${carrier.country})"
                                    }
                                }
                            },
                            modifier = Modifier.testTag("btn_spam_lookup")
                        ) {
                            Text("Check")
                        }
                    }

                    lookupResult?.let { res ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isLookupSpam) SpamRed.copy(alpha = 0.12f) else CallGreen.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = res,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isLookupSpam) SpamRed else CallGreen,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Protection Rules & Toggles
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "সুরক্ষা কনফিগারেশন" else "Call Protection Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto-block High Risk Scammers", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Instantly reject known financial fraud & OTP phishing", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = autoBlockHighRisk,
                            onCheckedChange = { autoBlockHighRisk = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Block Private / Hidden Numbers", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Reject calls from masked or private caller IDs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = blockHiddenNumbers,
                            onCheckedChange = { blockHiddenNumbers = it }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Real-Time Caller ID Pop-up", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Display caller identity and spam alerts during incoming calls", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = showCallerIdOverlay,
                            onCheckedChange = { showCallerIdOverlay = it }
                        )
                    }
                }
            }
        }

        // Action: Report Spam Number & Manage Blacklist Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showReportModal = true },
                    colors = ButtonDefaults.buttonColors(containerColor = SpamRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("btn_open_report_spam")
                ) {
                    Icon(Icons.Default.ReportProblem, contentDescription = "Report", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (language == AppLanguage.BENGALI) "রিপোর্ট স্প্যাম" else "Report Spam",
                        fontSize = 12.sp
                    )
                }

                if (onNavigateToCallBlocking != null) {
                    Button(
                        onClick = onNavigateToCallBlocking,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_open_call_blocking")
                    ) {
                        Icon(Icons.Default.Block, contentDescription = "Blacklist", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (language == AppLanguage.BENGALI) "ব্লকলিস্ট ম্যানেজার" else "Call Blacklist",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Currently Blocked Numbers List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (language == AppLanguage.BENGALI) "ব্লক করা নম্বরসমূহ (${blockedNumbers.size})" else "BLOCKED NUMBERS (${blockedNumbers.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                if (onNavigateToCallBlocking != null && blockedNumbers.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onNavigateToCallBlocking,
                        modifier = Modifier.testTag("btn_view_all_blocked")
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "সব দেখুন" else "Manage All",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        if (blockedNumbers.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "কোন নম্বর ব্লক করা নেই" else "No blocked numbers yet",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(blockedNumbers, key = { it.id }) { blocked ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Blocked",
                            tint = SpamRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (blocked.name.isNotBlank()) "${blocked.name} (${blocked.number})" else blocked.number,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Reason: ${blocked.reason}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = { viewModel.unblockNumber(blocked.number) },
                            modifier = Modifier.testTag("btn_unblock_${blocked.id}")
                        ) {
                            Text("Unblock", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // Report Spam Dialog
    if (showReportModal) {
        var reportNumber by remember { mutableStateOf("") }
        var reportName by remember { mutableStateOf("") }
        var reportCategory by remember { mutableStateOf("Scammer") }
        var reportNotes by remember { mutableStateOf("") }

        val categories = listOf("Scammer", "Telemarketer", "Loan Fraud", "Robocall", "Fake Delivery", "Harassment")

        AlertDialog(
            onDismissRequest = { showReportModal = false },
            title = { Text("Report Spam Caller", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = reportNumber,
                        onValueChange = { reportNumber = it },
                        label = { Text("Spam Phone Number *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reportName,
                        onValueChange = { reportName = it },
                        label = { Text("Identified Name / Organization *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Spam Category:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            FilterChip(
                                selected = reportCategory == cat,
                                onClick = { reportCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.drop(3).forEach { cat ->
                            FilterChip(
                                selected = reportCategory == cat,
                                onClick = { reportCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = reportNotes,
                        onValueChange = { reportNotes = it },
                        label = { Text("Details (e.g. Asks for OTP)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reportNumber.isNotBlank() && reportName.isNotBlank()) {
                            viewModel.reportSpamNumber(reportNumber, reportName, reportCategory, reportNotes)
                            showReportModal = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpamRed),
                    enabled = reportNumber.isNotBlank() && reportName.isNotBlank()
                ) {
                    Text("Submit & Block")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showReportModal = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
