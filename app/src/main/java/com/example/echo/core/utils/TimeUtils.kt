package com.example.echo.core.utils

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {

    // 1. Parse Supabase Timestamp (ISO 8601)
    fun parseTime(timestamp: String?): Instant {
        return try {
            if (timestamp == null) return Instant.now()
            // Supabase sends "2026-01-28T12:00:00.000Z"
            Instant.parse(timestamp)
        } catch (e: Exception) {
            Instant.now()
        }
    }

    // 2. Calculate remaining time (Feed Style: "45h left")
    fun getTimeLeftShort(createdAt: Instant): String {
        val deathTime = createdAt.plus(Duration.ofHours(48))
        val now = Instant.now()
        val duration = Duration.between(now, deathTime)

        return when {
            duration.isNegative -> "Expired"
            duration.toHours() > 0 -> "${duration.toHours()}h left"
            else -> "${duration.toMinutes()}m left"
        }
    }

    // 3. Calculate Countdown (Detail Style: "4 5 H  4 6 M  0 8 S")
    fun getCountdownString(createdAt: Instant): String {
        val deathTime = createdAt.plus(Duration.ofHours(48))
        val now = Instant.now()
        val duration = Duration.between(now, deathTime)

        if (duration.isNegative) return "0 0 H  0 0 M  0 0 S"

        val hours = duration.toHours()
        val minutes = duration.toMinutesPart()
        val seconds = duration.toSecondsPart()

        // Format with extra spaces to match your screenshot
        return "%02d H  %02d M  %02d S".format(hours, minutes, seconds)
            .replace("", " ") // Add spacing between every char
            .trim()
    }
}