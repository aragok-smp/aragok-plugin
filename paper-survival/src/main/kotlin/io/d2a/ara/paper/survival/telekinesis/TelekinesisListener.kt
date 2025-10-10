package io.d2a.ara.paper.survival.telekinesis

import io.d2a.ara.paper.survival.Constants
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.block.Container
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockDropItemEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack

class TelekinesisListener : Listener {

    private fun shouldApply(player: Player, tool: ItemStack?): Boolean {
        return player.gameMode == GameMode.SURVIVAL
                && tool != null
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