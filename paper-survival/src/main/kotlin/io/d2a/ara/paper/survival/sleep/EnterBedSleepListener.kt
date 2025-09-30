package io.d2a.ara.paper.survival.sleep

import io.d2a.ara.paper.base.activity.ActivityService
import io.d2a.ara.paper.base.extension.toAdventure
import io.papermc.paper.event.player.PlayerDeepSleepEvent
import net.kyori.adventure.sound.Sound.Emitter
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class EnterBedSleepListener : Listener, ActivityService.ActivityChangedListener {
    override fun onActivityChanged(
        player: Player,
        oldState: ActivityService.ActivityState,
        newState: ActivityService.ActivityState
    ) {
        when (newState) {
            ActivityService.ActivityState.ACTIVE -> player.isSleepingIgnored = false
            else -> player.isSleepingIgnored = true
        }
    }

    @EventHandler
    fun onDeepSleep(event: PlayerDeepSleepEvent) {
        event.player.world.playSound(
            Sound.BLOCK_AMETHYST_BLOCK_RESONATE.toAdventure()
                .volume(0.5f)
                .pitch(0.1f)
                .build(), Emitter.self()
        )
    }

}