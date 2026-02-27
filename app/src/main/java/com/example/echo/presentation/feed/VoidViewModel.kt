package com.example.echo.presentation.feed

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.NotificationDto
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
    private val repository: EchoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // --- CATEGORY SYSTEM STATE ---
    val categories = listOf("All", "Advice", "Vent", "Relationships", "Inner Thoughts")

    private val _selectedFeedCategory = MutableStateFlow("All")
    val selectedFeedCategory: StateFlow<String> = _selectedFeedCategory.asStateFlow()

    fun setFeedCategory(category: String) {
        _selectedFeedCategory.value = category
    }

    // --- FREQUENCY / EMOTION STATE ---
    // 0 = Angry, 1 = Sad, 2 = Neutral, 3 = Reflective, 4 = Hopeful, 5 = Happy
    private val _sessionFrequency = MutableStateFlow(2)
    val sessionFrequency: StateFlow<Int> = _sessionFrequency.asStateFlow()

    private val _showFrequencyModal = MutableStateFlow(true)
    val showFrequencyModal: StateFlow<Boolean> = _showFrequencyModal.asStateFlow()

    fun setFrequency(level: Int) {
        _sessionFrequency.value = level
        _showFrequencyModal.value = false
        // Re-sort current echoes based on new frequency immediately
        _echoes.value = sortEchoesByFrequency(_echoes.value, level)
    }

    fun dismissFrequencyModal() {
        _showFrequencyModal.value = false
    }

    fun openFrequencyModal() {
        _showFrequencyModal.value = true
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

    private val _hasUnread = MutableStateFlow(false)
    val hasUnread: StateFlow<Boolean> = _hasUnread.asStateFlow()

    // 5. UI STATE: Get Started Pop-up
    private val _showGetStarted = MutableStateFlow(false)
    val showGetStarted: StateFlow<Boolean> = _showGetStarted.asStateFlow()

    private val _deepLinkEcho = MutableStateFlow<EchoDto?>(null)
    val deepLinkEcho: StateFlow<EchoDto?> = _deepLinkEcho.asStateFlow()

    fun getUserId(): String = SharedPreferencesManager.getInstallationId(context)

    init {
        _showGetStarted.value = SharedPreferencesManager.isFirstLaunch(context)
        loadFeed()
        startSoulSimulation()
    }

    // --- EMOTIONAL SORTING LOGIC ---
    private fun sortEchoesByFrequency(list: List<EchoDto>, frequency: Int): List<EchoDto> {
        return list.sortedWith(compareByDescending<EchoDto> {
            // Priority 1: Exact mood match
            it.mood == frequency
        }.thenByDescending {
            // Priority 2: Nearby moods (Resonance)
            it.mood?.let { m -> kotlin.math.abs(m - frequency) <= 1 } ?: false
        }.thenByDescending {
            // Priority 3: Freshness
            it.createdAt
        })
    }

    fun handleDeepLink(echoId: String) {
        viewModelScope.launch {
            if (_echoes.value.isEmpty()) delay(500)
            val echo = repository.getEchoById(echoId)
            if (echo != null) _deepLinkEcho.value = echo
        }
    }

    fun consumeDeepLink() {
        val echoId = _deepLinkEcho.value?.id
        if (echoId != null) SharedPreferencesManager.clearUnreadReplyCount(context, echoId)
        _deepLinkEcho.value = null
    }

    fun loadFeed() {
        viewModelScope.launch {
            repository.getEchoes().collect { list ->
                _echoes.value = sortEchoesByFrequency(list, _sessionFrequency.value)
                _activeCount.value = list.size
                updateCount()
            }
            checkNotificationStatus()
        }
    }

    fun onRealtimeNotificationReceived() {
        _hasUnread.value = true
    }

    fun markGetStartedSeen() {
        SharedPreferencesManager.setFirstLaunchComplete(context)
        _showGetStarted.value = false
    }

    fun replayGetStarted() {
        _showGetStarted.value = true
    }

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

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            delay(1000)
            repository.getEchoes().collect { list ->
                _echoes.value = sortEchoesByFrequency(list, _sessionFrequency.value)
                _activeCount.value = list.size
                updateCount()
                _isRefreshing.value = false
            }
            checkNotificationStatus()
        }
    }

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

    // ✅ UPDATED: Now passes the session frequency to the repository
    fun postEcho(content: String, category: String, colorCode: String? = null) {
        viewModelScope.launch {
            repository.createEcho(
                content = content,
                category = category,
                mood = _sessionFrequency.value, // Pass memory state to DB
                colorCode = colorCode
            )
            delay(500)
            loadFeed()
        }
    }

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