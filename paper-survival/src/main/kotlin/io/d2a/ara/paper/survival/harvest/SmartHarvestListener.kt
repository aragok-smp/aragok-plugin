package io.d2a.ara.paper.survival.harvest

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Ageable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
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
        val hand = event.hand ?: return
        if (hand != EquipmentSlot.OFF_HAND) { // only on off hand
            return
        }
        logger.info { "OFF Hand was used" }

        val block = event.clickedBlock ?: return
        val type = block.type
        val data = block.blockData
        logger.info("Clicked Block ${block.type}")
        if (block.type !in harvestableBlocks) {
            return
        }

        // Check if block is fully grown
        if (!isHarvestable(block)) {
            return
        }
        logger.info("Block is ready to harvest")

        event.isCancelled = true // Prevent default interaction
        block.breakNaturally()

        // Check if player has same Material in inventory
        val seed = getSeedTypeFromCrop(type) ?: return
        val hasItemInInventory = player.inventory.contains(seed)
        logger.info { "Has item in inventory? => $hasItemInInventory" }
        if (!hasItemInInventory) {
            return
        }

        // Remove one item from inventory
        player.inventory.removeItem(ItemStack(seed, 1))

        // Replant the crop
        block.type = type
        if (data is Ageable) {
            data.age = 0
        }
        block.blockData = data
    }

    fun getSeedTypeFromCrop(crop: Material): Material? {
        return when (crop) {
            Material.WHEAT -> Material.WHEAT_SEEDS
            Material.CARROT -> Material.CARROT
            Material.POTATO -> Material.POTATO
            Material.BEETROOTS -> Material.BEETROOT_SEEDS
            Material.NETHER_WART -> Material.NETHER_WART
            else -> null
        }
    }

    fun isHarvestable(block: Block): Boolean {
        val data = block.blockData
        return when {
            data is Ageable && data.age == data.maximumAge -> true
            else -> false
        }
    }
}
