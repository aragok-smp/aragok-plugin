package io.d2a.ara.paper.survival

import io.d2a.ara.paper.base.activity.ActivityService
import io.d2a.ara.paper.base.extension.*
import io.d2a.ara.paper.survival.activity.ActivityChangedNotifier
import io.d2a.ara.paper.survival.border.BorderTask
import io.d2a.ara.paper.survival.coal.CoalType.*
import io.d2a.ara.paper.survival.coal.FurnaceSmeltCoalListener
import io.d2a.ara.paper.survival.coal.PickUpCoalEvent
import io.d2a.ara.paper.survival.commands.RestrictionCommand
import io.d2a.ara.paper.survival.commands.TrashCommand
import io.d2a.ara.paper.survival.devnull.CraftBagListener
import io.d2a.ara.paper.survival.devnull.DevNullItem
import io.d2a.ara.paper.survival.devnull.ItemPickupDevNullListener
import io.d2a.ara.paper.survival.floo.FlooItem.Companion.toEssenceItem
import io.d2a.ara.paper.survival.floo.FlooItem.Companion.toUnusedPowderItem
import io.d2a.ara.paper.survival.floo.FlooUseListeners
import io.d2a.ara.paper.survival.floo.WitchDropEssenceListener
import io.d2a.ara.paper.survival.hopper.HopperFilterListener
import io.d2a.ara.paper.survival.restriction.DimensionRestriction
import io.d2a.ara.paper.survival.sleep.EnterBedSleepListener
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.RecipeChoice
import org.bukkit.inventory.ShapedRecipe
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.plugin.java.JavaPlugin

class AragokPaperSurvival : JavaPlugin() {

    private var borderTask: BorderTask? = null

    val devNullRecipeKey = NamespacedKey(this, "dev_null")
    val enrichedCoalRecipeKey = NamespacedKey(this, "enriched_coal")
    val enrichedCharcoalRecipeKey = NamespacedKey(this, "enriched_coal_from_charcoal")
    val infusedCoalRecipeKey = NamespacedKey(this, "infused_coal")
    val superchargedCoalRecipeKey = NamespacedKey(this, "supercharged_coal")
    val unusedFlooPowderRecipeKey = NamespacedKey(this, "unused_floo_powder")


    override fun onEnable() {
        // start activity service
        val activityService = getService<ActivityService>()
            ?: return disableWithError("ActivityService not found")
        logger.info("Found activity service: $activityService")

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
        registerCommands(
            restrictionCommand,
            TrashCommand()
        )

        // features
        registerCoalFeature()
        registerDevNullFeature()
        registerFlooFeature()
        registerHopperFeature()

        logger.info("Enabled aragok-survival")
    }

    override fun onDisable() {
        closeQuietly(borderTask, "BorderTask")

        logger.info("Disabled aragok-survival")
    }

    fun registerCoalFeature() {
        logger.info("Registering coal item and listener...")

        val enrichedCoalItem = ENRICHED.toItem()
        val infusedCoalItem = INFUSED.toItem()
        val superchargedCoalItem = SUPERCHARGED.toItem()

        server.apply {
            addRecipe(
                ShapelessRecipe(
                    enrichedCoalRecipeKey,
                    enrichedCoalItem
                )
                    .addIngredient(1, Material.COAL)
                    .addIngredient(1, Material.IRON_NUGGET)
                    .addIngredient(1, Material.IRON_NUGGET)
            )
            addRecipe(
                ShapelessRecipe(
                    infusedCoalRecipeKey,
                    infusedCoalItem
                )
                    .addIngredient(RecipeChoice.ExactChoice(enrichedCoalItem))
                    .addIngredient(1, Material.BLAZE_POWDER)
                    .addIngredient(1, Material.REDSTONE)
            )
            addRecipe(
                ShapelessRecipe(
                    superchargedCoalRecipeKey,
                    superchargedCoalItem
                )
                    .addIngredient(RecipeChoice.ExactChoice(infusedCoalItem))
                    .addIngredient(1, Material.END_CRYSTAL)
                    .addIngredient(1, Material.AMETHYST_SHARD)
            )

            // charcoal variant -> enriched coal
            addRecipe(
                ShapelessRecipe(
                    enrichedCharcoalRecipeKey,
                    enrichedCoalItem
                )
                    .addIngredient(1, Material.CHARCOAL)
                    .addIngredient(1, Material.IRON_NUGGET)
                    .addIngredient(1, Material.IRON_NUGGET)
            )
        }

        registerEvents(
            FurnaceSmeltCoalListener(),
            PickUpCoalEvent(
                enrichedCoalRecipeKey,
                enrichedCharcoalRecipeKey,
                infusedCoalRecipeKey,
                superchargedCoalRecipeKey
            )
        )
    }

    fun registerDevNullFeature() {
        logger.info("Registering dev/null item and listener...")

        val devNullItem = DevNullItem.toItem()
        server.addRecipe(
            ShapedRecipe(
                devNullRecipeKey,
                devNullItem
            )
                .shape("OEO", "HRH", "OEO")
                .setIngredient('O', Material.OBSIDIAN)
                .setIngredient('E', Material.ENDER_PEARL)
                .setIngredient('H', Material.HOPPER)
                .setIngredient('R', Material.REDSTONE)
        )

        registerEvents(
            ItemPickupDevNullListener(),
            CraftBagListener(devNullRecipeKey)
        )
    }

    fun registerFlooFeature() {
        logger.info("Registering floo items and recipe...")

        val essenceItem = toEssenceItem()
        val unusedPowderItem = toUnusedPowderItem()

        server.apply {
            addRecipe(
                ShapedRecipe(
                    unusedFlooPowderRecipeKey,
                    unusedPowderItem
                )
                    .shape(" G ", "GPG", " G ")
                    .setIngredient('G', RecipeChoice.ExactChoice(essenceItem))
                    .setIngredient('P', Material.ENDER_PEARL)
            )
        }

        registerEvents(
            FlooUseListeners(logger),
            WitchDropEssenceListener(unusedFlooPowderRecipeKey)
        )
    }

    fun registerHopperFeature() {
        logger.info("Registering hopper filter listener...")

        registerEvents(HopperFilterListener())
    }

}
