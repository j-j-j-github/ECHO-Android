package com.example.echo.presentation.components

import android.graphics.BlurMaskFilter
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
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
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
import kotlin.math.*

// Colors
val VoidRed = Color(0xFFFF453A)
val TerminalGreen = Color(0xFF4ADE80)

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

@Composable
fun EchoCard(
    echoId: String, content: String, createdAt: String?, updatedAt: String? = null,
    replies: List<ReplyDto>?, signatureId: String? = null, colorCode: String? = null,
    category: String? = null,
    likes: Int = 0,
    isRelated: Boolean = false,
    onReport: () -> Unit, onEdit: () -> Unit,
    onClick: () -> Unit, onRelate: () -> Unit = {}, onUnrelate: () -> Unit = {}
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val timeObj = remember(createdAt) { TimeUtils.parseTime(createdAt) }
    val mySignature = remember { SharedPreferencesManager.getInstallationId(context) }

    val hasReplied = remember(replies, echoId) {
        replies?.any { it.signatureId == mySignature } == true || SharedPreferencesManager.hasRepliedTo(context, echoId)
    }

    val isOwner = signatureId == mySignature
    val isEditable = remember(createdAt) { TimeUtils.canEdit(createdAt) }
    val showEditButton = isOwner && isEditable

    val upperColor = colorCode?.uppercase() ?: ""
    val isCreator = upperColor == "#FFFFFF"
    val isSuperUser = isCreator || upperColor == "#FF453A" || upperColor == "#4ADE80"

    val cardColor = when {
        isCreator -> Color(0xFFFFFFFF)
        upperColor == "#FF453A" -> Color(0xFFFF453A)
        upperColor == "#4ADE80" -> Color(0xFF4ADE80)
        colorCode != null -> try { Color(android.graphics.Color.parseColor(colorCode)) } catch (e: Exception) { Color.White }
        else -> Stone900.copy(alpha = 0.2f)
    }

    val surfaceBrush = if (isSuperUser) {
        Brush.verticalGradient(listOf(cardColor.copy(alpha = if (isCreator) 0.95f else 0.25f), cardColor.copy(alpha = if (isCreator) 0.85f else 0.05f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent))
    }

    val borderColor = if (isSuperUser) cardColor.copy(alpha = if(isCreator) 1f else 0.6f) else (if (colorCode != null) cardColor.copy(alpha = 0.4f) else Stone800.copy(alpha = 0.3f))
    val contentTextColor = if (isCreator) VoidBlack else NeonWhite
    val subTextColor = if (isCreator) VoidBlack.copy(alpha = 0.6f) else NeonWhite.copy(alpha = 0.5f)
    val timeLeftString = TimeUtils.getTimeLeftShort(timeObj)

    val alpha = if (Duration.between(Instant.now(), timeObj.plus(Duration.ofHours(48))).toHours() > 6) 1f else 0.5f

    Box(modifier = Modifier.fillMaxWidth().height(240.dp).graphicsLayer { this.alpha = alpha }.clip(RoundedCornerShape(24.dp)).background(surfaceBrush).border(1.dp, borderColor, RoundedCornerShape(24.dp)).clickable { onClick() }.padding(24.dp)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            if (!category.isNullOrEmpty()) {
                Box(modifier = Modifier.padding(bottom = 12.dp).background(color = if (isCreator) VoidBlack.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text(text = category.uppercase(), style = VoidTypography.labelSmall.copy(fontSize = 8.sp, color = if (isCreator) VoidBlack.copy(alpha = 0.8f) else Stone400, letterSpacing = 1.sp))
                }
            } else { Spacer(modifier = Modifier.height(8.dp)) }
            Text(text = content, style = VoidTypography.bodyLarge.copy(color = contentTextColor, fontWeight = if (isSuperUser) FontWeight.Bold else FontWeight.Normal), maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Column(modifier = Modifier.fillMaxWidth()) {
                if (updatedAt != null) { Text("EDITED", style = VoidTypography.labelSmall.copy(fontSize = 8.sp, color = if (isCreator) VoidBlack.copy(alpha = 0.5f) else Stone700), modifier = Modifier.padding(bottom = 2.dp)) }
                if (!isSuperUser) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (hasReplied) { Icon(Icons.Default.CheckCircle, null, tint = if (isCreator) VoidBlack else TerminalGreen, modifier = Modifier.size(14.dp)); Spacer(modifier = Modifier.width(6.dp)) }
                            Text(timeLeftString.uppercase(), style = VoidTypography.labelSmall.copy(color = subTextColor))
                        }
                        Row {
                            if (showEditButton) { IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Edit, null, tint = if (isCreator) VoidBlack.copy(alpha = 0.5f) else Stone600, modifier = Modifier.size(16.dp)) }; Spacer(modifier = Modifier.width(8.dp)) }
                            IconButton(onClick = onReport, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Flag, null, tint = if (isCreator) VoidBlack.copy(alpha = 0.5f) else Stone700, modifier = Modifier.size(16.dp)) }
                        }
                    }
                } else { Text("SYSTEM BROADCAST", color = contentTextColor.copy(alpha = 0.6f), fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
fun EchoBannerCard(
    echoId: String, content: String, createdAt: String?, updatedAt: String? = null,
    replies: List<ReplyDto>?, signatureId: String? = null, colorCode: String? = null,
    category: String? = null,
    likes: Int,
    isRelated: Boolean,
    onReport: () -> Unit, onEdit: () -> Unit, onClick: () -> Unit,
    onRelate: () -> Unit, onUnrelate: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val upperColor = colorCode?.uppercase() ?: ""
    val isCreator = upperColor == "#FFFFFF"
    val isSuperUser = isCreator || upperColor == "#FF453A" || upperColor == "#4ADE80"

    val cardColor = when {
        isCreator -> Color(0xFFFFFFFF)
        upperColor == "#FF453A" -> Color(0xFFFF453A)
        upperColor == "#4ADE80" -> Color(0xFF4ADE80)
        colorCode != null -> try { Color(android.graphics.Color.parseColor(colorCode)) } catch (e: Exception) { Color.White }
        else -> Stone900.copy(alpha = 0.2f)
    }

    val surfaceBrush = if (isSuperUser) {
        Brush.verticalGradient(listOf(cardColor.copy(alpha = if (isCreator) 0.95f else 0.25f), cardColor.copy(alpha = if (isCreator) 0.85f else 0.05f)))
    } else {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent))
    }

    val borderColor = if (isSuperUser) cardColor.copy(alpha = if(isCreator) 1f else 0.6f) else (if (colorCode != null) cardColor.copy(alpha = 0.4f) else Stone800.copy(alpha = 0.3f))

    // --- RELATE BUTTON ANIMATION STATE ---
    val relateActiveColor = Color(0xFFFF453A)
    val buttonBg by animateColorAsState(targetValue = if (isRelated) relateActiveColor else Color.Transparent, animationSpec = tween(200), label = "bg")
    val buttonText by animateColorAsState(targetValue = if (isRelated) Color.White else Stone400, animationSpec = tween(200), label = "text")
    val buttonBorder by animateColorAsState(targetValue = if (isRelated) Color.Transparent else Stone400.copy(alpha = 0.5f), animationSpec = tween(200), label = "border")

    val c1OffsetX by animateDpAsState(targetValue = if (isRelated) 8.dp else 4.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c1x")
    val c2OffsetX by animateDpAsState(targetValue = if (isRelated) 8.dp else 12.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c2x")
    val c1Radius by animateDpAsState(targetValue = if (isRelated) 6.dp else 3.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c1r")
    val c2Radius by animateDpAsState(targetValue = if (isRelated) 2.5.dp else 3.dp, animationSpec = spring(dampingRatio = 0.6f, stiffness = 250f), label = "c2r")

    // ✅ FIXED: Locked height to 240.dp
    Box(modifier = Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(20.dp)).background(surfaceBrush).border(1.dp, borderColor, RoundedCornerShape(20.dp)).clickable { onClick() }.padding(16.dp)) {
        Column(modifier = Modifier.fillMaxSize()) {

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.background(color = if (isCreator) VoidBlack.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.06f), shape = RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                    Text((category ?: "THE VOID").uppercase(), style = VoidTypography.labelSmall.copy(fontSize = 8.sp, color = if (isCreator) VoidBlack.copy(alpha = 0.8f) else Stone400, letterSpacing = 1.sp))
                }
                IconButton(onClick = onReport, modifier = Modifier.size(24.dp)) { Icon(Icons.Outlined.Flag, null, tint = Stone600, modifier = Modifier.size(14.dp)) }
            }

            // ✅ FIXED: Added .weight(1f) to push the bottom row down
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(content, style = VoidTypography.bodyLarge.copy(fontSize = 16.sp, color = NeonWhite, textAlign = TextAlign.Center), maxLines = 4, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(TimeUtils.getTimeLeftShort(TimeUtils.parseTime(createdAt)).uppercase(), style = VoidTypography.labelSmall.copy(color = Stone600, fontSize = 9.sp))
                if (!isSuperUser) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(buttonBg)
                            .border(1.dp, buttonBorder, RoundedCornerShape(24.dp))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                if (isRelated) onUnrelate() else onRelate()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val startX = c1OffsetX.toPx()
                            val endX = c2OffsetX.toPx()
                            val r1 = c1Radius.toPx()
                            val r2 = c2Radius.toPx()

                            drawCircle(color = buttonText, radius = r1, center = Offset(startX, size.height / 2), style = Stroke(1.5.dp.toPx()))
                            drawCircle(color = buttonText, radius = r2, center = Offset(endX, size.height / 2), style = Stroke(1.5.dp.toPx()))

                            val lineStart = startX + r1
                            val lineEnd = endX - r2
                            if (lineEnd > lineStart) {
                                drawLine(color = buttonText, start = Offset(lineStart, size.height / 2), end = Offset(lineEnd, size.height / 2), strokeWidth = 1.2.dp.toPx(), cap = StrokeCap.Round)
                            }
                        }
                        // ✅ REMOVED: Likes count text block completely
                    }
                }
            }
        }
    }
}