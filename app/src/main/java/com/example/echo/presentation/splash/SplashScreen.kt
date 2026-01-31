package com.example.echo.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.echo.core.theme.NeonWhite
import com.example.echo.core.theme.Stone400
import com.example.echo.core.theme.Stone600
import com.example.echo.core.theme.VoidTypography
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(key1 = true) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = LinearEasing)
        )
        delay(500L)
        navController.navigate("void_feed") {
            popUpTo("splash_screen") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.alpha(alpha.value)
        ) {
            Text(
                text = "E C H O",
                style = VoidTypography.displayLarge.copy(
                    fontSize = 48.sp,
                    letterSpacing = 12.sp
                ),
                color = NeonWhite
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SEEK THE SILENCE",
                style = VoidTypography.labelSmall.copy(
                    letterSpacing = 3.sp,
                    color = Stone400
                )
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .alpha(alpha.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "v1.0.0",
                style = VoidTypography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = Stone600,
                    letterSpacing = 2.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // J3 LABS: SANS, BOLD, TIGHTER
            Text(
                text = "J3 Labs",
                style = VoidTypography.bodyLarge.copy(
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold, // Changed to Bold for thickness
                    color = NeonWhite,
                    letterSpacing = 1.sp
                )
            )
        }
    }
}