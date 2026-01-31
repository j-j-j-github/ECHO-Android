package com.example.echo.core.di

import com.example.echo.core.utils.UserPreferences // Import the helper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Named // Import Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://vvdzbnxhqdojeyrvhtvf.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InZ2ZHpibnhocWRvamV5cnZodHZmIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY5OTUzOTAsImV4cCI6MjA4MjU3MTM5MH0.W7TF-LpCUSEmruhtGDlK9jOXermYTAJeFIVcwpWp3s8"
        ) {
            install(Postgrest)
            install(Realtime)
        }
    }

    // --- NEW: Provide the Persistent ID ---
    @Provides
    @Singleton
    @Named("DeviceSignature") // Naming it so Hilt knows which String this is
    fun provideDeviceSignature(userPreferences: UserPreferences): String {
        return userPreferences.getOrCreateSignature()
    }
}