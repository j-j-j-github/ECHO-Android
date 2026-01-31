package com.example.echo.presentation.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.echo.core.theme.*
import com.example.echo.core.utils.TimeUtils
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

// --- FIX: Define the missing Red color here ---
val VoidRed = Color(0xFFFF453A)

// 1. Active Souls Chip
@Composable
fun ActiveSoulsChip(count: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse), label = "alpha"
    )

    Surface(
        color = Color.White.copy(alpha = 0.05f),
        shape = CircleShape,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier.wrapContentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha)
                    .background(Color(0xFF34D399), CircleShape)
            )
            Text(text = "$count ACTIVE", style = VoidTypography.labelSmall)
        }
    }
}

// 2. Updated Echo Card (Real Time Logic + Report Support)
@Composable
fun EchoCard(
    content: String,
    createdAt: String?,
    onReport: () -> Unit, // <--- ADDED: Callback for report action
    onClick: () -> Unit
) {
    // Parse time once
    val timeObj = remember(createdAt) { TimeUtils.parseTime(createdAt) }

    // Calculate for Blur Logic (Numeric)
    val hoursLeftNumeric = remember(timeObj) {
        val deathTime = timeObj.plus(Duration.ofHours(48))
        Duration.between(Instant.now(), deathTime).toHours()
    }

    // Calculate for Display Text (String: "45h left")
    val timeLeftString = TimeUtils.getTimeLeftShort(timeObj)

    // Visual Effects (Blur/Fade based on life)
    val alpha = if (hoursLeftNumeric > 12) 1f else (hoursLeftNumeric / 12f).coerceIn(0.5f, 1f)
    val blurRadius = if (hoursLeftNumeric < 12 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ((12 - hoursLeftNumeric) * 0.2).dp
    } else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer { this.alpha = alpha }
            .then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)
            .clip(RoundedCornerShape(24.dp))
            .background(Stone900.copy(alpha = 0.2f))
            .border(1.dp, Stone800.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = content,
                style = VoidTypography.bodyLarge,
                maxLines = 5,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // REAL TIME DISPLAY
                Text(
                    text = timeLeftString.uppercase(),
                    style = VoidTypography.labelSmall.copy(
                        color = if (timeLeftString == "Expired") VoidRed else NeonWhite.copy(alpha = 0.5f)
                    )
                )

                // REPORT BUTTON (Clickable Flag)
                IconButton(
                    onClick = onReport, // <--- Triggers the report dialog
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Flag,
                        contentDescription = "Report",
                        tint = Stone700,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}