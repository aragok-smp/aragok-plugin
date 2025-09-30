package io.d2a.ara.paper.base.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.tree.LiteralCommandNode
import io.d2a.ara.paper.base.flair.playerAdapter
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import net.luckperms.api.LuckPerms
import net.luckperms.api.model.user.User
import net.luckperms.api.node.NodeType
import net.luckperms.api.node.metadata.NodeMetadataKey
import net.luckperms.api.node.types.InheritanceNode
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.TimeUnit

class PrivilegesCommand(
    private val luckPerms: LuckPerms
) {

    data class PrivilegeInfo(val approver: UUID?)

    companion object {
        const val INFO_KEY = "aragokt:info"

        const val USE_PERMISSION = "aragokt.privileges.use"
        const val USE_OTHER_PERMISSION = "aragokt.privileges.use.other"

        const val SUPER_USER_GROUP_NAME = "su"
        private const val MAX_PRIVILEGE_DURATION_MINUTES = 15
    }

    private fun execute(sender: CommandSender, target: Player, durationMinutes: Int) {
        val user = luckPerms.playerAdapter().getUser(target)
        val existingNode = findExistingSuperUserGroupNode(user)

        if (existingNode != null) {
            revokePrivileges(sender, target, user, existingNode)
        } else {
            grantPrivileges(sender, target, user, durationMinutes)
        }
    }

    /**
     * Revokes existing super-user privileges from the target player.
     */
    private fun revokePrivileges(
        sender: CommandSender,
        target: Player,
        user: User,
        node: InheritanceNode
    ) {
        val senderUuid = uuidOrNull(sender)

        if (sender !is ConsoleCommandSender && sender !== target) {
            val info = node.getMetadata(NodeMetadataKey.of(INFO_KEY, PrivilegeInfo::class.java))

            if (info.isEmpty || info.get().approver != senderUuid) {
                sender.sendRichMessage("<red>error: <white>${target.name} <red>is already a super-user! You cannot override their privileges.")
                return
            }
        }

        user.data().remove(node)
        luckPerms.userManager.saveUser(user)

        if (sender !== target) {
            sender.sendRichMessage("<green>Removed existing super-user privileges from <white>${target.name}<green>.")
            target.sendRichMessage("<red>Your existing super-user privileges have been removed by <white>${sender.name}<red>.")
        } else {
            sender.sendRichMessage("<green>Your existing super-user privileges have been removed.")
        }
    }


    /**
     * Grants temporary super-user privileges to the target player.
     */
    private fun grantPrivileges(
        sender: CommandSender,
        target: Player,
        user: User,
        durationMinutes: Int
    ) {
        val group = luckPerms.groupManager.getGroup(SUPER_USER_GROUP_NAME) ?: run {
            sender.sendRichMessage("<red>error: Privileged group '$SUPER_USER_GROUP_NAME' does not exist.")
            return
        }

        val node = InheritanceNode.builder(group)
            .expiry(durationMinutes.toLong(), TimeUnit.MINUTES)
            // store the approver info so we know if a player can remove the privileges later
            .withMetadata(
                NodeMetadataKey.of(INFO_KEY, PrivilegeInfo::class.java),
                PrivilegeInfo(uuidOrNull(sender))
            )
            .build()

        user.data().add(node)
        luckPerms.userManager.saveUser(user)

        if (sender !== target) {
            sender.sendRichMessage(
                "<green>Granted <white>${target.name} <green>temporary admin privileges for <white>$durationMinutes minutes<green>."
            )
            target.sendRichMessage(
                "<green>You have been granted temporary admin privileges for <white>$durationMinutes minutes<green> by <white>${sender.name}<green>!"
            )
        } else {
            sender.sendRichMessage(
                "<green>You have been granted temporary admin privileges for <white>$durationMinutes minutes<green>!"
            )
        }
    }

    fun build(): LiteralCommandNode<CommandSourceStack> {
        val root: LiteralArgumentBuilder<CommandSourceStack> = Commands.literal("privileges")
            .requires(Commands.restricted { source ->
                source.sender.hasPermission(USE_PERMISSION)
            })

        root.executes { ctx ->
            val sender = ctx.source.sender
            val player = sender as? Player ?: run {
                sender.sendRichMessage("<red>error: <white>Console must specify a target player.")
                return@executes Command.SINGLE_SUCCESS
            }
            execute(player, player, MAX_PRIVILEGE_DURATION_MINUTES)
            Command.SINGLE_SUCCESS
        }

        root.then(
            Commands.argument("player", ArgumentTypes.player())
                .requires(Commands.restricted { source ->
                    source.sender.hasPermission(USE_OTHER_PERMISSION)
                })
                .executes { ctx ->
                    val target = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                        .resolve(ctx.source)
                        .first()
                    execute(ctx.source.sender, target, MAX_PRIVILEGE_DURATION_MINUTES)
                    Command.SINGLE_SUCCESS
                }
                .then(
                    Commands.argument(
                        "duration",
                        IntegerArgumentType.integer(1, MAX_PRIVILEGE_DURATION_MINUTES)
                    )
                        .executes { ctx ->
                            val target = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
                                .resolve(ctx.source)
                                .first()
                            val duration = IntegerArgumentType.getInteger(ctx, "duration")
                            execute(ctx.source.sender, target, duration)
                            Command.SINGLE_SUCCESS
                        }
                )
        )

        return root.build()
    }

    private fun uuidOrNull(sender: CommandSender?): UUID? =
        (sender as? Player)?.uniqueId

    private fun findExistingSuperUserGroupNode(user: User): InheritanceNode? =
        user.getNodes(NodeType.INHERITANCE)
            .firstOrNull { it.value && it.groupName == SUPER_USER_GROUP_NAME }

}