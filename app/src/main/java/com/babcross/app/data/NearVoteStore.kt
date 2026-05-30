package com.babcross.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class NearVoteStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadIdentity(fallback: () -> String): String {
        val saved = prefs.getString(KEY_IDENTITY, null)
        if (!saved.isNullOrBlank()) return saved
        val suggested = fallback()
        saveIdentity(suggested)
        return suggested
    }

    fun saveIdentity(identity: String) {
        prefs.edit().putString(KEY_IDENTITY, identity).apply()
    }

    fun loadAvatarId(fallback: () -> Int): Int {
        if (prefs.contains(KEY_AVATAR_ID)) return prefs.getInt(KEY_AVATAR_ID, 0)
        val suggested = fallback()
        saveAvatarId(suggested)
        return suggested
    }

    fun saveAvatarId(avatarId: Int) {
        prefs.edit().putInt(KEY_AVATAR_ID, avatarId).apply()
    }

    fun loadUserId(fallback: () -> String): String {
        val saved = prefs.getString(KEY_USER_ID, null)
        if (!saved.isNullOrBlank()) return saved
        val generated = fallback()
        prefs.edit().putString(KEY_USER_ID, generated).apply()
        return generated
    }

    fun isAutoConnectEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CONNECT, true)
    }

    fun saveAutoConnectEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CONNECT, enabled).apply()
    }

    fun loadPollDefaults(): PollDefaults {
        return PollDefaults(
            durationSeconds = prefs.getInt(KEY_DEFAULT_DURATION_SECONDS, 300),
            allowParticipantOptions = prefs.getBoolean(KEY_DEFAULT_ALLOW_PARTICIPANT_OPTIONS, false),
            revealSelections = prefs.getBoolean(KEY_DEFAULT_REVEAL_SELECTIONS, true)
        )
    }

    fun savePollDefaults(defaults: PollDefaults) {
        prefs.edit()
            .putInt(KEY_DEFAULT_DURATION_SECONDS, defaults.durationSeconds)
            .putBoolean(KEY_DEFAULT_ALLOW_PARTICIPANT_OPTIONS, defaults.allowParticipantOptions)
            .putBoolean(KEY_DEFAULT_REVEAL_SELECTIONS, defaults.revealSelections)
            .apply()
    }

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun saveOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun saveSessionState(stateJson: String) {
        prefs.edit().putString(KEY_SESSION_STATE, stateJson).apply()
    }

    fun loadSessionState(): String? {
        return prefs.getString(KEY_SESSION_STATE, null)
    }

    fun saveReceipt(receipt: VoteReceipt) {
        val receipts = JSONObject(prefs.getString(KEY_RECEIPTS, "{}").orEmpty().ifBlank { "{}" })
        receipts.put(receipt.pollId, receipt.toJson())
        prefs.edit().putString(KEY_RECEIPTS, receipts.toString()).apply()
    }

    fun loadReceipt(pollId: String): VoteReceipt? {
        val raw = prefs.getString(KEY_RECEIPTS, "{}").orEmpty().ifBlank { "{}" }
        val receipts = JSONObject(raw)
        return receipts.optJSONObject(pollId)?.let { VoteReceipt.fromJson(it) }
    }

    fun loadResultCardRarity(pollId: String): String? {
        val raw = prefs.getString(KEY_RESULT_CARD_RARITIES, "{}").orEmpty().ifBlank { "{}" }
        return JSONObject(raw).optString(pollId).takeIf { it.isNotBlank() }
    }

    fun saveResultCardRarity(pollId: String, rarityKey: String) {
        val rarities = JSONObject(prefs.getString(KEY_RESULT_CARD_RARITIES, "{}").orEmpty().ifBlank { "{}" })
        rarities.put(pollId, rarityKey)
        prefs.edit().putString(KEY_RESULT_CARD_RARITIES, rarities.toString()).apply()
    }

    fun nextResultCardRevealIndex(): Int {
        val nextIndex = prefs.getInt(KEY_RESULT_CARD_REVEAL_COUNT, 0)
        prefs.edit().putInt(KEY_RESULT_CARD_REVEAL_COUNT, nextIndex + 1).apply()
        return nextIndex
    }

    fun saveResult(result: SharedResult) {
        val existing = loadResultHistory()
            .filterNot { it.pollId == result.pollId }
            .toMutableList()
        existing.add(0, result)
        val results = JSONArray()
        existing.take(MAX_HISTORY_COUNT).forEach { results.put(it.toHistoryJson()) }
        prefs.edit().putString(KEY_RESULTS, results.toString()).apply()
    }

    fun loadResultHistory(): List<SharedResult> {
        val raw = prefs.getString(KEY_RESULTS, "[]").orEmpty().ifBlank { "[]" }
        val results = JSONArray(raw)
        return (0 until results.length()).mapNotNull { index ->
            runCatching { SharedResult.fromHistoryJson(results.getJSONObject(index)) }.getOrNull()
        }
    }

    fun loadTemplates(): List<PollTemplate> {
        val saved = prefs.getString(KEY_TEMPLATES, "[]").orEmpty().ifBlank { "[]" }
        val templates = JSONArray(saved)
        val userTemplates = (0 until templates.length()).mapNotNull { index ->
            runCatching { PollTemplate.fromJson(templates.getJSONObject(index)) }.getOrNull()
        }
        return defaultTemplates + userTemplates
    }

    fun saveTemplate(template: PollTemplate) {
        val userTemplates = loadUserTemplates()
            .filterNot { it.id == template.id }
            .toMutableList()
        userTemplates.add(0, template.copy(builtIn = false))
        persistUserTemplates(userTemplates)
    }

    fun deleteTemplate(templateId: String) {
        persistUserTemplates(loadUserTemplates().filterNot { it.id == templateId })
    }

    private fun loadUserTemplates(): List<PollTemplate> {
        val saved = prefs.getString(KEY_TEMPLATES, "[]").orEmpty().ifBlank { "[]" }
        val templates = JSONArray(saved)
        return (0 until templates.length()).mapNotNull { index ->
            runCatching { PollTemplate.fromJson(templates.getJSONObject(index)) }.getOrNull()
        }
    }

    private fun persistUserTemplates(templates: List<PollTemplate>) {
        val json = JSONArray()
        templates.forEach { json.put(it.toJson()) }
        prefs.edit().putString(KEY_TEMPLATES, json.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "bab_cross_prefs"
        private const val KEY_IDENTITY = "identity"
        private const val KEY_AVATAR_ID = "avatar_id"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_AUTO_CONNECT = "auto_connect"
        private const val KEY_DEFAULT_DURATION_SECONDS = "default_duration_seconds"
        private const val KEY_DEFAULT_ALLOW_PARTICIPANT_OPTIONS = "default_allow_participant_options"
        private const val KEY_DEFAULT_REVEAL_SELECTIONS = "default_reveal_selections"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_SESSION_STATE = "session_state"
        private const val KEY_RECEIPTS = "receipts"
        private const val KEY_RESULTS = "results"
        private const val KEY_RESULT_CARD_RARITIES = "result_card_rarities"
        private const val KEY_RESULT_CARD_REVEAL_COUNT = "result_card_reveal_count"
        private const val KEY_TEMPLATES = "templates"
        private const val MAX_HISTORY_COUNT = 20

        private val defaultTemplates = listOf(
            PollTemplate(
                id = "builtin_lunch",
                title = "메뉴 고르기 참 쉽죠?",
                question = "오늘 점심 뭐 먹지?",
                options = listOf("국밥", "제육", "김밥", "샐러드"),
                durationMinutes = 5,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_dinner",
                title = "회식 밥신호",
                question = "오늘 회식 어디로 교차할까?",
                options = listOf("삼겹살", "치킨", "이자카야", "곱창"),
                durationMinutes = 10,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_coffee",
                title = "후식도 밥이다",
                question = "밥 먹고 뭐 마실까?",
                options = listOf("아메리카노", "라떼", "아이스티", "패스"),
                durationMinutes = 5,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_chinese",
                title = "짜장파 vs 짬뽕파",
                question = "오늘 중식 우주는 어느 그릇으로 기울까?",
                options = listOf("짜장면", "짬뽕", "탕수육", "마파두부"),
                durationMinutes = 5,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_japanese",
                title = "일식 원정대",
                question = "오늘 젓가락은 어느 섬에 상륙할까?",
                options = listOf("초밥", "라멘", "돈카츠", "우동"),
                durationMinutes = 5,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_team_lead_card",
                title = "팀장님 카드 찬스",
                question = "팀장님 카드가 열린다면 어디까지 갈까?",
                options = listOf("소고기", "초밥", "파스타", "평양냉면"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_rainy_soup",
                title = "비 오는 날 국물전",
                question = "비 오는데 뜨끈한 국물은 뭐로 할까?",
                options = listOf("칼국수", "순댓국", "쌀국수", "부대찌개"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_fake_diet",
                title = "다이어트 하는 척",
                question = "가볍게 먹는 척하면서 만족할 메뉴는?",
                options = listOf("포케", "샐러드", "샤브샤브", "월남쌈"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_five_minutes",
                title = "5분 안에 정해야 함",
                question = "5분 안에 정해야 하는 오늘의 밥상은?",
                options = listOf("김밥", "덮밥", "버거", "라면"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_no_afterparty",
                title = "회식 2차 안 가는 메뉴",
                question = "2차 없이 깔끔하게 끝낼 회식 메뉴는?",
                options = listOf("삼겹살", "족발", "찜닭", "이탈리안"),
                durationMinutes = 10,
                durationSeconds = 600,
                builtIn = true,
                revealSelections = false
            ),
            PollTemplate(
                id = "builtin_no_repeat",
                title = "어제 먹은 거 금지",
                question = "어제 먹은 메뉴 빼고 오늘은 뭐 먹지?",
                options = listOf("제육", "카레", "돈까스", "비빔밥"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                allowParticipantOptions = true
            ),
            PollTemplate(
                id = "builtin_spicy_safe_zone",
                title = "맵찔이 보호 구역",
                question = "맵찔이도 웃으면서 먹을 메뉴는?",
                options = listOf("설렁탕", "돈까스", "오므라이스", "쌀국수"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true
            ),
            PollTemplate(
                id = "builtin_delivery_saver",
                title = "배달비 아끼는 밥판",
                question = "걸어서 바로 해결할 절약 메뉴는?",
                options = listOf("편의점 도시락", "김밥천국", "분식", "구내식당"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                allowParticipantOptions = true
            )
        )
    }
}

data class PollDefaults(
    val durationSeconds: Int = 300,
    val allowParticipantOptions: Boolean = false,
    val revealSelections: Boolean = true
)
