package io.d2a.ara.paper.base.flair

import io.d2a.ara.paper.base.activity.ActivityService
import io.d2a.ara.paper.base.extension.namedColor
import io.d2a.ara.paper.base.extension.toComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Scoreboard
import java.util.*
import java.util.logging.Logger

class NametagService(
    private val logger: Logger,
    private val scoreboard: Scoreboard,
    private val prefixSuffixProvider: PrefixSuffixProvider,
    private val activityService: ActivityService,
) {

    private val playerTeamNames = mutableMapOf<UUID, String>()

    fun applyTo(player: Player) {
        if (!player.isOnline) {
            logger.info("Player ${player.name} (${player.uniqueId}) is not online, skipping nametag application")
            return
        }

        logger.info("Applying nametag to player ${player.name} (${player.uniqueId})")

        val teamName = getTeamNameFor(player)
        val team = scoreboard.getTeam(teamName) ?: run {
            logger.info("Creating new team $teamName for player ${player.name} (${player.uniqueId})")
            scoreboard.registerNewTeam(teamName)
        }

        val prefixComponent = prefixSuffixProvider.prefix(player).toComponent()
        var suffix = prefixSuffixProvider.suffix(player).toComponent()
        if (activityService.getActivityState(player) == ActivityService.ActivityState.AWAY) {
            suffix = suffix.append(Component.text(" (away)", NamedTextColor.DARK_GRAY))
        }

        team.apply {
            // apply prefix, suffix and coloring
            prefix(prefixComponent)
            suffix(suffix)
            color(prefixComponent.namedColor())

            // add player to it
            if (!hasEntity(player)) {
                logger.fine("Adding player ${player.name} (${player.uniqueId}) to team $teamName")
                addEntity(player)
            }
        }

        if (player.scoreboard !== scoreboard) {
            logger.fine("Setting player ${player.name} (${player.uniqueId}) scoreboard to custom scoreboard")
            player.scoreboard = scoreboard
        }
    }

    fun remove(player: Player) {
        logger.info("Removing nametag from player ${player.name} (${player.uniqueId})")

        val teamName = playerTeamNames.remove(player.uniqueId) ?: return
        logger.info("Found team $teamName for player ${player.name} (${player.uniqueId})")

        val team = scoreboard.getTeam(teamName) ?: return
        logger.info("Found team $teamName in scoreboard for player ${player.name} (${player.uniqueId})")

        team.removeEntity(player)
        if (team.entries.isEmpty()) {
            logger.info("Removing empty team $teamName from scoreboard")
            team.unregister()
        }
    }

    private fun getTeamNameFor(player: Player): String =
        playerTeamNames.getOrPut(player.uniqueId) {
            "agkt-${player.uniqueId}".take(16)
        }

}