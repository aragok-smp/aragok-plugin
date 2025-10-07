package io.d2a.ara.paper.survival.harvest

import io.d2a.ara.paper.survival.Constants
import org.bukkit.Material
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

    private val harvestableBlocks = setOf(
        Material.WHEAT,
        Material.CARROT,
        Material.POTATO,
        Material.BEETROOTS,
        Material.NETHER_WART,
    )

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
        if (type !in harvestableBlocks) return

        // Check if block is fully grown
        val ageable = block.blockData as? Ageable ?: return
        if (ageable.age < ageable.maximumAge) return

        event.isCancelled = true // Prevent default interaction

        // break the seed
        block.breakNaturally(tool)

        // break the hoe
        player.damageItemStack(hand, 1)

        // Check if player has same Material in inventory
        val seed = getSeedTypeFromCrop(type) ?: return

        // try to remove the seed from inventory
        val seedRemoveResult = player.inventory.removeItem(ItemStack.of(seed))
        if (seedRemoveResult.isNotEmpty()) return // we couldn't remove a seed => they probably don't have it

        val newAgeable = ageable.clone() as? Ageable ?: return
        newAgeable.age = 0

        // Replant the crop
        block.type = type
        block.blockData = newAgeable
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
