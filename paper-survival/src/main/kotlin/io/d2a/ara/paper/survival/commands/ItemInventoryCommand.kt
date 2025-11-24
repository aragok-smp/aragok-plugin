package io.d2a.ara.paper.survival.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.d2a.ara.paper.base.commands.CommandBuilder
import io.d2a.ara.paper.base.extension.*
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Chest
import org.bukkit.block.ShulkerBox
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.inventory.meta.BundleMeta
import java.util.*
import java.util.logging.Logger
import kotlin.math.abs

class ItemInventoryCommand(
    private val logger: Logger
) : CommandBuilder {

    private data class Area(var pos1: Location?, var pos2: Location?) {
        fun getAreaSize(): Int {
            if (pos1 == null || pos2 == null) {
                return 0
            }
            return (abs(pos1!!.blockX - pos2!!.blockX) + 1) *
                    (abs(pos1!!.blockY - pos2!!.blockY) + 1) *
                    (abs(pos1!!.blockZ - pos2!!.blockZ) + 1)
        }
    }

    private val playerLocations = mutableMapOf<UUID, Area>()

    override fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("iteminventory")
            .requiresInternalPermission("iteminventory")
            .then(
                Commands.literal("pos1")
                    .executesPlayer { context, player ->
                        // set pos1 to player's location
                        if (!playerLocations.containsKey(player.uniqueId)) {
                            playerLocations[player.uniqueId] = Area(player.location, null)
                            return@executesPlayer context.success("Set pos1 to ${player.location.toBlockString()}")
                        }

                        val area = playerLocations[player.uniqueId]!!
                        area.pos1 = player.location

                        // if the player did not yet set pos2, do not calculate area size
                        if (area.pos2 == null) {
                            return@executesPlayer context.success("Set pos1 to ${player.location.toBlockString()}")
                        }

                        return@executesPlayer context.success("Set pos1 to ${player.location.toBlockString()} (${area.getAreaSize()} block(s))")
                    }
                    .build())
            .then(
                Commands.literal("pos2")
                    .executesPlayer { context, player ->
                        // set pos2 to player's location
                        if (!playerLocations.containsKey(player.uniqueId)) {
                            playerLocations[player.uniqueId] = Area(null, player.location)
                            return@executesPlayer context.success("Set pos2 to ${player.location.toBlockString()}")
                        }

                        val area = playerLocations[player.uniqueId]!!
                        area.pos2 = player.location

                        // if the player did not yet set pos1, do not calculate area size
                        if (area.pos1 == null) {
                            return@executesPlayer context.success("Set pos2 to ${player.location.toBlockString()}")
                        }

                        return@executesPlayer context.success("Set pos2 to ${player.location.toBlockString()} (${area.getAreaSize()} block(s))")
                    }
                    .build())
            .then(
                Commands.literal("start")
                    .executesPlayer { context, player ->
                        val area = playerLocations[player.uniqueId]
                        if (area == null || area.pos1 == null || area.pos2 == null) {
                            return@executesPlayer context.fail("You must set both pos1 and pos2 before starting the item inventory.")
                        }
                        // make sure the world is the same
                        val pos1 = area.pos1!!
                        val pos2 = area.pos2!!
                        if (pos1.world.uid != pos2.world.uid) {
                            return@executesPlayer context.fail("pos1 and pos2 must be in the same world.")
                        }

                        // iterate over all blocks in the area
                        val world = pos1.world
                        val minX = minOf(pos1.blockX, pos2.blockX)
                        val maxX = maxOf(pos1.blockX, pos2.blockX)
                        val minY = minOf(pos1.blockY, pos2.blockY)
                        val maxY = maxOf(pos1.blockY, pos2.blockY)
                        val minZ = minOf(pos1.blockZ, pos2.blockZ)
                        val maxZ = maxOf(pos1.blockZ, pos2.blockZ)

                        val items = mutableMapOf<Material, Long>()
                        var containerCount = 0

                        for (x in minX..maxX) {
                            for (y in minY..maxY) {
                                for (z in minZ..maxZ) {
                                    val block = world.getBlockAt(x, y, z)

                                    val chest = block.state as? Chest ?: continue
                                    containerCount++

                                    // note: using blockInventory instead of inventory, because blockInventory
                                    // only includes items stored in the chest's "half"
                                    processInventory(chest.blockInventory, items)
                                }
                            }
                        }

                        // print items, sorted by count descending
                        logger.info("Item inventory scan results for ${player.name} in area [${pos1.toBlockString()}] to [${pos2.toBlockString()}]:")

                        items.entries
                            .sortedByDescending { it.value }
                            .forEach { entry ->
                                logger.info("- ${entry.key.name}: ${entry.value}")
                                player.sendMessage(
                                    Component.text("- ", NamedTextColor.DARK_GRAY)
                                        .append(Component.text(entry.key.name, NamedTextColor.GRAY))
                                        .append(Component.text(": ", NamedTextColor.DARK_GRAY))
                                        .append(Component.text(entry.value, NamedTextColor.YELLOW))
                                )
                            }

                        context.success("Completed the item scan of $containerCount container(s). Found ${items.size} unique item type(s).")
                    }
                    .build())
            .build()

    private fun processItem(item: ItemStack, result: MutableMap<Material, Long>) {
        result[item.type] = result.getOrDefault(item.type, 0) + item.amount

        if (!item.hasItemMeta()) return

        // if this is a shulker box, also get the items inside
        val meta = item.itemMeta
        if (meta is BlockStateMeta) {
            val blockState = meta.blockState
            if (blockState is ShulkerBox) {
                return processInventory(blockState.inventory, result)
            }
        }

        if (meta is BundleMeta) {
            for (bundleItem in meta.items) {
                processItem(bundleItem, result)
            }
            return
        }
    }

    private fun processInventory(inventory: Inventory, result: MutableMap<Material, Long>) {
        inventory.contents.filter { it != null }.forEach { processItem(it!!, result) }
    }

}