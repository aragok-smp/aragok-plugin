package io.d2a.ara.paper.survival.sleep

import io.d2a.ara.paper.base.activity.ActivityService
import io.d2a.ara.paper.base.extension.toAdventure
import io.papermc.paper.event.player.PlayerDeepSleepEvent
import net.kyori.adventure.sound.Sound.Emitter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Sound
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerBedEnterEvent
import org.bukkit.event.player.PlayerQuitEvent
import kotlin.math.ceil

class EnterBedSleepListener(
    private val activityService: ActivityService,
) : Listener, ActivityService.ActivityChangedListener {

    override fun onActivityChanged(
        player: Player,
        oldState: ActivityService.ActivityState,
        newState: ActivityService.ActivityState
    ) {
        when (newState) {
            ActivityService.ActivityState.ACTIVE -> player.isSleepingIgnored = false
            else -> player.isSleepingIgnored = true
        }
        player.sendRichMessage("<white>Your activity state changed from <gray>$oldState <white>to <green>$newState")
    }

    fun advanceNightIfNeeded(world: World) {
        val sleepingPlayerCount = world.players.count { player -> player.isDeeplySleeping }
        if (sleepingPlayerCount == 0) {
            return
        }

        val requiredPlayerCount = ceil(world.players.count { player -> !player.isSleepingIgnored } / 2.0).toInt()
        if (sleepingPlayerCount < requiredPlayerCount) {
            val remaining = requiredPlayerCount - sleepingPlayerCount
            world.sendActionBar(
                Component.text("$remaining more player(s) need to sleep to skip the night.")
                    .color(NamedTextColor.YELLOW)
            )
            return
        }

        world.time = 1000
        world.weatherDuration = 0
        world.isThundering = false

        world.sendActionBar(
            Component.text("Night skipped! Good morning!")
                .color(NamedTextColor.GREEN)
        )
        world.playSound(
            Sound.BLOCK_AMETHYST_BLOCK_RESONATE.toAdventure()
                .volume(0.5f)
                .pitch(1.1f)
                .build(), Emitter.self()
        )
    }

    @EventHandler
    fun onBedEnter(event: PlayerBedEnterEvent) {
        val player = event.player
        if (activityService.getActivityState(player) != ActivityService.ActivityState.ACTIVE) {
            player.sendActionBar(
                Component.text("You cannot skip the night while being away.")
                    .color(NamedTextColor.RED)
            )
            event.isCancelled = true
            return
        }

        if (event.player.world.isDayTime) {
           return
        }

        // notify other players that we want to sleep through the night
        event.player.world.sendActionBar(
            Component.text("${event.player.name} wants to sleep through the night.")
                .color(NamedTextColor.GRAY)
                .decorate(TextDecoration.ITALIC)
        )
        event.player.world.playSound(
            Sound.BLOCK_AMETHYST_BLOCK_RESONATE.toAdventure()
                .volume(0.5f)
                .pitch(0.1f)
                .build(), Emitter.self()
        )
    }

    @EventHandler
    fun onDeepSleep(event: PlayerDeepSleepEvent) {
        advanceNightIfNeeded(event.player.world)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        advanceNightIfNeeded(event.player.world)
    }

}