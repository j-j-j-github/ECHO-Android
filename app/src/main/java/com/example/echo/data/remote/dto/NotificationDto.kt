package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NotificationDto(
    val id: String,
    @SerialName("echo_id") val echoId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String, // Ensure this matches Supabase JSON
    @SerialName("echoes") val echo: ParentEchoDto? = null
)

@Serializable
data class ParentEchoDto(
    val content: String,
    @SerialName("signature_id") val signatureId: String
)