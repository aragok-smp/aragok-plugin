package io.d2a.ara.paper.base.activity

import org.bukkit.entity.Player

interface ActivityService {

    enum class ActivityState {
        UNKNOWN,
        AWAY,
        ACTIVE;
    }

    /**
     * A callback interface for listening to player activity state changes.
     */
    fun interface ActivityChangedListener {
        fun onActivityChanged(player: Player, oldState: ActivityState, newState: ActivityState)
    }

    /**
     * Returns the players' last activity timestamp in unix epoch milliseconds.
     */
    fun getActivityState(player: Player): ActivityState

    /**
     * Registers a callback to be invoked whenever a player's activity state changes.
     *
     * @param listener The function to be called when a player's activity state changes.
     */
    fun registerListener(listener: ActivityChangedListener)

    /**
     * Unregisters a previously registered callback.
     *
     * @param listener The function to be removed from the list of callbacks.
     */
    fun unregisterListener(listener: ActivityChangedListener)

}