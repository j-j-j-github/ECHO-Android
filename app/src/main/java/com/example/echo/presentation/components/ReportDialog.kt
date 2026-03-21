package com.example.echo.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*

// Using your app's standard VoidRed for the destructive action
val ReportAccent = Color(0xFFFF453A)

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // --- PREMIUM LOCKED WINDOW ---
        Box(
            modifier = Modifier
                .fillMaxWidth(0.88f) // Keeps it tight and centered
                .clip(RoundedCornerShape(32.dp))
                .background(VoidBlack)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
        ) {
            // Elegant Top Glass Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Brush.verticalGradient(listOf(Color.White.copy(0.06f), Color.Transparent)))
            )

            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(ReportAccent.copy(alpha = 0.08f), CircleShape)
                        .border(1.dp, ReportAccent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PriorityHigh,
                        contentDescription = "Report",
                        tint = ReportAccent,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "REPORT ECHO",
                    style = VoidTypography.labelSmall.copy(color = NeonWhite, letterSpacing = 6.sp, fontSize = 11.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "\"If this whisper disrupts the peace of the void, it will be flagged. Enough flags, and it shall be silenced forever.\"",
                    style = VoidTypography.bodyLarge.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(36.dp))

                // --- CUSTOM PREMIUM BUTTONS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Cancel Button (Neutral Glass)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(0.5.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "CANCEL",
                            style = VoidTypography.labelSmall.copy(color = NeonWhite, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    // Report Button (Red Glass Accent)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            // A slightly stronger background so the danger action is clear
                            .background(ReportAccent.copy(alpha = 0.12f))
                            .border(1.dp, ReportAccent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SILENCE IT",
                            style = VoidTypography.labelSmall.copy(color = ReportAccent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}