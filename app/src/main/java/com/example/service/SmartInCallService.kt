package com.example.service

import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log
import com.example.MainActivity

/**
 * Required system InCallService implementation for Android default dialer role (ROLE_DIALER).
 * Handles incoming & ongoing telecom calls and links with the app's in-call UI.
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
                    // Call answered/received - start seconds timer!
                    CallStateManager.emit(CallStateEvent.CallAnswered)
                }
                Call.STATE_DISCONNECTED -> {
                    CallStateManager.emit(CallStateEvent.CallEnded)
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

        val number = call.details?.handle?.schemeSpecificPart ?: ""
        when (call.state) {
            Call.STATE_RINGING -> CallStateManager.emit(CallStateEvent.IncomingRinging(number))
            Call.STATE_DIALING, Call.STATE_CONNECTING -> CallStateManager.emit(CallStateEvent.OutgoingDialing(number))
            Call.STATE_ACTIVE -> CallStateManager.emit(CallStateEvent.CallAnswered)
        }

        // Launch MainActivity or bring UI to front when incoming call arrives
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("EXTRA_INCOMING_NUMBER", number)
                putExtra("EXTRA_CALL_STATE", call.state)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching incoming call UI", e)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d(TAG, "onCallRemoved")
        CallStateManager.emit(CallStateEvent.CallEnded)
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
        currentCall = null
        return super.onUnbind(intent)
    }
}
