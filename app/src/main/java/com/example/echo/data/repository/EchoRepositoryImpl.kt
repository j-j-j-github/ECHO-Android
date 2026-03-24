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
        // 🚀 We removed the try/catch here.
        // If Supabase times out, it throws an error to the ViewModel
        // INSTEAD of silently emitting an empty list that wipes the screen!
        val results = supabase.postgrest["echoes"]
            .select(columns = Columns.list("*", "replies(*)")) {
                order("created_at", order = Order.DESCENDING)
                limit(500)
            }.decodeList<EchoDto>()
            .map { echo ->
                echo.copy(replies = echo.replies?.sortedBy { it.createdAt })
            }
        emit(results)
    }

    override suspend fun createEcho(content: String, category: String, mood: Int, colorCode: String?) {
        val echoId = UUID.randomUUID().toString()
        val signatureId = SharedPreferencesManager.getInstallationId(context)

        try {
            val echo = EchoDto(
                id = echoId,
                content = content,
                category = category,
                mood = mood,
                signatureId = signatureId,
                colorCode = colorCode
            )
            supabase.postgrest["echoes"].insert(echo)
        } catch (e: Exception) {
            // 🛑 OFFLINE FIX: Network failed. Save to pending queue.
            Log.e("OFFLINE_QUEUE", "Network down. Saving Echo locally.")
            SharedPreferencesManager.savePendingEcho(context, echoId, content, category, mood.toString())
        }
    }

    override suspend fun createReply(echoId: String, content: String) {
        val signatureId = SharedPreferencesManager.getInstallationId(context)

        try {
            val replyMap = mapOf(
                "echo_id" to echoId,
                "content" to content,
                "signature_id" to signatureId
            )
            supabase.postgrest["replies"].insert(replyMap)
            SharedPreferencesManager.saveRepliedEcho(context, echoId)

        } catch (e: Exception) {
            // 🛑 OFFLINE FIX: Network failed. Save to pending queue.
            Log.e("OFFLINE_QUEUE", "Network down. Saving Reply locally.")
            val replyId = UUID.randomUUID().toString()
            SharedPreferencesManager.savePendingReply(context, replyId, echoId, content)
        }
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

    override suspend fun relateToEcho(echoId: String) {
        try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)

            // 1. Fetch the Echo to get the Author's ID and current count
            val targetEcho = supabase.postgrest["echoes"]
                .select(columns = Columns.list("signature_id", "likes")) {
                    filter { eq("id", echoId) }
                }.decodeSingleOrNull<EchoDto>()

            val authorSignature = targetEcho?.signatureId ?: return
            val currentEchoLikes = targetEcho.likes ?: 0

            // 2. PERMANENT LOG: Prevent double-liking via the UNIQUE constraint in SQL
            val relateLog = mapOf(
                "echo_id" to echoId,
                "sender_signature_id" to mySignature,
                "receiver_signature_id" to authorSignature
            )
            supabase.postgrest["relates_log"].insert(relateLog)

            // 3. EPHEMERAL UPDATE: Update the count on the Echo itself for the live feed
            supabase.postgrest["echoes"].update(
                { set("likes", currentEchoLikes + 1) }
            ) {
                filter { eq("id", echoId) }
            }

            // 4. PERMANENT UPDATE: Update the Author's lifetime stats
            val authorStats = supabase.postgrest["user_stats"]
                .select { filter { eq("signature_id", authorSignature) } }
                .decodeSingleOrNull<UserStatsDto>()

            val newLifetimeTotal = (authorStats?.totalRelatesReceived ?: 0) + 1

            supabase.postgrest["user_stats"].update(
                { set("total_relates_received", newLifetimeTotal) }
            ) {
                filter { eq("signature_id", authorSignature) }
            }

        } catch (e: Exception) {
            // If this triggers, it's usually because the user already related to this post
            // (Postgrest will throw an error on the UNIQUE constraint violation).
            android.util.Log.e("RELATE_SYNC", "Already related or network error: ${e.message}")
        }
    }

    override suspend fun unrelateFromEcho(echoId: String) {
        try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)

            // 1. DELETE PERMANENT LOG: Remove the record so they can relate again later if they want
            supabase.postgrest["relates_log"].delete {
                filter {
                    eq("echo_id", echoId)
                    eq("sender_signature_id", mySignature)
                }
            }

            // 2. Fetch the Echo to get the Author's ID and current count
            val targetEcho = supabase.postgrest["echoes"]
                .select(columns = Columns.list("signature_id", "likes")) {
                    filter { eq("id", echoId) }
                }.decodeSingleOrNull<EchoDto>()

            val authorSignature = targetEcho?.signatureId ?: return
            val currentEchoLikes = targetEcho.likes ?: 0

            // 3. EPHEMERAL UPDATE: Subtract from the live feed (safety check so it never goes below 0)
            val newLikes = if (currentEchoLikes > 0) currentEchoLikes - 1 else 0
            supabase.postgrest["echoes"].update(
                { set("likes", newLikes) }
            ) {
                filter { eq("id", echoId) }
            }

            // 4. PERMANENT UPDATE: Subtract from the Author's lifetime stats
            val authorStats = supabase.postgrest["user_stats"]
                .select { filter { eq("signature_id", authorSignature) } }
                .decodeSingleOrNull<UserStatsDto>()

            val currentLifetime = authorStats?.totalRelatesReceived ?: 0
            val newLifetimeTotal = if (currentLifetime > 0) currentLifetime - 1 else 0

            supabase.postgrest["user_stats"].update(
                { set("total_relates_received", newLifetimeTotal) }
            ) {
                filter { eq("signature_id", authorSignature) }
            }

        } catch (e: Exception) {
            android.util.Log.e("RELATE_SYNC", "Error unrelating: ${e.message}")
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

    override suspend fun getUserActivity(): List<EchoDto> {
        return try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)

            // 🔥 This query fetches ONLY your personal posts, no matter how old they are
            supabase.postgrest["echoes"]
                .select(columns = Columns.list("*", "replies(*)")) {
                    filter { eq("signature_id", mySignature) }
                    order("created_at", order = Order.DESCENDING)
                    // No limit here so the calendar gets your full history
                }.decodeList<EchoDto>()
                .map { echo ->
                    echo.copy(replies = echo.replies?.sortedBy { it.createdAt })
                }
        } catch (e: Exception) {
            Log.e("REPO_ERROR", "Failed to fetch personal activity: ${e.message}")
            emptyList()
        }
    }

    // Add this to the bottom of EchoRepositoryImpl.kt
    override suspend fun syncPendingOfflineData() {
        try {
            // 1. Check for Pending Echoes
            val pendingEchoes = SharedPreferencesManager.getPendingEchoes(context)
            if (pendingEchoes.isNotEmpty()) {
                pendingEchoes.forEach { (id, data) ->
                    val echo = EchoDto(
                        id = id,
                        content = data["content"] ?: "",
                        category = data["category"] ?: "Inner Thoughts",
                        mood = data["mood"]?.toIntOrNull() ?: 2,
                        signatureId = SharedPreferencesManager.getInstallationId(context),
                        colorCode = null
                    )
                    supabase.postgrest["echoes"].insert(echo)
                    SharedPreferencesManager.removePendingEcho(context, id)
                }
            }

            // 2. Check for Pending Replies
            val pendingReplies = SharedPreferencesManager.getPendingReplies(context)
            if (pendingReplies.isNotEmpty()) {
                pendingReplies.forEach { (replyId, data) ->
                    val replyMap = mapOf(
                        "echo_id" to data["echoId"],
                        "content" to data["content"],
                        "signature_id" to SharedPreferencesManager.getInstallationId(context)
                    )
                    supabase.postgrest["replies"].insert(replyMap)
                    SharedPreferencesManager.saveRepliedEcho(context, data["echoId"] ?: "")
                    SharedPreferencesManager.removePendingReply(context, replyId)
                }
            }
        } catch (e: Exception) {
            Log.e("OFFLINE_SYNC", "Still offline. Keeping items in queue.")
        }
    }
}