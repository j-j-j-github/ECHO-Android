package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReplyDto(
    val id: String? = null,
    @SerialName("echo_id") val echoId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null
    // REMOVED: signatureId (It does not exist in your DB table)
)