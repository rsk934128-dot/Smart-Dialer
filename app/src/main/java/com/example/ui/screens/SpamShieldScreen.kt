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
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SpamRiskLevel
import com.example.ui.theme.CallGreen
import com.example.ui.theme.SpamRed
import com.example.ui.theme.VerifiedTeal
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CarrierLookupEngine

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
