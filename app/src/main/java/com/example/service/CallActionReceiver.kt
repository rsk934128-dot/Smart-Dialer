package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity

class CallActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_ANSWER_CALL = "com.example.action.ANSWER_CALL"
        const val ACTION_DECLINE_CALL = "com.example.action.DECLINE_CALL"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_ANSWER_CALL -> {
                CallNotificationHelper.cancelIncomingCallNotification(context)
                SmartInCallService.answerCurrentCall()
                CallStateManager.emit(CallStateEvent.CallAnswered)
                try {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    }
                    context.startActivity(launchIntent)
                } catch (_: Exception) {
                }
            }
            ACTION_DECLINE_CALL -> {
                CallNotificationHelper.cancelIncomingCallNotification(context)
                SmartInCallService.disconnectCurrentCall()
                CallStateManager.emit(CallStateEvent.CallEnded)
            }
        }
    }
}
