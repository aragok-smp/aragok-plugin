package io.d2a.ara.paper.base.flair

import io.d2a.ara.paper.base.commands.PrivilegesCommand
import net.luckperms.api.LuckPerms
import net.luckperms.api.event.EventSubscription
import net.luckperms.api.event.group.GroupDataRecalculateEvent
import net.luckperms.api.event.user.UserDataRecalculateEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import java.io.Closeable
import java.util.*
import java.util.logging.Logger

class LuckPermsLiveUpdateExtension(
    private val plugin: Plugin,
    private val logger: Logger,
    private val service: NametagService,
    private val luckPerms: LuckPerms,
    private val debounce: Long = 3L
) : Closeable {

    private var userSubscription: EventSubscription<UserDataRecalculateEvent>? = null
    private var groupSubscription: EventSubscription<GroupDataRecalculateEvent>? = null

    private val userDebounce = mutableMapOf<UUID, BukkitTask>()
    private var groupDebounce: BukkitTask? = null

    override fun close() {
        userSubscription?.close()
        groupSubscription?.close()
    }

    fun subscribeFlairUserLiveUpdates() {
        userSubscription?.close()
        userSubscription = luckPerms.eventBus.subscribe(plugin, UserDataRecalculateEvent::class.java) { event ->
            val player = plugin.server.getPlayer(event.user.uniqueId) ?: return@subscribe
            if (!player.isOnline) return@subscribe

            userDebounce[player.uniqueId]?.cancel()
            userDebounce[player.uniqueId] = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                userDebounce.remove(player.uniqueId)

                logger.info("Detected changes for user ${event.user.username} (${event.user.uniqueId}), updating nametag")
                service.applyTo(player)
            }, debounce)
        }
    }

    fun subscribeFlairGroupLiveUpdates() {
        groupSubscription?.close()
        groupSubscription = luckPerms.eventBus.subscribe(plugin, GroupDataRecalculateEvent::class.java) { event ->
            // we only care about the super-user group changes
            if (event.group.name != PrivilegesCommand.SUPER_USER_GROUP_NAME) {
                return@subscribe
            }

            groupDebounce?.cancel()
            groupDebounce = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                groupDebounce = null

                logger.info("Detected changes in group ${event.group.name}, updating all online players")
                plugin.server.onlinePlayers.forEach(service::applyTo)
            }, debounce)
        }
    }
}