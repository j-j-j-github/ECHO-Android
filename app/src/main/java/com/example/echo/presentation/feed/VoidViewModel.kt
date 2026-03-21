package com.example.echo.presentation.feed

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.NotificationDto
import com.example.echo.data.remote.dto.UserStatsDto
import com.example.echo.data.remote.dto.MoodHistoryDto
import com.example.echo.domain.repository.EchoRepository
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.round
import kotlin.math.sin
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class VoidViewModel @Inject constructor(
    private val repository: EchoRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val appUpdateManager = AppUpdateManagerFactory.create(context)

    val categories = listOf("All", "Advice", "Vent", "Relationships", "Inner Thoughts")

    private val _selectedFeedCategory = MutableStateFlow("All")
    val selectedFeedCategory: StateFlow<String> = _selectedFeedCategory.asStateFlow()

    // ✅ FIXED: Added ': String' type annotation
    fun setFeedCategory(category: String) {
        _selectedFeedCategory.value = category
    }

    private val _sessionFrequency = MutableStateFlow(2)
    val sessionFrequency: StateFlow<Int> = _sessionFrequency.asStateFlow()

    private val _showFrequencyModal = MutableStateFlow(true)
    val showFrequencyModal: StateFlow<Boolean> = _showFrequencyModal.asStateFlow()

    fun setFrequency(level: Int) {
        _sessionFrequency.value = level
        _showFrequencyModal.value = false
        _echoes.value = sortEchoesByFrequency(_echoes.value, level)
    }

    fun dismissFrequencyModal() { _showFrequencyModal.value = false }
    fun openFrequencyModal() { _showFrequencyModal.value = true }

    private val _echoes = MutableStateFlow<List<EchoDto>>(emptyList())
    val echoes: StateFlow<List<EchoDto>> = _echoes.asStateFlow()

    private val _activeSouls = MutableStateFlow(0)
    val activeSouls: StateFlow<Int> = _activeSouls.asStateFlow()
    private val _activeCount = MutableStateFlow(0)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationDto>>(emptyList())
    val notifications: StateFlow<List<NotificationDto>> = _notifications.asStateFlow()

    private val _isNotificationPanelOpen = MutableStateFlow(false)
    val isNotificationPanelOpen: StateFlow<Boolean> = _isNotificationPanelOpen.asStateFlow()

    private val _hasUnread = MutableStateFlow(false)
    val hasUnread: StateFlow<Boolean> = _hasUnread.asStateFlow()

    private val _showGetStarted = MutableStateFlow(false)
    val showGetStarted: StateFlow<Boolean> = _showGetStarted.asStateFlow()

    private val _deepLinkEcho = MutableStateFlow<EchoDto?>(null)
    val deepLinkEcho: StateFlow<EchoDto?> = _deepLinkEcho.asStateFlow()

    private val _showRatePopup = MutableStateFlow(false)
    val showRatePopup = _showRatePopup.asStateFlow()

    private val _showJourneyOverlay = MutableStateFlow(false)
    val showJourneyOverlay: StateFlow<Boolean> = _showJourneyOverlay.asStateFlow()

    private val _userStats = MutableStateFlow<UserStatsDto?>(null)
    val userStats: StateFlow<UserStatsDto?> = _userStats.asStateFlow()

    private val _dominantMood = MutableStateFlow<Int?>(null)
    val dominantMood: StateFlow<Int?> = _dominantMood.asStateFlow()

    private val _activityStreak = MutableStateFlow(0)
    val activityStreak: StateFlow<Int> = _activityStreak.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    fun toggleJourneyOverlay() {
        val current = _showJourneyOverlay.value
        if (!current) {
            loadJourneyData()
        }
        _showJourneyOverlay.value = !current
    }

    fun closeJourneyOverlay() {
        _showJourneyOverlay.value = false
    }

    private fun loadJourneyData() {
        viewModelScope.launch {
            try {
                // 1. Fetch live telemetry from Supabase
                val stats = repository.getUserStats()
                _userStats.value = stats

                // ✅ Update UI name from DB
                _displayName.value = stats?.displayName

                // 2. Mood processing
                val currentEchoes = _echoes.value
                val myId = getUserId()
                val myRecentPosts = currentEchoes.filter { it.signatureId == myId }

                if (myRecentPosts.isNotEmpty()) {
                    val mode = myRecentPosts.mapNotNull { it.mood }
                        .groupingBy { it }
                        .eachCount()
                        .maxByOrNull { it.value }?.key

                    _dominantMood.value = mode ?: myRecentPosts.first().mood
                    val history = repository.getMoodHistory()
                    _activityStreak.value = calculateStreak(history)
                } else {
                    val history = repository.getMoodHistory()
                    if (history.isNotEmpty()) {
                        val historyMode = history.groupingBy { it.mood }
                            .eachCount()
                            .maxByOrNull { it.value }?.key
                        _dominantMood.value = historyMode ?: history.first().mood
                        _activityStreak.value = calculateStreak(history)
                    } else {
                        _dominantMood.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("JOURNEY_LOG", "Error: ${e.message}")
            }
        }
    }

    private fun calculateStreak(history: List<MoodHistoryDto>): Int {
        if (history.isEmpty()) return 0
        val zoneId = ZoneId.of("UTC")
        val today = LocalDate.now(zoneId)

        val activeDates = history.mapNotNull {
            try {
                Instant.parse(it.createdAt).atZone(zoneId).toLocalDate()
            } catch (e: Exception) { null }
        }.distinct().sortedDescending()

        if (activeDates.isEmpty()) return 0
        if (activeDates.first().isBefore(today.minusDays(1))) return 0

        var streak = 1
        for (i in 0 until activeDates.size - 1) {
            if (ChronoUnit.DAYS.between(activeDates[i + 1], activeDates[i]) == 1L) {
                streak++
            } else break
        }
        return streak
    }

    // ==========================================
    // --- IDENTITY MANAGEMENT LOGIC ---
    // ==========================================

    fun updateDisplayName(newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return

        viewModelScope.launch {
            try {
                _displayName.value = cleanName
                // ✅ Persists change to Supabase
                repository.updateUserDisplayName(cleanName)
            } catch (e: Exception) {
                Log.e("IDENTITY_SYNC", "Failed: ${e.message}")
            }
        }
    }

    suspend fun restoreIdentity(newKey: String): Boolean {
        val cleanKey = newKey.trim()
        if (cleanKey.isBlank() || cleanKey.length < 5) return false

        return try {
            val isDatabaseCheckSuccessful = repository.checkIfUserExists(cleanKey)
            if (isDatabaseCheckSuccessful) {
                SharedPreferencesManager.clearUserLocalCache(context)
                SharedPreferencesManager.saveInstallationId(context, cleanKey)
                _echoes.value = emptyList()
                _userStats.value = null
                _dominantMood.value = null
                _displayName.value = null
                loadFeed()
                loadJourneyData()
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getUserId(): String = SharedPreferencesManager.getInstallationId(context)

    init {
        _showGetStarted.value = SharedPreferencesManager.isFirstLaunch(context)
        loadFeed()
        // ✅ Startup loading of name/stats
        viewModelScope.launch {
            loadJourneyData()
        }
        startSoulSimulation()
        checkForUpdates()
    }

    private fun checkForUpdates() {
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo
        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) { /* Update check logic */ }
        }
    }

    fun triggerUpdateFlow(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.FLEXIBLE, activity, 1001)
            }
        }
    }

    private fun sortEchoesByFrequency(list: List<EchoDto>, frequency: Int): List<EchoDto> {
        return list.sortedWith(compareByDescending<EchoDto> {
            it.mood == frequency
        }.thenByDescending {
            it.mood?.let { m -> kotlin.math.abs(m - frequency) <= 1 } ?: false
        }.thenByDescending {
            it.createdAt
        })
    }

    fun handleDeepLink(echoId: String) {
        // ✅ SUPPRESS FREQUENCY MODAL: The user wants to see a post, don't interrupt them!
        _showFrequencyModal.value = false
        // Reset the timer so the modal doesn't annoy them right after they close the post
        SharedPreferencesManager.saveLastFrequencyTime(context, System.currentTimeMillis())

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
                if (_showJourneyOverlay.value) loadJourneyData()
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
        // ✅ SUPPRESS FREQUENCY MODAL here too, just in case.
        _showFrequencyModal.value = false
        SharedPreferencesManager.saveLastFrequencyTime(context, System.currentTimeMillis())

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
                if (_showJourneyOverlay.value) loadJourneyData()
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

    fun postEcho(content: String, category: String, colorCode: String? = null) {
        viewModelScope.launch {
            repository.createEcho(
                content = content,
                category = category,
                mood = _sessionFrequency.value,
                colorCode = colorCode
            )
            delay(800)
            repository.getEchoes().collect { list ->
                _echoes.value = sortEchoesByFrequency(list, _sessionFrequency.value)
                loadJourneyData()
                _activeCount.value = list.size
                updateCount()
            }
            checkReviewTrigger()
        }
    }

    private fun checkReviewTrigger() {
        viewModelScope.launch {
            val stats = repository.getUserStats()
            if (stats?.totalEchoesPosted != null && stats.totalEchoesPosted >= 3) {
                _showRatePopup.value = true
            }
        }
    }

    fun submitEchoPlusApplication(email: String) {
        viewModelScope.launch {
            try {
                val signatureId = SharedPreferencesManager.getInstallationId(context)
                repository.submitLead(email, signatureId)
            } catch (e: Exception) {
                Log.e("LEAD_GEN", "Error: ${e.message}")
            }
        }
    }

    fun resetRateTrigger() {
        _showRatePopup.value = false
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