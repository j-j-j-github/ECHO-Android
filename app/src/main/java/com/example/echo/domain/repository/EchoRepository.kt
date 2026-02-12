package com.example.echo.domain.repository

import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.NotificationDto
import kotlinx.coroutines.flow.Flow

interface EchoRepository {

    fun getEchoes(): Flow<List<EchoDto>>

    suspend fun createEcho(content: String, colorCode: String? = null)

    // --- NEW: EDITING METHODS ---
    suspend fun updateEcho(echoId: String, newContent: String)

    suspend fun updateReply(replyId: String, newContent: String)

    suspend fun createReply(echoId: String, content: String)

    suspend fun reportEcho(echoId: String)

    suspend fun getUserNotifications(): List<NotificationDto>

    suspend fun getEchoById(echoId: String): EchoDto?

    suspend fun hasUnreadNotifications(): Boolean

    fun markNotificationsAsRead()
}