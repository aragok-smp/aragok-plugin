package io.d2a.ara.paper.survival.activity

import io.d2a.ara.paper.base.activity.ActivityService
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player

class ActivityChangedNotifier : ActivityService.ActivityChangedListener {

    override fun onActivityChanged(
        player: Player,
        oldState: ActivityService.ActivityState,
        newState: ActivityService.ActivityState
    ) {
        player.sendActionBar(
            Component.empty()
                .append(Component.text("You are now ", NamedTextColor.GRAY))
                .append(Component.text(newState.name, NamedTextColor.GREEN))
        )
    }

}