package io.d2a.ara.paper.survival

import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.inventory.CraftItemEvent
import java.util.logging.Logger

/**
 * An EntityPickupItemEvent Listener that can unlock some recipes when an entity
 * picks up one of [materials].
 */
class PickUpCraftUnlockRecipeListener(
    private val logger: Logger,
    private val materials: Set<Material>,
    private val recipes: Set<NamespacedKey>
) : Listener {

    @EventHandler
    fun onPickup(event: EntityPickupItemEvent) {
        if (event.entity.type != EntityType.PLAYER) return

        val type = event.item.itemStack.type
        if (materials.contains(type)) {
            val player = event.entity as? Player ?: return
            for (key in recipes) {
                if (player.discoverRecipe(key)) {
                    logger.info("Player ${player.name} discovered recipe $key by picking up $type")
                }
            }
        }
    }


    @EventHandler(
        priority = EventPriority.HIGH,
        ignoreCancelled = false
    )
    fun onCraftBag(event: CraftItemEvent) {
        if (event.result == Event.Result.DENY) return

        val type = event.recipe.result.type
        if (materials.contains(type)) {
            val player = event.whoClicked as? Player ?: return

            for (key in recipes) {
                if (player.discoverRecipe(key)) {
                    logger.info("Player ${player.name} discovered recipe $key by crafting $type")
                }
            }
        }
    }

}