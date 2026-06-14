package com.babcross.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class PollCompletionPolicyTest {
    @Test
    fun promptAppearsWhenEveryReadyParticipantVoted() {
        val prompt = PollCompletionPolicy.promptFor(
            input(
                readyCount = 3,
                votedParticipantIds = setOf("host", "one", "two"),
                invitedParticipantIds = setOf("one", "two"),
                responseByParticipantId = mapOf("one" to "accepted", "two" to "accepted")
            )
        )

        assertNotNull(prompt)
        assertEquals("poll-test:ready-3:voted-3", prompt?.key)
        assertEquals(3, prompt?.readyCount)
        assertEquals(3, prompt?.votedCount)
    }

    @Test
    fun promptWaitsForLateInviteeVote() {
        val prompt = PollCompletionPolicy.promptFor(
            input(
                readyCount = 3,
                votedParticipantIds = setOf("host", "one"),
                invitedParticipantIds = setOf("one", "two"),
                responseByParticipantId = mapOf("one" to "accepted", "two" to "accepted")
            )
        )

        assertNull(prompt)
    }

    @Test
    fun promptDoesNotRepeatForSameReadyAndVoteCount() {
        val prompt = PollCompletionPolicy.promptFor(
            input(
                readyCount = 2,
                votedParticipantIds = setOf("host", "one"),
                invitedParticipantIds = setOf("one"),
                responseByParticipantId = mapOf("one" to "accepted"),
                shownPromptKeys = setOf("poll-test:ready-2:voted-2")
            )
        )

        assertNull(prompt)
    }

    @Test
    fun promptWaitsWhenInviteeHasNotAnswered() {
        val prompt = PollCompletionPolicy.promptFor(
            input(
                readyCount = 2,
                votedParticipantIds = setOf("host", "one"),
                invitedParticipantIds = setOf("one"),
                responseByParticipantId = emptyMap()
            )
        )

        assertNull(prompt)
    }

    @Test
    fun promptIgnoresDeclinedInviteeEvenIfVoteCountMatches() {
        val prompt = PollCompletionPolicy.promptFor(
            input(
                readyCount = 2,
                votedParticipantIds = setOf("host", "one"),
                invitedParticipantIds = setOf("one"),
                responseByParticipantId = mapOf("one" to PollCompletionPolicy.RESPONSE_DECLINED)
            )
        )

        assertNull(prompt)
    }

    @Test
    fun promptSkipsSoloAndCompletedPolls() {
        assertNull(
            PollCompletionPolicy.promptFor(
                input(
                    readyCount = 1,
                    votedParticipantIds = setOf("host"),
                    invitedParticipantIds = emptySet(),
                    responseByParticipantId = emptyMap()
                )
            )
        )
        assertNull(
            PollCompletionPolicy.promptFor(
                input(
                    isEnded = true,
                    readyCount = 2,
                    votedParticipantIds = setOf("host", "one"),
                    invitedParticipantIds = setOf("one"),
                    responseByParticipantId = mapOf("one" to "accepted")
                )
            )
        )
        assertNull(
            PollCompletionPolicy.promptFor(
                input(
                    isResultShared = true,
                    readyCount = 2,
                    votedParticipantIds = setOf("host", "one"),
                    invitedParticipantIds = setOf("one"),
                    responseByParticipantId = mapOf("one" to "accepted")
                )
            )
        )
    }

    private fun input(
        isEnded: Boolean = false,
        isResultShared: Boolean = false,
        readyCount: Int,
        votedParticipantIds: Set<String>,
        invitedParticipantIds: Set<String>,
        responseByParticipantId: Map<String, String>,
        shownPromptKeys: Set<String> = emptySet()
    ): PollCompletionPolicy.Input {
        return PollCompletionPolicy.Input(
            pollId = "poll-test",
            isEnded = isEnded,
            isResultShared = isResultShared,
            readyCount = readyCount,
            votedParticipantIds = votedParticipantIds,
            invitedParticipantIds = invitedParticipantIds,
            responseByParticipantId = responseByParticipantId,
            selfParticipantId = "host",
            shownPromptKeys = shownPromptKeys
        )
    }
}
