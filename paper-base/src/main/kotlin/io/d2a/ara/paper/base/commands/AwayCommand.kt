package io.d2a.ara.paper.base.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.d2a.ara.paper.base.activity.ActivityService
import io.d2a.ara.paper.base.activity.PlayerMovementActivity
import io.d2a.ara.paper.base.extension.executesPlayer
import io.d2a.ara.paper.base.extension.requiresInternalPermission
import io.d2a.ara.paper.base.extension.success
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

class AwayCommand(
    private val activityService: PlayerMovementActivity
) : CommandBuilder {

    override fun description(): String = "Set your activity state to AWAY"

    override fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("away")
            .requiresInternalPermission("away")
            .executesPlayer { ctx, player ->
                activityService.lastPlayerActivity[player.uniqueId] = 0L
                activityService.setState(player, ActivityService.ActivityState.AWAY)
                ctx.success("Your activity state has been set to AWAY.")
            }
            .build()

}