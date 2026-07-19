package com.cinnamon.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinnamon.app.data.ai.AiConversationMode
import com.cinnamon.app.data.ai.AiGatewayDeliveryCertainty
import com.cinnamon.app.data.ai.AiGatewayClient
import com.cinnamon.app.data.ai.AiGatewayResult
import com.cinnamon.app.data.ai.ChatMessage
import com.cinnamon.app.data.ai.OfflineGuidedPractice
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiViewModel : ViewModel() {

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var conversationMode = AiConversationMode.NativeCoach
    private var scenarioId: String? = null

    /**
     * The client passes an allow-listed mode and catalog identifier, never a provider
     * system prompt. The gateway owns prompt policy when live coaching is enabled.
     */
    fun configureSession(mode: AiConversationMode, scenarioId: String? = null) {
        if (conversationMode != mode || this.scenarioId != scenarioId) {
            conversationMode = mode
            this.scenarioId = scenarioId
            _messages.value = emptyList()
        }
    }

    fun sendMessage(content: String) {
        val message = content.trim()
        if (message.isEmpty() || _isLoading.value) return

        viewModelScope.launch {
            val outgoingMessage = ChatMessage(role = "user", content = message)
            _messages.value = _messages.value + outgoingMessage
            _isLoading.value = true

            try {
                when (val result = AiGatewayClient.send(
                    mode = conversationMode,
                    message = message,
                    scenarioId = scenarioId,
                    idempotencyKey = outgoingMessage.id
                )) {
                    is AiGatewayResult.Success -> appendAssistantMessage(result.reply)
                    AiGatewayResult.NotConfigured,
                    AiGatewayResult.NotAuthenticated -> {
                        _errorEvents.emit(OfflineGuidedPractice.notSentNotice)
                        appendAssistantMessage(
                            OfflineGuidedPractice.responseWhenNotSent(conversationMode, scenarioId)
                        )
                    }
                    is AiGatewayResult.Failure -> {
                        when (result.error.deliveryCertainty) {
                            AiGatewayDeliveryCertainty.NotSent -> {
                                _errorEvents.emit(OfflineGuidedPractice.notSentNotice)
                                appendAssistantMessage(
                                    OfflineGuidedPractice.responseWhenNotSent(conversationMode, scenarioId)
                                )
                            }
                            AiGatewayDeliveryCertainty.ResponseReceived -> {
                                _errorEvents.emit(OfflineGuidedPractice.confirmedFailureNotice)
                                appendAssistantMessage(
                                    OfflineGuidedPractice.responseAfterConfirmedFailure(
                                        conversationMode,
                                        scenarioId
                                    )
                                )
                            }
                            AiGatewayDeliveryCertainty.UnknownAfterSend -> {
                                _errorEvents.emit(OfflineGuidedPractice.deliveryUnconfirmedNotice)
                                appendAssistantMessage(
                                    OfflineGuidedPractice.responseAfterUnconfirmedDelivery(
                                        conversationMode,
                                        scenarioId
                                    )
                                )
                            }
                        }
                    }
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun appendAssistantMessage(content: String) {
        _messages.value = _messages.value + ChatMessage(role = "assistant", content = content)
    }

    fun resetChat() {
        _messages.value = emptyList()
    }
}
