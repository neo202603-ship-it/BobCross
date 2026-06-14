package com.babcross.app.protocol

object NearVoteMessageValidator {
    data class Limits(
        val maxSenderIdLength: Int,
        val maxMessageIdLength: Int,
        val maxPayloadJsonLength: Int
    )

    fun isValid(
        message: NearVoteMessage,
        limits: Limits,
        replayGuard: NearVoteMessageReplayGuard,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return message.schemaVersion in 1..NearVoteMessage.CURRENT_SCHEMA_VERSION &&
            message.messageId.isNotBlank() &&
            message.messageId.length <= limits.maxMessageIdLength &&
            message.senderId.isNotBlank() &&
            message.senderId.length <= limits.maxSenderIdLength &&
            message.payloadJson.length <= limits.maxPayloadJsonLength &&
            message.createdAtMillis > 0L &&
            replayGuard.shouldAccept(message, nowMillis)
    }
}
