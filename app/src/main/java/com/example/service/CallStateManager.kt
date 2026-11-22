package com.example.service

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class CallStateEvent {
    data class IncomingRinging(
        val number: String,
        val simSlot: Int = 1,
        val callerDisplayName: String = ""
    ) : CallStateEvent()

    data class OutgoingDialing(
        val number: String,
        val simSlot: Int = 1
    ) : CallStateEvent()

    object CallAnswered : CallStateEvent()
    object CallEnded : CallStateEvent()
    object CallHistoryRefreshNeeded : CallStateEvent()
}

object CallStateManager {
    private val _events = MutableSharedFlow<CallStateEvent>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    fun emit(event: CallStateEvent) {
        _events.tryEmit(event)
    }
}
