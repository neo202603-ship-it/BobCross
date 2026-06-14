package com.babcross.app.data

object PollCompletionPolicy {
    const val RESPONSE_DECLINED = "declined"

    data class Input(
        val pollId: String,
        val isEnded: Boolean,
        val isResultShared: Boolean,
        val readyCount: Int,
        val votedParticipantIds: Set<String>,
        val invitedParticipantIds: Set<String>,
        val responseByParticipantId: Map<String, String>,
        val selfParticipantId: String,
        val shownPromptKeys: Set<String>
    )

    data class Prompt(
        val key: String,
        val readyCount: Int,
        val votedCount: Int
    )

    fun promptFor(input: Input): Prompt? {
        if (input.isEnded || input.isResultShared) return null

        val votedCount = input.votedParticipantIds.size
        if (input.readyCount <= 1 || votedCount != input.readyCount) return null

        val promptKey = "${input.pollId}:ready-${input.readyCount}:voted-$votedCount"
        if (input.shownPromptKeys.contains(promptKey)) return null

        val requiredParticipantIds = input.invitedParticipantIds + input.selfParticipantId
        if (!requiredParticipantIds.all { participantId -> input.votedParticipantIds.contains(participantId) }) {
            return null
        }

        val hasUnansweredInvitees = input.invitedParticipantIds.any { participantId ->
            !input.responseByParticipantId.containsKey(participantId)
        }
        val hasDeclinedInvitees = input.invitedParticipantIds.any { participantId ->
            input.responseByParticipantId[participantId] == RESPONSE_DECLINED
        }
        if (hasUnansweredInvitees || hasDeclinedInvitees) return null

        return Prompt(
            key = promptKey,
            readyCount = input.readyCount,
            votedCount = votedCount
        )
    }
}
