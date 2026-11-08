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
import com.example.data.repository.NumberLookupResult
import com.example.util.CarrierLookupEngine
import com.example.util.T9SearchEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val isOnHold: Boolean = false
)

class DialerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: DialerRepository
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    init {
        val db = AppDatabase.getInstance(application, viewModelScope)
        repository = DialerRepository(db.appDao(), application)
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
            repository.syncDeviceContactsIfAvailable()
        }
    }

    // UI Navigation & Language
    private val _currentTab = MutableStateFlow(AppTab.KEYPAD)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _currentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

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
                        log.carrier.contains(search, ignoreCase = true)
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

    // Call Actions
    fun startCall(rawNumber: String) {
        if (rawNumber.isBlank()) return
        val number = rawNumber.trim()
        viewModelScope.launch {
            val lookup = repository.lookupNumber(number)
            if (lookup.isBlocked) {
                showSnackbar("Call cannot be placed: Number is in blocklist")
                return@launch
            }
            // Launch simulated active call
            _activeCallState.value = ActiveCallState(
                isActive = true,
                isIncomingRinging = false,
                callerName = lookup.displayName,
                number = number,
                carrier = lookup.carrierName,
                location = lookup.location,
                isSpam = lookup.isSpam,
                spamCategory = lookup.spamReport?.category ?: "",
                spamReportsCount = lookup.spamReport?.reportsCount ?: 0,
                riskLevel = lookup.riskLevel,
                durationSeconds = 0
            )
            startCallTimer()
            // Also attempt system intent to dial if running on physical device
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${Uri.encode(number)}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                getApplication<Application>().startActivity(intent)
            } catch (_: Exception) {
                // In sandbox or no CALL_PHONE permission granted yet; our in-app simulation takes over!
            }
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
        _activeCallState.value = ActiveCallState(
            isActive = false,
            isIncomingRinging = true,
            callerName = name,
            number = number,
            carrier = carrier.name,
            location = if (carrier.circle.isNotEmpty()) carrier.circle else carrier.country,
            isSpam = isSpam,
            spamCategory = category,
            spamReportsCount = reportsCount,
            riskLevel = if (isSpam) SpamRiskLevel.HIGH_RISK else SpamRiskLevel.SAFE,
            durationSeconds = 0
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

    fun acceptIncomingCall() {
        val current = _activeCallState.value
        _activeCallState.value = current.copy(
            isActive = true,
            isIncomingRinging = false,
            durationSeconds = 0
        )
        startCallTimer()
    }

    fun declineIncomingCall() {
        val current = _activeCallState.value
        viewModelScope.launch {
            repository.logCall(
                number = current.number,
                cachedName = current.callerName,
                type = if (current.isSpam) CallType.REJECTED else CallType.MISSED,
                durationSeconds = 0,
                carrier = current.carrier,
                location = current.location,
                isSpam = current.isSpam,
                spamCategory = current.spamCategory
            )
        }
        dismissCall()
        showSnackbar("Call declined")
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
            repository.logCall(
                number = current.number,
                cachedName = current.callerName,
                type = CallType.BLOCKED,
                durationSeconds = 0,
                carrier = current.carrier,
                location = current.location,
                isSpam = true,
                spamCategory = current.spamCategory
            )
        }
        dismissCall()
        showSnackbar("${current.number} blocked and reported as spam!")
    }

    fun endActiveCall() {
        val current = _activeCallState.value
        val duration = current.durationSeconds
        viewModelScope.launch {
            repository.logCall(
                number = current.number,
                cachedName = current.callerName,
                type = if (current.durationSeconds > 0) CallType.OUTGOING else CallType.MISSED,
                durationSeconds = duration,
                carrier = current.carrier,
                location = current.location,
                isSpam = current.isSpam,
                spamCategory = current.spamCategory
            )
        }
        dismissCall()
        showSnackbar("Call ended (${duration}s)")
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
