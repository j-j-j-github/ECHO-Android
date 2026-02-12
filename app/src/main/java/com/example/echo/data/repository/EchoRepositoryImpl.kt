package com.example.echo.data.repository

import android.content.Context
import android.util.Log
import com.example.echo.core.utils.SharedPreferencesManager
import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.NotificationDto
import com.example.echo.data.remote.dto.ReplyDto
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

    override suspend fun createEcho(content: String, colorCode: String?) {
        try {
            val echo = EchoDto(
                id = UUID.randomUUID().toString(),
                content = content,
                signatureId = SharedPreferencesManager.getInstallationId(context),
                colorCode = colorCode
            )
            supabase.postgrest["echoes"].insert(echo)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // UPDATED: Now saves signature_id to enable author-only edits
    override suspend fun createReply(echoId: String, content: String) {
        try {
            val replyMap = mapOf(
                "echo_id" to echoId,
                "content" to content,
                "signature_id" to SharedPreferencesManager.getInstallationId(context) // ✅ Added
            )
            supabase.postgrest["replies"].insert(replyMap)

            SharedPreferencesManager.saveRepliedEcho(context, echoId)

        } catch (e: Exception) { e.printStackTrace() }
    }

    // NEW: Implementation for updating Echoes
    override suspend fun updateEcho(echoId: String, newContent: String) {
        try {
            supabase.postgrest["echoes"].update(
                { set("content", newContent) }
            ) {
                filter { eq("id", echoId) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    // NEW: Implementation for updating Resonances
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
}