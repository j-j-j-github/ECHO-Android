package com.example.echo.core.utils

import android.content.Context

object SharedPreferencesManager {
    private const val PREF_NAME = "EchoPreferences"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"

    // --- NEW KEYS FOR NOTIFICATIONS ---
    private const val KEY_LAST_ECHO_ID = "last_seen_echo_id"
    private const val KEY_LAST_REPLY_COUNT = "last_reply_count"

    fun isFirstLaunch(context: Context): Boolean {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchComplete(context: Context) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(KEY_FIRST_LAUNCH, false)
            apply()
        }
    }

    // --- NOTIFICATION HELPERS ---

    fun getLastEchoId(context: Context): String? {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(KEY_LAST_ECHO_ID, null)
    }

    fun setLastEchoId(context: Context, id: String) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString(KEY_LAST_ECHO_ID, id)
            apply()
        }
    }

    fun getLastReplyCount(context: Context): Int {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return sharedPref.getInt(KEY_LAST_REPLY_COUNT, 0)
    }

    fun setLastReplyCount(context: Context, count: Int) {
        val sharedPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt(KEY_LAST_REPLY_COUNT, count)
            apply()
        }
    }
}