package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("echo_id") val echoId: String,
    val content: String, // The Reply content
    @SerialName("created_at") val createdAt: String,

    // Nested object from the "Join" query
    val echoes: EchoSummaryDto? = null
)

@Serializable
data class EchoSummaryDto(
    val content: String // The Original Echo content
)