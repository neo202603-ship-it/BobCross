package com.babcross.app.protocol

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearVoteMessageTest {
    @Test
    fun messageJsonIncludesSchemaMetadata() {
        val message = NearVoteMessage(
            type = NearVoteMessageType.POLL,
            senderId = "user-1",
            payloadJson = "{}",
            messageId = "message-1",
            createdAtMillis = 1234L
        )

        val parsed = JSONObject(message.toJson())

        assertEquals(NearVoteMessage.CURRENT_SCHEMA_VERSION, parsed.getInt("schemaVersion"))
        assertEquals("message-1", parsed.getString("messageId"))
        assertEquals(1234L, parsed.getLong("createdAtMillis"))
    }

    @Test
    fun legacyMessageJsonStillParsesWithDefaults() {
        val legacyJson = JSONObject()
            .put("type", NearVoteMessageType.PING.name)
            .put("senderId", "user-1")
            .put("payloadJson", "{}")
            .toString()

        val message = NearVoteMessage.fromJson(legacyJson)

        assertEquals(NearVoteMessageType.PING, message.type)
        assertEquals(NearVoteMessage.CURRENT_SCHEMA_VERSION, message.schemaVersion)
        assertTrue(message.messageId.startsWith("legacy-"))
        assertTrue(message.createdAtMillis > 0L)
    }

    @Test
    fun replayGuardRejectsDuplicateMessageId() {
        val guard = NearVoteMessageReplayGuard()
        val message = NearVoteMessage(
            type = NearVoteMessageType.PING,
            senderId = "user-1",
            payloadJson = "{}",
            messageId = "message-1",
            createdAtMillis = 10_000L
        )

        assertTrue(guard.shouldAccept(message, nowMillis = 10_000L))
        assertFalse(guard.shouldAccept(message, nowMillis = 10_001L))
    }

    @Test
    fun replayGuardRejectsStaleAndFutureMessages() {
        val guard = NearVoteMessageReplayGuard(
            messageTtlMillis = 1_000L,
            futureSkewMillis = 100L
        )

        assertFalse(
            guard.shouldAccept(
                NearVoteMessage(
                    type = NearVoteMessageType.PING,
                    senderId = "user-1",
                    payloadJson = "{}",
                    messageId = "old-message",
                    createdAtMillis = 8_999L
                ),
                nowMillis = 10_000L
            )
        )
        assertFalse(
            guard.shouldAccept(
                NearVoteMessage(
                    type = NearVoteMessageType.PING,
                    senderId = "user-1",
                    payloadJson = "{}",
                    messageId = "future-message",
                    createdAtMillis = 10_101L
                ),
                nowMillis = 10_000L
            )
        )
    }
}
