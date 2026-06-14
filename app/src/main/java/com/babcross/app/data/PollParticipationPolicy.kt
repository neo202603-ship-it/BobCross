package com.babcross.app.data

object PollParticipationPolicy {
    const val RESPONSE_ACCEPTED = "accepted"
    const val RESPONSE_DECLINED = "declined"

    data class HostSnapshot(
        val participantCount: Int,
        val selectedCount: Int,
        val waitingVoteCount: Int,
        val declinedCount: Int
    ) {
        fun detailText(): String {
            return buildList {
                add("밥판 참여 ${participantCount}명")
                add("선택 완료 ${selectedCount}명")
                if (waitingVoteCount > 0) add("메뉴 대기 ${waitingVoteCount}명")
                if (declinedCount > 0) add("거절 ${declinedCount}명")
            }.joinToString(" · ")
        }
    }

    fun hostSnapshot(
        selfParticipantId: String,
        responseByParticipantId: Map<String, String>,
        votedParticipantIds: Set<String>
    ): HostSnapshot {
        val acceptedRespondents = responseByParticipantId.count { (participantId, response) ->
            participantId != selfParticipantId && response == RESPONSE_ACCEPTED
        }
        val votedRespondents = votedParticipantIds.count { participantId ->
            participantId != selfParticipantId
        }
        val participantCount = acceptedRespondents.coerceAtLeast(votedRespondents) + 1
        val waitingVoteCount = responseByParticipantId.count { (participantId, response) ->
            participantId != selfParticipantId &&
                response == RESPONSE_ACCEPTED &&
                !votedParticipantIds.contains(participantId)
        }
        val declinedCount = responseByParticipantId.count { (_, response) ->
            response == RESPONSE_DECLINED
        }
        return HostSnapshot(
            participantCount = participantCount,
            selectedCount = votedParticipantIds.size,
            waitingVoteCount = waitingVoteCount,
            declinedCount = declinedCount
        )
    }

    fun inviteeStatusText(
        inviteeParticipantIds: Set<String>,
        responseByParticipantId: Map<String, String>,
        votedParticipantIds: Set<String>
    ): String? {
        if (inviteeParticipantIds.isEmpty()) return null

        val completed = inviteeParticipantIds.count { participantId ->
            votedParticipantIds.contains(participantId)
        }
        val declined = inviteeParticipantIds.count { participantId ->
            responseByParticipantId[participantId] == RESPONSE_DECLINED
        }
        val waitingVote = inviteeParticipantIds.count { participantId ->
            responseByParticipantId[participantId] == RESPONSE_ACCEPTED &&
                !votedParticipantIds.contains(participantId)
        }
        val awaitingResponse = inviteeParticipantIds.count { participantId ->
            !responseByParticipantId.containsKey(participantId)
        }
        return "선택 완료 ${completed}명 · 메뉴 대기 ${waitingVote}명 · 미응답 ${awaitingResponse}명 · 거절 ${declined}명"
    }
}
