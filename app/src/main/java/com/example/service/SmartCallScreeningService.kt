package com.example.service

import android.os.Build
import android.telecom.Call
import android.telecom.CallScreeningService
import com.example.data.db.AppDatabase
import com.example.util.SimManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Screens real incoming carrier calls on Android 7.0+ (and direction-checked on Android 10+).
 * Automatically rejects calls from user-blocked numbers and passes legitimate calls cleanly to SmartInCallService.
 */
class SmartCallScreeningService : CallScreeningService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onScreenCall(callDetails: Call.Details) {
        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        } else {
            true
        }

        val rawNumber = callDetails.handle?.schemeSpecificPart.orEmpty()
        if (!isIncoming || rawNumber.isBlank()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        val cleanNumber = rawNumber.replace("[^0-9+]".toRegex(), "")
        val isBlocked = runBlocking {
            withTimeoutOrNull(750L) {
                try {
                    val dao = AppDatabase.getInstance(applicationContext, serviceScope).appDao()
                    dao.isNumberBlocked(rawNumber) || (cleanNumber.isNotEmpty() && dao.isNumberBlocked(cleanNumber))
                } catch (_: Exception) {
                    false
                }
            } ?: false
        }

        if (isBlocked) {
            val rejectResponse = CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(false)
                .setSkipNotification(true)
                .build()
            respondToCall(callDetails, rejectResponse)
            CallStateManager.emit(CallStateEvent.CallHistoryRefreshNeeded)
        } else {
            val simSlot = SimManager.resolveSimSlot(this, callDetails.accountHandle)
            respondToCall(callDetails, CallResponse.Builder().build())
            CallStateManager.emit(
                CallStateEvent.IncomingRinging(
                    number = rawNumber,
                    simSlot = simSlot,
                    callerDisplayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        callDetails.callerDisplayName.orEmpty()
                    } else {
                        ""
                    }
                )
            )
        }
    }
}
