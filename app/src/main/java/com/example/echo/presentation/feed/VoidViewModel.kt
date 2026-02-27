package com.example.echo.presentation.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.NotificationDto
// ✅ Changed to the Interface
import com.example.echo.domain.repository.EchoRepository
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
    private val repository: EchoRepository, // ✅ Changed to the Interface
    @ApplicationContext private val context: Context
) : ViewModel() {

    // --- NEW: CATEGORY SYSTEM STATE ---
    val categories = listOf("All", "Advice", "Vent", "Relationships", "Inner Thoughts")

    private val _selectedFeedCategory = MutableStateFlow("All")
    val selectedFeedCategory: StateFlow<String> = _selectedFeedCategory.asStateFlow()

    fun setFeedCategory(category: String) {
        _selectedFeedCategory.value = category
    }

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

    // --- UNREAD INDICATOR STATE ---
    private val _hasUnread = MutableStateFlow(false)
    val hasUnread: StateFlow<Boolean> = _hasUnread.asStateFlow()

    // 5. UI STATE: Get Started Pop-up
    private val _showGetStarted = MutableStateFlow(false)
    val showGetStarted: StateFlow<Boolean> = _showGetStarted.asStateFlow()

    // --- NEW: DEEP LINK STATE ---
    private val _deepLinkEcho = MutableStateFlow<EchoDto?>(null)
    val deepLinkEcho: StateFlow<EchoDto?> = _deepLinkEcho.asStateFlow()

    // --- NEW: EDIT STATE ---
    fun getUserId(): String = SharedPreferencesManager.getInstallationId(context)

    init {
        _showGetStarted.value = SharedPreferencesManager.isFirstLaunch(context)
        loadFeed()
        startSoulSimulation()
    }

    // --- DEEP LINK HANDLER ---
    fun handleDeepLink(echoId: String) {
        viewModelScope.launch {
            if (_echoes.value.isEmpty()) {
                delay(500)
            }
            val echo = repository.getEchoById(echoId)
            if (echo != null) {
                _deepLinkEcho.value = echo
            }
        }
    }

    // --- FIX: RESET COUNT ON CONSUME ---
    fun consumeDeepLink() {
        val echoId = _deepLinkEcho.value?.id
        if (echoId != null) {
            SharedPreferencesManager.clearUnreadReplyCount(context, echoId)
        }
        _deepLinkEcho.value = null
    }

    // --- PUBLIC RELOAD FUNCTION ---
    fun loadFeed() {
        viewModelScope.launch {
            repository.getEchoes().collect { list ->
                _echoes.value = list
                _activeCount.value = list.size
                updateCount()
            }
            checkNotificationStatus()
        }
    }

    fun onRealtimeNotificationReceived() {
        _hasUnread.value = true
    }

    // --- GET STARTED ACTIONS ---
    fun markGetStartedSeen() {
        SharedPreferencesManager.setFirstLaunchComplete(context)
        _showGetStarted.value = false
    }

    fun replayGetStarted() {
        _showGetStarted.value = true
    }

    // --- NOTIFICATION ACTIONS ---
    private fun checkNotificationStatus() {
        viewModelScope.launch {
            _hasUnread.value = repository.hasUnreadNotifications()
        }
    }

    fun toggleNotificationPanel() {
        val current = _isNotificationPanelOpen.value
        if (!current) {
            loadNotifications()
            repository.markNotificationsAsRead()
            _hasUnread.value = false
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
            SharedPreferencesManager.clearUnreadReplyCount(context, echoId)

            val echo = repository.getEchoById(echoId)
            if (echo != null) {
                onLoaded(echo)
                _isNotificationPanelOpen.value = false
            }
        }
    }

    // --- REFRESH ACTION ---
    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1000)

            repository.getEchoes().collect { list ->
                _echoes.value = list
                _activeCount.value = list.size
                updateCount()
                _isRefreshing.value = false
            }
            checkNotificationStatus()
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
    // ✅ NEW: Added category parameter to postEcho
    fun postEcho(content: String, category: String, colorCode: String? = null) {
        viewModelScope.launch {
            repository.createEcho(content, category, colorCode)
            delay(500)
            loadFeed()
        }
    }

    // NEW: Update existing Echo
    fun updateEcho(echoId: String, content: String) {
        viewModelScope.launch {
            repository.updateEcho(echoId, content)
            delay(500)
            loadFeed()
        }
    }

    fun postReply(echoId: String, content: String) {
        viewModelScope.launch {
            repository.createReply(echoId, content)
            loadFeed()
        }
    }

    // NEW: Update existing Resonance
    fun updateReply(replyId: String, content: String) {
        viewModelScope.launch {
            repository.updateReply(replyId, content)
            delay(500)
            loadFeed()
        }
    }

    fun reportEcho(echoId: String) {
        viewModelScope.launch {
            repository.reportEcho(echoId)
            _echoes.value = _echoes.value.filter { it.id != echoId }
        }
    }

    fun onEchoClicked(echoId: String) {
        SharedPreferencesManager.clearUnreadReplyCount(context, echoId)
    }
}