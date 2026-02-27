package com.example.echo.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*
import com.example.echo.data.remote.dto.NotificationDto

val Stone600 = Color(0xFF57534E)
val Stone700 = Color(0xFF44403C)

@Composable
fun NotificationPanel(
    notifications: List<NotificationDto>,
    onClose: () -> Unit,
    onNotificationClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Color(0xFF050505)),
        color = Color(0xFF050505),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- HEADER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 20.dp)
            ) {
                Text(
                    text = "THE SECRET LOG",
                    style = VoidTypography.labelSmall.copy(
                        letterSpacing = 2.sp,
                        color = Stone400,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.align(Alignment.CenterStart)
                )

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        "Close",
                        tint = Stone400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Divider(color = Stone800.copy(alpha = 0.3f))

            // --- LIST ---
            if (notifications.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    // ✅ UPDATED: Matches the ECHO aesthetic (Uppercase, Letter Spacing, Centered)
                    Text(
                        text = "THE VOID IS SILENT.",
                        style = VoidTypography.labelSmall.copy(
                            color = Stone600,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(items = notifications, key = { it.id }) { item ->
                        FadeInItem {
                            NotificationItem(item = item, onClick = { onNotificationClick(item.echoId) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(
    item: NotificationDto,
    onClick: () -> Unit
) {
    // --- ECHO-LIKE WINDOW BOX ---
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Stone900.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .border(1.dp, Stone800.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        // 1. Context (Original Echo)
        val originalText = item.echo?.content ?: "Unknown Echo"

        Text(
            text = "REPLY: ${originalText.take(25).uppercase()}",
            style = VoidTypography.labelSmall.copy(
                fontSize = 9.sp,
                color = Stone700,
                letterSpacing = 1.sp
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. The Reply Content
        Text(
            text = item.content,
            style = VoidTypography.bodyLarge.copy(
                fontSize = 14.sp,
                color = NeonWhite,
                lineHeight = 20.sp
            ),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 3. Footer (Time)
        Text(
            text = "NEW RESONANCE",
            style = VoidTypography.labelSmall.copy(
                fontSize = 8.sp,
                color = Stone600
            ),
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@Composable
fun FadeInItem(content: @Composable () -> Unit) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600)
        )
    }

    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha.value }) {
        content()
    }
}