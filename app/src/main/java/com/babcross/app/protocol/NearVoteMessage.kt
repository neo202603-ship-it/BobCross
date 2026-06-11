package com.babcross.app.protocol

import org.json.JSONObject
import java.util.UUID

enum class NearVoteMessageType {
    PROFILE,
    POLL,
    POLL_RESPONSE,
    VOTE,
    RECEIPT,
    RESULT_BLOCK,
    GOSSIP,
    PING
}

data class NearVoteMessage(
    val type: NearVoteMessageType,
    val senderId: String,
    val payloadJson: String,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val messageId: String = UUID.randomUUID().toString(),
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        return JSONObject()
            .put("schemaVersion", schemaVersion)
            .put("messageId", messageId)
            .put("createdAtMillis", createdAtMillis)
            .put("type", type.name)
            .put("senderId", senderId)
            .put("payloadJson", payloadJson)
            .toString()
    }

    companion object {
        const val CURRENT_SCHEMA_VERSION = 1

        fun fromJson(json: String): NearVoteMessage {
            val parsed = JSONObject(json)
            val messageId = parsed.optString("messageId")
            return NearVoteMessage(
                type = NearVoteMessageType.valueOf(parsed.getString("type")),
                senderId = parsed.getString("senderId"),
                payloadJson = parsed.getString("payloadJson"),
                schemaVersion = parsed.optInt("schemaVersion", CURRENT_SCHEMA_VERSION),
                messageId = messageId.ifBlank { "legacy-${json.hashCode()}" },
                createdAtMillis = parsed.optLong("createdAtMillis", System.currentTimeMillis())
            )
        }

        fun ping(senderId: String): NearVoteMessage {
            return NearVoteMessage(
                type = NearVoteMessageType.PING,
                senderId = senderId,
                payloadJson = "{}"
            )
        }
    }
}
