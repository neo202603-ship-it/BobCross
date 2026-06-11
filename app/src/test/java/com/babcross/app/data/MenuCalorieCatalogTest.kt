package com.babcross.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class MenuCalorieCatalogTest {
    @Test
    fun rouletteDrinkDessertAndSnackMenusHaveCalorieHints() {
        val rouletteMenus = listOf("DRINK", "DESSERT", "SNACK")
            .flatMap { categoryKey -> MenuCatalog.rouletteOptionsFor(categoryKey) }

        rouletteMenus.forEach { menu ->
            assertNotNull("$menu should have a calorie hint", MenuCalorieCatalog.estimateFor(menu))
        }
    }

    @Test
    fun everyRouletteMenuHasCategoryAndCalorieHint() {
        val rouletteMenus = listOf("MEAL", "DISH", "DRINK", "DESSERT", "SNACK")
            .flatMap { categoryKey -> MenuCatalog.rouletteOptionsFor(categoryKey) }

        rouletteMenus.forEach { menu ->
            assertNotNull("$menu should have a category", MenuCatalog.categoryKeyFor(menu))
            assertNotNull("$menu should have a calorie hint", MenuCalorieCatalog.estimateFor(menu))
        }
    }

    @Test
    fun drinkDessertAndSnackRoulettePoolsStayBalanced() {
        listOf("DRINK", "DESSERT", "SNACK").forEach { categoryKey ->
            val rouletteMenus = MenuCatalog.rouletteOptionsFor(categoryKey)
            assertEquals("$categoryKey roulette menus should be unique", rouletteMenus.size, rouletteMenus.distinct().size)
            assertEquals("$categoryKey should have at least 12 roulette menus", true, rouletteMenus.size >= 12)
        }
    }

    @Test
    fun calorieCatalogMatchesCommonMenuAliases() {
        assertEquals(160, MenuCalorieCatalog.estimateFor("복숭아 아이스티")?.kcal)
        assertEquals(550, MenuCalorieCatalog.estimateFor("매운 떡볶이")?.kcal)
        assertEquals(420, MenuCalorieCatalog.estimateFor("군만두")?.kcal)
        assertEquals(250, MenuCalorieCatalog.estimateFor("아이스 바닐라 라떼")?.kcal)
        assertEquals(260, MenuCalorieCatalog.estimateFor("녹차라떼")?.kcal)
    }

    @Test
    fun menuCatalogMatchesAliasCategories() {
        assertEquals("DISH", MenuCatalog.categoryKeyFor("돈카츠"))
        assertEquals("MEAL", MenuCatalog.categoryKeyFor("버섯 샤브"))
        assertEquals("DISH", MenuCatalog.categoryKeyFor("제육볶음"))
    }

    @Test
    fun nonFoodOptionsAreKnownButHaveNoCalories() {
        assertEquals(true, MenuCatalog.isKnownFoodOrException("패스"))
        assertEquals(null, MenuCalorieCatalog.estimateFor("패스"))
    }
}
