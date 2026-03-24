package com.example.echo.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserStatsDto(
    @SerialName("signature_id")
    val signatureId: String,

    @SerialName("total_echoes_posted")
    val totalEchoesPosted: Int = 0,

    @SerialName("total_resonances_sent")
    val totalResonancesSent: Int = 0,

    @SerialName("total_resonances_received")
    val totalResonancesReceived: Int = 0,

    @SerialName("updated_at")
    val updatedAt: String? = null,

    // --- THIS IS THE MAGIC LINE ---
    @SerialName("display_name")
    val displayName: String? = null,

    @SerialName("total_relates_received")
    val totalRelatesReceived: Int = 0
)