package com.example.domain.repository

import com.example.data.ai.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getChatHistory(scenarioId: String): Flow<List<ChatMessage>>
    suspend fun saveMessage(scenarioId: String, message: ChatMessage)
    suspend fun clearHistory(scenarioId: String)
}
