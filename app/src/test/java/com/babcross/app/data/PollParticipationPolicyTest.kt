package com.babcross.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PollParticipationPolicyTest {
    @Test
    fun hostSnapshotCountsAcceptedAndVotedParticipants() {
        val snapshot = PollParticipationPolicy.hostSnapshot(
            selfParticipantId = "host",
            responseByParticipantId = mapOf(
                "one" to PollParticipationPolicy.RESPONSE_ACCEPTED,
                "two" to PollParticipationPolicy.RESPONSE_ACCEPTED
            ),
            votedParticipantIds = setOf("host", "one")
        )

        assertEquals(3, snapshot.participantCount)
        assertEquals(2, snapshot.selectedCount)
        assertEquals(1, snapshot.waitingVoteCount)
        assertEquals(0, snapshot.declinedCount)
        assertEquals("밥판 참여 3명 · 선택 완료 2명 · 메뉴 대기 1명", snapshot.detailText())
    }

    @Test
    fun hostSnapshotCountsLateVoteEvenBeforeAcceptedResponseArrives() {
        val snapshot = PollParticipationPolicy.hostSnapshot(
            selfParticipantId = "host",
            responseByParticipantId = emptyMap(),
            votedParticipantIds = setOf("host", "late")
        )

        assertEquals(2, snapshot.participantCount)
        assertEquals(2, snapshot.selectedCount)
        assertEquals(0, snapshot.waitingVoteCount)
    }

    @Test
    fun hostSnapshotKeepsDeclinedParticipantsOutOfReadyCount() {
        val snapshot = PollParticipationPolicy.hostSnapshot(
            selfParticipantId = "host",
            responseByParticipantId = mapOf(
                "one" to PollParticipationPolicy.RESPONSE_ACCEPTED,
                "two" to PollParticipationPolicy.RESPONSE_DECLINED
            ),
            votedParticipantIds = setOf("host", "one")
        )

        assertEquals(2, snapshot.participantCount)
        assertEquals(2, snapshot.selectedCount)
        assertEquals(0, snapshot.waitingVoteCount)
        assertEquals(1, snapshot.declinedCount)
        assertEquals("밥판 참여 2명 · 선택 완료 2명 · 거절 1명", snapshot.detailText())
    }

    @Test
    fun inviteeStatusTextSummarizesCompletedWaitingUnansweredAndDeclined() {
        val text = PollParticipationPolicy.inviteeStatusText(
            inviteeParticipantIds = setOf("one", "two", "three", "four"),
            responseByParticipantId = mapOf(
                "one" to PollParticipationPolicy.RESPONSE_ACCEPTED,
                "two" to PollParticipationPolicy.RESPONSE_ACCEPTED,
                "three" to PollParticipationPolicy.RESPONSE_DECLINED
            ),
            votedParticipantIds = setOf("one")
        )

        assertEquals("선택 완료 1명 · 메뉴 대기 1명 · 미응답 1명 · 거절 1명", text)
    }

    @Test
    fun inviteeStatusTextIsHiddenWithoutInvitees() {
        val text = PollParticipationPolicy.inviteeStatusText(
            inviteeParticipantIds = emptySet(),
            responseByParticipantId = emptyMap(),
            votedParticipantIds = emptySet()
        )

        assertNull(text)
    }
}
