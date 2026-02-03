package com.example.echo.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.echo.core.theme.EchoTheme
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.presentation.feed.VoidScreen
import com.example.echo.presentation.feed.VoidViewModel
import com.example.echo.presentation.splash.SplashScreen
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class FcmTokenEntry(
    val installation_id: String,
    val fcm_token: String
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabase: SupabaseClient

    // 1. Inject ViewModel at Activity level to handle Intents (Deep Links)
    private val mainViewModel: VoidViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // --- FORCE 120Hz / MAX REFRESH RATE ---
        enableHighRefreshRate()

        // 2. Subscribe to "all_users" topic for New Echo Alerts & Reminders
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("EchoSetup", "Topic subscription failed")
                }
            }

        // 3. Check for Deep Link (If app opened from Notification)
        checkDeepLink(intent)

        // --- PRODUCTION TOKEN SYNC ---
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) return@OnCompleteListener

            val token = task.result
            val installationId = SharedPreferencesManager.getInstallationId(this)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val entry = FcmTokenEntry(installationId, token)
                    supabase.from("fcm_tokens").upsert(entry, onConflict = "installation_id")
                } catch (e: Exception) {
                    Log.e("EchoSync", "Token sync failed", e)
                }
            }
        })

        setContent {
            EchoTheme {
                val context = LocalContext.current
                val navController = rememberNavController()

                val notificationLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                    onResult = { /* Permission handled */ }
                )

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                NavHost(navController = navController, startDestination = "splash_screen") {
                    composable("splash_screen") { SplashScreen(navController) }
                    composable("void_feed") {
                        // 4. Pass the Activity-scoped ViewModel to the Screen
                        VoidScreen(viewModel = mainViewModel)
                    }
                }
            }
        }
    }

    // 5. Handle Deep Links if app is already running in background
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        checkDeepLink(intent)
    }

    private fun checkDeepLink(intent: Intent?) {
        // Look for 'echo_id' (This matches what we set in EchoFirebaseMessagingService)
        val echoId = intent?.getStringExtra("echo_id")

        if (echoId != null) {
            Log.d("EchoDeepLink", "Found deep link ID: $echoId")
            mainViewModel.handleDeepLink(echoId)
        }
    }

    // --- HELPER: UNLOCK 120Hz ---
    private fun enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val window = this.window
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                this.display
            } else {
                @Suppress("DEPRECATION")
                window.windowManager.defaultDisplay
            }

            display?.let {
                val modes = it.supportedModes
                // Find the mode with the highest refresh rate
                val maxMode = modes.maxByOrNull { mode -> mode.refreshRate }

                maxMode?.let { mode ->
                    val lp = window.attributes
                    lp.preferredDisplayModeId = mode.modeId
                    window.attributes = lp
                    Log.d("EchoDisplay", "Set refresh rate to: ${mode.refreshRate} Hz")
                }
            }
        }
    }
}