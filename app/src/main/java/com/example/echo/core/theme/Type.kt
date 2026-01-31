package com.example.echo.core.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.echo.R

// 1. Define the Font Family (Loading locally from res/font/eb_garamond_extralight.ttf)
val EBGaramond = FontFamily(
    Font(R.font.eb_garamond_extralight, FontWeight.ExtraLight)
)

// 2. Apply it to your Typography
val VoidTypography = Typography(
    // HEADER: The elegant, thin, spaced-out look
    displayLarge = TextStyle(
        fontFamily = EBGaramond,
        fontWeight = FontWeight.ExtraLight,
        fontSize = 32.sp,
        letterSpacing = 0.2.em,
        color = NeonWhite
    ),
    // BODY: The raw terminal look
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Light,
        fontSize = 14.sp,
        lineHeight = 24.sp,
        color = Stone300
    ),
    // LABELS: Small UI elements
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        letterSpacing = 0.15.em,
        color = Stone400
    )
)