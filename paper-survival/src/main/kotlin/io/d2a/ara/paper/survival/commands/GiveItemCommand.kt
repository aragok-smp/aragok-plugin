package io.d2a.ara.paper.survival.commands

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.tree.LiteralCommandNode
import io.d2a.ara.paper.base.commands.CommandBuilder
import io.d2a.ara.paper.base.extension.executesPlayer
import io.d2a.ara.paper.base.extension.fail
import io.d2a.ara.paper.base.extension.requiresInternalPermission
import io.d2a.ara.paper.base.extension.success
import io.d2a.ara.paper.survival.coal.CoalType
import io.d2a.ara.paper.survival.devnull.DevNullItem
import io.d2a.ara.paper.survival.enderchest.EnderStorageItem
import io.d2a.ara.paper.survival.floo.FlooItem
import io.d2a.ara.paper.survival.hopper.HopperFilterItem
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import org.bukkit.inventory.ItemStack
import java.util.logging.Logger

class GiveItemCommand(
    private val logger: Logger
) : CommandBuilder {

    override fun description() = "Gives an (aragok) item to a player"

    private fun give(name: String, stack: ItemStack) = Commands.literal(name)
        .executesPlayer { ctx, player ->
            logger.info("getitem: Giving 1x $name to ${player.name}")
            val remaining = player.inventory.addItem(stack)
            if (remaining.isNotEmpty()) {

                ctx.fail("Could not fit item in inventory, dropping on ground")
            } else {
                ctx.success("Gave $name")
            }
        }
        .then(
            Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                .executesPlayer { ctx, player ->
                    val amount = IntegerArgumentType.getInteger(ctx, "amount")
                    logger.info("getitem: Giving ${amount}x $name to ${player.name}")

                    val toGive = stack.clone()
                    toGive.amount = amount

                    val remaining = player.inventory.addItem(toGive)
                    if (remaining.isNotEmpty()) {
                        ctx.fail("Could not fit item in inventory, dropping on ground")
                    } else {
                        ctx.success("Gave $amount x $name")
                    }
                })
        .build()

    override fun build(): LiteralCommandNode<CommandSourceStack> = Commands.literal("getitem")
        .requiresInternalPermission("getitem")
        .then(
            Commands.literal("coal")
                .requiresInternalPermission("getitem", "coal")
                .then(give("enriched", CoalType.ENRICHED.toItem()))
                .then(give("infused", CoalType.INFUSED.toItem()))
                .then(give("supercharged", CoalType.SUPERCHARGED.toItem()))
                .build()
        )
        .then(give("devnull", DevNullItem.toItem()))
        .then(give("enderstorage", EnderStorageItem.toItem()))
        .then(
            Commands.literal("floo")
                .requiresInternalPermission("getitem", "floo")
                .then(give("essence", FlooItem.toEssenceItem()))
                .then(give("unused", FlooItem.toUnusedPowderItem()))
                .then(give("powder", FlooItem.toPowderItem()))
                .build()
        )
        .then(
            Commands.literal("hopper")
                .requiresInternalPermission("getitem", "hopper")
                .then(give("smart", HopperFilterItem.toSmartHopperItem()))
                .then(
                    Commands.literal("filter")
                        .requiresInternalPermission("getitem", "hopper", "filter")
                        .then(give("allow", HopperFilterItem.toItem(HopperFilterItem.HopperFilterType.ALLOW)))
                        .then(give("deny", HopperFilterItem.toItem(HopperFilterItem.HopperFilterType.DENY)))
                        .then(give("delete", HopperFilterItem.toItem(HopperFilterItem.HopperFilterType.DELETE)))
                        .build()
                )
                .build()
        )
        .build()

}