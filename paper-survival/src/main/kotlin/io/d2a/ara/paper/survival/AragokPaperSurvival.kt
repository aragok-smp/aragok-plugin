package io.d2a.ara.paper.survival

import io.d2a.ara.paper.base.extension.closeQuietly
import io.d2a.ara.paper.base.extension.registerEvents
import io.d2a.ara.paper.base.extension.withCommandRegistrar
import io.d2a.ara.paper.survival.border.BorderTask
import io.d2a.ara.paper.survival.border.TestAdvanceCommand
import io.d2a.ara.paper.survival.coal.CoalType
import io.d2a.ara.paper.survival.coal.FurnaceSmeltCoalListener
import io.d2a.ara.paper.survival.devnull.DevNullItem
import io.d2a.ara.paper.survival.devnull.ItemPickupDevNullListener
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
