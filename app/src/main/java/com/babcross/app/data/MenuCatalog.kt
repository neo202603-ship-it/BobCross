package com.babcross.app.data

data class MenuCatalogItem(
    val name: String,
    val categoryKey: String,
    val kcal: Int,
    val aliases: List<String> = emptyList(),
    val showInRoulette: Boolean = true,
    val useInTemplates: Boolean = true
)

object MenuCatalog {
    private val nonFoodOptions = setOf("패스", "아무거나", "미정")

    val items = listOf(
        MenuCatalogItem("김밥", "MEAL", 420),
        MenuCatalogItem("덮밥", "MEAL", 720),
        MenuCatalogItem("쌀국수", "MEAL", 520),
        MenuCatalogItem("샐러드", "MEAL", 320),
        MenuCatalogItem("포케", "MEAL", 520),
        MenuCatalogItem("월남쌈", "MEAL", 480),
        MenuCatalogItem("버거", "MEAL", 760),
        MenuCatalogItem("초밥", "MEAL", 550),
        MenuCatalogItem("카레", "MEAL", 720),
        MenuCatalogItem("비빔밥", "MEAL", 650),
        MenuCatalogItem("파스타", "MEAL", 760),
        MenuCatalogItem("냉면", "MEAL", 480),
        MenuCatalogItem("오므라이스", "MEAL", 730),
        MenuCatalogItem("샤브샤브", "MEAL", 560, aliases = listOf("샤브")),
        MenuCatalogItem("라면", "MEAL", 500),
        MenuCatalogItem("평양냉면", "MEAL", 480, showInRoulette = false),
        MenuCatalogItem("편의점 도시락", "MEAL", 750, showInRoulette = false),
        MenuCatalogItem("김밥천국", "MEAL", 650, showInRoulette = false),
        MenuCatalogItem("구내식당", "MEAL", 700, showInRoulette = false),
        MenuCatalogItem("국밥", "DISH", 700),
        MenuCatalogItem("제육", "DISH", 780, aliases = listOf("제육볶음")),
        MenuCatalogItem("돈까스", "DISH", 850, aliases = listOf("돈카츠")),
        MenuCatalogItem("칼국수", "DISH", 650, aliases = listOf("국수")),
        MenuCatalogItem("부대찌개", "DISH", 820, aliases = listOf("찌개")),
        MenuCatalogItem("순댓국", "DISH", 720),
        MenuCatalogItem("라멘", "DISH", 720),
        MenuCatalogItem("삼겹살", "DISH", 900, aliases = listOf("삼겹")),
        MenuCatalogItem("치킨", "DISH", 950),
        MenuCatalogItem("이자카야", "DISH", 850),
        MenuCatalogItem("곱창", "DISH", 920),
        MenuCatalogItem("짜장면", "DISH", 800, aliases = listOf("짜장")),
        MenuCatalogItem("짬뽕", "DISH", 690),
        MenuCatalogItem("탕수육", "DISH", 900),
        MenuCatalogItem("마파두부", "DISH", 720),
        MenuCatalogItem("소고기", "DISH", 820, showInRoulette = false),
        MenuCatalogItem("우동", "DISH", 520, showInRoulette = false),
        MenuCatalogItem("족발", "DISH", 850, showInRoulette = false),
        MenuCatalogItem("찜닭", "DISH", 780, showInRoulette = false),
        MenuCatalogItem("이탈리안", "DISH", 820, showInRoulette = false),
        MenuCatalogItem("설렁탕", "DISH", 620, showInRoulette = false),
        MenuCatalogItem("분식", "SNACK", 620, showInRoulette = false),
        MenuCatalogItem("아메리카노", "DRINK", 10),
        MenuCatalogItem("라떼", "DRINK", 180),
        MenuCatalogItem("아이스티", "DRINK", 160),
        MenuCatalogItem("콜라", "DRINK", 140),
        MenuCatalogItem("사이다", "DRINK", 150),
        MenuCatalogItem("주스", "DRINK", 120),
        MenuCatalogItem("말차라떼", "DRINK", 260, aliases = listOf("말차 라떼", "녹차라떼")),
        MenuCatalogItem("바닐라라떼", "DRINK", 250, aliases = listOf("바닐라 라떼")),
        MenuCatalogItem("카푸치노", "DRINK", 120),
        MenuCatalogItem("에이드", "DRINK", 180),
        MenuCatalogItem("스무디", "DRINK", 320),
        MenuCatalogItem("밀크티", "DRINK", 300),
        MenuCatalogItem("초코라떼", "DRINK", 330, aliases = listOf("초코 라떼", "아이스초코")),
        MenuCatalogItem("탄산수", "DRINK", 0),
        MenuCatalogItem("보리차", "DRINK", 5),
        MenuCatalogItem("식혜", "DRINK", 180),
        MenuCatalogItem("아이스크림", "DESSERT", 220),
        MenuCatalogItem("케이크", "DESSERT", 380),
        MenuCatalogItem("빙수", "DESSERT", 520),
        MenuCatalogItem("푸딩", "DESSERT", 180),
        MenuCatalogItem("와플", "DESSERT", 420),
        MenuCatalogItem("마카롱", "DESSERT", 120),
        MenuCatalogItem("쿠키", "DESSERT", 220),
        MenuCatalogItem("도넛", "DESSERT", 300),
        MenuCatalogItem("브라우니", "DESSERT", 360),
        MenuCatalogItem("티라미수", "DESSERT", 420),
        MenuCatalogItem("크로플", "DESSERT", 450),
        MenuCatalogItem("파르페", "DESSERT", 380),
        MenuCatalogItem("젤라토", "DESSERT", 240),
        MenuCatalogItem("타르트", "DESSERT", 330),
        MenuCatalogItem("과일", "DESSERT", 120),
        MenuCatalogItem("요거트", "DESSERT", 160),
        MenuCatalogItem("떡볶이", "SNACK", 550),
        MenuCatalogItem("순대", "SNACK", 450),
        MenuCatalogItem("핫도그", "SNACK", 330),
        MenuCatalogItem("튀김", "SNACK", 500),
        MenuCatalogItem("만두", "SNACK", 420),
        MenuCatalogItem("감자칩", "SNACK", 340),
        MenuCatalogItem("팝콘", "SNACK", 300),
        MenuCatalogItem("컵라면", "SNACK", 470),
        MenuCatalogItem("샌드위치", "SNACK", 420),
        MenuCatalogItem("토스트", "SNACK", 430),
        MenuCatalogItem("붕어빵", "SNACK", 220),
        MenuCatalogItem("어묵", "SNACK", 180),
        MenuCatalogItem("닭강정", "SNACK", 650),
        MenuCatalogItem("프레첼", "SNACK", 280),
        MenuCatalogItem("견과류", "SNACK", 200),
        MenuCatalogItem("초콜릿", "SNACK", 260)
    ).distinctBy { it.name to it.categoryKey }

    fun rouletteOptionsFor(categoryKey: String): List<String> {
        return items
            .filter { item -> item.categoryKey == categoryKey && item.showInRoulette }
            .map { item -> item.name }
    }

    fun categoryKeyFor(menuName: String): String? {
        return findItem(menuName)?.categoryKey
    }

    fun isKnownFoodOrException(menuName: String): Boolean {
        val normalized = normalize(menuName)
        if (normalized in nonFoodOptions) return true
        return findItem(normalized) != null
    }

    fun estimateCalories(menuName: String): MenuCalorieHint? {
        val normalized = normalize(menuName)
        if (normalized.isBlank() || normalized in nonFoodOptions) return null
        exactItemFor(normalized)?.let { (item, matchedName) ->
            return MenuCalorieHint(item.kcal, matchedName)
        }
        val compact = normalized.replace(" ", "")
        keywordCandidates().firstOrNull { (keyword, _) -> compact.contains(keyword) }?.let { (keyword, item) ->
            return MenuCalorieHint(item.kcal, keyword)
        }
        return null
    }

    private fun findItem(menuName: String): MenuCatalogItem? {
        val normalized = normalize(menuName)
        if (normalized.isBlank()) return null
        exactItemFor(normalized)?.let { (item, _) -> return item }
        val compact = normalized.replace(" ", "")
        return keywordCandidates().firstOrNull { (keyword, _) -> compact.contains(keyword) }?.second
    }

    private fun exactItemFor(normalizedName: String): Pair<MenuCatalogItem, String>? {
        return items.firstNotNullOfOrNull { item ->
            val names = listOf(item.name) + item.aliases
            names.firstOrNull { alias -> normalize(alias) == normalizedName }?.let { matchedName -> item to matchedName }
        }
    }

    private fun keywordCandidates(): List<Pair<String, MenuCatalogItem>> {
        return items.flatMap { item ->
            (item.aliases + item.name).map { keyword -> normalize(keyword).replace(" ", "") to item }
        }
            .filter { (keyword, _) -> keyword.isNotBlank() }
            .sortedByDescending { (keyword, _) -> keyword.length }
    }

    private fun normalize(value: String): String {
        return value.trim().replace(Regex("\\s+"), " ")
    }
}
