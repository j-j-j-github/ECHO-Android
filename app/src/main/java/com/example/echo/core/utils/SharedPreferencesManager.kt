package com.example.echo.core.utils

import android.content.Context
import java.util.UUID

object SharedPreferencesManager {
    private const val PREF_NAME = "EchoPreferences"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_INSTALLATION_ID = "installation_id"

    // Notification tracking
    private const val KEY_LAST_ECHO_ID = "last_seen_echo_id"
    private const val KEY_LAST_REPLY_COUNT = "last_reply_count"
    private const val KEY_LAST_CHECKED_TIME = "last_notification_checked_time"

    // --- NEW: REPLIED ECHOES TRACKING (Green Tick) ---
    private const val KEY_REPLIED_ECHOES = "replied_echoes_set"

    // --- NEW: NOTIFICATION GROUPING COUNTS ---
    private const val PREF_NOTIF_COUNTS = "notif_counts_pref"

    private fun getPrefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // --- IDENTITY METHOD ---
    fun getInstallationId(context: Context): String {
        val prefs = getPrefs(context)
        var id = prefs.getString(KEY_INSTALLATION_ID, null)
        if (id == null) {
            id = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_INSTALLATION_ID, id).apply()
        }
        return id!!
    }

    // --- NOTIFICATION TIMESTAMPS ---
    fun saveLastCheckedTime(context: Context, timestamp: String) {
        getPrefs(context).edit().putString(KEY_LAST_CHECKED_TIME, timestamp).apply()
    }

    fun getLastCheckedTime(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_CHECKED_TIME, "") ?: ""
    }

    // --- REPLIED ECHO ID (Green Tick Logic) ---
    fun saveRepliedEcho(context: Context, echoId: String) {
        val prefs = getPrefs(context)
        val currentSet = prefs.getStringSet(KEY_REPLIED_ECHOES, emptySet()) ?: emptySet()
        val newSet = HashSet(currentSet) // ✅ defensive copy
        newSet.add(echoId)
        prefs.edit().putStringSet(KEY_REPLIED_ECHOES, newSet).apply()
    }

    fun hasRepliedTo(context: Context, echoId: String): Boolean {
        val prefs = getPrefs(context)
        val currentSet = prefs.getStringSet(KEY_REPLIED_ECHOES, emptySet()) ?: emptySet()
        return currentSet.contains(echoId)
    }

    // --- NOTIFICATION GROUPING LOGIC ---

    fun getUnreadReplyCount(context: Context, echoId: String): Int {
        val prefs = context.getSharedPreferences(PREF_NOTIF_COUNTS, Context.MODE_PRIVATE)
        return prefs.getInt(echoId, 0)
    }

    fun incrementUnreadReplyCount(context: Context, echoId: String): Int {
        val prefs = context.getSharedPreferences(PREF_NOTIF_COUNTS, Context.MODE_PRIVATE)
        val current = prefs.getInt(echoId, 0)
        val newCount = current + 1
        prefs.edit().putInt(echoId, newCount).apply()
        return newCount
    }

    fun clearUnreadReplyCount(context: Context, echoId: String) {
        val prefs = context.getSharedPreferences(PREF_NOTIF_COUNTS, Context.MODE_PRIVATE)
        prefs.edit().remove(echoId).apply()
    }

    // ✅ NEW — reset count explicitly when a new echo is created
    fun resetUnreadReplyCount(context: Context, echoId: String) {
        val prefs = context.getSharedPreferences(PREF_NOTIF_COUNTS, Context.MODE_PRIVATE)
        prefs.edit().putInt(echoId, 0).apply()
    }

    // --- EXISTING METHODS ---
    fun isFirstLaunch(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchComplete(context: Context) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun getLastEchoId(context: Context): String? {
        return getPrefs(context).getString(KEY_LAST_ECHO_ID, null)
    }

    fun setLastEchoId(context: Context, id: String) {
        getPrefs(context).edit().putString(KEY_LAST_ECHO_ID, id).apply()
    }

    fun getLastReplyCount(context: Context): Int {
        return getPrefs(context).getInt(KEY_LAST_REPLY_COUNT, 0)
    }

    fun setLastReplyCount(context: Context, count: Int) {
        getPrefs(context).edit().putInt(KEY_LAST_REPLY_COUNT, count).apply()
    }
}