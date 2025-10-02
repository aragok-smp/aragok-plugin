package io.d2a.ara.paper.survival.coal

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent

class PickUpCoalEvent(
    vararg val coalRecipeKeys: NamespacedKey
) : Listener {

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        if (event.entity.type != EntityType.PLAYER) return
        val type = event.item.itemStack.type
        if (type == Material.COAL || type == Material.CHARCOAL) {
            val player = event.entity as Player
            for (key in coalRecipeKeys) {
                player.discoverRecipe(key)
            }
        }
    }

}