package io.d2a.ara.paper.base.maintenance

import io.d2a.ara.paper.base.extension.broadcastRichMessage
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import org.bukkit.scheduler.BukkitTask
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

class ServerStopListener(
    private val plugin: Plugin,
    private val adminPermission: String,
) : Listener {

    companion object {
        private const val DEBOUNCE_SECONDS = 20L
    }

    @OptIn(ExperimentalAtomicApi::class)
    private var stopFlag = AtomicBoolean(true)

    private var pendingShutdownTask: BukkitTask? = null

    @OptIn(ExperimentalAtomicApi::class)
    var stopServer: Boolean
        get() = stopFlag.load()
        set(value) {
            if (!stopFlag.compareAndSet(value, value)) {
                evaluateAndMaybeSchedule()
            }
        }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        if (!event.player.hasPermission(adminPermission)) {
            cancelPendingShutdown()
        }
        if (!stopServer) {
            cancelPendingShutdown()
        }
    }

    @EventHandler
    fun onPlayerQuit(@Suppress("unused") event: PlayerQuitEvent) {
        evaluateAndMaybeSchedule()
    }

    private fun evaluateAndMaybeSchedule() {
        if (!stopServer) return cancelPendingShutdown()

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
                val stillNoNonAdmin = plugin.server.onlinePlayers
                    .none { !it.hasPermission(adminPermission) }
                val stillShouldStop = stopServer

                if (stillShouldStop && stillNoNonAdmin) {
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