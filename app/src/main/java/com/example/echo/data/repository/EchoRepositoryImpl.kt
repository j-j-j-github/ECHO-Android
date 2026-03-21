package com.example.echo.data.repository

import android.content.Context
import android.util.Log
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.NotificationDto
import com.example.echo.data.remote.dto.UserStatsDto
import com.example.echo.data.remote.dto.MoodHistoryDto
import com.example.echo.domain.repository.EchoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

// --- HELPER CLASSES FOR ALIAS UPDATES ---
@kotlinx.serialization.Serializable
data class NameUpdate(
    @kotlinx.serialization.SerialName("display_name") val displayName: String
)

@kotlinx.serialization.Serializable
data class UserAliasInsert(
    @kotlinx.serialization.SerialName("signature_id") val signatureId: String,
    @kotlinx.serialization.SerialName("display_name") val displayName: String
)

class EchoRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context
) : EchoRepository {

    override fun getEchoes(): Flow<List<EchoDto>> = flow {
        try {
            val results = supabase.postgrest["echoes"]
                .select(columns = Columns.list("*", "replies(*)")) {
                    order("created_at", order = Order.DESCENDING)
                    limit(50)
                }.decodeList<EchoDto>()
                .map { echo ->
                    echo.copy(replies = echo.replies?.sortedBy { it.createdAt })
                }
            emit(results)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun createEcho(content: String, category: String, mood: Int, colorCode: String?) {
        try {
            val echo = EchoDto(
                id = UUID.randomUUID().toString(),
                content = content,
                category = category,
                mood = mood,
                signatureId = SharedPreferencesManager.getInstallationId(context),
                colorCode = colorCode
            )
            supabase.postgrest["echoes"].insert(echo)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun createReply(echoId: String, content: String) {
        try {
            val replyMap = mapOf(
                "echo_id" to echoId,
                "content" to content,
                "signature_id" to SharedPreferencesManager.getInstallationId(context)
            )
            supabase.postgrest["replies"].insert(replyMap)

            SharedPreferencesManager.saveRepliedEcho(context, echoId)

        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun updateEcho(echoId: String, newContent: String) {
        try {
            supabase.postgrest["echoes"].update(
                { set("content", newContent) }
            ) {
                filter { eq("id", echoId) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun updateReply(replyId: String, newContent: String) {
        try {
            supabase.postgrest["replies"].update(
                { set("content", newContent) }
            ) {
                filter { eq("id", replyId) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun reportEcho(echoId: String) {
        try {
            val report = mapOf(
                "echo_id" to echoId,
                "signature" to SharedPreferencesManager.getInstallationId(context)
            )
            supabase.postgrest["reports"].insert(report)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun hasUnreadNotifications(): Boolean {
        return try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)
            val lastCheckedString = SharedPreferencesManager.getLastCheckedTime(context)

            val latestNotification = supabase.postgrest["replies"]
                .select(columns = Columns.list("id", "echo_id", "content", "created_at", "echoes!inner(content, signature_id)")) {
                    filter { eq("echoes.signature_id", mySignature) }
                    order("created_at", order = Order.DESCENDING)
                    limit(1)
                }.decodeSingleOrNull<NotificationDto>()

            if (latestNotification == null) {
                return false
            }

            if (lastCheckedString.isEmpty()) return true

            val latestTime = Instant.parse(latestNotification.createdAt)
            val lastCheckedTime = Instant.parse(lastCheckedString)

            return latestTime.isAfter(lastCheckedTime)

        } catch (e: Exception) {
            Log.e("EchoGlow", "Error calculating glow: ${e.message}")
            false
        }
    }

    override fun markNotificationsAsRead() {
        val now = Instant.now().toString()
        SharedPreferencesManager.saveLastCheckedTime(context, now)
    }

    override suspend fun getUserNotifications(): List<NotificationDto> {
        return try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)
            supabase.postgrest["replies"]
                .select(columns = Columns.list("id", "echo_id", "content", "created_at", "echoes!inner(content, signature_id)")) {
                    filter { eq("echoes.signature_id", mySignature) }
                    order("created_at", order = Order.DESCENDING)
                    limit(20)
                }.decodeList<NotificationDto>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    override suspend fun getEchoById(echoId: String): EchoDto? {
        return try {
            val echo = supabase.postgrest["echoes"]
                .select(columns = Columns.list("*", "replies(*)")) {
                    filter { eq("id", echoId) }
                }.decodeSingleOrNull<EchoDto>()

            echo?.copy(replies = echo.replies?.sortedBy { it.createdAt })

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // ==========================================
    // --- JOURNEY, ANALYTICS & IDENTITY ---
    // ==========================================

    override suspend fun getUserStats(): UserStatsDto? {
        return try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)
            supabase.postgrest["user_stats"]
                .select {
                    filter { eq("signature_id", mySignature) }
                }.decodeSingleOrNull<UserStatsDto>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun checkIfUserExists(signatureId: String): Boolean {
        return try {
            val response = supabase.postgrest["user_stats"]
                .select {
                    filter { eq("signature_id", signatureId) }
                    limit(1)
                }
                .decodeList<UserStatsDto>()

            response.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun updateUserDisplayName(newName: String): Boolean {
        return try {
            val signatureId = SharedPreferencesManager.getInstallationId(context)

            // 1. Check if this specific signature_id exists in user_stats
            val exists = checkIfUserExists(signatureId)

            if (exists) {
                // 2. Just update the display_name column
                supabase.postgrest["user_stats"].update(
                    { set("display_name", newName) }
                ) {
                    filter { eq("signature_id", signatureId) }
                }
            } else {
                // 3. Create the row for the first time
                // We include the ID and the Name. Other ints will default to 0 based on your schema.
                val newUser = mapOf(
                    "signature_id" to signatureId,
                    "display_name" to newName,
                    "total_echoes_posted" to 0,
                    "total_resonances_sent" to 0,
                    "total_resonances_received" to 0
                )
                supabase.postgrest["user_stats"].insert(newUser)
            }
            true
        } catch (e: Exception) {
            Log.e("DATABASE", "Identity Sync Failed: ${e.message}")
            false
        }
    }

    override suspend fun submitLead(email: String, signatureId: String) {
        try {
            supabase.postgrest["echo_plus_leads"].insert(
                mapOf("email" to email, "signature_id" to signatureId)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getMoodHistory(): List<MoodHistoryDto> {
        return try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)
            supabase.postgrest["mood_history"]
                .select {
                    filter { eq("signature_id", mySignature) }
                    order("created_at", order = Order.DESCENDING)
                }.decodeList<MoodHistoryDto>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}