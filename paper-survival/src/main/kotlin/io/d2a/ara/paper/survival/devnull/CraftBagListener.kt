package io.d2a.ara.paper.survival.devnull

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.CraftItemEvent

class CraftBagListener(
    val devNullRecipeKey: NamespacedKey
) : Listener {

    @EventHandler(
        priority = EventPriority.HIGH,
        ignoreCancelled = false
    )
    fun onCraftBag(event: CraftItemEvent) {
        if (event.result == Event.Result.DENY) {
            return
        }
        val type = event.recipe.result.type
        if (type == Material.BUNDLE || type.name.endsWith("_BUNDLE")) {
            event.whoClicked.discoverRecipe(devNullRecipeKey)
        }
    }

}