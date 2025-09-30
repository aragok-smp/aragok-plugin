package io.d2a.ara.paper.survival

import io.d2a.ara.paper.base.activity.ActivityService
import io.d2a.ara.paper.base.extension.*
import io.d2a.ara.paper.survival.activity.ActivityChangedNotifier
import io.d2a.ara.paper.survival.border.BorderTask
import io.d2a.ara.paper.survival.border.TestAdvanceCommand
import io.d2a.ara.paper.survival.coal.CoalType
import io.d2a.ara.paper.survival.coal.FurnaceSmeltCoalListener
import io.d2a.ara.paper.survival.devnull.DevNullItem
import io.d2a.ara.paper.survival.devnull.ItemPickupDevNullListener
import io.d2a.ara.paper.survival.sleep.EnterBedSleepListener
import org.bukkit.plugin.java.JavaPlugin

class AragokPaperSurvival : JavaPlugin() {

    private var borderTask: BorderTask? = null

    override fun onEnable() {
        borderTask = BorderTask(plugin = this)

        withCommandRegistrar {
            register(TestAdvanceCommand(borderTask).build())
        }

        registerCoalFeature()
        registerDevNullFeature()

        val activityService = getService<ActivityService>()
            ?: return disableWithError("ActivityService not found")
        logger.info("Found activity service: $activityService")

        val sleepListener = EnterBedSleepListener()
        activityService.registerListener(sleepListener)
        activityService.registerListener(ActivityChangedNotifier())
        registerEvents(sleepListener)

        logger.info("Enabled aragok-survival")
    }

    override fun onDisable() {
        closeQuietly(borderTask, "BorderTask")

        logger.info("Disabled aragok-survival")
    }

    fun registerCoalFeature() {
        logger.info("Registering coal item and listener...")
        CoalType.registerRecipes(this)

        registerEvents(FurnaceSmeltCoalListener())
    }

    fun registerDevNullFeature() {
        logger.info("Registering dev/null item and listener...")
        DevNullItem.registerRecipe(this)

        registerEvents(ItemPickupDevNullListener())
    }

}
