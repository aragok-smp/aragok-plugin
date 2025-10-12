package io.d2a.ara.paper.base.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.d2a.ara.paper.base.extension.requiresInternalPermission
import io.d2a.ara.paper.base.extension.success
import io.d2a.ara.paper.base.maintenance.ServerStopListener
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

class MaintenanceCommand(
    val serverStopListener: ServerStopListener
) : CommandBuilder {

    override fun build(): LiteralCommandNode<CommandSourceStack> = Commands.literal("maintenance")
        .requiresInternalPermission("maintenance")
        .then(
            Commands.literal("server")
                .requiresInternalPermission("maintenance", "server")
                .then(
                    Commands.literal("stop")
                        .requiresInternalPermission("maintenance", "server", "stop")
                        .executes { ctx ->
                            serverStopListener.stopFlag = !serverStopListener.stopFlag
                            if (serverStopListener.stopFlag) {
                                ctx.success("Server will stop when no players are online")
                            } else {
                                ctx.success("Server will NOT stop when no players are online")
                            }
                        }
                        .build()
                )
                .then(
                    Commands.literal("status")
                        .requiresInternalPermission("maintenance", "server", "status")
                        .executes { ctx ->
                            if (serverStopListener.stopFlag) {
                                ctx.success("<gray>Server <red>WILL stop</red> when no players are online")
                            } else {
                                ctx.success("<gray>Server <yellow>will NOT</yellow> stop when no players are online")
                            }
                        }
                        .build())
                .build()
        )

        .build()

}