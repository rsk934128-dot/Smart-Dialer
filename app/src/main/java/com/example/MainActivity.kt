package com.example

import android.content.Intent
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.remote.CloudSyncRepository
import com.example.util.DefaultDialerManager
import com.example.util.PermissionHelper
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOutGoogle
import com.example.ui.components.ActiveCallDialog
import com.example.ui.components.CallerIdOverlayDialog
import com.example.ui.components.CloudAccountDialog
import com.example.ui.components.MissingPermissionsBanner
import com.example.ui.components.PermissionRationaleDialog
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.AuthScreen
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
import com.example.ui.viewmodel.CloudSyncViewModel
import com.example.ui.viewmodel.DialerViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: DialerViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        handleIncomingIntent(intent)

        setContent {
            MyApplicationTheme {
                val currentUser by Firebase.auth.authStateFlow()
                    .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)
                val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
                val activeCallState by viewModel.activeCallState.collectAsStateWithLifecycle()

                if (currentUser == null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AuthScreen(
                            language = currentLanguage,
                            onToggleLanguage = { viewModel.toggleLanguage() }
                        )

                        // Keep incoming/active carrier call overlays functional even on lock/auth screen
                        if (activeCallState.isActive) {
                            ActiveCallDialog(
                                callState = activeCallState,
                                language = currentLanguage,
                                onEndCall = {
                                    com.example.service.SmartInCallService.disconnectCurrentCall()
                                    viewModel.endActiveCall()
                                },
                                onToggleMute = { viewModel.toggleMute() },
                                onToggleSpeaker = { viewModel.toggleSpeaker() },
                                onToggleHold = { viewModel.toggleHold() }
                            )
                        }
                        if (activeCallState.isIncomingRinging) {
                            CallerIdOverlayDialog(
                                callState = activeCallState,
                                language = currentLanguage,
                                onAccept = {
                                    com.example.service.SmartInCallService.answerCurrentCall()
                                    viewModel.acceptIncomingCall()
                                },
                                onDecline = {
                                    com.example.service.SmartInCallService.disconnectCurrentCall()
                                    viewModel.declineIncomingCall()
                                },
                                onBlockAndReport = {
                                    com.example.service.SmartInCallService.disconnectCurrentCall()
                                    viewModel.blockAndRejectIncomingCall()
                                },
                                onQuickSms = { msg -> viewModel.showSnackbar("SMS Sent: $msg") }
                            )
                        }
                    }
                } else {
                    val authenticatedUser = currentUser!!
                    key(authenticatedUser.uid) {
                        val appContext = LocalContext.current.applicationContext
                        val cloudViewModel: CloudSyncViewModel = viewModel(
                            key = authenticatedUser.uid,
                            factory = viewModelFactory {
                                initializer {
                                    CloudSyncViewModel(
                                        cloudRepo = CloudSyncRepository(appContext),
                                        currentUserId = authenticatedUser.uid
                                    )
                                }
                            }
                        )
                        LaunchedEffect(authenticatedUser.uid) {
                            cloudViewModel.ensureProfile(authenticatedUser)
                        }
                        MainAppContent(
                            viewModel = viewModel,
                            cloudViewModel = cloudViewModel,
                            currentUser = authenticatedUser
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val callType = intent?.getStringExtra("EXTRA_CALL_TYPE")
        val number = intent?.getStringExtra("EXTRA_INCOMING_NUMBER")
        val simSlot = intent?.getIntExtra("EXTRA_SIM_SLOT", 1) ?: 1
        val callerName = intent?.getStringExtra("EXTRA_CALLER_NAME").orEmpty()
        if (callType == "INCOMING" && !number.isNullOrBlank()) {
            viewModel.handleIncomingCall(
                number = number,
                simSlot = simSlot,
                callerDisplayName = callerName
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    viewModel: DialerViewModel,
    cloudViewModel: CloudSyncViewModel,
    currentUser: FirebaseUser
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeCallState by viewModel.activeCallState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val inputNumber by viewModel.inputNumber.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val blockedNumbers by viewModel.blockedNumbers.collectAsState()
    val spamReports by viewModel.spamReports.collectAsState()

    val permissionStatus by viewModel.permissionStatus.collectAsStateWithLifecycle()
    val cloudSummaryState by cloudViewModel.cloudSummaryState.collectAsStateWithLifecycle()
    val isCloudBusy by cloudViewModel.isSyncBusy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAboutDialog by remember { mutableStateOf(false) }
    var showCallBlockingScreen by remember { mutableStateOf(false) }
    var showCloudAccountDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var isPermanentlyDenied by remember { mutableStateOf(false) }
    var dismissMissingPermissionsBanner by remember { mutableStateOf(false) }

    var isDefaultDialer by remember { mutableStateOf(DefaultDialerManager.isDefaultDialer(context)) }
    var dismissDefaultBanner by remember { mutableStateOf(false) }

    val corePermissions = remember { PermissionHelper.corePermissionsArray() }

    // Re-check status, SIM slots, and real call history whenever user returns to app
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultDialer = DefaultDialerManager.isDefaultDialer(context)
        viewModel.refreshSimsAndSyncHistory(showFeedback = false)
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val granted = DefaultDialerManager.isDefaultDialer(context)
        isDefaultDialer = granted
        if (granted) {
            viewModel.refreshSimsAndSyncHistory(showFeedback = false)
            viewModel.showSnackbar(
                if (currentLanguage == AppLanguage.BENGALI)
                    "স্মার্ট ডায়ালার সফলভাবে আপনার ডিফল্ট কলিং অ্যাপ হিসেবে সেট হয়েছে!"
                else
                    "Smart Dialer is now your default phone app!"
            )
        }
    }

    // Request telephony, dual-SIM, contacts, call-log & notification permissions gracefully
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshSimsAndSyncHistory(showFeedback = false)
        val remainingMissing = PermissionHelper.getMissingPermissions(context, corePermissions)
        isPermanentlyDenied = PermissionHelper.isPermanentlyDenied(context, remainingMissing)
    }

    fun triggerCorePermissionFlow() {
        val missing = PermissionHelper.getMissingPermissions(context, corePermissions)
        if (missing.isEmpty()) {
            viewModel.refreshSimsAndSyncHistory(showFeedback = true)
            return
        }
        val permDenied = PermissionHelper.isPermanentlyDenied(context, missing)
        if (permDenied) {
            isPermanentlyDenied = true
            showPermissionRationaleDialog = true
        } else {
            isPermanentlyDenied = false
            PermissionHelper.markPermissionsPrompted(context, missing)
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    LaunchedEffect(Unit) {
        val missing = PermissionHelper.getMissingPermissions(context, corePermissions)
        if (missing.isNotEmpty()) {
            val activity = with(PermissionHelper) { context.findActivity() }
            if (PermissionHelper.shouldShowRationale(activity, missing)) {
                isPermanentlyDenied = false
                showPermissionRationaleDialog = true
            } else if (!PermissionHelper.isPermanentlyDenied(context, missing)) {
                PermissionHelper.markPermissionsPrompted(context, missing)
                permissionLauncher.launch(missing.toTypedArray())
            } else {
                isPermanentlyDenied = true
            }
        } else {
            viewModel.refreshSimsAndSyncHistory(showFeedback = false)
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

    Box(modifier = Modifier.fillMaxSize()) {
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
                    // Default Dialer Role Status Chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isDefaultDialer) 
                            VerifiedTeal.copy(alpha = 0.15f) 
                        else 
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .testTag("badge_default_dialer_status"),
                        onClick = {
                            if (!isDefaultDialer) {
                                val intent = DefaultDialerManager.createDefaultDialerIntent(context)
                                defaultDialerLauncher.launch(intent)
                            } else {
                                viewModel.showSnackbar(
                                    if (currentLanguage == AppLanguage.BENGALI)
                                        "স্মার্ট ডায়ালার ইতিমধ্যে আপনার ডিফল্ট কলিং অ্যাপ হিসেবে সক্রিয়"
                                    else
                                        "Smart Dialer is already your default phone app"
                                )
                            }
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isDefaultDialer) Icons.Default.CheckCircle else Icons.Default.Phone,
                                contentDescription = "Default Status",
                                tint = if (isDefaultDialer) VerifiedTeal else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDefaultDialer) {
                                    if (currentLanguage == AppLanguage.BENGALI) "ডিফল্ট" else "Default"
                                } else {
                                    if (currentLanguage == AppLanguage.BENGALI) "ডিফল্ট করুন" else "Set Default"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDefaultDialer) VerifiedTeal else MaterialTheme.colorScheme.error
                            )
                        }
                    }

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

                    // Cloud Sync & Google Account manager
                    IconButton(
                        onClick = { showCloudAccountDialog = true },
                        modifier = Modifier.testTag("btn_cloud_account")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Cloud Backup & Account",
                            tint = VerifiedTeal
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Default Dialer Banner Callout
            if (!isDefaultDialer && !dismissDefaultBanner) {
                DefaultDialerBanner(
                    language = currentLanguage,
                    onSetDefault = {
                        val intent = DefaultDialerManager.createDefaultDialerIntent(context)
                        defaultDialerLauncher.launch(intent)
                    },
                    onOpenSettings = {
                        val intent = DefaultDialerManager.createDefaultAppsSettingsIntent(context)
                        context.startActivity(intent)
                    },
                    onDismiss = { dismissDefaultBanner = true }
                )
            }

            // Missing Core Permissions Banner (Call Logs, Phone State, Contacts)
            if (!permissionStatus.isCoreGranted && !dismissMissingPermissionsBanner) {
                MissingPermissionsBanner(
                    language = currentLanguage,
                    status = permissionStatus,
                    onRequestPermissions = { triggerCorePermissionFlow() },
                    onDismiss = { dismissMissingPermissionsBanner = true }
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                Crossfade(targetState = currentTab, label = "tab_crossfade") { tab ->
                    when (tab) {
                        AppTab.KEYPAD -> DialerScreen(viewModel = viewModel)
                        AppTab.RECENTS -> RecentsScreen(
                            viewModel = viewModel,
                            onOpenCloudSync = { showCloudAccountDialog = true }
                        )
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
            language = currentLanguage,
            onEndCall = {
                com.example.service.SmartInCallService.disconnectCurrentCall()
                viewModel.endActiveCall()
            },
            onToggleMute = { viewModel.toggleMute() },
            onToggleSpeaker = { viewModel.toggleSpeaker() },
            onToggleHold = { viewModel.toggleHold() }
        )
    }

    // Incoming Call Ringing Overlay (Smart Caller ID live simulation)
    if (activeCallState.isIncomingRinging) {
        CallerIdOverlayDialog(
            callState = activeCallState,
            language = currentLanguage,
            onAccept = {
                com.example.service.SmartInCallService.answerCurrentCall()
                viewModel.acceptIncomingCall()
            },
            onDecline = {
                com.example.service.SmartInCallService.disconnectCurrentCall()
                viewModel.declineIncomingCall()
            },
            onBlockAndReport = {
                com.example.service.SmartInCallService.disconnectCurrentCall()
                viewModel.blockAndRejectIncomingCall()
            },
            onQuickSms = { msg -> viewModel.showSnackbar("SMS Sent: $msg") }
        )
    }

    // Permission Rationale / App Settings Redirect Dialog
    if (showPermissionRationaleDialog) {
        PermissionRationaleDialog(
            language = currentLanguage,
            status = permissionStatus,
            isPermanentlyDenied = isPermanentlyDenied,
            onGrantPermissions = {
                showPermissionRationaleDialog = false
                val missing = PermissionHelper.getMissingPermissions(context, corePermissions)
                if (missing.isNotEmpty()) {
                    PermissionHelper.markPermissionsPrompted(context, missing)
                    permissionLauncher.launch(missing.toTypedArray())
                }
            },
            onOpenSettings = {
                showPermissionRationaleDialog = false
                context.startActivity(PermissionHelper.createAppSettingsIntent(context))
            },
            onDismiss = { showPermissionRationaleDialog = false }
        )
    }

    // Cloud Backup, Restore & Sign-Out Dialog
    if (showCloudAccountDialog) {
        CloudAccountDialog(
            user = currentUser,
            language = currentLanguage,
            cloudState = cloudSummaryState,
            isSyncBusy = isCloudBusy,
            onBackupNow = {
                cloudViewModel.backupToCloud(
                    contacts = contacts,
                    callLogs = callLogs,
                    blockedNumbers = blockedNumbers,
                    spamReports = spamReports
                ) { res ->
                    res.onSuccess { count ->
                        viewModel.showSnackbar(
                            if (currentLanguage == AppLanguage.BENGALI)
                                "ফায়ারবেস ক্লাউডে $count টি আইটেম ব্যাকআপ হয়েছে!"
                            else
                                "Backed up $count items to Firebase Cloud!"
                        )
                    }.onFailure { err ->
                        viewModel.showSnackbar(
                            if (currentLanguage == AppLanguage.BENGALI)
                                "ক্লাউড ব্যাকআপ ব্যর্থ: ${err.localizedMessage ?: "ত্রুটি"}"
                            else
                                "Cloud backup failed: ${err.localizedMessage ?: "Error"}"
                        )
                    }
                }
            },
            onRestoreNow = {
                cloudViewModel.restoreFromCloud(context) { res ->
                    res.onSuccess { restored ->
                        viewModel.showSnackbar(
                            if (currentLanguage == AppLanguage.BENGALI)
                                "ক্লাউড থেকে $restored টি নতুন ডেটা রিস্টোর হয়েছে!"
                            else
                                "Restored $restored items from Firebase Cloud!"
                        )
                    }.onFailure { err ->
                        viewModel.showSnackbar(
                            if (currentLanguage == AppLanguage.BENGALI)
                                "ক্লাউড রিস্টোর ব্যর্থ: ${err.localizedMessage ?: "ত্রুটি"}"
                            else
                                "Cloud restore failed: ${err.localizedMessage ?: "Error"}"
                        )
                    }
                }
            },
            onSignOut = {
                showCloudAccountDialog = false
                scope.launch {
                    signOutGoogle(context)
                }
            },
            onDismiss = { showCloudAccountDialog = false }
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
}

@Composable
fun DefaultDialerBanner(
    language: AppLanguage,
    onSetDefault: () -> Unit,
    onOpenSettings: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("banner_default_dialer"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Default Phone App",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (language == AppLanguage.BENGALI) "কল ডিফল্ট অ্যাপ হিসেবে সেট করুন" else "Set as Default Phone App",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (language == AppLanguage.BENGALI)
                        "ইনকামিং কলার আইডি, স্প্যাম ব্লকিং ও কল ডায়ালিং সক্রিয় করতে স্মার্ট ডায়ালারকে ডিফল্ট করুন"
                    else
                        "Enable live caller ID, automatic spam rejection, and direct calling",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    lineHeight = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onSetDefault,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_set_default_dialer")
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "ডিফল্ট করুন" else "Set Default",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (onOpenSettings != null) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("btn_banner_open_settings")
                        ) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "সেটিংস" else "Settings",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("btn_dismiss_default_banner")
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "পরে" else "Later",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

