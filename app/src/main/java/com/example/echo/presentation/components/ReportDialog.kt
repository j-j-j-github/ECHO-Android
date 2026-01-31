package com.example.echo.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.NeonWhite
import com.example.echo.core.theme.Stone300
import com.example.echo.core.theme.Stone800
import com.example.echo.core.theme.VoidTypography

// Renamed colors to avoid conflict
val ReportBg = Color(0xFF3E1F1F)
val ReportAccent = Color(0xFFFF4D4D)

@Composable
fun ReportDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    // Just the Card content (Sharp)
    // The blur and scrim are handled by the parent (VoidScreen)
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            border = BorderStroke(1.dp, Stone800.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(ReportBg, CircleShape)
                        .border(1.dp, ReportAccent.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.PriorityHigh,
                        contentDescription = "Report",
                        tint = ReportAccent
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Report this Echo?",
                    style = VoidTypography.displayLarge.copy(fontSize = 20.sp),
                    color = NeonWhite
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "\"If this whisper disrupts the peace of the void, it will be flagged. Enough flags, and it shall be silenced forever.\"",
                    style = VoidTypography.bodyLarge.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Stone300
                    ),
                    textAlign = TextAlign.Center,
                    fontStyle = FontStyle.Italic
                )

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Stone800),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Stone300),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", style = VoidTypography.labelSmall)
                    }

                    Button(
                        onClick = onConfirm,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ReportBg),
                        border = BorderStroke(1.dp, ReportAccent.copy(alpha = 0.3f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "REPORT",
                            style = VoidTypography.labelSmall.copy(color = ReportAccent)
                        )
                    }
                }
            }
        }
    }
}