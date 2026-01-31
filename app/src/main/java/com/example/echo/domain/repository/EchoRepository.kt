package com.example.echo.domain.repository

import com.example.echo.data.remote.dto.EchoDto
import kotlinx.coroutines.flow.Flow

interface EchoRepository {
    fun getEchoes(): Flow<List<EchoDto>>
    suspend fun createEcho(content: String)

    // NEW: Add these two lines so the Impl can override them
    suspend fun createReply(echoId: String, content: String)
    suspend fun reportEcho(echoId: String)
}