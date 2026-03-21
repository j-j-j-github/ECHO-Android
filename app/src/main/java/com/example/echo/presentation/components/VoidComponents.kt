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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush // ✅ Required for gradients
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.core.utils.TimeUtils
import com.example.echo.data.remote.dto.ReplyDto
import java.time.Duration
import java.time.Instant

// Colors
val VoidRed = Color(0xFFFF453A)
val TerminalGreen = Color(0xFF4ADE80)

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

// 2. Updated Echo Card
@Composable
fun EchoCard(
    echoId: String,
    content: String,
    createdAt: String?,
    updatedAt: String? = null,
    replies: List<ReplyDto>?,
    signatureId: String? = null,
    colorCode: String? = null,
    category: String? = null,
    onReport: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val timeObj = remember(createdAt) { TimeUtils.parseTime(createdAt) }
    val mySignature = remember { SharedPreferencesManager.getInstallationId(context) }

    val hasReplied = remember(replies, echoId) {
        val serverSaysYes = replies?.any { it.signatureId == mySignature } == true
        val localSaysYes = SharedPreferencesManager.hasRepliedTo(context, echoId)
        serverSaysYes || localSaysYes
    }

    // --- EDIT LOGIC ---
    val isOwner = signatureId == mySignature
    val isEditable = remember(createdAt) { TimeUtils.canEdit(createdAt) }
    val showEditButton = isOwner && isEditable

    // ✅ SUPER USER LOGIC
    val upperColor = colorCode?.uppercase() ?: ""
    val isCreator = upperColor == "#FFFFFF"
    val isAlert = upperColor == "#FF453A"
    val isUpdate = upperColor == "#4ADE80"
    val isSuperUser = isCreator || isAlert || isUpdate

    // ✅ APPLY THE CORRECT OVERRIDE COLOR
    val cardColor = when {
        isCreator -> Color(0xFFFFFFFF) // White
        isAlert -> Color(0xFFFF453A)   // Red
        isUpdate -> Color(0xFF4ADE80)  // Green
        colorCode != null -> try { Color(android.graphics.Color.parseColor(colorCode)) } catch (e: Exception) { Color.White }
        else -> Stone900.copy(alpha = 0.2f)
    }

    // --- NEW: FIXED PREMIUM LIGHT GRADIENT ---
    val surfaceBrush = if (isSuperUser) {
        Brush.verticalGradient(
            listOf(
                cardColor.copy(alpha = if (isCreator) 0.95f else 0.25f),
                cardColor.copy(alpha = if (isCreator) 0.85f else 0.05f)
            )
        )
    } else {
        // Universal elegant glare from the top, fading into the Void
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f), // Premium light from top
                Color.Transparent                // Fades into the background
            )
        )
    }

    val borderColor = if (isSuperUser) {
        cardColor.copy(alpha = if(isCreator) 1f else 0.6f)
    } else {
        if (colorCode != null) cardColor.copy(alpha = 0.4f) else Stone800.copy(alpha = 0.3f)
    }

    val contentTextColor = if (isCreator) VoidBlack else NeonWhite
    val subTextColor = if (isCreator) VoidBlack.copy(alpha = 0.6f) else NeonWhite.copy(alpha = 0.5f)

    // --- EXPIRY LOGIC (Blur removed, Timer highlighted) ---
    val hoursLeftNumeric = remember(timeObj) {
        val deathTime = timeObj.plus(Duration.ofHours(48))
        Duration.between(Instant.now(), deathTime).toHours()
    }
    val timeLeftString = TimeUtils.getTimeLeftShort(timeObj)

    // Smooth fade out at the very end, but NO BLUR
    // If more than 6 hours left, stay fully visible (1f)
// If under 6 hours, fade down based on remaining time (using 6f as the divisor)
    val alpha = if (hoursLeftNumeric > 6) 1f else (hoursLeftNumeric / 6f).coerceIn(0.5f, 1f)

// Correct: Red timer starts at the 6-hour mark
    val isExpiringSoon = hoursLeftNumeric < 6

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceBrush) // ✅ Fixed universal light gradient applied here
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            // CATEGORY FLAIR HEADER
            if (category != null && category.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .background(
                            color = if (isCreator) VoidBlack.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = category.uppercase(),
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = if (isCreator) VoidBlack.copy(alpha = 0.8f) else Stone400,
                            letterSpacing = 1.sp
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Main Text Content
            Text(
                text = content,
                style = VoidTypography.bodyLarge.copy(
                    color = contentTextColor,
                    fontWeight = if (isSuperUser) FontWeight.Bold else FontWeight.Normal
                ),
                maxLines = 4,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // FOOTER
            Column(modifier = Modifier.fillMaxWidth()) {
                if (updatedAt != null) {
                    Text(
                        text = "EDITED",
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = if (isCreator) VoidBlack.copy(alpha = 0.5f) else Stone700,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                // ✅ HIDE NORMAL ACTIONS IF SUPER USER
                if (!isSuperUser) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasReplied) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = if (isCreator) VoidBlack else TerminalGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // ✅ TIMER LOGIC: Turns Red when expiring soon
                            Text(
                                text = timeLeftString.uppercase(),
                                style = VoidTypography.labelSmall.copy(
                                    color = when {
                                        timeLeftString == "Expired" -> VoidRed
                                        isExpiringSoon -> VoidRed // Turns Red under 12 hours
                                        isCreator -> VoidBlack.copy(alpha = 0.7f)
                                        else -> subTextColor
                                    },
                                    fontWeight = if (isExpiringSoon) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (showEditButton) {
                                IconButton(
                                    onClick = onEdit,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = if (isCreator) VoidBlack.copy(alpha = 0.5f) else Stone600,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            IconButton(
                                onClick = onReport,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Flag,
                                    contentDescription = "Report",
                                    tint = if (isCreator) VoidBlack.copy(alpha = 0.5f) else Stone700,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                } else {
                    // ✅ SHOW BROADCAST TAG INSTEAD OF ACTIONS
                    Text(
                        text = "SYSTEM BROADCAST",
                        color = contentTextColor.copy(alpha = 0.6f),
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}