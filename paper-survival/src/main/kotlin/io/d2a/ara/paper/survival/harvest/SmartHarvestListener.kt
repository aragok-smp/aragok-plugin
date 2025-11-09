package io.d2a.ara.paper.survival.harvest

import io.d2a.ara.paper.survival.Constants
import io.d2a.ara.paper.survival.telekinesis.TelekinesisUtil
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.logging.Logger

class SmartHarvestListener(
    val logger: Logger,
) : Listener {

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val hand = event.hand ?: return
        val tool = player.inventory.getItem(hand)
        if (tool.type != Material.WOODEN_HOE &&
            tool.type != Material.COPPER_HOE &&
            tool.type != Material.STONE_HOE &&
            tool.type != Material.IRON_HOE &&
            tool.type != Material.GOLDEN_HOE &&
            tool.type != Material.DIAMOND_HOE &&
            tool.type != Material.NETHERITE_HOE
        ) return

        if (tool.enchantments.keys.none { it.key() == Constants.GREEN_THUMB }) return

        val block = event.clickedBlock ?: return
        val type = block.type

        // Check if block is a crop and get corresponding seed type
        val seed = getSeedTypeFromCrop(type) ?: return

        // Check if crop is fully grown
        val ageable = block.blockData as? Ageable ?: return
        if (ageable.age < ageable.maximumAge) return

        event.isCancelled = true // Prevent default interaction

        val drops = block.getDrops(tool, player)
        val dropLocation = block.location.clone().add(.5, .1, .5)

        // "break" the block
        block.type = Material.AIR // temporarily set to air to mimic breaking

        if (TelekinesisUtil.getTelekinesisLevel(player, tool) != null) {
            for (drop in drops) {
                val remainder = TelekinesisUtil.pickupStack(player, drop, dropLocation)
                if (remainder != null) {
                    dropLocation.world.dropItemNaturally(dropLocation, remainder)
                }
            }
        } else {
            for (drop in drops) {
                block.world.dropItemNaturally(dropLocation, drop)
            }
        }

        // damage the hoe
        player.damageItemStack(hand, 1)

        // try to remove the seed from inventory
        val seedRemoveResult = player.inventory.removeItem(ItemStack.of(seed))
        if (seedRemoveResult.isNotEmpty()) return // we couldn't remove a seed => they probably don't have it

        val newAgeable = ageable.clone() as? Ageable ?: return
        newAgeable.age = 0

        // Replant the crop
        block.type = type
        block.blockData = newAgeable

        // play effect
        block.world.spawnParticle(Particle.COMPOSTER, dropLocation, 4, 0.05, 0.05, 0.05, 0.0)
        block.world.spawnParticle(Particle.END_ROD, dropLocation, 1, 0.0, 0.02, 0.0, 0.01)
    }


    fun getSeedTypeFromCrop(crop: Material): Material? {
        return when (crop) {
            Material.WHEAT -> Material.WHEAT_SEEDS
            Material.CARROTS -> Material.CARROT
            Material.POTATOES -> Material.POTATO
            Material.BEETROOTS -> Material.BEETROOT_SEEDS
            Material.NETHER_WART -> Material.NETHER_WART
            else -> null
        }
    }

}
