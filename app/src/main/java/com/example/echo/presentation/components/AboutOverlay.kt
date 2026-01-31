package com.example.echo.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*

@Composable
fun AboutOverlay(
    onDismiss: () -> Unit,
    onReplayProtocol: () -> Unit
) {
    val context = LocalContext.current

    fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.98f))
            .clickable(enabled = false) {}
            .padding(24.dp)
    ) {
        // CLOSE BUTTON
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding()
        ) {
            Icon(Icons.Default.Close, "Close", tint = Stone400)
        }

        // CENTER CONTENT
        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "E C H O",
                style = VoidTypography.displayLarge.copy(fontSize = 32.sp, letterSpacing = 8.sp),
                color = NeonWhite
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "\"A fleeting space for the things we carry. No accounts, no history, no judgment. Every thought fades after 48 hours.\"",
                style = VoidTypography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp, color = Stone300),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "48H LIFECYCLE  •  NO IDENTITY  •  PURE ECHO",
                style = VoidTypography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 2.sp, color = Stone600)
            )

            Spacer(modifier = Modifier.height(48.dp))
            Divider(color = Stone800.copy(alpha = 0.3f), modifier = Modifier.width(120.dp))
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "DESIGNED BY",
                style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = Stone600)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "J3 Labs",
                style = VoidTypography.bodyLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonWhite,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // SOCIALS
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = { openLink("https://www.thej3labs.com") },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Stone800),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Stone400)
                ) { Text("WEBSITE", style = VoidTypography.labelSmall.copy(fontSize = 10.sp)) }

                OutlinedButton(
                    onClick = { openLink("https://www.linkedin.com/company/j3labs/") },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Stone800),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Stone400)
                ) { Text("LINKEDIN", style = VoidTypography.labelSmall.copy(fontSize = 10.sp)) }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // SUPPORT
            Text(
                text = "SUPPORT THE SILENCE",
                style = VoidTypography.labelSmall.copy(fontSize = 10.sp, color = Stone600, letterSpacing = 2.sp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { openLink("https://buymeacoffee.com/jeevaljollyjacob") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F2015)),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF1E3A28), Color(0xFF2A5A3B)))),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("GLOBAL", style = VoidTypography.labelSmall.copy(color = Color(0xFF86EFAC), letterSpacing = 1.sp, fontSize = 10.sp))
                }

                Button(
                    onClick = { openLink("https://buymeachai.ezee.li/thej3labs") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1B0A)),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(Color(0xFF4A3B2A), Color(0xFF6A5B4A)))),
                    modifier = Modifier.height(40.dp)
                ) {
                    Text("INDIA", style = VoidTypography.labelSmall.copy(color = Color(0xFFFDBA74), letterSpacing = 1.sp, fontSize = 10.sp))
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // REPLAY PROTOCOL
            Text(
                text = "REPLAY PROTOCOL",
                style = VoidTypography.labelSmall.copy(color = Stone600, fontSize = 10.sp),
                modifier = Modifier.clickable { onReplayProtocol() }
            )
        }

        // FOOTER
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Privacy",
                style = VoidTypography.labelSmall.copy(color = Stone700, fontSize = 10.sp, textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable { openLink("https://www.voidecho.space/privacy") }
            )
            Text(
                text = "Terms",
                style = VoidTypography.labelSmall.copy(color = Stone700, fontSize = 10.sp, textDecoration = TextDecoration.Underline),
                modifier = Modifier.clickable { openLink("https://www.voidecho.space/terms") }
            )
        }
    }
}