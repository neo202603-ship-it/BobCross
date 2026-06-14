package com.babcross.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedResultInsightsTest {
    @Test
    fun rankedOptionsKeepOriginalOrderForTies() {
        val result = sampleResult(
            options = listOf("한식", "분식", "카페"),
            counts = mapOf("한식" to 2, "분식" to 2, "카페" to 1)
        )

        assertEquals(listOf("한식", "분식", "카페"), SharedResultInsights.rankedOptions(result))
        assertEquals("공동 1등: 한식, 분식", SharedResultInsights.decisionMenuHeadline(result))
    }

    @Test
    fun noVoteResultHasPendingTexts() {
        val result = sampleResult(
            counts = mapOf("한식" to 0, "분식" to 0),
            participantIds = emptyList(),
            participantSelections = emptyMap()
        )

        assertFalse(SharedResultInsights.hasVotes(result))
        assertNull(SharedResultInsights.singleWinningMenu(result))
        assertEquals("결정 보류", SharedResultInsights.decisionMenuHeadline(result))
        assertEquals("아직 표가 모이지 않아 다음 밥판으로 넘겼습니다.", SharedResultInsights.decisionReasonText(result, "one"))
    }

    @Test
    fun tieBreakPrefersProposerSelectionThenCurrentUserSelection() {
        val proposerTie = sampleResult(
            counts = mapOf("한식" to 1, "분식" to 1),
            participantSelections = mapOf("proposer" to "분식", "one" to "한식")
        )
        val userTie = proposerTie.copy(
            participantSelections = mapOf("one" to "한식", "two" to "분식")
        )

        assertEquals("분식", SharedResultInsights.tieBreakSuggestion(proposerTie, "one"))
        assertEquals("한식", SharedResultInsights.tieBreakSuggestion(userTie, "one"))
    }

    @Test
    fun shareTextIncludesAppNameCandidatesReasonAndInvite() {
        val result = sampleResult(
            options = listOf("한식", "분식", "카페"),
            counts = mapOf("한식" to 2, "분식" to 1, "카페" to 0),
            participantIds = listOf("proposer", "one", "two")
        )

        val text = SharedResultInsights.buildShareText(result, "one", maxVisibleCandidates = 2)

        assertTrue(text.contains("밥크로스 오늘의 밥결정: 한식"))
        assertTrue(text.contains("후보: 한식 2표, 분식 1표 외 1개"))
        assertTrue(text.contains("이유: 한식이(가) 2표로 가장 많이 선택됐습니다."))
        assertTrue(text.contains("다음 밥판은 같이 고르기"))
    }

    private fun sampleResult(
        options: List<String> = listOf("한식", "분식"),
        counts: Map<String, Int> = mapOf("한식" to 1, "분식" to 1),
        participantIds: List<String> = listOf("proposer", "one"),
        participantSelections: Map<String, String> = mapOf("proposer" to "한식", "one" to "분식")
    ): SharedResult {
        return SharedResult(
            pollId = "poll-test",
            proposerId = "proposer",
            proposerName = "차분한노트",
            question = "점심메뉴는?",
            options = options,
            counts = counts,
            participantIds = participantIds,
            participantNames = participantIds,
            participantSelections = participantSelections,
            participantCount = participantIds.size,
            createdAtMillis = 1L,
            resultHash = SharedResult.computeHash(
                pollId = "poll-test",
                question = "점심메뉴는?",
                options = options,
                counts = counts,
                participantIds = participantIds,
                participantSelections = participantSelections
            )
        )
    }
}
