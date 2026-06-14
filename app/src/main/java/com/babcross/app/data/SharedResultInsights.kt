package com.babcross.app.data

object SharedResultInsights {
    fun rankedOptions(result: SharedResult): List<String> {
        return result.options.withIndex()
            .sortedWith(compareByDescending<IndexedValue<String>> { result.counts[it.value] ?: 0 }.thenBy { it.index })
            .map { it.value }
    }

    fun hasVotes(result: SharedResult): Boolean {
        return result.counts.values.any { count -> count > 0 }
    }

    fun singleWinningMenu(result: SharedResult): String? {
        val winningCount = result.counts.values.maxOrNull() ?: return null
        if (winningCount <= 0) return null
        val winningOptions = rankedOptions(result).filter { option -> result.counts[option] == winningCount }
        return winningOptions.singleOrNull()
    }

    fun tieBreakSuggestion(result: SharedResult, currentUserId: String): String? {
        val winningCount = result.counts.values.maxOrNull() ?: return null
        if (winningCount <= 0) return null
        val winningOptions = rankedOptions(result).filter { option -> result.counts[option] == winningCount }
        if (winningOptions.size < 2) return null
        val proposerSelection = result.participantSelections[result.proposerId]
        if (proposerSelection in winningOptions) return proposerSelection
        val mySelection = result.participantSelections[currentUserId]
        if (mySelection in winningOptions) return mySelection
        return winningOptions.firstOrNull()
    }

    fun decisionMenuHeadline(result: SharedResult): String {
        if (!hasVotes(result)) return "결정 보류"
        val winningCount = result.counts.values.maxOrNull() ?: return "결정 보류"
        val winningOptions = rankedOptions(result).filter { option -> result.counts[option] == winningCount }
        return if (winningOptions.size == 1) {
            winningOptions.first()
        } else {
            "공동 1등: ${winningOptions.joinToString(", ")}"
        }
    }

    fun shareCandidateSummary(result: SharedResult, maxVisibleCandidates: Int): String {
        val ranked = rankedOptions(result)
        return ranked.take(maxVisibleCandidates).joinToString(", ") { option ->
            "$option ${(result.counts[option] ?: 0)}표"
        } + if (ranked.size > maxVisibleCandidates) " 외 ${ranked.size - maxVisibleCandidates}개" else ""
    }

    fun decisionReasonText(result: SharedResult, currentUserId: String): String {
        if (!hasVotes(result)) return "아직 표가 모이지 않아 다음 밥판으로 넘겼습니다."
        val winningCount = result.counts.values.maxOrNull() ?: 0
        val winningOptions = rankedOptions(result).filter { option -> result.counts[option] == winningCount }
        return if (winningOptions.size == 1) {
            "${winningOptions.first()}이(가) ${winningCount}표로 가장 많이 선택됐습니다."
        } else {
            "공동 1등이라 밥판장 기준으로 ${tieBreakSuggestion(result, currentUserId) ?: winningOptions.first()}을(를) 추천합니다."
        }
    }

    fun buildShareText(result: SharedResult, currentUserId: String, maxVisibleCandidates: Int): String {
        return "밥크로스 오늘의 밥결정: ${decisionMenuHeadline(result)}\n" +
            "후보: ${shareCandidateSummary(result, maxVisibleCandidates)}\n" +
            "이유: ${decisionReasonText(result, currentUserId)}\n" +
            "다음 밥판은 같이 고르기"
    }
}
