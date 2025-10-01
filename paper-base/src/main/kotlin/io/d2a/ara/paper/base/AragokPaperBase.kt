package io.d2a.ara.paper.base

import io.d2a.ara.paper.base.activity.ActivityService
import io.d2a.ara.paper.base.activity.PlayerMovementActivity
import io.d2a.ara.paper.base.commands.PrivilegesCommand
import io.d2a.ara.paper.base.custom.PreventCraftingListener
import io.d2a.ara.paper.base.extension.*
import io.d2a.ara.paper.base.flair.*
import io.d2a.ara.paper.base.flair.listener.InjectFlairToChatListener
import io.d2a.ara.paper.base.flair.listener.UpdatePlayerTagOnJoinLeaveListener
import io.papermc.paper.command.brigadier.Commands
import net.luckperms.api.LuckPerms
import org.bukkit.plugin.java.JavaPlugin

class AragokPaperBase : JavaPlugin() {

    private var luckPermsLiveUserUpdate: LuckPermsLiveUpdateExtension? = null
    private var playerMovementActivity: PlayerMovementActivity? = null

    override fun onEnable() {
        val luckPerms = server.servicesManager.load(LuckPerms::class.java)
            ?: return disableWithError("LuckPerms not found, disabling plugin")

        // custom items
        registerEvents(PreventCraftingListener(logger))

        val activityService = PlayerMovementActivity(this, 15 * 60 * 1000) // 15 minutes
        registerEvents(activityService)
        registerService<ActivityService>(activityService)
        playerMovementActivity = activityService

        withCommandRegistrar {
            register(PrivilegesCommand(luckPerms).build(), "Gain Admin Privileges")
            register(
                Commands.literal("away")
                    .requiresPermission("aragok.base.command.away")
                    .executesPlayer { ctx, player ->
                        playerMovementActivity?.let { service ->
                            service.lastPlayerActivity[player.uniqueId] = 0L
                            service.setState(player, ActivityService.ActivityState.AWAY)
                        }
                        ctx.success("Your activity state has been set to AWAY.")
                    }.build()
            )
        }

        registerFlairFeature(luckPerms, activityService)

        logger.info("Enabled aragok-base")
    }

    override fun onDisable() {
        closeQuietly(luckPermsLiveUserUpdate, "LuckPermsLiveUpdateExtension")
        closeQuietly(playerMovementActivity, "PlayerMovementActivity")

        logger.info("Disabled aragok-base")
    }

    fun registerFlairFeature(luckPerms: LuckPerms, activityService: ActivityService) {
        logger.info("Registering flair listeners and services...")

        val scoreboard = server.scoreboardManager.mainScoreboard

        val prefixSuffixProvider: PrefixSuffixProvider = LuckPermsPrefixSuffixProvider(luckPerms)
        val nametagService = NametagService(logger, scoreboard, prefixSuffixProvider, activityService)

        registerEvents(
            InjectFlairToChatListener(prefixSuffixProvider),
            UpdatePlayerTagOnJoinLeaveListener(nametagService)
        )

        activityService.registerListener(AwayTagListener(nametagService))

        // subscribe to rank / prefix / suffix changes
        luckPermsLiveUserUpdate = LuckPermsLiveUpdateExtension(this, logger, nametagService, luckPerms).apply {
            subscribeFlairUserLiveUpdates()
            subscribeFlairGroupLiveUpdates()
        }

        // apply nametags to online players (in case of reload)
        server.onlinePlayers.forEach(nametagService::applyTo)
    }

}
