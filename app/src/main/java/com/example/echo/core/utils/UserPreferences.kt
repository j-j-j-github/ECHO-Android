package com.example.echo.core.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("echo_prefs", Context.MODE_PRIVATE)
    private val KEY_SIGNATURE = "signature_id"

    fun getOrCreateSignature(): String {
        // 1. Try to read existing ID
        var signature = prefs.getString(KEY_SIGNATURE, null)

        // 2. If missing (First run), create and save it
        if (signature == null) {
            signature = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_SIGNATURE, signature).apply()
        }

        return signature!!
    }
}