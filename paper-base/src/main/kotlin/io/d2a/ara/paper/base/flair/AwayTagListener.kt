package io.d2a.ara.paper.base.flair

import io.d2a.ara.paper.base.activity.ActivityService
import org.bukkit.entity.Player

class AwayTagListener(
    private val nametagService: NametagService,
) : ActivityService.ActivityChangedListener {

    override fun onActivityChanged(
        player: Player,
        oldState: ActivityService.ActivityState,
        newState: ActivityService.ActivityState
    ) {
        nametagService.applyTo(player)
    }

}