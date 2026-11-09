package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.automirrored.filled.PhoneMissed
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallLogEntity
import com.example.ui.components.CallLogCard
import com.example.ui.components.FilterChipRow
import com.example.ui.theme.CallGreen
import com.example.ui.theme.SpamRed
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RecentsScreen(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.filteredCallLogs.collectAsState()
    val selectedFilter by viewModel.recentsFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val language by viewModel.currentLanguage.collectAsState()
    val context = LocalContext.current

    var selectedLogForDetails by remember { mutableStateOf<CallLogEntity?>(null) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("recents_screen")
    ) {
        // Search & Clear Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = {
                    Text(
                        if (language == AppLanguage.BENGALI) "কল হিস্ট্রি খুঁজুন..." else "Search recents by name or number...",
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
                    .testTag("search_recents_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { showClearConfirmDialog = true },
                modifier = Modifier.testTag("btn_clear_call_logs")
            ) {
                Icon(
                    imageVector = Icons.Default.ClearAll,
                    contentDescription = "Clear Call Logs",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Filter chips row
        FilterChipRow(
            selectedFilter = selectedFilter,
            onFilterSelected = { viewModel.setRecentsFilter(it) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Logs list
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.PhoneMissed,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (language == AppLanguage.BENGALI) "কোন কল লগ পাওয়া যায়নি" else "No recent calls found",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    CallLogCard(
                        callLog = log,
                        onCallClick = { viewModel.startCall(log.number) },
                        onDetailsClick = { selectedLogForDetails = log }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    // Call Details Modal
    selectedLogForDetails?.let { log ->
        AlertDialog(
            onDismissRequest = { selectedLogForDetails = null },
            title = {
                Text(
                    text = if (log.cachedName.isNotBlank()) log.cachedName else log.number,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Number: ${log.number}", fontSize = 14.sp)
                    Text("Type: ${log.type.name}", fontSize = 14.sp)
                    Text(
                        "Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (log.durationSeconds > 0) {
                        Text("Duration: ${log.durationSeconds} seconds", fontSize = 13.sp)
                    }
                    if (log.carrier.isNotBlank()) {
                        Text("Operator: ${log.carrier}", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    if (log.isSpam) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = SpamRed.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "Reported as: ${log.spamCategory}",
                                color = SpamRed,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.startCall(log.number)
                            selectedLogForDetails = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CallGreen)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Call")
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.sendSms(context, log.number)
                            selectedLogForDetails = null
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
                    OutlinedButton(
                        onClick = {
                            viewModel.blockNumber(log.number, log.cachedName, "Blocked from recents")
                            selectedLogForDetails = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SpamRed)
                    ) {
                        Icon(Icons.Default.Block, contentDescription = "Block", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Block")
                    }

                    IconButton(
                        onClick = {
                            viewModel.deleteCallLog(log.id)
                            selectedLogForDetails = null
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SpamRed)
                    }
                }
            }
        )
    }

    // Clear confirmation dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(if (language == AppLanguage.BENGALI) "কল হিস্ট্রি মুছবেন?" else "Clear All Call Logs?") },
            text = {
                Text(if (language == AppLanguage.BENGALI) "সমস্ত কল হিস্ট্রি মুছে যাবে।" else "This will permanently delete all entries in your call history.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllLogs()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpamRed)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
