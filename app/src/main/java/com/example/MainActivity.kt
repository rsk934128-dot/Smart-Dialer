package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.core.content.ContextCompat
import com.example.ui.components.ActiveCallDialog
import com.example.ui.components.CallerIdOverlayDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.CallBlockingScreen
import com.example.ui.screens.ContactsScreen
import com.example.ui.screens.DialerScreen
import com.example.ui.screens.RecentsScreen
import com.example.ui.screens.SpamShieldScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VerifiedTeal
import com.example.ui.viewmodel.ActiveCallState
import com.example.ui.viewmodel.AppLanguage
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.DialerViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: DialerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(viewModel: DialerViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeCallState by viewModel.activeCallState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val inputNumber by viewModel.inputNumber.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCallBlockingScreen by remember { mutableStateOf(false) }

    // Request necessary telephony & contact permissions gracefully
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions handled; internal Room cache ensures smooth operation either way
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CALL_PHONE)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CONTACTS)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_CALL_LOG)
        }
        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    // Snackbar handling
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissSnackbar()
        }
    }

    // Back handling: close call blocking, pop input, or return to keypad
    BackHandler(enabled = showCallBlockingScreen || currentTab != AppTab.KEYPAD || inputNumber.isNotEmpty()) {
        if (showCallBlockingScreen) {
            showCallBlockingScreen = false
        } else if (inputNumber.isNotEmpty()) {
            viewModel.clearInput()
        } else if (currentTab != AppTab.KEYPAD) {
            viewModel.selectTab(AppTab.KEYPAD)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Shield",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (currentLanguage == AppLanguage.BENGALI) "স্মার্ট ডায়ালার" else "Smart Dialer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentLanguage == AppLanguage.BENGALI) "কলার আইডি ও স্প্যাম প্রোটেকশন" else "Caller ID & Spam Shield",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Language Switcher
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                        onClick = { viewModel.toggleLanguage() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentLanguage == AppLanguage.BENGALI) "বাংলা" else "EN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Call Blacklist Manager quick action
                    IconButton(
                        onClick = { showCallBlockingScreen = true },
                        modifier = Modifier.testTag("btn_top_blacklist")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Call Blacklist",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Architecture & Info dialog trigger
                    IconButton(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.testTag("btn_about_app")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                val navItems = listOf(
                    Triple(
                        AppTab.KEYPAD,
                        Icons.Default.Dialpad,
                        if (currentLanguage == AppLanguage.BENGALI) "কীপ্যাড" else "Keypad"
                    ),
                    Triple(
                        AppTab.RECENTS,
                        Icons.Default.History,
                        if (currentLanguage == AppLanguage.BENGALI) "রিসেন্ট" else "Recents"
                    ),
                    Triple(
                        AppTab.CONTACTS,
                        Icons.Default.Contacts,
                        if (currentLanguage == AppLanguage.BENGALI) "কন্টাক্ট" else "Contacts"
                    ),
                    Triple(
                        AppTab.SPAM_SHIELD,
                        Icons.Default.Security,
                        if (currentLanguage == AppLanguage.BENGALI) "স্প্যাম শিল্ড" else "Shield"
                    ),
                    Triple(
                        AppTab.ANALYTICS,
                        Icons.Default.QueryStats,
                        if (currentLanguage == AppLanguage.BENGALI) "অ্যানালিটিক্স" else "Analytics"
                    )
                )

                navItems.forEach { (tab, icon, label) ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                when (tab) {
                    AppTab.KEYPAD -> DialerScreen(viewModel = viewModel)
                    AppTab.RECENTS -> RecentsScreen(viewModel = viewModel)
                    AppTab.CONTACTS -> ContactsScreen(viewModel = viewModel)
                    AppTab.SPAM_SHIELD -> SpamShieldScreen(
                        viewModel = viewModel,
                        onNavigateToCallBlocking = { showCallBlockingScreen = true }
                    )
                    AppTab.ANALYTICS -> AnalyticsScreen(viewModel = viewModel)
                }
            }
        }
    }

    // Full-screen Dedicated Call Blocking & Blacklist View
    if (showCallBlockingScreen) {
        CallBlockingScreen(
            viewModel = viewModel,
            onNavigateBack = { showCallBlockingScreen = false }
        )
    }

    // Active Call Dialog Overlay (Triggered during outgoing or answered call)
    if (activeCallState.isActive) {
        ActiveCallDialog(
            callState = activeCallState,
            onEndCall = { viewModel.endActiveCall() },
            onToggleMute = { viewModel.toggleMute() },
            onToggleSpeaker = { viewModel.toggleSpeaker() },
            onToggleHold = { viewModel.toggleHold() }
        )
    }

    // Incoming Call Ringing Overlay (Smart Caller ID live simulation)
    if (activeCallState.isIncomingRinging) {
        CallerIdOverlayDialog(
            callState = activeCallState,
            onAccept = { viewModel.acceptIncomingCall() },
            onDecline = { viewModel.declineIncomingCall() },
            onBlockAndReport = { viewModel.blockAndRejectIncomingCall() },
            onQuickSms = { msg -> viewModel.showSnackbar("SMS Sent: $msg") }
        )
    }

    // Architecture & Project Info Dialog
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Logo",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Smart Dialer & Caller ID", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Inspired by Sheikh Farid's repository (Smart-Dialer-Caller-ID)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• Architecture: MVVM with Jetpack Compose & Clean Architecture\n" +
                                "• Local Persistence: Room Database with reactive Kotlin StateFlow\n" +
                                "• Smart T9 Search: Instant keypad alphanumeric name and digit matching\n" +
                                "• Real-Time Caller ID: Prefix operator identification (Grameenphone, Banglalink, Robi, Airtel, Teletalk)\n" +
                                "• Community Spam Shield: Fraud & OTP phishing detection with one-tap blocking\n" +
                                "• Call Analytics: Comprehensive duration, frequency, and distribution telemetry\n" +
                                "• Bengali & English bilingual localized support",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                OutlinedButton(onClick = { showAboutDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}
