package io.d2a.ara.paper.base

import io.d2a.ara.paper.base.commands.PrivilegesCommand
import io.d2a.ara.paper.base.extension.closeQuietly
import io.d2a.ara.paper.base.extension.disableWithError
import io.d2a.ara.paper.base.extension.registerEvents
import io.d2a.ara.paper.base.extension.withCommandRegistrar
import io.d2a.ara.paper.base.flair.LuckPermsLiveUpdateExtension
import io.d2a.ara.paper.base.flair.LuckPermsPrefixSuffixProvider
import io.d2a.ara.paper.base.flair.NametagService
import io.d2a.ara.paper.base.flair.PrefixSuffixProvider
import io.d2a.ara.paper.base.flair.listener.InjectFlairToChatListener
import io.d2a.ara.paper.base.flair.listener.UpdatePlayerTagOnJoinLeaveListener
import net.luckperms.api.LuckPerms
import org.bukkit.plugin.java.JavaPlugin

class AragokPaperBase : JavaPlugin() {

    private var luckPermsLiveUserUpdate: LuckPermsLiveUpdateExtension? = null

    override fun onEnable() {
        val luckPerms = server.servicesManager.load(LuckPerms::class.java)
            ?: return disableWithError("LuckPerms not found, disabling plugin")

        withCommandRegistrar {
            register(PrivilegesCommand(luckPerms).build(), "Gain Admin Privileges")
        }

        registerFlairFeature(luckPerms)

        logger.info("Enabled aragok-base")
    }

    override fun onDisable() {
        closeQuietly(luckPermsLiveUserUpdate, "LuckPermsLiveUpdateExtension")

        logger.info("Disabled aragok-base")
    }

    fun registerFlairFeature(luckPerms: LuckPerms) {
        logger.info("Registering flair listeners and services...")

        val scoreboard = server.scoreboardManager.mainScoreboard

        val prefixSuffixProvider: PrefixSuffixProvider = LuckPermsPrefixSuffixProvider(luckPerms)
        val nametagService = NametagService(logger, scoreboard, prefixSuffixProvider)

        registerEvents(
            InjectFlairToChatListener(prefixSuffixProvider),
            UpdatePlayerTagOnJoinLeaveListener(nametagService)
        )

        // subscribe to rank / prefix / suffix changes
        luckPermsLiveUserUpdate = LuckPermsLiveUpdateExtension(this, logger, nametagService, luckPerms).apply {
            subscribeFlairUserLiveUpdates()
            subscribeFlairGroupLiveUpdates()
        }

        // apply nametags to online players (in case of reload)
        server.onlinePlayers.forEach(nametagService::applyTo)
    }

}
