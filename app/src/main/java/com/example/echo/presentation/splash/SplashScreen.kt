package com.example.echo.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider // Added
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity // Added
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density // Added
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
            // CHANGE THIS: Was 2000 (2 seconds), try 1000 (1 second) or 800 (0.8s) for a snappier feel
            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
        )

        // CHANGE THIS: Was 500L, try 200L or remove it entirely if you want instant entry
        delay(200L)

        navController.navigate("void_feed") {
            popUpTo("splash_screen") { inclusive = true }
        }
    }

    // THE FIX: This forces the density to ignore system font scaling
    // It makes 'sp' behave exactly like 'dp' (pixels), locking the size.
    val currentDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(currentDensity.density, fontScale = 1f)
    ) {
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
                    color = NeonWhite,
                    maxLines = 1,          // Added: Prevents wrapping
                    softWrap = false       // Added: Forces single line
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "SEEK THE SILENCE",
                    style = VoidTypography.labelSmall.copy(
                        letterSpacing = 3.sp,
                        color = Stone400
                    ),
                    maxLines = 1,          // Added: Prevents wrapping
                    softWrap = false       // Added: Forces single line
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
                    text = "v1.2.1",
                    style = VoidTypography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = Stone600,
                        letterSpacing = 2.sp
                    ),
                    maxLines = 1,      // Added: Prevents wrapping
                    softWrap = false   // Added: Forces single line
                )

                Spacer(modifier = Modifier.height(6.dp))

                // J3 LABS: SANS, BOLD, TIGHTER
                Text(
                    text = "J3 Labs",
                    style = VoidTypography.bodyLarge.copy(
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonWhite,
                        letterSpacing = 1.sp
                    ),
                    maxLines = 1,      // Added: Prevents wrapping
                    softWrap = false   // Added: Forces single line
                )
            }
        }
    }
}