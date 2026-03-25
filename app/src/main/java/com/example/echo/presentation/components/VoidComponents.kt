package com.example.echo.presentation.components

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.core.utils.TimeUtils
import com.example.echo.data.remote.dto.ReplyDto
import java.time.Duration
import java.time.Instant

// ✅ RESTORED MISSING COLORS
val VoidRed = Color(0xFFFF453A)
val TerminalGreen = Color(0xFF4ADE80)

// ✅ RESTORED MISSING ACTIVE SOULS CHIP
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

// 2. Updated Echo Card (Standard Feed)
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
    likes: Int = 0,
    onReport: () -> Unit,
    onEdit: () -> Unit,
    onClick: () -> Unit,
    onRelate: () -> Unit = {},
    onUnrelate: () -> Unit = {}
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

    val upperColor = colorCode?.uppercase() ?: ""
    val isCreator = upperColor == "#FFFFFF"
    val isAlert = upperColor == "#FF453A"
    val isUpdate = upperColor == "#4ADE80"
    val isSuperUser = isCreator || isAlert || isUpdate

    val cardColor = when {
        isCreator -> Color(0xFFFFFFFF) // White
        isAlert -> Color(0xFFFF453A)   // Red
        isUpdate -> Color(0xFF4ADE80)  // Green
        colorCode != null -> try { Color(android.graphics.Color.parseColor(colorCode)) } catch (e: Exception) { Color.White }
        else -> Stone900.copy(alpha = 0.2f)
    }

    val surfaceBrush = if (isSuperUser) {
        Brush.verticalGradient(
            listOf(
                cardColor.copy(alpha = if (isCreator) 0.95f else 0.25f),
                cardColor.copy(alpha = if (isCreator) 0.85f else 0.05f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.Transparent
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

    val hoursLeftNumeric = remember(timeObj) {
        val deathTime = timeObj.plus(Duration.ofHours(48))
        Duration.between(Instant.now(), deathTime).toHours()
    }
    val timeLeftString = TimeUtils.getTimeLeftShort(timeObj)

    val alpha = if (hoursLeftNumeric > 6) 1f else (hoursLeftNumeric / 6f).coerceIn(0.5f, 1f)
    val isExpiringSoon = hoursLeftNumeric < 6

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(24.dp))
            .background(surfaceBrush)
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

                            Text(
                                text = timeLeftString.uppercase(),
                                style = VoidTypography.labelSmall.copy(
                                    color = when {
                                        timeLeftString == "Expired" -> VoidRed
                                        isExpiringSoon -> VoidRed
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
                                        imageVector = Icons.Outlined.Edit,
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

// ==========================================
// ✅ THE NEW ECHO BANNER CARD (WITH RELATE)
// ==========================================
@Composable
fun EchoBannerCard(
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
    onClick: () -> Unit,
    onRelate: () -> Unit,
    onUnrelate: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val mySignature = remember { SharedPreferencesManager.getInstallationId(context) }

    var isRelated by remember { mutableStateOf(false) }

    val upperColor = colorCode?.uppercase() ?: ""
    val isCreator = upperColor == "#FFFFFF"
    val isAlert = upperColor == "#FF453A"
    val isUpdate = upperColor == "#4ADE80"
    val isSuperUser = isCreator || isAlert || isUpdate

    val accentColor = when {
        isCreator -> Color(0xFFFFFFFF)
        isAlert -> Color(0xFFFF453A)
        isUpdate -> Color(0xFF4ADE80)
        else -> try { Color(android.graphics.Color.parseColor(colorCode)) } catch (e: Exception) { Color.White }
    }

    val relateActiveColor = Color(0xFFFF453A)
    val buttonBg by animateColorAsState(targetValue = if (isRelated) relateActiveColor else Color.Transparent, animationSpec = tween(200), label = "bg")
    val buttonText by animateColorAsState(targetValue = if (isRelated) Color.White else Stone400, animationSpec = tween(200), label = "text")
    val buttonBorder by animateColorAsState(targetValue = if (isRelated) Color.Transparent else Stone400.copy(alpha = 0.5f), animationSpec = tween(200), label = "border")

    val c1OffsetX by animateDpAsState(targetValue = if (isRelated) 8.dp else 4.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c1x")
    val c2OffsetX by animateDpAsState(targetValue = if (isRelated) 8.dp else 12.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c2x")
    val c1Radius by animateDpAsState(targetValue = if (isRelated) 6.dp else 3.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c1r")
    val c2Radius by animateDpAsState(targetValue = if (isRelated) 2.5.dp else 3.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c2r")

    val cardColor = when {
        isCreator -> Color(0xFFFFFFFF)
        isAlert -> Color(0xFFFF453A)
        isUpdate -> Color(0xFF4ADE80)
        colorCode != null -> try { Color(android.graphics.Color.parseColor(colorCode)) } catch (e: Exception) { Color.White }
        else -> Stone900.copy(alpha = 0.2f)
    }

    val surfaceBrush = if (isSuperUser) {
        Brush.verticalGradient(
            listOf(
                cardColor.copy(alpha = if (isCreator) 0.95f else 0.25f),
                cardColor.copy(alpha = if (isCreator) 0.85f else 0.05f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.08f),
                Color.Transparent
            )
        )
    }

    val borderColor = if (isSuperUser) {
        cardColor.copy(alpha = if(isCreator) 1f else 0.6f)
    } else {
        if (colorCode != null) cardColor.copy(alpha = 0.4f) else Stone800.copy(alpha = 0.3f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surfaceBrush)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // --- TOP ROW: Category & Report ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ✅ ADDED: The Background Box for the category flair
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isCreator) VoidBlack.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = (category ?: "THE VOID").uppercase(),
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 8.sp,
                            color = if (isCreator) VoidBlack.copy(alpha = 0.8f) else Stone400,
                            letterSpacing = 1.sp
                        )
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (signatureId == mySignature && !isSuperUser) {
                        IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = "Edit",
                                tint = Stone600,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (!isSuperUser && signatureId != mySignature) {
                        IconButton(onClick = onReport, modifier = Modifier.size(24.dp)) {
                            Icon(
                                Icons.Outlined.Flag,
                                contentDescription = "Report",
                                tint = Stone600,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            } // <-- END OF TOP ROW

            // --- CENTER: Echo Content ---
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = content,
                    style = VoidTypography.bodyLarge.copy(
                        fontSize = 16.sp,
                        color = NeonWhite,
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTTOM ROW: Time (Left), Relate Button (Right) ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                val timeObj = remember(createdAt) { TimeUtils.parseTime(createdAt) }
                val timeLeftString = TimeUtils.getTimeLeftShort(timeObj)

                Text(
                    text = timeLeftString.uppercase(),
                    style = VoidTypography.labelSmall.copy(color = Stone600, fontSize = 9.sp)
                )

                if (!isSuperUser) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(buttonBg)
                            .border(1.dp, buttonBorder, RoundedCornerShape(24.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                isRelated = !isRelated
                                if (isRelated) onRelate() else onUnrelate()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val startX = c1OffsetX.toPx()
                            val endX = c2OffsetX.toPx()
                            val r1 = c1Radius.toPx()
                            val r2 = c2Radius.toPx()

                            drawCircle(
                                color = buttonText,
                                radius = r1,
                                center = Offset(startX, size.height / 2),
                                style = Stroke(1.5.dp.toPx())
                            )
                            drawCircle(
                                color = buttonText,
                                radius = r2,
                                center = Offset(endX, size.height / 2),
                                style = Stroke(1.5.dp.toPx())
                            )

                            val lineStart = startX + r1
                            val lineEnd = endX - r2
                            if (lineEnd > lineStart) {
                                drawLine(
                                    color = buttonText,
                                    start = Offset(lineStart, size.height / 2),
                                    end = Offset(lineEnd, size.height / 2),
                                    strokeWidth = 1.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
            } // <-- END OF BOTTOM ROW
        }
    }
}