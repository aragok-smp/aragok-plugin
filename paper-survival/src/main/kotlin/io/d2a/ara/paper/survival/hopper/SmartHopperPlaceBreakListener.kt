package io.d2a.ara.paper.survival.hopper

import io.d2a.ara.paper.base.extension.italic
import io.d2a.ara.paper.base.extension.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.Hopper
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import java.util.logging.Logger

/**
 * The purpose of this listener is to handle the placement and breaking of smart hoppers.
 * When a smart hopper is placed, it is initialized with special properties and effects.
 * When a smart hopper is broken, it drops as a normal hopper but with some visual feedback.
 */
class SmartHopperPlaceBreakListener(
    val logger: Logger
) : Listener {

    @EventHandler
    fun onHopperPlace(event: BlockPlaceEvent) {
        if (event.itemInHand.type != Material.HOPPER) return
        if (!HopperFilterItem.isSmartHopperItem(event.itemInHand)) return

        val state = event.block.state as? Hopper ?: return
        HopperFilterItem.setSmartHopper(state)

        event.block.world.spawnParticle(
            Particle.HEART,
            event.block.location.toCenterLocation(),
            10, 0.5, 0.5, 0.5, 0.1
        )

        // say hello to the smart hop(p)er!
        event.block.world.playSound(event.block.location, Sound.ENTITY_VILLAGER_CELEBRATE, 0.4f, 1.0f)
        logger.info("${event.player.name} placed a smart hopper at ${event.block.location}")
    }

    @EventHandler(
        priority = EventPriority.HIGH,
        ignoreCancelled = false
    )
    fun onHopperBreak(event: BlockBreakEvent) {
        if (event.block.type != Material.HOPPER) return

        val state = event.block.state as? Hopper ?: return
        if (!HopperFilterItem.isSmartHopperBlock(state)) return

        event.block.world.spawnParticle(
            Particle.SMOKE,
            event.block.location.toCenterLocation(),
            20, 0.5, 0.5, 0.5, 0.1
        )

        // the smart hopper drops as normal hopper
        // but at least you get some XP /shrug
        event.expToDrop = 15

        (event.block.state as? Hopper)?.customName(
            "Former Smart Hopper".text()
                .color(NamedTextColor.LIGHT_PURPLE)
                .italic(false)
        )

        logger.info("${event.player.name} broke a smart hopper at ${event.block.location}")
    }

}