package com.babcross.app.data

data class MenuCalorieHint(
    val kcal: Int,
    val matchedName: String
)

object MenuCalorieCatalog {
    fun estimateFor(menuName: String): MenuCalorieHint? {
        return MenuCatalog.estimateCalories(menuName)
    }
}
