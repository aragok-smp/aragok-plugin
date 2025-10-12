package io.d2a.ara.paper.base.maintenance

import io.d2a.ara.paper.base.extension.broadcastRichMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask

class ServerStopListener(
    private val plugin: Plugin,
    private val adminPermission: String,
) : Listener {

    companion object {
        private const val DEBOUNCE_SECONDS = 20L
    }

    var stopFlag = false
        set(value) {
            if (field == value) return

            field = value
            if (value) {
                evaluateAndMaybeSchedule()

                plugin.server.broadcastRichMessage("<green>aragok: <gray>Automatic server stop enabled")
            } else {
                cancelPendingShutdown()

                plugin.server.broadcastRichMessage("<green>aragok: <gray>Automatic server stop disabled")
            }
        }

    private var pendingShutdownTask: BukkitTask? = null

    @EventHandler
    fun onPlayerJoin(@Suppress("unused") event: PlayerJoinEvent) {
        cancelPendingShutdown()
    }

    @EventHandler
    fun onPlayerQuit(@Suppress("unused") event: PlayerQuitEvent) {
        evaluateAndMaybeSchedule()
    }

    private fun evaluateAndMaybeSchedule() {
        if (!stopFlag) return cancelPendingShutdown()

        val nonAdminsOnline = plugin.server.onlinePlayers
            .any { !it.hasPermission(adminPermission) }
        if (nonAdminsOnline) {
            return cancelPendingShutdown()
        }

        if (pendingShutdownTask == null) {
            plugin.server.broadcastRichMessage(
                "<green>aragok: <gray>No non-admin players online, " +
                        "stopping server in $DEBOUNCE_SECONDS seconds if no one joins"
            )
            pendingShutdownTask = plugin.server.scheduler.runTaskLater(plugin, Runnable {
                val stillShouldStop = stopFlag
                val noPlayerOnline = plugin.server.onlinePlayers.isEmpty()

                if (stillShouldStop && noPlayerOnline) {
                    plugin.server.broadcastRichMessage("<green>aragok: <gray>No non-admin players online, stopping server now")
                    plugin.server.shutdown()
                } else {
                    plugin.server.broadcastRichMessage("<green>aragok: <gray>Server stop cancelled, a player joined or stop was disabled")
                }
                pendingShutdownTask = null
            }, DEBOUNCE_SECONDS * 20)
        }
    }

    fun cancelPendingShutdown() {
        pendingShutdownTask?.cancel()
        pendingShutdownTask = null
    }

}