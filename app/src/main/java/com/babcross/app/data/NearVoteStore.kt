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

    fun loadRecentFoodCategoryKey(): String? {
        return prefs.getString(KEY_RECENT_FOOD_CATEGORY, null)?.takeIf { it.isNotBlank() }
    }

    fun saveRecentFoodCategoryKey(categoryKey: String) {
        prefs.edit().putString(KEY_RECENT_FOOD_CATEGORY, categoryKey).apply()
    }

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    }

    fun saveOnboardingCompleted() {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
    }

    fun hasCompletedDemo(): Boolean {
        return prefs.getBoolean(KEY_DEMO_DONE, false)
    }

    fun saveDemoCompleted() {
        prefs.edit().putBoolean(KEY_DEMO_DONE, true).apply()
    }

    fun saveSessionState(stateJson: String) {
        prefs.edit().putString(KEY_SESSION_STATE, stateJson).apply()
    }

    fun loadSessionState(): String? {
        return prefs.getString(KEY_SESSION_STATE, null)
    }

    fun saveReceipt(receipt: VoteReceipt) {
        val receipts = loadJsonObject(KEY_RECEIPTS)
        receipts.put(receipt.pollId, receipt.toJson())
        prefs.edit().putString(KEY_RECEIPTS, receipts.toString()).apply()
    }

    fun loadReceipt(pollId: String): VoteReceipt? {
        val receipts = loadJsonObject(KEY_RECEIPTS)
        return receipts.optJSONObject(pollId)?.let { json ->
            runCatching { VoteReceipt.fromJson(json) }.getOrNull()
        }
    }

    fun loadResultCardRarity(pollId: String): String? {
        return loadJsonObject(KEY_RESULT_CARD_RARITIES).optString(pollId).takeIf { it.isNotBlank() }
    }

    fun saveResultCardRarity(pollId: String, rarityKey: String) {
        val rarities = loadJsonObject(KEY_RESULT_CARD_RARITIES)
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
        val results = loadJsonArray(KEY_RESULTS)
        return (0 until results.length()).mapNotNull { index ->
            runCatching { SharedResult.fromHistoryJson(results.getJSONObject(index)) }.getOrNull()
        }
    }

    fun loadTemplates(): List<PollTemplate> {
        val templates = loadJsonArray(KEY_TEMPLATES)
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

    fun clearResultHistory() {
        prefs.edit()
            .remove(KEY_RESULTS)
            .remove(KEY_RESULT_CARD_RARITIES)
            .remove(KEY_RESULT_CARD_REVEAL_COUNT)
            .remove(KEY_SESSION_STATE)
            .apply()
    }

    fun clearReceipts() {
        prefs.edit()
            .remove(KEY_RECEIPTS)
            .remove(KEY_RESULT_CARD_RARITIES)
            .remove(KEY_RESULT_CARD_REVEAL_COUNT)
            .apply()
    }

    fun clearUserTemplates() {
        prefs.edit().remove(KEY_TEMPLATES).apply()
    }

    fun clearProfileAndDefaults() {
        prefs.edit()
            .remove(KEY_IDENTITY)
            .remove(KEY_AVATAR_ID)
            .remove(KEY_USER_ID)
            .remove(KEY_DEFAULT_DURATION_SECONDS)
            .remove(KEY_DEFAULT_ALLOW_PARTICIPANT_OPTIONS)
            .remove(KEY_DEFAULT_REVEAL_SELECTIONS)
            .remove(KEY_RECENT_FOOD_CATEGORY)
            .apply()
    }

    fun clearAllLocalData() {
        prefs.edit().clear().apply()
    }

    private fun loadUserTemplates(): List<PollTemplate> {
        val templates = loadJsonArray(KEY_TEMPLATES)
        return (0 until templates.length()).mapNotNull { index ->
            runCatching { PollTemplate.fromJson(templates.getJSONObject(index)) }.getOrNull()
        }
    }

    private fun loadJsonObject(key: String): JSONObject {
        val raw = prefs.getString(key, "{}").orEmpty().ifBlank { "{}" }
        return runCatching { JSONObject(raw) }.getOrElse {
            prefs.edit().remove(key).apply()
            JSONObject()
        }
    }

    private fun loadJsonArray(key: String): JSONArray {
        val raw = prefs.getString(key, "[]").orEmpty().ifBlank { "[]" }
        return runCatching { JSONArray(raw) }.getOrElse {
            prefs.edit().remove(key).apply()
            JSONArray()
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
        private const val KEY_RECENT_FOOD_CATEGORY = "recent_food_category"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_DEMO_DONE = "demo_done"
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
                builtIn = true,
                categoryKey = "MEAL"
            ),
            PollTemplate(
                id = "builtin_dinner",
                title = "회식 밥신호",
                question = "오늘 회식 뭐 먹을까?",
                options = listOf("삼겹살", "치킨", "이자카야", "곱창"),
                durationMinutes = 10,
                builtIn = true,
                categoryKey = "DISH"
            ),
            PollTemplate(
                id = "builtin_coffee",
                title = "후식도 밥이다",
                question = "밥 먹고 뭐 마실까?",
                options = listOf("아메리카노", "라떼", "아이스티", "패스"),
                durationMinutes = 5,
                builtIn = true,
                categoryKey = "DRINK"
            ),
            PollTemplate(
                id = "builtin_cafe_order",
                title = "카페 주문 정하기",
                question = "카페에서 뭐 마실까요?",
                options = listOf("아메리카노", "라떼", "바닐라라떼", "말차라떼"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "DRINK"
            ),
            PollTemplate(
                id = "builtin_cool_drink",
                title = "시원한 음료 고르기",
                question = "더울 때 시원하게 뭐 마실까?",
                options = listOf("아이스티", "에이드", "스무디", "탄산수"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "DRINK"
            ),
            PollTemplate(
                id = "builtin_meeting_drink",
                title = "회의 전 한 잔",
                question = "회의 전에 한 잔만 고르면?",
                options = listOf("아메리카노", "카푸치노", "밀크티", "보리차"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "DRINK"
            ),
            PollTemplate(
                id = "builtin_sweet_after_meal",
                title = "밥 먹고 달달하게",
                question = "밥 먹고 달달하게 뭐 먹을까?",
                options = listOf("아이스크림", "케이크", "마카롱", "푸딩"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "DESSERT"
            ),
            PollTemplate(
                id = "builtin_cafe_dessert",
                title = "카페 디저트 고르기",
                question = "커피 옆에 둘 디저트는?",
                options = listOf("와플", "쿠키", "브라우니", "티라미수"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "DESSERT"
            ),
            PollTemplate(
                id = "builtin_hot_day_dessert",
                title = "더운 날 후식",
                question = "더운 날 후식은 뭐가 좋을까?",
                options = listOf("빙수", "젤라토", "파르페", "과일"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "DESSERT"
            ),
            PollTemplate(
                id = "builtin_convenience_snack",
                title = "편의점 간식",
                question = "편의점에서 간식 하나 고르면?",
                options = listOf("감자칩", "컵라면", "샌드위치", "초콜릿"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "SNACK"
            ),
            PollTemplate(
                id = "builtin_overtime_snack",
                title = "야근 전 간식",
                question = "야근 전에 뭐 하나 먹고 갈까?",
                options = listOf("토스트", "닭강정", "만두", "견과류"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "SNACK"
            ),
            PollTemplate(
                id = "builtin_bunsik_snack",
                title = "분식 간식",
                question = "분식 간식은 뭐로 할까?",
                options = listOf("떡볶이", "순대", "튀김", "어묵"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "SNACK"
            ),
            PollTemplate(
                id = "builtin_chinese",
                title = "짜장파 vs 짬뽕파",
                question = "오늘 중식은 뭐로 할까?",
                options = listOf("짜장면", "짬뽕", "탕수육", "마파두부"),
                durationMinutes = 5,
                builtIn = true,
                categoryKey = "DISH"
            ),
            PollTemplate(
                id = "builtin_japanese",
                title = "일식 원정대",
                question = "오늘 일식은 뭐 먹을까?",
                options = listOf("초밥", "라멘", "돈카츠", "우동"),
                durationMinutes = 5,
                builtIn = true,
                categoryKey = "DISH"
            ),
            PollTemplate(
                id = "builtin_team_lead_card",
                title = "팀장님 카드 찬스",
                question = "팀장님 카드라면 뭐 먹을까?",
                options = listOf("소고기", "초밥", "파스타", "평양냉면"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "MEAL"
            ),
            PollTemplate(
                id = "builtin_rainy_soup",
                title = "비 오는 날 국물전",
                question = "비 오는데 뜨끈한 국물은 뭐로 할까?",
                options = listOf("칼국수", "순댓국", "쌀국수", "부대찌개"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "DISH"
            ),
            PollTemplate(
                id = "builtin_fake_diet",
                title = "다이어트 하는 척",
                question = "가볍게 먹는 척하면서 만족할 메뉴는?",
                options = listOf("포케", "샐러드", "샤브샤브", "월남쌈"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "MEAL"
            ),
            PollTemplate(
                id = "builtin_five_minutes",
                title = "5분 안에 정해야 함",
                question = "5분 안에 정해야 하면 뭐 먹을까?",
                options = listOf("김밥", "덮밥", "버거", "라면"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "MEAL"
            ),
            PollTemplate(
                id = "builtin_no_afterparty",
                title = "회식 2차 안 가는 메뉴",
                question = "2차 없이 깔끔하게 끝낼 회식 메뉴는?",
                options = listOf("삼겹살", "족발", "찜닭", "이탈리안"),
                durationMinutes = 10,
                durationSeconds = 600,
                builtIn = true,
                revealSelections = false,
                categoryKey = "DISH"
            ),
            PollTemplate(
                id = "builtin_no_repeat",
                title = "어제 먹은 거 금지",
                question = "어제 먹은 메뉴 빼고 오늘은 뭐 먹지?",
                options = listOf("제육", "카레", "돈까스", "비빔밥"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                allowParticipantOptions = true,
                categoryKey = "MEAL"
            ),
            PollTemplate(
                id = "builtin_spicy_safe_zone",
                title = "맵찔이 보호 구역",
                question = "맵찔이도 웃으면서 먹을 메뉴는?",
                options = listOf("설렁탕", "돈까스", "오므라이스", "쌀국수"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                categoryKey = "DISH"
            ),
            PollTemplate(
                id = "builtin_delivery_saver",
                title = "배달비 아끼는 밥판",
                question = "걸어서 바로 먹을 수 있는 메뉴는?",
                options = listOf("편의점 도시락", "김밥천국", "분식", "구내식당"),
                durationMinutes = 5,
                durationSeconds = 300,
                builtIn = true,
                allowParticipantOptions = true,
                categoryKey = "MEAL"
            )
        )
    }
}

data class PollDefaults(
    val durationSeconds: Int = 300,
    val allowParticipantOptions: Boolean = false,
    val revealSelections: Boolean = true
)
