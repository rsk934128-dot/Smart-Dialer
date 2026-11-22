package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.ContactEntity
import com.example.data.model.SpamReportEntity
import com.example.data.remote.CloudBackupSummary
import com.example.data.remote.CloudSyncRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudUiState<out T> {
    data object Loading : CloudUiState<Nothing>
    data class Success<T>(val data: T) : CloudUiState<T>
    data class Error(val message: String) : CloudUiState<Nothing>
}

class CloudSyncViewModel(
    private val cloudRepo: CloudSyncRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _isSyncBusy = MutableStateFlow(false)
    val isSyncBusy: StateFlow<Boolean> = _isSyncBusy.asStateFlow()

    val cloudSummaryState: StateFlow<CloudUiState<CloudBackupSummary>> = combine(
        cloudRepo.observeUserProfile(currentUserId),
        cloudRepo.observeCloudCallLogs(currentUserId),
        cloudRepo.observeCloudContacts(currentUserId),
        cloudRepo.observeCloudBlockedNumbers(currentUserId),
        cloudRepo.observeCloudSpamReports(currentUserId)
    ) { profile, logs, contacts, blocked, spam ->
        CloudBackupSummary(
            profile = profile,
            callLogs = logs,
            contacts = contacts,
            blockedNumbers = blocked,
            spamReports = spam
        )
    }
        .map<CloudBackupSummary, CloudUiState<CloudBackupSummary>> { summary ->
            CloudUiState.Success(summary)
        }
        .catch { error ->
            val firestoreException = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreException != null) {
                emit(CloudUiState.Error(firestoreException.message ?: "Cloud sync unavailable"))
            } else {
                if (error is CancellationException) throw error
                emit(CloudUiState.Error(error.message ?: "Cloud sync error"))
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CloudUiState.Loading
        )

    fun ensureProfile(user: FirebaseUser) {
        viewModelScope.launch {
            cloudRepo.ensureUserProfile(user)
        }
    }

    fun backupToCloud(
        contacts: List<ContactEntity>,
        callLogs: List<CallLogEntity>,
        blockedNumbers: List<BlockedNumberEntity>,
        spamReports: List<SpamReportEntity>,
        onResult: (Result<Int>) -> Unit
    ) {
        if (_isSyncBusy.value) return
        viewModelScope.launch {
            _isSyncBusy.value = true
            try {
                val res = cloudRepo.backupAllToCloud(
                    contacts = contacts,
                    callLogs = callLogs,
                    blockedNumbers = blockedNumbers,
                    spamReports = spamReports
                )
                onResult(res)
            } finally {
                _isSyncBusy.value = false
            }
        }
    }

    fun restoreFromCloud(
        context: Context,
        onResult: (Result<Int>) -> Unit
    ) {
        if (_isSyncBusy.value) return
        viewModelScope.launch {
            _isSyncBusy.value = true
            try {
                val dao = AppDatabase.getInstance(context.applicationContext, viewModelScope).appDao()
                val res = cloudRepo.restoreFromCloudToRoom(dao)
                onResult(res)
            } finally {
                _isSyncBusy.value = false
            }
        }
    }
}
