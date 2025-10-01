package io.d2a.ara.paper.base.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.papermc.paper.command.brigadier.CommandSourceStack

interface CommandBuilder {

    fun build(): LiteralCommandNode<CommandSourceStack>

    fun description(): String = "No description provided"

}