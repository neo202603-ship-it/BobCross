package com.babcross.app.protocol

class NearVoteMessageReplayGuard(
    private val maxEntries: Int = DEFAULT_MAX_ENTRIES,
    private val messageTtlMillis: Long = DEFAULT_MESSAGE_TTL_MILLIS,
    private val futureSkewMillis: Long = DEFAULT_FUTURE_SKEW_MILLIS
) {
    private val seenMessageTimes = linkedMapOf<String, Long>()

    fun shouldAccept(message: NearVoteMessage, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (message.createdAtMillis < nowMillis - messageTtlMillis) return false
        if (message.createdAtMillis > nowMillis + futureSkewMillis) return false

        purgeExpired(nowMillis)
        if (seenMessageTimes.containsKey(message.messageId)) return false

        seenMessageTimes[message.messageId] = message.createdAtMillis
        trimToMaxEntries()
        return true
    }

    fun clear() {
        seenMessageTimes.clear()
    }

    private fun purgeExpired(nowMillis: Long) {
        val iterator = seenMessageTimes.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value < nowMillis - messageTtlMillis) {
                iterator.remove()
            }
        }
    }

    private fun trimToMaxEntries() {
        while (seenMessageTimes.size > maxEntries) {
            val oldestKey = seenMessageTimes.keys.firstOrNull() ?: return
            seenMessageTimes.remove(oldestKey)
        }
    }

    companion object {
        const val DEFAULT_MAX_ENTRIES = 256
        const val DEFAULT_MESSAGE_TTL_MILLIS = 10 * 60 * 1000L
        const val DEFAULT_FUTURE_SKEW_MILLIS = 30 * 1000L
    }
}
