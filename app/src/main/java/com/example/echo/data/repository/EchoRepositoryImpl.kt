package com.example.echo.data.repository

import android.content.Context
import android.util.Log
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.NotificationDto
import com.example.echo.data.remote.dto.ReplyDto
import com.example.echo.data.remote.dto.UserStatsDto
import com.example.echo.data.remote.dto.MoodHistoryDto
import com.example.echo.domain.repository.EchoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID
import javax.inject.Inject
import kotlin.math.min

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
        val currentUserId = SharedPreferencesManager.getInstallationId(context)

        try {
            // 1. Fetch echoes via your RPC
            val results = supabase.postgrest.rpc(
                function = "get_echoes_for_user",
                parameters = mapOf("requesting_signature" to currentUserId)
            ).decodeList<EchoDto>()

            val echoIds = results.mapNotNull { it.id }

            if (echoIds.isEmpty()) {
                emit(emptyList())
                return@flow
            }

            // 2. Fetch all replies for these echoes
            val allReplies = supabase.postgrest["replies"]
                .select {
                    filter { isIn("echo_id", echoIds) }
                }.decodeList<ReplyDto>()

            // ✅ 3. FETCH THE USER's RELATES FOR THESE ECHOES
            // We ask Supabase: "Give me all relates_log entries for these echoes WHERE I am the sender"
            @kotlinx.serialization.Serializable
            data class RelateLogResult(val echo_id: String)

            val myRelates = supabase.postgrest["relates_log"]
                .select(columns = Columns.list("echo_id")) {
                    filter {
                        isIn("echo_id", echoIds)
                        eq("sender_signature_id", currentUserId)
                    }
                }.decodeList<RelateLogResult>()

            val myRelatedEchoIds = myRelates.map { it.echo_id }.toSet()

            // 4. Combine everything and emit
            val finalResults = results.map { echo ->
                val echoReplies = allReplies.filter { it.echoId == echo.id }.sortedBy { it.createdAt }

                // ✅ explicitly set isRelated to true if the echo_id exists in our relate fetch
                echo.copy(
                    replies = echoReplies,
                    isRelated = myRelatedEchoIds.contains(echo.id)
                )
            }

            emit(finalResults)
        } catch (e: Exception) {
            Log.e("REPO_ERROR", "Failed to fetch echoes via RPC: ${e.message}")
            throw e
        }
    }

    override suspend fun createEcho(content: String, category: String, mood: Int, colorCode: String?) {
        val echoId = UUID.randomUUID().toString()
        createEchoWithId(echoId, content, category, mood, colorCode)
    }

    override suspend fun createEchoWithId(echoId: String, content: String, category: String, mood: Int, colorCode: String?) {
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
            // ✅ Removed .execute()
            supabase.postgrest["echoes"].insert(echo)

            val moodLog = mapOf(
                "signature_id" to signatureId,
                "mood" to mood
            )
            // ✅ Removed .execute()
            supabase.postgrest["mood_history"].insert(moodLog)

            // ✅ AWARD ENERGY: +5 for posting (Max 2 per day)
            awardEnergy(signatureId, 5, "daily_echoes_counted", 2)

        } catch (e: Exception) {
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
            // ✅ Removed .execute()
            supabase.postgrest["replies"].insert(replyMap)
            SharedPreferencesManager.saveRepliedEcho(context, echoId)

            // ✅ AWARD ENERGY TO SENDER: +3 for resonating (Max 10 per day)
            awardEnergy(signatureId, 3, "daily_resonances_counted", 10)

            // ✅ AWARD ENERGY TO RECEIVER: +4 for receiving resonance (Max 20 per day)
            val targetEcho = supabase.postgrest["echoes"]
                .select(columns = Columns.list("signature_id")) { filter { eq("id", echoId) } }
                .decodeSingleOrNull<EchoDto>()

            targetEcho?.signatureId?.let { authorId ->
                if (authorId != signatureId) {
                    awardEnergy(authorId, 4, "daily_resonances_received", 20)
                }
            }

        } catch (e: Exception) {
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
            } // ✅ Removed .execute()
        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun updateReply(replyId: String, newContent: String) {
        try {
            supabase.postgrest["replies"].update(
                { set("content", newContent) }
            ) {
                filter { eq("id", replyId) }
            } // ✅ Removed .execute()
        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun reportEcho(echoId: String) {
        try {
            val report = mapOf(
                "echo_id" to echoId,
                "signature" to SharedPreferencesManager.getInstallationId(context)
            )
            // ✅ Removed .execute()
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

            val currentUserId = SharedPreferencesManager.getInstallationId(context)
            val relateCheck = supabase.postgrest["relates_log"]
                .select {
                    filter {
                        eq("echo_id", echoId)
                        eq("sender_signature_id", currentUserId)
                    }
                }.data

            echo?.copy(
                isRelated = relateCheck != "[]",
                replies = echo.replies?.sortedBy { it.createdAt }
            )

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun relateToEcho(echoId: String) {
        try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)

            val targetEcho = supabase.postgrest["echoes"]
                .select(columns = Columns.list("signature_id", "likes", "content")) {
                    filter { eq("id", echoId) }
                }.decodeSingleOrNull<EchoDto>()

            val authorSignature = targetEcho?.signatureId ?: return
            val currentEchoLikes = targetEcho.likes ?: 0

            val relateLog = mapOf(
                "echo_id" to echoId,
                "sender_signature_id" to mySignature,
                "receiver_signature_id" to authorSignature
            )
            // ✅ Removed .execute()
            supabase.postgrest["relates_log"].insert(relateLog)

            // Make sure we pass the integer correctly to set()
            val newLikesCount: Int = currentEchoLikes + 1
            supabase.postgrest["echoes"].update(
                { set("likes", newLikesCount) }
            ) {
                filter { eq("id", echoId) }
            } // ✅ Removed .execute()

            // ✅ AWARD ENERGY TO RECEIVER: +1 for receiving a relate (Max 20 per day)
            if (authorSignature != mySignature) {
                awardEnergy(authorSignature, 1, "daily_relates_received", 20)
            }

            val authorStats = supabase.postgrest["user_stats"]
                .select { filter { eq("signature_id", authorSignature) } }
                .decodeSingleOrNull<UserStatsDto>()

            val newLifetimeTotal: Int = (authorStats?.totalRelatesReceived ?: 0) + 1

            supabase.postgrest["user_stats"].update(
                { set("total_relates_received", newLifetimeTotal) }
            ) {
                filter { eq("signature_id", authorSignature) }
            } // ✅ Removed .execute()

        } catch (e: Exception) {
            Log.e("RELATE_SYNC", "Already related or network error: ${e.message}")
        }
    }

    override suspend fun unrelateFromEcho(echoId: String) {
        try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)

            supabase.postgrest["relates_log"].delete {
                filter {
                    eq("echo_id", echoId)
                    eq("sender_signature_id", mySignature)
                }
            } // ✅ Removed .execute()

            val targetEcho = supabase.postgrest["echoes"]
                .select(columns = Columns.list("signature_id", "likes", "content")) {
                    filter { eq("id", echoId) }
                }.decodeSingleOrNull<EchoDto>()

            val authorSignature = targetEcho?.signatureId ?: return
            val currentEchoLikes = targetEcho.likes ?: 0

            val newLikes: Int = if (currentEchoLikes > 0) currentEchoLikes - 1 else 0
            supabase.postgrest["echoes"].update(
                { set("likes", newLikes) }
            ) {
                filter { eq("id", echoId) }
            } // ✅ Removed .execute()

            val authorStats = supabase.postgrest["user_stats"]
                .select { filter { eq("signature_id", authorSignature) } }
                .decodeSingleOrNull<UserStatsDto>()

            val currentLifetime = authorStats?.totalRelatesReceived ?: 0
            val newLifetimeTotal: Int = if (currentLifetime > 0) currentLifetime - 1 else 0

            supabase.postgrest["user_stats"].update(
                { set("total_relates_received", newLifetimeTotal) }
            ) {
                filter { eq("signature_id", authorSignature) }
            } // ✅ Removed .execute()

        } catch (e: Exception) {
            Log.e("RELATE_SYNC", "Error unrelating: ${e.message}")
        }
    }

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
            val exists = checkIfUserExists(signatureId)

            if (exists) {
                supabase.postgrest["user_stats"].update(
                    { set("display_name", newName) }
                ) {
                    filter { eq("signature_id", signatureId) }
                } // ✅ Removed .execute()
            } else {
                val newUser = mapOf(
                    "signature_id" to signatureId,
                    "display_name" to newName,
                    "total_echoes_posted" to 0,
                    "total_resonances_sent" to 0,
                    "total_resonances_received" to 0
                )
                // ✅ Removed .execute()
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
            // ✅ Removed .execute()
            supabase.postgrest["echo_plus_leads"].insert(
                mapOf("email" to email, "signature_id" to signatureId)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override suspend fun getMoodHistory(): List<MoodHistoryDto> {
        return try {
            val mySignature = SharedPreferencesManager.getInstallationId(context).trim()
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

            val results = supabase.postgrest.rpc(
                function = "get_echoes_for_user",
                parameters = mapOf("requesting_signature" to mySignature)
            ).decodeList<EchoDto>()

            results
        } catch (e: Exception) {
            Log.e("REPO_ERROR", "Failed to fetch activity: ${e.message}")
            emptyList()
        }
    }

    // ✅ HELPER FUNCTION: Resets the daily tracking counters
    private suspend fun resetDailyCaps(signatureId: String) {
        try {
            val zero: Int = 0
            supabase.postgrest["user_stats"].update({
                set("daily_echoes_counted", zero)
                set("daily_resonances_counted", zero)
                set("daily_relates_received", zero)
                set("daily_resonances_received", zero)
            }) { filter { eq("signature_id", signatureId) } } // ✅ Removed .execute()
        } catch (e: Exception) {
            Log.e("ENERGY_SYSTEM", "Failed to reset daily caps: ${e.message}")
        }
    }

    override suspend fun processDailyLogin(signatureId: String) {
        try {
            val stats: UserStatsDto = supabase.postgrest["user_stats"].select {
                filter { eq("signature_id", signatureId) }
            }.decodeSingleOrNull<UserStatsDto>() ?: return

            val today = LocalDate.now(ZoneId.of("UTC"))

            val lastActiveStr: String? = stats.lastActiveDate
            val lastActive: LocalDate = if (!lastActiveStr.isNullOrBlank()) {
                try {
                    LocalDate.parse(lastActiveStr)
                } catch(e: Exception) {
                    today.minusDays(1)
                }
            } else {
                today.minusDays(1)
            }

            val daysInactive = ChronoUnit.DAYS.between(lastActive, today)

            var newEnergy: Int = stats.energy ?: 0
            var newStreak: Int = stats.streakDays ?: 0

            if (daysInactive > 0) {
                resetDailyCaps(signatureId)

                if (daysInactive == 1L) {
                    newStreak += 1
                    newEnergy += 4

                    when (newStreak) {
                        3 -> newEnergy += 5
                        7 -> newEnergy += 10
                        14 -> newEnergy += 15
                    }
                } else {
                    newStreak = 0
                    if (daysInactive >= 3) {
                        val decayDays = daysInactive - 3
                        newEnergy = maxOf(0, newEnergy - (decayDays * 10).toInt())
                    }
                }

                newEnergy = minOf(100, newEnergy)

                supabase.postgrest["user_stats"].update({
                    set("energy", newEnergy)
                    set("streak_days", newStreak)
                    set("last_active_date", today.toString())
                }) { filter { eq("signature_id", signatureId) } } // ✅ Removed .execute()
            }
        } catch(e: Exception) {
            Log.e("ENERGY_SYSTEM", "Error processing daily login: ${e.message}")
        }
    }

    // ✅ NEW: Added actual implementation for Daily Login Energy Award
    override suspend fun awardDailyLoginEnergy(userId: String): Boolean {
        return try {
            val stats = supabase.postgrest["user_stats"]
                .select { filter { eq("signature_id", userId) } }
                .decodeSingleOrNull<UserStatsDto>() ?: return false

            // Standard daily energy is +5.
            val newEnergy = minOf(100, (stats.energy ?: 0) + 5)

            supabase.postgrest["user_stats"].update({
                set("energy", newEnergy)
            }) { filter { eq("signature_id", userId) } }

            true
        } catch (e: Exception) {
            Log.e("REPO", "Daily energy award failed: ${e.message}")
            false
        }
    }

    // ✅ NEW: Added actual implementation for Syncing Offline Energy
    override suspend fun syncOfflineEnergy(userId: String, echoes: Int, replies: Int): Boolean {
        return try {
            val stats = supabase.postgrest["user_stats"]
                .select { filter { eq("signature_id", userId) } }
                .decodeSingleOrNull<UserStatsDto>() ?: return false

            // Echoes are +5 each, Replies are +3 each.
            val offlineEnergyEarned = (echoes * 5) + (replies * 3)
            val newEnergy = minOf(100, (stats.energy ?: 0) + offlineEnergyEarned)

            supabase.postgrest["user_stats"].update({
                set("energy", newEnergy)
            }) { filter { eq("signature_id", userId) } }

            true
        } catch (e: Exception) {
            Log.e("REPO", "Offline energy sync failed: ${e.message}")
            false
        }
    }


    // ✅ HELPER: Safely awards energy while respecting daily limits
    private suspend fun awardEnergy(signatureId: String, amount: Int, limitField: String, dailyLimit: Int) {
        try {
            val stats = supabase.postgrest["user_stats"]
                .select { filter { eq("signature_id", signatureId) } }
                .decodeSingleOrNull<UserStatsDto>() ?: return

            val currentLimitCount = when(limitField) {
                "daily_echoes_counted" -> stats.dailyEchoesCounted ?: 0
                "daily_resonances_counted" -> stats.dailyResonancesCounted ?: 0
                "daily_resonances_received" -> stats.dailyResonancesReceived ?: 0
                "daily_relates_received" -> stats.dailyRelatesReceived ?: 0
                else -> 0
            }

            if (currentLimitCount < dailyLimit) {
                val newEnergy = minOf(100, (stats.energy ?: 0) + amount)
                val newLimitCount = currentLimitCount + 1

                supabase.postgrest["user_stats"].update({
                    set("energy", newEnergy)
                    set(limitField, newLimitCount)
                }) { filter { eq("signature_id", signatureId) } } // ✅ Removed .execute()
            }
        } catch (e: Exception) {
            Log.e("ENERGY", "Failed to award energy: ${e.message}")
        }
    }

    override suspend fun activateSpotlight(echoId: String, category: String): String {
        try {
            val mySignature = SharedPreferencesManager.getInstallationId(context)
            val stats = supabase.postgrest["user_stats"]
                .select { filter { eq("signature_id", mySignature) } }
                .decodeSingleOrNull<UserStatsDto>() ?: return "ERROR_USER_NOT_FOUND"

            val energy = stats.energy ?: 0
            if (energy < 100) return "ERROR_LOW_ENERGY"

            // 1. Enforce the 48-Hour Cooldown
            val lastSpotlightStr = stats.lastSpotlightTime
            if (lastSpotlightStr != null) {
                val lastTime = Instant.parse(lastSpotlightStr)
                val hoursSinceLast = ChronoUnit.HOURS.between(lastTime, Instant.now())
                if (hoursSinceLast < 48) {
                    return "ERROR_COOLDOWN"
                }
            }

            // 2. Count Active Spotlights in this category
            val activeEchoes = supabase.postgrest["echoes"]
                .select(columns = Columns.list("id")) {
                    filter {
                        eq("category", category)
                        eq("spotlight_status", "active")
                    }
                }.decodeList<EchoDto>()

            val statusToApply: String
            val expiresAt = Instant.now().plus(48, ChronoUnit.HOURS).toString()

            if (activeEchoes.size < 2) {
                statusToApply = "active"
            } else {
                // 3. Max active reached. Count Queued Spotlights.
                val queuedEchoes = supabase.postgrest["echoes"]
                    .select(columns = Columns.list("id")) {
                        filter {
                            eq("category", category)
                            eq("spotlight_status", "queued")
                        }
                    }.decodeList<EchoDto>()

                if (queuedEchoes.size < 5) {
                    statusToApply = "queued"
                } else {
                    return "ERROR_QUEUE_FULL"
                }
            }

            // 4. Apply the status to the Echo
            supabase.postgrest["echoes"].update({
                set("is_spotlight", true) // Keep for backward compatibility for now
                set("spotlight_status", statusToApply)
                if (statusToApply == "active") {
                    set("spotlight_expires_at", expiresAt)
                }
            }) { filter { eq("id", echoId) } }


            // 5. Deduct 100 energy and log the usage time
            supabase.postgrest["user_stats"].update({
                set("energy", 0)
                set("last_spotlight_time", Instant.now().toString())
            }) { filter { eq("signature_id", mySignature) } }

            return statusToApply // Returns "active" or "queued"
        } catch(e: Exception) {
            Log.e("SPOTLIGHT", "Failed to activate spotlight: ${e.message}")
            return "ERROR_NETWORK"
        }
    }

    override suspend fun syncPendingOfflineData() {
        try {
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
                    // ✅ Removed .execute()
                    supabase.postgrest["echoes"].insert(echo)
                    SharedPreferencesManager.removePendingEcho(context, id)
                }
            }

            val pendingReplies = SharedPreferencesManager.getPendingReplies(context)
            if (pendingReplies.isNotEmpty()) {
                pendingReplies.forEach { (replyId, data) ->
                    val replyMap = mapOf(
                        "echo_id" to data["echoId"],
                        "content" to data["content"],
                        "signature_id" to SharedPreferencesManager.getInstallationId(context)
                    )
                    // ✅ Removed .execute()
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