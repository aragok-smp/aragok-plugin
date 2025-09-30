package io.d2a.ara.paper.base.activity

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.Plugin
import java.io.Closeable
import java.util.*

/**
 * An activity service which tracks player movement to determine their activity state.
 *
 * If a player has not moved for [awayThresholdMillis], they are considered AWAY.
 * Otherwise, they are considered ACTIVE.
 */
class PlayerMovementActivity(
    val plugin: Plugin,
    val awayThresholdMillis: Long = 5 * 60 * 1000, // 5 minutes
) : ActivityService, Listener, Closeable {

    // contains the last activity timestamp for each player
    val lastPlayerActivity = mutableMapOf<UUID, Long>()
    var lastPlayerState = mutableMapOf<UUID, ActivityService.ActivityState>()

    // contains the registered callbacks for state changes
    val activityChangedListeners = mutableListOf<ActivityService.ActivityChangedListener>()

    val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
        checkForAwayPlayers()
    }, 20L, 20L)

    /**
     * Cancels the scheduled task when the service is closed.
     */
    override fun close() {
        task.cancel()
    }

    fun isAway(lastActivity: Long): Boolean {
        return System.currentTimeMillis() - lastActivity > awayThresholdMillis
    }

    /**
     * Returns the player's activity state based on their last movement.
     * If the player has not moved for [awayThresholdMillis], they are considered AWAY.
     * Otherwise, they are considered ACTIVE.
     */
    override fun getActivityState(player: Player): ActivityService.ActivityState {
        val lastActivity = lastPlayerActivity[player.uniqueId] ?: return ActivityService.ActivityState.UNKNOWN
        return if (isAway(lastActivity)) {
            ActivityService.ActivityState.AWAY
        } else {
            ActivityService.ActivityState.ACTIVE
        }
    }

    override fun registerListener(listener: ActivityService.ActivityChangedListener) {
        activityChangedListeners.add(listener)
    }

    override fun unregisterListener(listener: ActivityService.ActivityChangedListener) {
        activityChangedListeners.remove(listener)
    }

    fun checkForAwayPlayers() {
        for (uuid in lastPlayerActivity.keys) {
            val player = plugin.server.getPlayer(uuid) ?: continue
            val newState = getActivityState(player)
            setState(player, newState)
        }
    }

    /**
     * Sends an update to all registered listeners about a player's activity state change.
     */
    fun setState(player: Player, newState: ActivityService.ActivityState) {
        val previousState = lastPlayerState[player.uniqueId] ?: ActivityService.ActivityState.UNKNOWN
        if (previousState == newState) {
            return
        }
        lastPlayerState[player.uniqueId] = newState
        activityChangedListeners.forEach { it.onActivityChanged(player, previousState, newState) }
    }

    fun setPlayerActive(player: Player) {
        lastPlayerActivity[player.uniqueId] = System.currentTimeMillis()
        setState(player, ActivityService.ActivityState.ACTIVE)
    }

    @EventHandler
    fun onPlayerChat(event: AsyncChatEvent) {
        setPlayerActive(event.player)
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (event.from.blockX == event.to.blockX &&
            event.from.blockY == event.to.blockY &&
            event.from.blockZ == event.to.blockZ
        ) {
            return
        }
        setPlayerActive(event.player)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        setPlayerActive(event.player)
    }

    // clean up
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        lastPlayerActivity.remove(event.player.uniqueId)
        setState(event.player, ActivityService.ActivityState.UNKNOWN)
    }

}