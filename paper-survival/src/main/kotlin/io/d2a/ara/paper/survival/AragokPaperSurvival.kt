package io.d2a.ara.paper.survival

import io.d2a.ara.paper.base.activity.ActivityService
import io.d2a.ara.paper.base.custom.RecipeDiscoveryService
import io.d2a.ara.paper.base.extension.*
import io.d2a.ara.paper.survival.activity.ActivityChangedNotifier
import io.d2a.ara.paper.survival.border.BorderTask
import io.d2a.ara.paper.survival.coal.CoalType
import io.d2a.ara.paper.survival.coal.FurnaceSmeltCoalListener
import io.d2a.ara.paper.survival.commands.RestrictionCommand
import io.d2a.ara.paper.survival.devnull.DevNullItem
import io.d2a.ara.paper.survival.devnull.ItemPickupDevNullListener
import io.d2a.ara.paper.survival.floo.FlooItem
import io.d2a.ara.paper.survival.floo.FlooUseListeners
import io.d2a.ara.paper.survival.floo.WitchDropEssenceListener
import io.d2a.ara.paper.survival.restriction.DimensionRestriction
import io.d2a.ara.paper.survival.sleep.EnterBedSleepListener
import org.bukkit.plugin.java.JavaPlugin

class AragokPaperSurvival : JavaPlugin() {

    private var borderTask: BorderTask? = null

    override fun onEnable() {
        // start activity service
        val activityService = getService<ActivityService>()
            ?: return disableWithError("ActivityService not found")
        logger.info("Found activity service: $activityService")

        val discoveryService = getService<RecipeDiscoveryService>()
            ?: return disableWithError("RecipeDiscoveryService not found")
        logger.info("Found recipe discovery service: $discoveryService")

        // register activity listener which shows the activity state in the action bar
        activityService.registerListener(ActivityChangedNotifier())

        val sleepListener = withListenerRegistration(EnterBedSleepListener())
        activityService.registerListener(sleepListener)
        logger.info("Registered sleep listener to activity service")
        // end activity service

        // border task
        borderTask = BorderTask(plugin = this)

        val dimensionRestriction = withListenerRegistration(DimensionRestriction(this))
        val restrictionCommand = RestrictionCommand(borderTask, dimensionRestriction)

        // commands
        registerCommands(restrictionCommand)

        // features
        registerCoalFeature(discoveryService)
        registerDevNullFeature()
        registerFlooFeature()

        logger.info("Enabled aragok-survival")
    }

    override fun onDisable() {
        closeQuietly(borderTask, "BorderTask")

        logger.info("Disabled aragok-survival")
    }

    fun registerCoalFeature(discoveryService: RecipeDiscoveryService) {
        logger.info("Registering coal item and listener...")
        CoalType.registerRecipes(this, discoveryService)

        registerEvents(FurnaceSmeltCoalListener())
    }

    fun registerDevNullFeature() {
        logger.info("Registering dev/null item and listener...")
        DevNullItem.registerRecipe(this)

        registerEvents(ItemPickupDevNullListener())
    }

    fun registerFlooFeature() {
        logger.info("Registering floo items and recipe...")
        FlooItem.registerRecipe(this)

        registerEvents(
            FlooUseListeners(logger),
            WitchDropEssenceListener()
        )
    }

}
