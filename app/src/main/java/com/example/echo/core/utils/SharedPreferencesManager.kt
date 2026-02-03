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
    // --- NEW KEY FOR GLOWING BELL ---
    private const val KEY_LAST_CHECKED_TIME = "last_notification_checked_time"

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

    // --- NOTIFICATION TIMESTAMPS (NEW) ---
    // Saves the timestamp of when we last opened the notifications
    fun saveLastCheckedTime(context: Context, timestamp: String) {
        getPrefs(context).edit().putString(KEY_LAST_CHECKED_TIME, timestamp).apply()
    }

    // Returns the saved timestamp (default to empty string if never checked)
    fun getLastCheckedTime(context: Context): String {
        return getPrefs(context).getString(KEY_LAST_CHECKED_TIME, "") ?: ""
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