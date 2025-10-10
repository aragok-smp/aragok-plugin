package io.d2a.ara.paper.survival.telekinesis

import io.d2a.ara.paper.survival.Constants
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack

class TelekinesisListener : Listener {

    // axes, pickaxes, shovel, hoes, shear
    private val toolMaterials = setOf<Material>(
        Material.WOODEN_AXE, Material.WOODEN_PICKAXE, Material.WOODEN_SHOVEL, Material.WOODEN_HOE,
        Material.STONE_AXE, Material.STONE_PICKAXE, Material.STONE_SHOVEL, Material.STONE_HOE,
        Material.COPPER_AXE, Material.COPPER_PICKAXE, Material.COPPER_SHOVEL, Material.COPPER_HOE,
        Material.IRON_AXE, Material.IRON_PICKAXE, Material.IRON_SHOVEL, Material.IRON_HOE,
        Material.GOLDEN_AXE, Material.GOLDEN_PICKAXE, Material.GOLDEN_SHOVEL, Material.GOLDEN_HOE,
        Material.DIAMOND_AXE, Material.DIAMOND_PICKAXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE,
        Material.NETHERITE_AXE, Material.NETHERITE_PICKAXE, Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE,
        Material.SHEARS,
    )

    private fun shouldApply(player: Player, tool: ItemStack?): Boolean {
        return player.gameMode == GameMode.SURVIVAL
                && tool != null
                && tool.type in toolMaterials
                && tool.enchantments.keys.any { it.key() == Constants.TELEKINESIS }
    }

    @EventHandler
    fun onBlockDropItem(event: BlockDropItemEvent) {
        val player = event.player
        val tool = player.inventory.itemInMainHand
        if (!shouldApply(player, tool)) return

        if (event.blockState is Container) return

        val toProcess = event.items.toList()
        for (itemEntity in toProcess) {
            val stack = itemEntity.itemStack
            if (stack.isEmpty) continue

            @Suppress("UnstableApiUsage") val attempt = EntityPickupItemEvent(
                player,
                itemEntity,
                stack.amount
            )
            Bukkit.getPluginManager().callEvent(attempt)
            if (attempt.isCancelled) continue

            val leftover = player.inventory.addItem(stack)
            if (leftover.isEmpty()) {
                player.playPickupItemAnimation(itemEntity, stack.amount)

                itemEntity.remove()
                event.items.remove(itemEntity)
            } else {
                val remainder = leftover.values.first()
                if (remainder.amount < stack.amount) {
                    // some was picked up
                    val pickedUp = stack.amount - remainder.amount
                    player.playPickupItemAnimation(itemEntity, pickedUp)
                }
                itemEntity.itemStack = remainder
            }
        }
    }

}