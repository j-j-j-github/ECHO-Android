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

    @SerialName("display_name")
    val displayName: String? = null,

    @SerialName("total_relates_received")
    val totalRelatesReceived: Int = 0,

    // ✅ ADDED FOR SOUL ENERGY SYSTEM
    val energy: Int? = 0,

    @SerialName("streak_days")
    val streakDays: Int? = 0,

    @SerialName("last_active_date")
    val lastActiveDate: String? = null,

    @SerialName("daily_echoes_counted")
    val dailyEchoesCounted: Int? = 0,

    @SerialName("daily_resonances_counted")
    val dailyResonancesCounted: Int? = 0,

    @SerialName("daily_relates_received")
    val dailyRelatesReceived: Int? = 0,

    @SerialName("daily_resonances_received")
    val dailyResonancesReceived: Int? = 0,

    @SerialName("last_spotlight_time") val lastSpotlightTime: String? = null
)