package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.model.SpamReportEntity
import com.example.data.model.SpamRiskLevel
import com.example.data.repository.DialerRepository
import com.example.service.CallStateEvent
import com.example.service.CallStateManager
import com.example.util.CarrierLookupEngine
import com.example.util.DefaultDialerManager
import com.example.util.DialerPermissionsStatus
import com.example.util.PermissionHelper
import com.example.util.SimManager
import com.example.util.SimSlotInfo
import com.example.util.T9SearchEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab {
    KEYPAD,
    RECENTS,
    CONTACTS,
    SPAM_SHIELD,
    ANALYTICS
}

enum class RecentsFilter {
    ALL,
    MISSED,
    INCOMING,
    OUTGOING,
    BLOCKED
}

enum class AppLanguage {
    ENGLISH,
    BENGALI
}

data class ActiveCallState(
    val isActive: Boolean = false,
    val isIncomingRinging: Boolean = false,
    val isCallAnswered: Boolean = false,
    val callerName: String = "",
    val number: String = "",
    val carrier: String = "",
    val location: String = "",
    val isSpam: Boolean = false,
    val spamCategory: String = "",
    val spamReportsCount: Int = 0,
    val riskLevel: SpamRiskLevel = SpamRiskLevel.SAFE,
    val durationSeconds: Long = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isOnHold: Boolean = false,
    val simSlot: Int = 1,
    val simCarrierLabel: String = "",
    val isSystemCall: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class DialerViewModel(application: Application) : AndroidViewModel(application) {

    private val database: AppDatabase = AppDatabase.getInstance(application, viewModelScope)
    private val repository: DialerRepository = DialerRepository(
        appDao = database.appDao(),
        context = application,
        callLogDao = database.callLogDao()
    )
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    // UI Navigation & Language
    private val _currentTab = MutableStateFlow(AppTab.KEYPAD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    // Dual-SIM State
    private val _availableSims = MutableStateFlow<List<SimSlotInfo>>(emptyList())
    val availableSims: StateFlow<List<SimSlotInfo>> = _availableSims.asStateFlow()

    private val _selectedSimSlot = MutableStateFlow(1)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    private val _hasPhonePermission = MutableStateFlow(false)
    val hasPhonePermission: StateFlow<Boolean> = _hasPhonePermission.asStateFlow()

    private val _hasCallLogPermission = MutableStateFlow(false)
    val hasCallLogPermission: StateFlow<Boolean> = _hasCallLogPermission.asStateFlow()

    private val _hasContactsPermission = MutableStateFlow(false)
    val hasContactsPermission: StateFlow<Boolean> = _hasContactsPermission.asStateFlow()

    private val _permissionStatus = MutableStateFlow(PermissionHelper.checkStatus(application))
    val permissionStatus: StateFlow<DialerPermissionsStatus> = _permissionStatus.asStateFlow()

    private val _isSyncingCallLogs = MutableStateFlow(false)
    val isSyncingCallLogs: StateFlow<Boolean> = _isSyncingCallLogs.asStateFlow()

    // Keypad Input & T9 Matching
    private val _inputNumber = MutableStateFlow("")
    val inputNumber: StateFlow<String> = _inputNumber.asStateFlow()

    private val _isKeypadVisible = MutableStateFlow(true)
    val isKeypadVisible: StateFlow<Boolean> = _isKeypadVisible.asStateFlow()

    // Recents Filter
    private val _recentsFilter = MutableStateFlow(RecentsFilter.ALL)
    val recentsFilter: StateFlow<RecentsFilter> = _recentsFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Active Call & Simulation
    private val _activeCallState = MutableStateFlow(ActiveCallState())
    val activeCallState: StateFlow<ActiveCallState> = _activeCallState.asStateFlow()

    private var callTimerJob: Job? = null

    // Notifications / Snackbars
    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Data streams from repository
    val contacts: StateFlow<List<ContactEntity>> = repository.allContacts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val favorites: StateFlow<List<ContactEntity>> = repository.favoriteContacts.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val callLogs: StateFlow<List<CallLogEntity>> = repository.allCallLogs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val spamReports: StateFlow<List<SpamReportEntity>> = repository.allSpamReports.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    val blockedNumbers: StateFlow<List<BlockedNumberEntity>> = repository.blockedNumbers.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_DTMF, 60)
        } catch (_: Exception) {
            toneGenerator = null
        }
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = application.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        viewModelScope.launch {
            refreshSimsAndSyncHistory(showFeedback = false)
        }

        viewModelScope.launch {
            CallStateManager.events.collect { event ->
                when (event) {
                    is CallStateEvent.IncomingRinging -> {
                        if (!_activeCallState.value.isActive && !_activeCallState.value.isIncomingRinging) {
                            handleIncomingCall(
                                number = event.number,
                                simSlot = event.simSlot,
                                callerDisplayName = event.callerDisplayName
                            )
                        }
                    }
                    is CallStateEvent.OutgoingDialing -> {
                        if (_activeCallState.value.isActive) {
                            val simCarrier = _availableSims.value
                                .firstOrNull { it.slotNumber == event.simSlot }
                                ?.carrierName
                                .orEmpty()
                            _activeCallState.value = _activeCallState.value.copy(
                                simSlot = event.simSlot,
                                simCarrierLabel = simCarrier.ifBlank { _activeCallState.value.simCarrierLabel },
                                isSystemCall = true
                            )
                        }
                    }
                    is CallStateEvent.CallAnswered -> onCallAnswered()
                    is CallStateEvent.CallEnded -> {
                        if (_activeCallState.value.isActive && _activeCallState.value.isCallAnswered) {
                            endActiveCall()
                        } else if (_activeCallState.value.isActive && _activeCallState.value.isSystemCall) {
                            dismissCall()
                            scheduleRealCallLogSync()
                        } else if (_activeCallState.value.isIncomingRinging) {
                            dismissCall()
                            scheduleRealCallLogSync()
                        }
                    }
                    is CallStateEvent.CallHistoryRefreshNeeded -> {
                        scheduleRealCallLogSync()
                    }
                }
            }
        }

        viewModelScope.launch {
            _hasCallLogPermission
                .flatMapLatest { granted ->
                    if (granted) {
                        repository.callHistoryRepository?.observeSystemCallLogChanges() ?: emptyFlow()
                    } else {
                        emptyFlow()
                    }
                }
                .collect {
                    scheduleRealCallLogSync()
                }
        }
    }

    // T9 search results combining keypad input and all contacts
    val t9Matches: StateFlow<List<T9SearchEngine.T9MatchResult>> = combine(
        _inputNumber,
        contacts
    ) { query, allContacts ->
        if (query.isEmpty()) {
            emptyList()
        } else {
            T9SearchEngine.filterContacts(query, allContacts)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered call logs
    val filteredCallLogs: StateFlow<List<CallLogEntity>> = combine(
        callLogs,
        _recentsFilter,
        _searchQuery
    ) { logs, filter, search ->
        logs.filter { log ->
            val matchesFilter = when (filter) {
                RecentsFilter.ALL -> true
                RecentsFilter.MISSED -> log.type == CallType.MISSED
                RecentsFilter.INCOMING -> log.type == CallType.INCOMING
                RecentsFilter.OUTGOING -> log.type == CallType.OUTGOING
                RecentsFilter.BLOCKED -> log.type == CallType.BLOCKED || log.isSpam
            }
            val matchesSearch = if (search.isBlank()) true else {
                log.number.contains(search, ignoreCase = true) ||
                    log.cachedName.contains(search, ignoreCase = true) ||
                    log.carrier.contains(search, ignoreCase = true) ||
                    "sim ${log.simSlot}".contains(search, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    fun setLanguage(lang: AppLanguage) {
        _currentLanguage.value = lang
    }

    fun toggleLanguage() {
        _currentLanguage.value = if (_currentLanguage.value == AppLanguage.ENGLISH) {
            AppLanguage.BENGALI
        } else {
            AppLanguage.ENGLISH
        }
    }

    fun selectSimSlot(slotNumber: Int) {
        _selectedSimSlot.value = slotNumber.coerceIn(1, 2)
    }

    fun refreshSimsAndSyncHistory(showFeedback: Boolean = false) {
        val app = getApplication<Application>()
        val status = PermissionHelper.checkStatus(app)
        _permissionStatus.value = status
        _hasPhonePermission.value = status.hasPhoneState
        _hasCallLogPermission.value = status.hasCallLog
        _hasContactsPermission.value = status.hasContacts

        val detectedSims = SimManager.getAvailableSims(app)
        _availableSims.value = detectedSims
        if (detectedSims.isNotEmpty() && detectedSims.none { it.slotNumber == _selectedSimSlot.value }) {
            _selectedSimSlot.value = detectedSims.first().slotNumber
        }

        viewModelScope.launch {
            _isSyncingCallLogs.value = true
            try {
                repository.syncDeviceContactsIfAvailable()
                val importedCount = repository.syncDeviceCallLogsIfAvailable()
                if (showFeedback) {
                    val msg = if (_currentLanguage.value == AppLanguage.BENGALI) {
                        if (importedCount > 0) {
                            "রিয়েল কল হিস্টরি থেকে $importedCount টি কল ব্যাকআপ সম্পন্ন হয়েছে"
                        } else if (!status.hasCallLog) {
                            "কল হিস্টরি আনতে Call Log পারমিশন অনুমতি দিন"
                        } else {
                            "সমস্ত রিয়েল কল হিস্টরি ইতিমধ্যে ব্যাকআপ করা আছে"
                        }
                    } else {
                        if (importedCount > 0) {
                            "Backed up $importedCount real calls from device history"
                        } else if (!status.hasCallLog) {
                            "Grant Call Log permission to backup device call history"
                        } else {
                            "Device call history is already up to date"
                        }
                    }
                    showSnackbar(msg)
                }
            } finally {
                _isSyncingCallLogs.value = false
            }
        }
    }

    fun syncContactsNow(showFeedback: Boolean = true) {
        val app = getApplication<Application>()
        val status = PermissionHelper.checkStatus(app)
        _permissionStatus.value = status
        _hasContactsPermission.value = status.hasContacts

        viewModelScope.launch {
            val importedContacts = repository.syncDeviceContactsIfAvailable()
            if (showFeedback) {
                val msg = if (_currentLanguage.value == AppLanguage.BENGALI) {
                    if (!status.hasContacts) {
                        "ফোনের কন্টাক্ট সিঙ্ক করতে Contacts পারমিশন দিন"
                    } else if (importedContacts > 0) {
                        "ফোনবুক থেকে $importedContacts টি কন্টাক্ট ইমপোর্ট হয়েছে"
                    } else {
                        "সব কন্টাক্ট ইতিমধ্যে সিঙ্ক করা আছে"
                    }
                } else {
                    if (!status.hasContacts) {
                        "Grant Contacts permission to sync your phonebook"
                    } else if (importedContacts > 0) {
                        "Imported $importedContacts contacts from device"
                    } else {
                        "All device contacts are already synced"
                    }
                }
                showSnackbar(msg)
            }
        }
    }

    private fun scheduleRealCallLogSync() {
        viewModelScope.launch {
            delay(700)
            repository.syncDeviceCallLogsIfAvailable()
        }
    }

    fun setRecentsFilter(filter: RecentsFilter) {
        _recentsFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleKeypadVisibility() {
        _isKeypadVisible.value = !_isKeypadVisible.value
    }

    fun setKeypadVisible(visible: Boolean) {
        _isKeypadVisible.value = visible
    }

    // Keypad actions
    fun appendDigit(digit: Char) {
        playKeypadFeedback(digit)
        _inputNumber.value = _inputNumber.value + digit
    }

    fun deleteDigit() {
        playHapticFeedback()
        val current = _inputNumber.value
        if (current.isNotEmpty()) {
            _inputNumber.value = current.dropLast(1)
        }
    }

    fun clearInput() {
        playHapticFeedback()
        _inputNumber.value = ""
    }

    fun setInputNumber(number: String) {
        _inputNumber.value = number
    }

    private fun playKeypadFeedback(char: Char) {
        playHapticFeedback()
        try {
            val tone = when (char) {
                '1' -> ToneGenerator.TONE_DTMF_1
                '2' -> ToneGenerator.TONE_DTMF_2
                '3' -> ToneGenerator.TONE_DTMF_3
                '4' -> ToneGenerator.TONE_DTMF_4
                '5' -> ToneGenerator.TONE_DTMF_5
                '6' -> ToneGenerator.TONE_DTMF_6
                '7' -> ToneGenerator.TONE_DTMF_7
                '8' -> ToneGenerator.TONE_DTMF_8
                '9' -> ToneGenerator.TONE_DTMF_9
                '0' -> ToneGenerator.TONE_DTMF_0
                '*' -> ToneGenerator.TONE_DTMF_S
                '#' -> ToneGenerator.TONE_DTMF_P
                else -> ToneGenerator.TONE_PROP_BEEP
            }
            toneGenerator?.startTone(tone, 80)
        } catch (_: Exception) {}
    }

    private fun playHapticFeedback() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }

    // Call Actions with SIM 1 / SIM 2 selection
    fun startCall(rawNumber: String, overrideSimSlot: Int? = null) {
        if (rawNumber.isBlank()) return
        val number = rawNumber.trim()
        val app = getApplication<Application>()

        val targetSlot = overrideSimSlot ?: _selectedSimSlot.value
        val selectedSim = _availableSims.value.firstOrNull { it.slotNumber == targetSlot }
            ?: _availableSims.value.firstOrNull()

        viewModelScope.launch {
            val lookup = repository.lookupNumber(number)
            if (lookup.isBlocked) {
                showSnackbar(
                    if (_currentLanguage.value == AppLanguage.BENGALI)
                        "ব্লকলিস্টে থাকায় এই নাম্বারে কল করা যাচ্ছে না"
                    else
                        "Call cannot be placed: Number is in blocklist"
                )
                return@launch
            }

            val isDefaultDialer = DefaultDialerManager.isDefaultDialer(app)
            val placedViaSystem = SimManager.placeOutgoingCall(app, number, selectedSim)

            val simSlot = selectedSim?.slotNumber ?: targetSlot
            val simCarrier = selectedSim?.carrierName.orEmpty()

            if (placedViaSystem && !isDefaultDialer) {
                // External OS dialer handles the call; record locally if CallLog access is unavailable
                if (!repository.hasCallLogPermission()) {
                    repository.logCall(
                        number = number,
                        cachedName = lookup.displayName,
                        type = CallType.OUTGOING,
                        durationSeconds = 0,
                        carrier = lookup.carrierName,
                        location = lookup.location,
                        isSpam = lookup.isSpam,
                        spamCategory = lookup.spamReport?.category ?: "",
                        simSlot = simSlot
                    )
                }
                return@launch
            }

            _activeCallState.value = ActiveCallState(
                isActive = true,
                isIncomingRinging = false,
                isCallAnswered = false,
                callerName = lookup.displayName,
                number = number,
                carrier = lookup.carrierName,
                location = lookup.location,
                isSpam = lookup.isSpam,
                spamCategory = lookup.spamReport?.category ?: "",
                spamReportsCount = lookup.spamReport?.reportsCount ?: 0,
                riskLevel = lookup.riskLevel,
                durationSeconds = 0,
                simSlot = simSlot,
                simCarrierLabel = simCarrier,
                isSystemCall = placedViaSystem && isDefaultDialer
            )

            // Only run simulated 2.5s answer when NOT in a real Android Telecom call (e.g. emulator/sandbox)
            if (!(placedViaSystem && isDefaultDialer)) {
                viewModelScope.launch {
                    delay(2500)
                    if (_activeCallState.value.isActive && !_activeCallState.value.isCallAnswered) {
                        onCallAnswered()
                    }
                }
            }
        }
    }

    fun handleIncomingCall(
        number: String,
        simSlot: Int = 1,
        callerDisplayName: String = ""
    ) {
        viewModelScope.launch {
            val lookup = repository.lookupNumber(number)
            val resolvedName = if (lookup.isKnownContact || lookup.isSpam) {
                lookup.displayName
            } else {
                callerDisplayName.ifBlank { lookup.displayName }
            }
            val simCarrier = _availableSims.value
                .firstOrNull { it.slotNumber == simSlot }
                ?.carrierName
                .orEmpty()

            _activeCallState.value = ActiveCallState(
                isActive = false,
                isIncomingRinging = true,
                isCallAnswered = false,
                callerName = resolvedName,
                number = number,
                carrier = lookup.carrierName,
                location = lookup.location,
                isSpam = lookup.isSpam,
                spamCategory = lookup.spamReport?.category ?: "",
                spamReportsCount = lookup.spamReport?.reportsCount ?: 0,
                riskLevel = lookup.riskLevel,
                durationSeconds = 0,
                simSlot = simSlot.coerceIn(1, 2),
                simCarrierLabel = simCarrier,
                isSystemCall = true
            )
        }
    }

    // Simulate an incoming call (e.g. for Caller ID demo)
    fun simulateIncomingCall(
        number: String = "+880 1899-001122",
        name: String = "Fake Bank KYC Agent",
        isSpam: Boolean = true,
        category: String = "Scammer",
        reportsCount: Int = 1420
    ) {
        val carrier = CarrierLookupEngine.lookup(number)
        val slot = _selectedSimSlot.value
        val simCarrier = _availableSims.value.firstOrNull { it.slotNumber == slot }?.carrierName.orEmpty()
        _activeCallState.value = ActiveCallState(
            isActive = false,
            isIncomingRinging = true,
            isCallAnswered = false,
            callerName = name,
            number = number,
            carrier = carrier.name,
            location = if (carrier.circle.isNotEmpty()) carrier.circle else carrier.country,
            isSpam = isSpam,
            spamCategory = category,
            spamReportsCount = reportsCount,
            riskLevel = if (isSpam) SpamRiskLevel.HIGH_RISK else SpamRiskLevel.SAFE,
            durationSeconds = 0,
            simSlot = slot,
            simCarrierLabel = simCarrier,
            isSystemCall = false
        )
    }

    fun simulateSafeIncomingCall() {
        simulateIncomingCall(
            number = "+880 1712-345678",
            name = "Sheikh Farid",
            isSpam = false,
            category = "Verified Contact",
            reportsCount = 0
        )
    }

    fun onCallAnswered() {
        val current = _activeCallState.value
        if (current.isCallAnswered) return
        _activeCallState.value = current.copy(
            isActive = true,
            isIncomingRinging = false,
            isCallAnswered = true,
            durationSeconds = 0
        )
        startCallTimer()
    }

    fun acceptIncomingCall() {
        onCallAnswered()
    }

    fun declineIncomingCall() {
        val current = _activeCallState.value
        viewModelScope.launch {
            if (!current.isSystemCall || !repository.hasCallLogPermission()) {
                repository.logCall(
                    number = current.number,
                    cachedName = current.callerName,
                    type = if (current.isSpam) CallType.REJECTED else CallType.MISSED,
                    durationSeconds = 0,
                    carrier = current.carrier,
                    location = current.location,
                    isSpam = current.isSpam,
                    spamCategory = current.spamCategory,
                    simSlot = current.simSlot
                )
            } else {
                scheduleRealCallLogSync()
            }
        }
        dismissCall()
        showSnackbar(if (_currentLanguage.value == AppLanguage.BENGALI) "কল কাটা হয়েছে" else "Call declined")
    }

    fun blockAndRejectIncomingCall() {
        val current = _activeCallState.value
        viewModelScope.launch {
            repository.reportSpam(
                number = current.number,
                name = current.callerName,
                category = if (current.spamCategory.isNotEmpty()) current.spamCategory else "Scammer",
                notes = "Blocked from live Caller ID popup"
            )
            if (!current.isSystemCall || !repository.hasCallLogPermission()) {
                repository.logCall(
                    number = current.number,
                    cachedName = current.callerName,
                    type = CallType.BLOCKED,
                    durationSeconds = 0,
                    carrier = current.carrier,
                    location = current.location,
                    isSpam = true,
                    spamCategory = current.spamCategory,
                    simSlot = current.simSlot
                )
            } else {
                scheduleRealCallLogSync()
            }
        }
        dismissCall()
        showSnackbar("${current.number} blocked and reported as spam!")
    }

    fun endActiveCall() {
        val current = _activeCallState.value
        val duration = current.durationSeconds
        viewModelScope.launch {
            if (current.isSystemCall && repository.hasCallLogPermission()) {
                delay(700)
                val synced = repository.syncDeviceCallLogsIfAvailable()
                if (synced == 0 && current.number.isNotBlank()) {
                    repository.logCall(
                        number = current.number,
                        cachedName = current.callerName,
                        type = if (duration > 0) CallType.OUTGOING else CallType.MISSED,
                        durationSeconds = duration,
                        carrier = current.carrier,
                        location = current.location,
                        isSpam = current.isSpam,
                        spamCategory = current.spamCategory,
                        simSlot = current.simSlot
                    )
                }
            } else if (current.number.isNotBlank()) {
                repository.logCall(
                    number = current.number,
                    cachedName = current.callerName,
                    type = if (duration > 0) CallType.OUTGOING else CallType.MISSED,
                    durationSeconds = duration,
                    carrier = current.carrier,
                    location = current.location,
                    isSpam = current.isSpam,
                    spamCategory = current.spamCategory,
                    simSlot = current.simSlot
                )
            }
        }
        dismissCall()
        showSnackbar(
            if (_currentLanguage.value == AppLanguage.BENGALI) "কল শেষ হয়েছে (${duration}s)"
            else "Call ended (${duration}s)"
        )
    }

    private fun dismissCall() {
        callTimerJob?.cancel()
        callTimerJob = null
        _activeCallState.value = ActiveCallState()
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _activeCallState.value = _activeCallState.value.copy(
                    durationSeconds = _activeCallState.value.durationSeconds + 1
                )
            }
        }
    }

    // In-call toggles
    fun toggleMute() {
        _activeCallState.value = _activeCallState.value.copy(
            isMuted = !_activeCallState.value.isMuted
        )
    }

    fun toggleSpeaker() {
        _activeCallState.value = _activeCallState.value.copy(
            isSpeakerOn = !_activeCallState.value.isSpeakerOn
        )
    }

    fun toggleHold() {
        _activeCallState.value = _activeCallState.value.copy(
            isOnHold = !_activeCallState.value.isOnHold
        )
    }

    // Contact Management
    fun createContact(name: String, number: String, email: String = "", company: String = "") {
        viewModelScope.launch {
            val newContact = ContactEntity(
                name = name.trim(),
                phoneNumber = number.trim(),
                email = email.trim(),
                company = company.trim(),
                avatarColorIndex = (0..7).random()
            )
            repository.addContact(newContact)
            showSnackbar("Contact saved: $name")
        }
    }

    fun toggleFavorite(contact: ContactEntity) {
        viewModelScope.launch {
            repository.updateContact(contact.copy(isFavorite = !contact.isFavorite))
        }
    }

    fun deleteContact(contact: ContactEntity) {
        viewModelScope.launch {
            repository.deleteContact(contact)
            showSnackbar("Contact removed: ${contact.name}")
        }
    }

    // Spam & Blocklist
    fun blockNumber(number: String, name: String = "", reason: String = "Manual Block") {
        viewModelScope.launch {
            repository.blockNumber(number, name, reason)
            showSnackbar("Number blocked: $number")
        }
    }

    fun unblockNumber(number: String) {
        viewModelScope.launch {
            repository.unblockNumber(number)
            showSnackbar("Unblocked $number")
        }
    }

    fun reportSpamNumber(number: String, name: String, category: String, notes: String = "") {
        viewModelScope.launch {
            repository.reportSpam(number, name, category, notes, autoBlock = true)
            showSnackbar("Spam reported: $name ($category)")
        }
    }

    fun deleteCallLog(id: Long) {
        viewModelScope.launch {
            repository.deleteCallLog(id)
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearCallLogs()
            showSnackbar("Call history cleared")
        }
    }

    fun sendSms(context: Context, number: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${Uri.encode(number)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            showSnackbar("Cannot open SMS client")
        }
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
        toneGenerator = null
        callTimerJob?.cancel()
    }
}
