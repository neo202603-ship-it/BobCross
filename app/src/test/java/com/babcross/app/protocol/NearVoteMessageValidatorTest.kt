package com.babcross.app.protocol

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NearVoteMessageValidatorTest {
    private val limits = NearVoteMessageValidator.Limits(
        maxSenderIdLength = 8,
        maxMessageIdLength = 12,
        maxPayloadJsonLength = 16
    )

    @Test
    fun acceptsValidMessageOnce() {
        val guard = NearVoteMessageReplayGuard()
        val message = message(messageId = "message-1")

        assertTrue(NearVoteMessageValidator.isValid(message, limits, guard, nowMillis = 10_000L))
        assertFalse(NearVoteMessageValidator.isValid(message, limits, guard, nowMillis = 10_001L))
    }

    @Test
    fun rejectsInvalidSchemaAndCreatedAt() {
        val guard = NearVoteMessageReplayGuard()

        assertFalse(
            NearVoteMessageValidator.isValid(
                message(schemaVersion = 0, messageId = "schema-low"),
                limits,
                guard,
                nowMillis = 10_000L
            )
        )
        assertFalse(
            NearVoteMessageValidator.isValid(
                message(schemaVersion = NearVoteMessage.CURRENT_SCHEMA_VERSION + 1, messageId = "schema-high"),
                limits,
                guard,
                nowMillis = 10_000L
            )
        )
        assertFalse(
            NearVoteMessageValidator.isValid(
                message(createdAtMillis = 0L, messageId = "zero-time"),
                limits,
                guard,
                nowMillis = 10_000L
            )
        )
    }

    @Test
    fun rejectsBlankAndOversizedIdentifiers() {
        val guard = NearVoteMessageReplayGuard()

        assertFalse(NearVoteMessageValidator.isValid(message(senderId = "", messageId = "blank-s"), limits, guard, nowMillis = 10_000L))
        assertFalse(NearVoteMessageValidator.isValid(message(senderId = "sender-too-long", messageId = "long-s"), limits, guard, nowMillis = 10_000L))
        assertFalse(NearVoteMessageValidator.isValid(message(messageId = ""), limits, guard, nowMillis = 10_000L))
        assertFalse(NearVoteMessageValidator.isValid(message(messageId = "message-id-too-long"), limits, guard, nowMillis = 10_000L))
    }

    @Test
    fun rejectsOversizedPayload() {
        val guard = NearVoteMessageReplayGuard()
        val oversizedPayload = "x".repeat(limits.maxPayloadJsonLength + 1)

        assertFalse(
            NearVoteMessageValidator.isValid(
                message(payloadJson = oversizedPayload, messageId = "payload"),
                limits,
                guard,
                nowMillis = 10_000L
            )
        )
    }

    @Test
    fun rejectsReplayGuardStaleAndFutureMessages() {
        val guard = NearVoteMessageReplayGuard(
            messageTtlMillis = 1_000L,
            futureSkewMillis = 100L
        )

        assertFalse(
            NearVoteMessageValidator.isValid(
                message(messageId = "old", createdAtMillis = 8_999L),
                limits,
                guard,
                nowMillis = 10_000L
            )
        )
        assertFalse(
            NearVoteMessageValidator.isValid(
                message(messageId = "future", createdAtMillis = 10_101L),
                limits,
                guard,
                nowMillis = 10_000L
            )
        )
    }

    private fun message(
        senderId: String = "sender",
        messageId: String = "message",
        payloadJson: String = "{}",
        schemaVersion: Int = NearVoteMessage.CURRENT_SCHEMA_VERSION,
        createdAtMillis: Long = 10_000L
    ): NearVoteMessage {
        return NearVoteMessage(
            type = NearVoteMessageType.PING,
            senderId = senderId,
            payloadJson = payloadJson,
            schemaVersion = schemaVersion,
            messageId = messageId,
            createdAtMillis = createdAtMillis
        )
    }
}
