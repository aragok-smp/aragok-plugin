package io.d2a.ara.paper.survival.hopper

import io.d2a.ara.paper.base.extension.VILLAGER_NO_SOUND
import io.d2a.ara.paper.base.extension.failActionBar
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDispenseEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.event.inventory.PrepareSmithingEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import java.util.logging.Logger

/**
 * The purpose of this listener is to make the hopper filters,
 * which are shulker boxes under the hood (-> expensive) as useless as possible.
 */
class MakeHopperFilterUselessListener(
    val logger: Logger
) : Listener {

    // you can place shulkers with a dispenser...
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onFilterDispense(event: BlockDispenseEvent) {
        if (HopperFilterItem.isHopperFilter(event.item)) {
            event.isCancelled = true

            // small punishment for trying to use it in a dispenser
            event.block.breakNaturally()

            logger.warning("A block tried to dispense a hopper filter at ${event.block.location}")
        }
    }

    // you can also place those babies...
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onBlockPlace(event: BlockPlaceEvent) {
        if (HopperFilterItem.isHopperFilter(event.itemInHand)) {
            event.isCancelled = true
            event.player.failActionBar("You cannot place a hopper filter block.", sound = VILLAGER_NO_SOUND)

            logger.warning("${event.player.name} tried to place a hopper filter at ${event.block.location}")
        }
    }

    // or dye them...
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onPrepareItemCraft(event: PrepareItemCraftEvent) {
        if (event.inventory.matrix.any { it != null && HopperFilterItem.isHopperFilter(it) }) {
            logger.warning("A player tried to craft with a hopper filter in the crafting grid.")

            event.inventory.result = null
        }
    }

    // or rename them...
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onPrepareAnvil(event: PrepareAnvilEvent) {
        if (event.inventory.contents.any { it != null && HopperFilterItem.isHopperFilter(it) }) {
            logger.warning("A player tried to rename a hopper filter in an anvil.")
            event.result = null
        }
    }

    // or whatever you can do with smithing tables
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onPrepareSmith(event: PrepareSmithingEvent) {
        if (event.inventory.contents.any { it != null && HopperFilterItem.isHopperFilter(it) }) {
            logger.warning("A player tried to smith a hopper filter in a smithing table.")
            event.result = null
        }
    }

    // or place them on entities (like item frames, armor stands)...
    @EventHandler
    fun onEntityInteract(event: PlayerInteractEntityEvent) {
        val item = event.player.inventory.getItem(event.hand)
        if (HopperFilterItem.isHopperFilter(item)) {
            event.isCancelled = true
            event.player.failActionBar("You cannot use a hopper filter on entities.", sound = VILLAGER_NO_SOUND)

            logger.warning("${event.player.name} tried to use a hopper filter on an entity (${event.rightClicked.type}) at ${event.rightClicked.location}")
        }
    }

}