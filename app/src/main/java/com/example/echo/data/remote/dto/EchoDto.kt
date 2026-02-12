package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EchoDto(
    val id: String? = null,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("signature_id") val signatureId: String? = null,
    @SerialName("color_code") val colorCode: String? = null, // ✅ NEW FIELD
    val replies: List<ReplyDto>? = null
)