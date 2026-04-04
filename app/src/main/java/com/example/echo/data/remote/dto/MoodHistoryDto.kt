package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoodHistoryDto(
    // ✅ Switched to String? so it won't crash if Supabase sends a UUID
    val id: String? = null,

    @SerialName("signature_id")
    val signatureId: String? = null,

    val mood: Int? = null,

    // ✅ Added a default empty string so a missing date doesn't crash the whole list
    @SerialName("created_at")
    val createdAt: String = ""
)