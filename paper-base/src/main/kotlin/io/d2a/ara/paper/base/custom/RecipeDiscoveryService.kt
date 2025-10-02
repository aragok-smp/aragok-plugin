package io.d2a.ara.paper.base.custom

import org.bukkit.NamespacedKey

interface RecipeDiscoveryService {

    fun addRecipe(recipe: NamespacedKey)

}