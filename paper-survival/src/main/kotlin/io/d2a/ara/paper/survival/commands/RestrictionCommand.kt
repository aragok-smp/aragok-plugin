package io.d2a.ara.paper.survival.commands

import com.mojang.brigadier.tree.LiteralCommandNode
import io.d2a.ara.paper.base.commands.CommandBuilder
import io.d2a.ara.paper.base.extension.fail
import io.d2a.ara.paper.base.extension.requiresInternalPermission
import io.d2a.ara.paper.base.extension.success
import io.d2a.ara.paper.survival.border.BorderTask
import io.d2a.ara.paper.survival.restriction.DimensionRestriction
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands

class RestrictionCommand(
    val borderTask: BorderTask?,
    val dimensionRestriction: DimensionRestriction
) : CommandBuilder {

    override fun description() = "Manage border and dimension restrictions"

    override fun build(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("restriction")
            .requiresInternalPermission("restriction")
            .then(buildBorderCommand())
            .then(buildDimensionCommand())
            .build()

    fun buildBorderCommand(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("border")
            .requiresInternalPermission("border")
            .then(
                Commands.literal("check")
                    .requiresInternalPermission("border", "check")
                    .executes { ctx ->
                        borderTask?.run() ?: ctx.fail("Border task not found")
                        ctx.success("Checked border advancement")
                    }
                    .build())
            .then(
                Commands.literal("forceadvance")
                    .requiresInternalPermission("border", "forceadvance")
                    .executes { ctx ->
                        borderTask?.advance() ?: ctx.fail("Border task not found")
                        ctx.success("Advanced border")
                    }
                    .build()
            )
            .build()

    fun buildDimensionCommand(): LiteralCommandNode<CommandSourceStack> =
        Commands.literal("dimension")
            .requiresInternalPermission("dimension")
            .then(
                Commands.literal("nether")
                    .then(
                        Commands.literal("allow")
                            .requiresInternalPermission("dimension", "nether", "allow")
                            .executes { ctx ->
                                dimensionRestriction.netherEnabled.set(true)
                                ctx.success("Nether enabled")
                            }
                            .build()
                    )
                    .then(
                        Commands.literal("deny")
                            .requiresInternalPermission("dimension", "nether", "deny")
                            .executes { ctx ->
                                dimensionRestriction.netherEnabled.set(false)
                                ctx.success("Nether disabled")
                            }
                            .build()
                    )
                    .build()
            )
            .then(
                Commands.literal("end")
                    .then(
                        Commands.literal("allow")
                            .requiresInternalPermission("dimension", "end", "allow")
                            .executes { ctx ->
                                dimensionRestriction.endEnabled.set(true)
                                ctx.success("End enabled")
                            }
                            .build()
                    )
                    .then(
                        Commands.literal("deny")
                            .requiresInternalPermission("dimension", "end", "deny")
                            .executes { ctx ->
                                dimensionRestriction.endEnabled.set(false)
                                ctx.success("End disabled")
                            }
                            .build()
                    )
                    .build())
            .build()

}