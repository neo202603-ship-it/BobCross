package com.babcross.app.data

data class MenuCalorieHint(
    val kcal: Int,
    val matchedName: String
)

object MenuCalorieCatalog {
    private val exactCalories = mapOf(
        "국밥" to 700,
        "제육" to 780,
        "제육볶음" to 780,
        "김밥" to 420,
        "샐러드" to 320,
        "삼겹살" to 900,
        "치킨" to 950,
        "이자카야" to 850,
        "곱창" to 920,
        "아메리카노" to 10,
        "라떼" to 180,
        "아이스티" to 160,
        "짜장면" to 800,
        "짬뽕" to 690,
        "탕수육" to 900,
        "마파두부" to 720,
        "초밥" to 550,
        "라멘" to 720,
        "돈카츠" to 850,
        "돈까스" to 850,
        "우동" to 520,
        "소고기" to 820,
        "파스타" to 760,
        "평양냉면" to 480,
        "칼국수" to 650,
        "순댓국" to 720,
        "쌀국수" to 520,
        "부대찌개" to 820,
        "포케" to 520,
        "샤브샤브" to 560,
        "월남쌈" to 480,
        "덮밥" to 720,
        "버거" to 760,
        "라면" to 500,
        "족발" to 850,
        "찜닭" to 780,
        "이탈리안" to 820,
        "카레" to 720,
        "비빔밥" to 650,
        "설렁탕" to 620,
        "오므라이스" to 730,
        "편의점 도시락" to 750,
        "김밥천국" to 650,
        "분식" to 620,
        "구내식당" to 700
    )

    private val keywordCalories = listOf(
        "샐러드" to 320,
        "아메리카노" to 10,
        "라떼" to 180,
        "김밥" to 420,
        "냉면" to 480,
        "라면" to 500,
        "우동" to 520,
        "쌀국수" to 520,
        "포케" to 520,
        "초밥" to 550,
        "샤브" to 560,
        "국수" to 620,
        "분식" to 620,
        "국밥" to 700,
        "덮밥" to 720,
        "카레" to 720,
        "파스타" to 760,
        "버거" to 760,
        "제육" to 780,
        "찜닭" to 780,
        "짜장" to 800,
        "찌개" to 820,
        "돈까스" to 850,
        "돈카츠" to 850,
        "족발" to 850,
        "삼겹" to 900,
        "탕수육" to 900,
        "곱창" to 920,
        "치킨" to 950
    )

    fun estimateFor(menuName: String): MenuCalorieHint? {
        val normalized = menuName.trim().replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return null

        exactCalories[normalized]?.let { kcal ->
            return MenuCalorieHint(kcal, normalized)
        }

        val compact = normalized.replace(" ", "")
        keywordCalories.firstOrNull { (keyword, _) -> compact.contains(keyword) }?.let { (keyword, kcal) ->
            return MenuCalorieHint(kcal, keyword)
        }

        return null
    }
}
