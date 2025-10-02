package io.d2a.ara.paper.base.custom

import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.logging.Logger

class PlayerJoinRecipeDiscoveryService(
    val logger: Logger
) : RecipeDiscoveryService, Listener {

    val discoveredRecipes = mutableSetOf<NamespacedKey>()

    override fun addRecipe(recipe: NamespacedKey) {
        logger.info("Added recipe ${recipe.key} to be discovered on join")
        this.discoveredRecipes.add(recipe)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        for (recipe in this.discoveredRecipes) {
            if (!player.hasDiscoveredRecipe(recipe)) {
                player.discoverRecipe(recipe)
                logger.info("Discovered recipe ${recipe.key} for player ${player.name}")
            }
        }
    }

}