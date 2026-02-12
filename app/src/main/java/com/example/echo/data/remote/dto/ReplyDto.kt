package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReplyDto(
    val id: String? = null,
    @SerialName("echo_id") val echoId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("signature_id") val signatureId: String? = null, // ✅ Added back for the new column
    @SerialName("updated_at") val updatedAt: String? = null
)