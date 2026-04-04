package com.example.echo.presentation.feed

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.core.utils.NetworkMonitor
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.NotificationDto
import com.example.echo.data.remote.dto.UserStatsDto
import com.example.echo.data.remote.dto.MoodHistoryDto
import com.example.echo.domain.repository.EchoRepository
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.ZoneId
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs // ✅ FIXED: Added missing import
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
    private val networkMonitor = NetworkMonitor(context)
    private val syncMutex = Mutex()

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    val categories = listOf("All", "Advice", "Vent", "Relationships", "Inner Thoughts")

    private val _selectedFeedCategory = MutableStateFlow("All")
    val selectedFeedCategory: StateFlow<String> = _selectedFeedCategory.asStateFlow()

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
        checkAndShowDailyLoginToast()
    }

    fun dismissFrequencyModal() {
        _showFrequencyModal.value = false
        checkAndShowDailyLoginToast()
    }

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
    private val sessionSystemAlerts = mutableListOf<NotificationDto>()
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

    private val _userActivity = MutableStateFlow<List<EchoDto>>(emptyList())
    val userActivity: StateFlow<List<EchoDto>> = _userActivity.asStateFlow()

    private val _moodHistory = MutableStateFlow<List<MoodHistoryDto>>(emptyList())
    val moodHistory: StateFlow<List<MoodHistoryDto>> = _moodHistory.asStateFlow()

    private val _displayName = MutableStateFlow<String?>(null)
    val displayName: StateFlow<String?> = _displayName.asStateFlow()

    private var lastTrackedEnergy = -1
    private var pendingDailyLoginToast = false

    private val _energyToastMessage = MutableStateFlow<Pair<String, Boolean>?>(null)
    val energyToastMessage: StateFlow<Pair<String, Boolean>?> = _energyToastMessage.asStateFlow()

    init {
        _showGetStarted.value = !SharedPreferencesManager.hasSeenOnboardingV2(context)

        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _isOffline.value = !online
                if (online) {
                    syncMutex.withLock {
                        val userId = getUserId()
                        if (userId.isNotBlank() && !userId.contains("00000000")) {
                            val prefs = context.getSharedPreferences("echo_prefs", Context.MODE_PRIVATE)
                            val lastLogin = prefs.getString("LAST_LOGIN_DATE", "")
                            val today = LocalDate.now().toString()

                            if (lastLogin != today) {
                                pendingDailyLoginToast = true
                                prefs.edit().putString("LAST_LOGIN_DATE", today).apply()
                            }

                            val pendingEchoes = prefs.getInt("pending_offline_echoes", 0)
                            val pendingReplies = prefs.getInt("pending_offline_replies", 0)

                            if (pendingEchoes > 0 || pendingReplies > 0) {
                                val success = repository.syncOfflineEnergy(userId, pendingEchoes, pendingReplies)
                                if (success) {
                                    prefs.edit()
                                        .putInt("pending_offline_echoes", 0)
                                        .putInt("pending_offline_replies", 0)
                                        .apply()

                                    _energyToastMessage.value = Pair("Void synced. Offline contributions rewarded.", false)
                                    refreshUserStats()
                                    launch { delay(3500); _energyToastMessage.value = null }
                                }
                            }
                            repository.processDailyLogin(userId)
                        }
                        repository.syncPendingOfflineData()
                        loadFeed()
                        loadJourneyData()
                    }
                }
            }
        }
        startSoulSimulation()
        checkForUpdates()
    }

    fun toggleJourneyOverlay() {
        if (!_showJourneyOverlay.value) loadJourneyData()
        _showJourneyOverlay.value = !_showJourneyOverlay.value
    }


    fun closeJourneyOverlay() { _showJourneyOverlay.value = false }

    fun clearEnergyToast() { _energyToastMessage.value = null }

    fun refreshUserStats() {
        viewModelScope.launch {
            try {
                repository.getUserStats()?.let { stats ->
                    _userStats.value = stats
                    stats.energy?.let { checkEnergyMilestones(it) }
                }
            } catch (e: Exception) {
                Log.e("STATS_REFRESH", "Error: ${e.message}")
            }
        }
    }

    private fun loadJourneyData() {
        viewModelScope.launch {
            try {
                repository.getUserStats()?.let { stats ->
                    _userStats.value = stats
                    _displayName.value = stats.displayName
                    if (lastTrackedEnergy == -1) lastTrackedEnergy = stats.energy ?: 0
                }
                val myActivity = repository.getUserActivity()
                val myId = getUserId()
                _userActivity.value = myActivity

                if (myActivity.isNotEmpty()) {
                    _dominantMood.value = myActivity.mapNotNull { it.mood }
                        .groupingBy { it }.eachCount()
                        .maxByOrNull { it.value }?.key
                }
                val history = repository.getMoodHistory()
                _moodHistory.value = history
                _activityStreak.value = calculateStreak(history, myActivity, myId)
            } catch (e: Exception) { Log.e("JOURNEY_LOG", "Error: ${e.message}") }
        }
    }

    private fun calculateStreak(history: List<MoodHistoryDto>, allEchoes: List<EchoDto>, userId: String): Int {
        val zoneId = ZoneId.systemDefault()
        val today = LocalDate.now(zoneId)
        val activeDates = mutableSetOf<LocalDate>()

        history.forEach { try { activeDates.add(Instant.parse(it.createdAt).atZone(zoneId).toLocalDate()) } catch (e: Exception) {} }
        allEchoes.forEach { echo ->
            if (echo.signatureId == userId) try { activeDates.add(Instant.parse(echo.createdAt).atZone(zoneId).toLocalDate()) } catch (e: Exception) {}
            echo.replies?.forEach { reply ->
                try {
                    val replyClass = reply!!::class.java
                    val sigId = (replyClass.methods.find { it.name.contains("SignatureId", true) }?.invoke(reply) as? String)
                        ?: (replyClass.methods.find { it.name.contains("UserId", true) }?.invoke(reply) as? String)
                    val createdAt = replyClass.methods.find { it.name.contains("CreatedAt", true) }?.invoke(reply) as? String
                    if (sigId == userId && createdAt != null) activeDates.add(Instant.parse(createdAt).atZone(zoneId).toLocalDate())
                } catch(e: Exception) {}
            }
        }
        val sortedDates = activeDates.sortedDescending()
        if (sortedDates.isEmpty() || sortedDates.first().isBefore(today.minusDays(1))) return 0
        var streak = 1
        for (i in 0 until sortedDates.size - 1) {
            if (ChronoUnit.DAYS.between(sortedDates[i + 1], sortedDates[i]) == 1L) streak++ else break
        }
        return streak
    }

    fun loadFeed() {
        viewModelScope.launch {
            try {
                repository.getEchoes().collect { list ->
                    _echoes.value = sortEchoesByFrequency(list, _sessionFrequency.value)
                    _activeCount.value = list.size
                    updateCount()
                    if (_showJourneyOverlay.value) loadJourneyData()
                }
                checkNotificationStatus()
            } catch (e: Exception) { Log.e("FEED", "Feed fetch failed") }
        }
    }

    fun updateDisplayName(newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return
        viewModelScope.launch {
            try {
                _displayName.value = cleanName
                repository.updateUserDisplayName(cleanName)
            } catch (e: Exception) { Log.e("IDENTITY_SYNC", "Failed") }
        }
    }

    suspend fun restoreIdentity(newKey: String): Boolean {
        val cleanKey = newKey.trim()
        if (cleanKey.isBlank() || cleanKey.length < 5) return false
        return try {
            if (repository.checkIfUserExists(cleanKey)) {
                SharedPreferencesManager.clearUserLocalCache(context)
                SharedPreferencesManager.saveInstallationId(context, cleanKey)
                _echoes.value = emptyList()
                _userStats.value = null
                loadFeed()
                loadJourneyData()
                true
            } else false
        } catch (e: Exception) { false }
    }

    fun getUserId(): String = SharedPreferencesManager.getInstallationId(context)

    private fun checkForUpdates() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) { /* FLEXIBLE */ } }
    }

    fun triggerUpdateFlow(activity: Activity, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) appUpdateManager.startUpdateFlowForResult(it, AppUpdateType.FLEXIBLE, activity, 1001) }
    }

    private fun sortEchoesByFrequency(list: List<EchoDto>, frequency: Int): List<EchoDto> {
        return list.sortedWith(compareByDescending<EchoDto> { it.mood == frequency }.thenByDescending { it.mood?.let { m -> abs(m - frequency) <= 1 } ?: false }.thenByDescending { it.createdAt })
    }

    fun handleDeepLink(echoId: String) {
        _showFrequencyModal.value = false
        SharedPreferencesManager.saveLastFrequencyTime(context, System.currentTimeMillis())
        viewModelScope.launch {
            if (_echoes.value.isEmpty()) delay(500)
            repository.getEchoById(echoId)?.let { _deepLinkEcho.value = it }
        }
    }

    fun consumeDeepLink() {
        _deepLinkEcho.value?.id?.let { SharedPreferencesManager.clearUnreadReplyCount(context, it) }
        _deepLinkEcho.value = null
    }

    fun onRealtimeNotificationReceived() { _hasUnread.value = true }

    fun markGetStartedSeen() {
        SharedPreferencesManager.markOnboardingV2Seen(context)
        SharedPreferencesManager.setFirstLaunchComplete(context)
        _showGetStarted.value = false
    }

    fun replayGetStarted() { _showGetStarted.value = true }

    private fun checkNotificationStatus() {
        viewModelScope.launch { if (repository.hasUnreadNotifications()) _hasUnread.value = true }
    }

    fun toggleNotificationPanel() {
        if (!_isNotificationPanelOpen.value) {
            loadNotifications()
            repository.markNotificationsAsRead()
            _hasUnread.value = false
        }
        _isNotificationPanelOpen.value = !_isNotificationPanelOpen.value
    }

    fun closeNotificationPanel() { _isNotificationPanelOpen.value = false }

    private fun loadNotifications() {
        viewModelScope.launch { _notifications.value = sessionSystemAlerts + repository.getUserNotifications() }
    }

    fun openEchoFromNotification(echoId: String, onLoaded: (EchoDto) -> Unit) {
        _showFrequencyModal.value = false
        SharedPreferencesManager.saveLastFrequencyTime(context, System.currentTimeMillis())
        viewModelScope.launch {
            SharedPreferencesManager.clearUnreadReplyCount(context, echoId)
            repository.getEchoById(echoId)?.let { onLoaded(it); _isNotificationPanelOpen.value = false }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                delay(1000)
                repository.getEchoes().collect { list ->
                    _echoes.value = sortEchoesByFrequency(list, _sessionFrequency.value)
                    _activeCount.value = list.size
                    updateCount()
                    if (_showJourneyOverlay.value) loadJourneyData()
                }
                checkNotificationStatus()
            } catch (e: Exception) { Log.e("REFRESH", "Failed") } finally { _isRefreshing.value = false }
        }
    }

    private fun checkEnergyMilestones(newEnergy: Int) {
        val prefs = context.getSharedPreferences("echo_prefs", Context.MODE_PRIVATE)
        val thresholds = listOf(25, 50, 75, 100)
        thresholds.forEach { target ->
            val milestoneKey = "milestone_v2_reached_$target"
            if (!prefs.getBoolean(milestoneKey, false) && newEnergy >= target) {
                val message = when(target) {
                    25 -> "A spark ignites. Energy at 25%."
                    50 -> "Halfway there. Your voice holds weight. Energy at 50%."
                    75 -> "Almost full capacity. Energy at 75%."
                    100 -> "Energy full. The Spotlight awaits."
                    else -> "Energy reached $target%."
                }
                val systemAlert = NotificationDto(java.util.UUID.randomUUID().toString(), "SYSTEM", message, Instant.now().toString(), null)
                sessionSystemAlerts.add(0, systemAlert)
                _notifications.value = sessionSystemAlerts + _notifications.value.filter { it.echoId != "SYSTEM" }
                _hasUnread.value = true
                prefs.edit().putBoolean(milestoneKey, true).apply()
            }
        }
    }

    private fun startSoulSimulation() {
        viewModelScope.launch { while (true) { updateCount(); delay(30_000L + Random.nextLong(60_000L)) } }
    }

    private fun updateCount() {
        val baseCount = if (_activeCount.value > 0) _activeCount.value else 3
        val now = System.currentTimeMillis()
        val timeVariance = round(sin(now / 600000.0) * 2).toInt()
        val dailySalt = LocalDate.now(ZoneId.of("UTC")).dayOfMonth % 3
        _activeSouls.value = (baseCount + timeVariance + dailySalt).coerceAtLeast(3)
    }

    fun postEcho(content: String, category: String, colorCode: String? = null, useSpotlight: Boolean = false) {
        viewModelScope.launch {
            val id = getUserId()
            if (id.isBlank() || id.contains("00000000")) return@launch

            if (_isOffline.value) {
                _energyToastMessage.value = Pair("You're Offline. Action queued.", true)
                val prefs = context.getSharedPreferences("echo_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("pending_offline_echoes", prefs.getInt("pending_offline_echoes", 0) + 1).apply()
                repository.createEchoWithId(java.util.UUID.randomUUID().toString(), content, category, _sessionFrequency.value, colorCode)
                delay(3500); _energyToastMessage.value = null
                return@launch
            }

            val newEchoId = java.util.UUID.randomUUID().toString()
            repository.createEchoWithId(newEchoId, content, category, _sessionFrequency.value, colorCode)

            if (useSpotlight) {
                when (repository.activateSpotlight(newEchoId, category)) {
                    "active" -> {
                        // ✅ FIXED: Used named parameters here to resolve the type mismatch error
                        val spotlightEcho = EchoDto(
                            id = newEchoId,
                            content = content,
                            category = category,
                            mood = _sessionFrequency.value,
                            signatureId = id,
                            colorCode = colorCode,
                            isSpotlight = true,
                            createdAt = Instant.now().toString()
                        )
                        _echoes.value = listOf(spotlightEcho) + _echoes.value
                        _energyToastMessage.value = Pair("Spotlight Active. Your voice is echoing.", false)
                    }
                    "queued" -> _energyToastMessage.value = Pair("Added to Spotlight Queue. Will echo soon.", false)
                    else -> _energyToastMessage.value = Pair("Not enough energy or network error.", true)
                }
            } else {
                val currentEnergy = _userStats.value?.energy ?: 0
                val echoesToday = _userStats.value?.dailyEchoesCounted ?: 0
                if (currentEnergy >= 100) _energyToastMessage.value = Pair("Energy full. Spotlight enabled.", false)
                else if (echoesToday >= 2) _energyToastMessage.value = Pair("Daily Energy limit reached for Echoes.", true)
                else _energyToastMessage.value = Pair("Your Echo added Energy.", false)
            }
            refreshUserStats()
            launch { delay(3500); _energyToastMessage.value = null }
            delay(800); loadFeed(); checkReviewTrigger()
        }
    }

    fun postEcho(content: String, category: String, colorCode: String? = null) = postEcho(content, category, colorCode, false)

    private fun checkReviewTrigger() {
        viewModelScope.launch { if ((repository.getUserStats()?.totalEchoesPosted ?: 0) >= 3) _showRatePopup.value = true }
    }

    fun submitEchoPlusApplication(email: String) {
        viewModelScope.launch { try { repository.submitLead(email, getUserId()) } catch (e: Exception) { Log.e("LEAD_GEN", "${e.message}") } }
    }

    fun relateToEcho(echoId: String) {
        // ✅ 1. OPTIMISTIC UPDATE: Change the UI state INSTANTLY
        _echoes.value = _echoes.value.map {
            if (it.id == echoId) it.copy(likes = (it.likes ?: 0) + 1, isRelated = true) else it
        }

        // ✅ 2. Fire and forget the network request in the background
        viewModelScope.launch {
            try {
                repository.relateToEcho(echoId)
                // No need to update state here unless you want to sync exact server count
            } catch (e: Exception) {
                Log.e("RELATE", "Error", e)
                // ❌ REVERT: If the network fails, flip it back so the user isn't lied to
                _echoes.value = _echoes.value.map {
                    if (it.id == echoId) it.copy(likes = maxOf(0, (it.likes ?: 1) - 1), isRelated = false) else it
                }
            }
        }
    }

    fun unrelateFromEcho(echoId: String) {
        // ✅ 1. OPTIMISTIC UPDATE: Change the UI state INSTANTLY
        _echoes.value = _echoes.value.map {
            if (it.id == echoId) it.copy(likes = maxOf(0, (it.likes ?: 1) - 1), isRelated = false) else it
        }

        // ✅ 2. Network call in background
        viewModelScope.launch {
            try {
                repository.unrelateFromEcho(echoId)
            } catch (e: Exception) {
                Log.e("UNRELATE", "Error", e)
                // ❌ REVERT on failure
                _echoes.value = _echoes.value.map {
                    if (it.id == echoId) it.copy(likes = (it.likes ?: 0) + 1, isRelated = true) else it
                }
            }
        }
    }

    fun resetRateTrigger() { _showRatePopup.value = false }

    fun updateEcho(echoId: String, content: String) {
        viewModelScope.launch { repository.updateEcho(echoId, content); delay(500); loadFeed() }
    }

    fun postReply(echoId: String, content: String) {
        viewModelScope.launch {
            val id = getUserId()
            if (id.isBlank() || id.contains("00000000")) {
                Log.e("POST_ERROR", "Invalid Signature ID. Aborting reply.")
                return@launch
            }

            if (_isOffline.value) {
                _energyToastMessage.value = Pair("Offline. Resonance saved for later.", true)
                val prefs = context.getSharedPreferences("echo_prefs", Context.MODE_PRIVATE)
                prefs.edit().putInt("pending_offline_replies", prefs.getInt("pending_offline_replies", 0) + 1).apply()
                repository.createReply(echoId, content)
                delay(3500); _energyToastMessage.value = null
                return@launch
            }

            val currentEnergy = _userStats.value?.energy ?: 0
            val resonancesToday = _userStats.value?.dailyResonancesCounted ?: 0
            repository.createReply(echoId, content)
            if (currentEnergy >= 100) _energyToastMessage.value = Pair("Energy full. Spotlight enabled.", false)
            else if (resonancesToday >= 10) _energyToastMessage.value = Pair("Daily limit reached.", true)
            else _energyToastMessage.value = Pair("Your Resonance added Energy.", false)

            refreshUserStats()
            launch { delay(3500); _energyToastMessage.value = null }
            loadFeed()
        }
    }

    fun updateReply(replyId: String, content: String) {
        viewModelScope.launch { repository.updateReply(replyId, content); delay(500); loadFeed() }
    }

    fun reportEcho(echoId: String) {
        viewModelScope.launch { repository.reportEcho(echoId); _echoes.value = _echoes.value.filter { it.id != echoId } }
    }

    fun onEchoClicked(echoId: String) { SharedPreferencesManager.clearUnreadReplyCount(context, echoId) }

    private fun checkAndShowDailyLoginToast() {
        if (pendingDailyLoginToast) {
            pendingDailyLoginToast = false
            viewModelScope.launch {
                val success = repository.awardDailyLoginEnergy(getUserId())
                if (success) {
                    delay(800)
                    _energyToastMessage.value = Pair("Daily check-in complete. Energy added.", false)
                    refreshUserStats()
                    delay(3500); _energyToastMessage.value = null
                }
            }
        }
    }

    fun triggerOfflineToast() {
        _energyToastMessage.value = Pair(" You're Offline. Action queued.", true)
        viewModelScope.launch { delay(3500); _energyToastMessage.value = null }
    }
}