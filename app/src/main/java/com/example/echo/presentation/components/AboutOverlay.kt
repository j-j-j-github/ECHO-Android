package com.example.echo.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*

@Composable
fun AboutOverlay(
    onDismiss: () -> Unit,
    onReplayProtocol: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun sendEmail() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:thej3labs@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "ECHO App Support")
        }
        context.startActivity(intent)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // --- LOCKED CONSOLE WINDOW ---
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(32.dp))
                .background(VoidBlack)
                .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            // Premium Top Glass Gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Brush.verticalGradient(listOf(Color.White.copy(0.06f), Color.Transparent)))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // MINIMALIST HEADER
                Box(modifier = Modifier.fillMaxWidth().height(32.dp)) {
                    Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("THE ARCHIVE", style = VoidTypography.labelSmall.copy(color = NeonWhite, letterSpacing = 8.sp, fontSize = 9.sp))
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.width(30.dp).height(1.dp).background(Color.White.copy(0.2f)))
                    }
                    Icon(
                        Icons.Default.Close, null, tint = Color.White.copy(0.3f),
                        modifier = Modifier.align(Alignment.TopEnd).size(20.dp).clickable { onDismiss() }
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // LOGO / MISSION
                Text(
                    text = "E C H O",
                    style = VoidTypography.displayLarge.copy(fontSize = 36.sp, letterSpacing = 12.sp, color = NeonWhite)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "\"A fleeting space for the things we carry. No accounts, no history, no judgment. Every thought fades after 48 hours.\"",
                    style = VoidTypography.bodyLarge.copy(color = Color.White.copy(0.5f), fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 22.sp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "48H LIFECYCLE • NO IDENTITY • PURE ECHO",
                    style = VoidTypography.labelSmall.copy(color = NeonWhite.copy(0.3f), fontSize = 9.sp, letterSpacing = 3.sp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // ==========================================
                // --- CREATOR MODULE ---
                // ==========================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White.copy(0.02f), RoundedCornerShape(20.dp))
                        .border(0.5.dp, Color.White.copy(0.08f), RoundedCornerShape(20.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("DESIGNED BY", style = VoidTypography.labelSmall.copy(color = NeonWhite.copy(0.4f), fontSize = 9.sp, letterSpacing = 4.sp))
                    Spacer(Modifier.height(16.dp))

                    // Restored original J3 Labs font styling
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

                    Spacer(Modifier.height(28.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(0.05f))
                                .border(0.5.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                                .clickable { openLink("https://www.thej3labs.com") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("WEBSITE", style = VoidTypography.labelSmall.copy(color = NeonWhite, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold))
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(0.05f))
                                .border(0.5.dp, Color.White.copy(0.1f), RoundedCornerShape(12.dp))
                                .clickable { sendEmail() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("REPORT ISSUE", style = VoidTypography.labelSmall.copy(color = NeonWhite, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                // REPLAY PROTOCOL
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onReplayProtocol() }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GET STARTED",
                        style = VoidTypography.labelSmall.copy(
                            color = Color(0xFF10B981),
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // FOOTER LINKS
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Text(
                        "PRIVACY",
                        style = VoidTypography.labelSmall.copy(color = Color.White.copy(0.3f), fontSize = 9.sp, letterSpacing = 2.sp),
                        modifier = Modifier.clickable { openLink("https://www.voidecho.space/privacy") }
                    )
                    Text(
                        "TERMS",
                        style = VoidTypography.labelSmall.copy(color = Color.White.copy(0.3f), fontSize = 9.sp, letterSpacing = 2.sp),
                        modifier = Modifier.clickable { openLink("https://www.voidecho.space/terms") }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}