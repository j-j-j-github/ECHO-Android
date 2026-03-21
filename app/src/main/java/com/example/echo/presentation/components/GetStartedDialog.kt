package com.example.echo.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.echo.core.theme.*

@Composable
fun GetStartedDialog(
    onEnterClick: () -> Unit
) {
    val context = LocalContext.current

    fun openLink(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    // THE LOCK: Force this entire dialog to ignore system font scaling
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(currentDensity.density, fontScale = 1f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 40.dp)
                // Block background taps
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
            contentAlignment = Alignment.Center
        ) {
            // --- PREMIUM LOCKED WINDOW WRAPPER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(32.dp))
                    .background(VoidBlack)
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            ) {
                // Elegant Top Glass Gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Brush.verticalGradient(listOf(Color.White.copy(0.08f), Color.Transparent)))
                )

                Column(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header
                    Text(
                        text = "E C H O",
                        style = VoidTypography.displayLarge.copy(
                            fontSize = 32.sp,
                            letterSpacing = 8.sp
                        ),
                        color = NeonWhite,
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SEEK THE SILENCE",
                        style = VoidTypography.labelSmall.copy(
                            letterSpacing = 3.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        ),
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    Text(
                        text = "INITIATING PROTOCOL 48",
                        style = VoidTypography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            letterSpacing = 4.sp,
                            color = NeonWhite
                        ),
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(40.dp))

                    // Features
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FeatureItem(
                            Icons.Outlined.Person,
                            "ANONYMOUS",
                            "You are a Soul without a name. No identity.\nNo past."
                        )
                        FeatureItem(
                            Icons.Outlined.Schedule,
                            "EPHEMERAL",
                            "Nothing lasts forever. Every Echo vanishes after 48h."
                        )
                        FeatureItem(
                            Icons.Outlined.Forum,
                            "RESONANCE",
                            "The Void is silent, but not empty. Whisper and be heard."
                        )
                    }

                    Spacer(modifier = Modifier.height(56.dp))

                    // --- PREMIUM WHITE GLASS BUTTON ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.verticalGradient(listOf(Color.White, Color.White.copy(alpha = 0.85f))))
                            .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                            .clickable { onEnterClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "ENTER THE VOID",
                            style = VoidTypography.labelSmall.copy(
                                fontSize = 12.sp,
                                letterSpacing = 4.sp,
                                color = VoidBlack,
                                fontWeight = FontWeight.Black
                            ),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Legal Footer
                    val annotatedString = buildAnnotatedString {
                        append("// By initiating session, user accepts ")
                        pushStringAnnotation(
                            tag = "terms",
                            annotation = "https://www.voidecho.space/terms"
                        )
                        withStyle(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            append("protocol_terms")
                        }
                        pop()
                        append("\n// and ")
                        pushStringAnnotation(
                            tag = "privacy",
                            annotation = "https://www.voidecho.space/privacy"
                        )
                        withStyle(
                            style = SpanStyle(
                                textDecoration = TextDecoration.Underline,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        ) {
                            append("privacy_directives")
                        }
                        pop()
                    }

                    ClickableText(
                        text = annotatedString,
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.3f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        onClick = { offset ->
                            annotatedString.getStringAnnotations(
                                tag = "terms",
                                start = offset,
                                end = offset
                            ).firstOrNull()?.let { openLink(it.item) }
                            annotatedString.getStringAnnotations(
                                tag = "privacy",
                                start = offset,
                                end = offset
                            ).firstOrNull()?.let { openLink(it.item) }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, desc: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(85.dp)
    ) {
        // Elegant Glass Ring Icon
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.05f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = NeonWhite,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            title,
            style = VoidTypography.labelSmall.copy(
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.8f)
            ),
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            desc,
            style = VoidTypography.bodyLarge.copy(
                fontSize = 9.sp,
                lineHeight = 12.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        )
    }
}