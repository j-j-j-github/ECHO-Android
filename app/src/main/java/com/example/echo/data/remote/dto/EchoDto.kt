package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EchoDto(
    val id: String? = null,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("signature_id") val signatureId: String? = null,
    @SerialName("color_code") val colorCode: String? = null,
    val category: String? = null,
    val mood: Int? = null,
    val replies: List<ReplyDto>? = null,
    val likes: Int? = 0,
    @SerialName("is_related") val isRelated: Boolean? = false, // ✅ NEW: Tracks if the current user related to this
    @SerialName("is_spotlight") val isSpotlight: Boolean? = false,
    @SerialName("spotlight_status") val spotlightStatus: String? = "none",
    @SerialName("spotlight_expires_at") val spotlightExpiresAt: String? = null
)