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

    // --- NEW: REPLIED ECHOES TRACKING ---
    private const val KEY_REPLIED_ECHOES = "replied_echoes_set"

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

    // --- NEW: SAVE REPLIED ECHO ID ---
    // Called when you successfully send a reply. Saves the Echo ID locally.
    fun saveRepliedEcho(context: Context, echoId: String) {
        val prefs = getPrefs(context)
        val currentSet = prefs.getStringSet(KEY_REPLIED_ECHOES, emptySet()) ?: emptySet()
        val newSet = currentSet.toMutableSet()
        newSet.add(echoId)
        prefs.edit().putStringSet(KEY_REPLIED_ECHOES, newSet).apply()
    }

    // --- NEW: CHECK IF REPLIED ---
    // Returns true if your local storage says you replied to this Echo.
    fun hasRepliedTo(context: Context, echoId: String): Boolean {
        val prefs = getPrefs(context)
        val currentSet = prefs.getStringSet(KEY_REPLIED_ECHOES, emptySet()) ?: emptySet()
        return currentSet.contains(echoId)
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