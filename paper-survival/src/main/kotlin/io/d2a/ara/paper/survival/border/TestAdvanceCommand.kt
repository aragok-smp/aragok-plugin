package io.d2a.ara.paper.survival.border

import com.mojang.brigadier.tree.LiteralCommandNode
import io.d2a.ara.paper.base.extension.requiresPermission
import io.d2a.ara.paper.base.extension.success
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

class TestAdvanceCommand(
    val borderTask: BorderTask?
) {

    fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("testadvance")
            .requiresPermission("aragokt.testadvance")
            .then(
                Commands.literal("check")
                    .requiresPermission("aragokt.testadvance.check")
                    .executes { ctx ->
                        borderTask?.run()
                        ctx.success("Checked border advancement")
                    }
                    .build())
            .then(
                Commands.literal("advance")
                    .requiresPermission("aragokt.testadvance.advance")
                    .executes { ctx ->
                        borderTask?.advance()
                        ctx.success("Advanced border")
                    }
                    .build())
            .build()

}