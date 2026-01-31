package com.example.echo.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border // <--- ENSURE THIS IS IMPORTED
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0A)),
            border = BorderStroke(1.dp, Stone800.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Text(
                    text = "E C H O",
                    style = VoidTypography.displayLarge.copy(fontSize = 32.sp, letterSpacing = 8.sp),
                    color = NeonWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SEEK THE SILENCE",
                    style = VoidTypography.labelSmall.copy(letterSpacing = 3.sp, color = Stone400)
                )

                Spacer(modifier = Modifier.height(48.dp))

                Text(
                    text = "INITIATING PROTOCOL 48",
                    style = VoidTypography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        color = NeonWhite
                    )
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Features
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FeatureItem(Icons.Outlined.Person, "ANONYMOUS", "You are a Soul without a name. No identity.\nNo past.")
                    FeatureItem(Icons.Outlined.Schedule, "EPHEMERAL", "Nothing lasts forever. Every Echo vanishes after 48h.")
                    FeatureItem(Icons.Outlined.Forum, "RESONANCE", "The Void is silent, but not empty. Whisper and be heard.")
                }

                Spacer(modifier = Modifier.height(56.dp))

                // Enter Button
                Button(
                    onClick = onEnterClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, Stone600),
                    modifier = Modifier
                        .height(50.dp)
                        .fillMaxWidth(0.8f)
                ) {
                    Text(
                        "ENTER THE VOID",
                        style = VoidTypography.labelSmall.copy(
                            fontSize = 12.sp,
                            letterSpacing = 3.sp,
                            color = NeonWhite
                        )
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Legal Footer
                val annotatedString = buildAnnotatedString {
                    append("// By initiating session, user accepts ")
                    pushStringAnnotation(tag = "terms", annotation = "https://www.voidecho.space/terms")
                    withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline, color = Stone400)) {
                        append("protocol_terms")
                    }
                    pop()
                    append("\n// and ")
                    pushStringAnnotation(tag = "privacy", annotation = "https://www.voidecho.space/privacy")
                    withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline, color = Stone400)) {
                        append("privacy_directives")
                    }
                    pop()
                }

                ClickableText(
                    text = annotatedString,
                    style = VoidTypography.labelSmall.copy(
                        fontSize = 9.sp,
                        color = Stone600,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "terms", start = offset, end = offset)
                            .firstOrNull()?.let { openLink(it.item) }
                        annotatedString.getStringAnnotations(tag = "privacy", start = offset, end = offset)
                            .firstOrNull()?.let { openLink(it.item) }
                    }
                )
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
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Stone900.copy(alpha = 0.5f))
                // FIX: Used BorderStroke explicitly to avoid overload ambiguity or missing reference
                .border(BorderStroke(1.dp, Stone800), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Stone400, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(title, style = VoidTypography.labelSmall.copy(fontSize = 9.sp, letterSpacing = 1.sp, color = Stone400))
        Spacer(modifier = Modifier.height(6.dp))
        Text(desc, style = VoidTypography.bodyLarge.copy(fontSize = 9.sp, lineHeight = 12.sp, color = Stone600, textAlign = TextAlign.Center))
    }
}