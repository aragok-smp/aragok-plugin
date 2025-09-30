package io.d2a.ara.paper.base.extension

import com.mojang.brigadier.Command
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player

/**
 * Sends a failure message to the command sender, prefixed with red color.
 */
fun CommandContext<CommandSourceStack>.fail(message: String): Int {
    this.source.sender.sendRichMessage("<red>$message")
    return Command.SINGLE_SUCCESS
}

/**
 * Sends a success message to the command sender, prefixed with green color.
 */
fun CommandContext<CommandSourceStack>.success(message: String): Int {
    this.source.sender.sendRichMessage("<green>$message")
    return Command.SINGLE_SUCCESS
}

/**
 * Executes the command only if the sender is a player.
 * If the sender is not a player, sends the specified failure message.
 */
inline fun CommandContext<CommandSourceStack>.withPlayer(
    notPlayerMessage: String = "This command can only be run by a player",
    block: (Player) -> Int
): Int {
    val sender = this.source.sender
    return if (sender is Player) {
        block(sender)
    } else {
        fail(notPlayerMessage)
    }
}

/**
 * Adds an execution block that only runs if the command sender is a player.
 * If the sender is not a player, sends the specified failure message.
 */
fun <T : ArgumentBuilder<CommandSourceStack, T>> T.executesPlayer(
    block: (CommandContext<CommandSourceStack>, Player) -> Int
): T = this.executes { ctx ->
    ctx.withPlayer { player ->
        block(ctx, player)
    }
}

fun <T : ArgumentBuilder<CommandSourceStack, T>> T.requiresPermission(permission: String): T = this.requires { ctx ->
    ctx.sender.hasPermission(permission)
}