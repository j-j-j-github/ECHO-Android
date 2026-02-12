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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
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

// 2. Updated Echo Card (Cleaned Layout for Edited Tags & Icons)
@Composable
fun EchoCard(
    echoId: String,
    content: String,
    createdAt: String?,
    updatedAt: String? = null,
    replies: List<ReplyDto>?,
    signatureId: String? = null,
    colorCode: String? = null,
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

    // --- DYNAMIC STYLING LOGIC ---
    val isCreatorPost = colorCode == "#FFFFFF"

    val surfaceColor = remember(colorCode) {
        if (colorCode != null) {
            Color(android.graphics.Color.parseColor(colorCode))
                .copy(alpha = if (isCreatorPost) 0.95f else 0.15f)
        } else Stone900.copy(alpha = 0.2f)
    }

    val borderColor = remember(colorCode) {
        if (colorCode != null) {
            Color(android.graphics.Color.parseColor(colorCode)).copy(alpha = 0.6f)
        } else Stone800.copy(alpha = 0.4f)
    }

    val contentTextColor = if (isCreatorPost) VoidBlack else NeonWhite
    val subTextColor = if (isCreatorPost) VoidBlack.copy(alpha = 0.6f) else NeonWhite.copy(alpha = 0.5f)

    // Visual Effects (Blur & Alpha)
    val hoursLeftNumeric = remember(timeObj) {
        val deathTime = timeObj.plus(Duration.ofHours(48))
        Duration.between(Instant.now(), deathTime).toHours()
    }
    val timeLeftString = TimeUtils.getTimeLeftShort(timeObj)
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
            .background(surfaceColor)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
            .clickable { onClick() }
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = content,
                style = VoidTypography.bodyLarge.copy(color = contentTextColor),
                maxLines = 5,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // ✅ FOOTER: Stacking Edited tag above the status row
            Column(modifier = Modifier.fillMaxWidth()) {
                if (updatedAt != null) {
                    Text(
                        text = "EDITED",
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = if (isCreatorPost) VoidBlack.copy(alpha = 0.5f) else Stone700,
                            letterSpacing = 1.sp
                        ),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

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
                                tint = if (isCreatorPost) VoidBlack else TerminalGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        Text(
                            text = timeLeftString.uppercase(),
                            style = VoidTypography.labelSmall.copy(
                                color = when {
                                    timeLeftString == "Expired" -> VoidRed
                                    isCreatorPost -> VoidBlack.copy(alpha = 0.7f)
                                    else -> subTextColor
                                }
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
                                    tint = if (isCreatorPost) VoidBlack.copy(alpha = 0.5f) else Stone600,
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
                                tint = if (isCreatorPost) VoidBlack.copy(alpha = 0.5f) else Stone700,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}