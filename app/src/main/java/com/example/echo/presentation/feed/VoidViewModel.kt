package com.example.echo.presentation.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.NotificationDto
import com.example.echo.data.repository.EchoRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.round
import kotlin.math.sin
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class VoidViewModel @Inject constructor(
    private val repository: EchoRepositoryImpl,
    @ApplicationContext private val context: Context // NEW: Inject Context for SharedPreferences
) : ViewModel() {

    // 1. DATA: Echoes
    private val _echoes = MutableStateFlow<List<EchoDto>>(emptyList())
    val echoes: StateFlow<List<EchoDto>> = _echoes.asStateFlow()

    // 2. DATA: Active Souls
    private val _activeSouls = MutableStateFlow(0)
    val activeSouls: StateFlow<Int> = _activeSouls.asStateFlow()

    private val _activeCount = MutableStateFlow(0)

    // 3. UI STATE: Refreshing Loading Indicator
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // 4. UI STATE: Notifications
    private val _notifications = MutableStateFlow<List<NotificationDto>>(emptyList())
    val notifications: StateFlow<List<NotificationDto>> = _notifications.asStateFlow()

    private val _isNotificationPanelOpen = MutableStateFlow(false)
    val isNotificationPanelOpen: StateFlow<Boolean> = _isNotificationPanelOpen.asStateFlow()

    // 5. UI STATE: Get Started Pop-up (NEW)
    private val _showGetStarted = MutableStateFlow(false)
    val showGetStarted: StateFlow<Boolean> = _showGetStarted.asStateFlow()

    init {
        // Check if this is the first launch
        _showGetStarted.value = SharedPreferencesManager.isFirstLaunch(context)

        loadEchoes()
        startSoulSimulation()
    }

    // --- GET STARTED ACTIONS (NEW) ---

    fun markGetStartedSeen() {
        SharedPreferencesManager.setFirstLaunchComplete(context)
        _showGetStarted.value = false
    }

    fun replayGetStarted() {
        _showGetStarted.value = true
    }

    private fun loadEchoes() {
        viewModelScope.launch {
            repository.getEchoes().collect { list ->
                _echoes.value = list
                _activeCount.value = list.size
                updateCount()
            }
        }
    }

    // --- NOTIFICATION ACTIONS ---

    fun toggleNotificationPanel() {
        val current = _isNotificationPanelOpen.value
        if (!current) {
            // If opening, fetch fresh notifications
            loadNotifications()
        }
        _isNotificationPanelOpen.value = !current
    }

    fun closeNotificationPanel() {
        _isNotificationPanelOpen.value = false
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            val list = repository.getUserNotifications()
            _notifications.value = list
        }
    }

    fun openEchoFromNotification(echoId: String, onLoaded: (EchoDto) -> Unit) {
        viewModelScope.launch {
            // Fetch the specific echo to open in detail view
            val echo = repository.getEchoById(echoId)
            if (echo != null) {
                onLoaded(echo)
                _isNotificationPanelOpen.value = false // Close panel automatically
            }
        }
    }

    // --- REFRESH ACTION ---
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1000) // Artificial delay for better UX

            repository.getEchoes().collect { list ->
                _echoes.value = list
                _activeCount.value = list.size
                updateCount()
                _isRefreshing.value = false
            }
        }
    }

    // --- SIMULATION LOGIC ---
    private fun startSoulSimulation() {
        viewModelScope.launch {
            while (true) {
                updateCount()
                delay(30_000L + Random.nextLong(60_000L))
            }
        }
    }

    private fun updateCount() {
        val baseCount = if (_activeCount.value > 0) _activeCount.value else 3
        val now = System.currentTimeMillis()
        val timeVariance = round(sin(now / 600000.0) * 2).toInt()
        val dailySalt = LocalDate.now(ZoneId.of("UTC")).dayOfMonth % 3
        _activeSouls.value = (baseCount + timeVariance + dailySalt).coerceAtLeast(3)
    }

    // --- USER ACTIONS ---
    fun postEcho(content: String) {
        viewModelScope.launch {
            repository.createEcho(content)
            delay(500)
            loadEchoes()
        }
    }

    fun postReply(echoId: String, content: String) {
        viewModelScope.launch {
            repository.createReply(echoId, content)
            delay(500)
            loadEchoes()
        }
    }

    fun reportEcho(echoId: String) {
        viewModelScope.launch {
            repository.reportEcho(echoId)
            _echoes.value = _echoes.value.filter { it.id != echoId }
        }
    }
}