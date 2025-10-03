package io.d2a.ara.paper.survival.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.tree.LiteralCommandNode
import io.d2a.ara.paper.base.commands.CommandBuilder
import io.d2a.ara.paper.base.extension.executesPlayer
import io.d2a.ara.paper.base.extension.requiresInternalPermission
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit

class TrashCommand : CommandBuilder {

    override fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("trash")
            .requiresInternalPermission("trash")
            .executesPlayer { ctx, player ->
                player.openInventory(Bukkit.createInventory(null, 54, Component.text("Trash", NamedTextColor.RED)))
                Command.SINGLE_SUCCESS
            }
            .build()

}