package com.example.echo.data.repository

import com.example.echo.data.remote.dto.EchoDto
import com.example.echo.data.remote.dto.ReplyDto
import com.example.echo.domain.repository.EchoRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import com.example.echo.data.remote.dto.NotificationDto

class EchoRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    // INJECT THE PERSISTENT ID HERE
    @Named("DeviceSignature") private val deviceSignature: String
) : EchoRepository {

    override fun getEchoes(): Flow<List<EchoDto>> = flow {
        try {
            val results = supabase.postgrest["echoes"]
                .select(columns = Columns.list("*", "replies(*)")) {
                    order("created_at", order = Order.DESCENDING)
                    limit(50)
                }.decodeList<EchoDto>()
            emit(results)
        } catch (e: Exception) {
            e.printStackTrace()
            emit(emptyList())
        }
    }

    override suspend fun createEcho(content: String) {
        try {
            val echo = EchoDto(
                id = UUID.randomUUID().toString(),
                content = content,
                // Uses the PERSISTENT signature now
                signatureId = deviceSignature
            )
            supabase.postgrest["echoes"].insert(echo)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun createReply(echoId: String, content: String) {
        try {
            val reply = ReplyDto(
                echoId = echoId,
                content = content
            )
            supabase.postgrest["replies"].insert(reply)
        } catch (e: Exception) { e.printStackTrace() }
    }

    override suspend fun reportEcho(echoId: String) {
        try {
            val report = mapOf(
                "echo_id" to echoId,
                "signature" to deviceSignature // Uses persistent ID for reports
            )
            supabase.postgrest["reports"].insert(report)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // 1. Fetch Notifications (Replies to MY echoes)
    suspend fun getUserNotifications(): List<NotificationDto> {
        return try {
            supabase.postgrest["replies"]
                .select(columns = Columns.list("id", "echo_id", "content", "created_at", "echoes!inner(content, signature_id)")) {
                    // FILTER: Only where the parent Echo's signature matches MY device
                    filter {
                        eq("echoes.signature_id", deviceSignature)
                    }
                    order("created_at", order = Order.DESCENDING)
                    limit(20)
                }.decodeList<NotificationDto>()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // 2. Fetch Single Echo (For navigation)
    suspend fun getEchoById(echoId: String): EchoDto? {
        return try {
            supabase.postgrest["echoes"]
                .select(columns = Columns.list("*", "replies(*)")) {
                    filter { eq("id", echoId) }
                }.decodeSingleOrNull<EchoDto>()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}