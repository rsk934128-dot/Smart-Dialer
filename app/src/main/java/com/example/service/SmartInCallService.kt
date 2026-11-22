package com.example.service

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.example.MainActivity
import com.example.util.SimManager

/**
 * Required system InCallService implementation for Android default dialer role (ROLE_DIALER).
 * Handles incoming & ongoing telecom calls, extracts dual-SIM slot info, posts high-priority
 * full-screen incoming call notifications (Android 10+ background wake), and links with UI.
 */
class SmartInCallService : InCallService() {

    companion object {
        private const val TAG = "SmartInCallService"

        @Volatile
        var currentCall: Call? = null
            private set

        fun answerCurrentCall() {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    currentCall?.answer(0)
                } else {
                    @Suppress("DEPRECATION")
                    currentCall?.answer(0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to answer call", e)
            }
        }

        fun disconnectCurrentCall() {
            try {
                currentCall?.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disconnect call", e)
            }
        }
    }

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            Log.d(TAG, "Call state changed: $state")
            when (state) {
                Call.STATE_ACTIVE -> {
                    CallNotificationHelper.cancelIncomingCallNotification(this@SmartInCallService)
                    CallStateManager.emit(CallStateEvent.CallAnswered)
                }
                Call.STATE_DISCONNECTED -> {
                    CallNotificationHelper.cancelIncomingCallNotification(this@SmartInCallService)
                    CallStateManager.emit(CallStateEvent.CallEnded)
                    CallStateManager.emit(CallStateEvent.CallHistoryRefreshNeeded)
                    if (currentCall == call) {
                        currentCall = null
                    }
                }
            }
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d(TAG, "onCallAdded: state=${call.state}")
        currentCall = call
        call.registerCallback(callCallback)

        val number = call.details?.handle?.schemeSpecificPart.orEmpty()
        val callerDisplayName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            call.details?.callerDisplayName.orEmpty()
        } else {
            ""
        }
        val simSlot = SimManager.resolveSimSlot(this, call.details?.accountHandle)

        val isIncoming = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            call.details?.callDirection == Call.Details.DIRECTION_INCOMING || call.state == Call.STATE_RINGING
        } else {
            call.state == Call.STATE_RINGING
        }

        if (isIncoming && call.state == Call.STATE_RINGING) {
            // Post Full-Screen Notification first so Android 10+ wakes lockscreen / background
            CallNotificationHelper.showIncomingCallNotification(
                context = this,
                number = number,
                callerName = callerDisplayName.ifBlank { number },
                simSlot = simSlot
            )
            CallStateManager.emit(
                CallStateEvent.IncomingRinging(
                    number = number,
                    simSlot = simSlot,
                    callerDisplayName = callerDisplayName
                )
            )
            // Launch MainActivity directly for foreground responsiveness
            try {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("EXTRA_CALL_TYPE", "INCOMING")
                    putExtra("EXTRA_INCOMING_NUMBER", number)
                    putExtra("EXTRA_SIM_SLOT", simSlot)
                    putExtra("EXTRA_CALLER_NAME", callerDisplayName)
                    putExtra("EXTRA_CALL_STATE", call.state)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Background launch fell back to full-screen notification", e)
            }
        } else if (call.state == Call.STATE_DIALING || call.state == Call.STATE_CONNECTING) {
            CallStateManager.emit(CallStateEvent.OutgoingDialing(number, simSlot))
        } else if (call.state == Call.STATE_ACTIVE) {
            CallNotificationHelper.cancelIncomingCallNotification(this)
            CallStateManager.emit(CallStateEvent.CallAnswered)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved")
        CallNotificationHelper.cancelIncomingCallNotification(this)
        CallStateManager.emit(CallStateEvent.CallEnded)
        CallStateManager.emit(CallStateEvent.CallHistoryRefreshNeeded)
        call.unregisterCallback(callCallback)
        if (currentCall == call) {
            currentCall = null
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.d(TAG, "onBind Telecom InCallService")
        return super.onBind(intent)
    }

    override fun onUnbind(intent: Intent): Boolean {
        Log.d(TAG, "onUnbind Telecom InCallService")
        CallNotificationHelper.cancelIncomingCallNotification(this)
        currentCall = null
        return super.onUnbind(intent)
    }
}
