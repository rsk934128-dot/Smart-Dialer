package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log

/**
 * Fallback listener for TelephonyManager.ACTION_PHONE_STATE_CHANGED when SmartInCallService
 * is not actively bound (e.g., if the user hasn't finished setting Default Dialer or OEM limits).
 * Strictly reacts to EXTRA_STATE_RINGING so outgoing calls never trigger false incoming alerts.
 */
class PhoneStateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "PhoneStateReceiver"
    }

    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != TelephonyManager.ACTION_PHONE_STATE_CHANGED) return

        // If SmartInCallService already owns the active Telecom Call, avoid duplicate handling.
        if (SmartInCallService.currentCall != null) return

        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return
        when (state) {
            TelephonyManager.EXTRA_STATE_RINGING -> {
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER).orEmpty()
                if (incomingNumber.isNotBlank()) {
                    Log.d(TAG, "Real incoming ringing detected via PHONE_STATE")
                    CallNotificationHelper.showIncomingCallNotification(
                        context = context,
                        number = incomingNumber,
                        callerName = incomingNumber,
                        simSlot = 1
                    )
                    CallStateManager.emit(
                        CallStateEvent.IncomingRinging(
                            number = incomingNumber,
                            simSlot = 1
                        )
                    )
                }
            }
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                CallNotificationHelper.cancelIncomingCallNotification(context)
            }
            TelephonyManager.EXTRA_STATE_IDLE -> {
                CallNotificationHelper.cancelIncomingCallNotification(context)
                CallStateManager.emit(CallStateEvent.CallHistoryRefreshNeeded)
            }
        }
    }
}
