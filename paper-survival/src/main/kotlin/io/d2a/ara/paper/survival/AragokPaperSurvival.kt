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
import io.d2a.ara.paper.survival.enderchest.EnderChestChannelStorage
import io.d2a.ara.paper.survival.enderchest.EnderChestPlaceBreakListener
import io.d2a.ara.paper.survival.floo.FlooItem.Companion.toEssenceItem
import io.d2a.ara.paper.survival.floo.FlooItem.Companion.toUnusedPowderItem
import io.d2a.ara.paper.survival.floo.FlooUseListeners
import io.d2a.ara.paper.survival.floo.WitchDropEssenceListener
import io.d2a.ara.paper.survival.hopper.*
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
    private var enderStorage: EnderChestChannelStorage? = null

    val devNullRecipeKey = NamespacedKey(this, "dev_null")
    val enrichedCoalRecipeKey = NamespacedKey(this, "enriched_coal")
    val enrichedCharcoalRecipeKey = NamespacedKey(this, "enriched_coal_from_charcoal")
    val infusedCoalRecipeKey = NamespacedKey(this, "infused_coal")
    val superchargedCoalRecipeKey = NamespacedKey(this, "supercharged_coal")
    val unusedFlooPowderRecipeKey = NamespacedKey(this, "unused_floo_powder")

    // filter recipes
    val smartHopperRecipeKey = NamespacedKey(this, "smart_hopper")
    val acceptHopperFilterRecipeKey = NamespacedKey(this, "hopper_filter_accept")
    val denyHopperFilterRecipeKey = NamespacedKey(this, "hopper_filter_deny")
    val deleteHopperFilterRecipeKey = NamespacedKey(this, "hopper_filter_delete")


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
        registerEndStorageFeature()

        logger.info("Enabled aragok-survival")
    }

    override fun onDisable() {
        closeQuietly(borderTask, "BorderTask")

        enderStorage?.let {
            it.stopAutosave()
            it.flushAllSync()
        }

        logger.info("Disabled aragok-survival")
    }

    fun registerCoalFeature() {
        logger.info("Registering coal item and listener...")

        val enrichedCoalItem = ENRICHED.toItem()
        val infusedCoalItem = INFUSED.toItem()
        val superchargedCoalItem = SUPERCHARGED.toItem()

        registerRecipe(
            ShapelessRecipe(
                enrichedCoalRecipeKey,
                enrichedCoalItem
            )
                .addIngredient(1, Material.COAL)
                .addIngredient(1, Material.IRON_NUGGET)
                .addIngredient(1, Material.IRON_NUGGET),
            ShapelessRecipe(
                infusedCoalRecipeKey,
                infusedCoalItem
            )
                .addIngredient(RecipeChoice.ExactChoice(enrichedCoalItem))
                .addIngredient(1, Material.BLAZE_POWDER)
                .addIngredient(1, Material.REDSTONE),
            ShapelessRecipe(
                superchargedCoalRecipeKey,
                superchargedCoalItem
            )
                .addIngredient(RecipeChoice.ExactChoice(infusedCoalItem))
                .addIngredient(1, Material.END_CRYSTAL)
                .addIngredient(1, Material.AMETHYST_SHARD),
            ShapelessRecipe(
                enrichedCharcoalRecipeKey,
                enrichedCoalItem
            )
                .addIngredient(1, Material.CHARCOAL)
                .addIngredient(1, Material.IRON_NUGGET)
                .addIngredient(1, Material.IRON_NUGGET)
        )

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
        registerRecipe(
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

        registerRecipe(
            ShapedRecipe(
                unusedFlooPowderRecipeKey,
                unusedPowderItem
            )
                .shape(" G ", "GPG", " G ")
                .setIngredient('G', RecipeChoice.ExactChoice(essenceItem))
                .setIngredient('P', Material.ENDER_PEARL)
        )

        registerEvents(
            FlooUseListeners(logger),
            WitchDropEssenceListener(unusedFlooPowderRecipeKey)
        )
    }

    fun registerHopperFeature() {
        logger.info("Registering hopper filter listener...")

        registerRecipe(
            ShapelessRecipe(
                smartHopperRecipeKey,
                HopperFilterItem.toSmartHopperItem(),
            )
                .addIngredient(1, Material.HOPPER)
                .addIngredient(1, Material.COMPARATOR)
                .addIngredient(1, Material.QUARTZ),

            ShapedRecipe(
                acceptHopperFilterRecipeKey,
                HopperFilterItem.toItem(HopperFilterItem.HopperFilterType.ALLOW),
            ).shape("IGI", "RHC", "IQI")
                .setIngredient('I', Material.IRON_INGOT)
                .setIngredient('G', Material.GREEN_DYE)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('H', Material.HOPPER)
                .setIngredient('C', Material.COMPARATOR)
                .setIngredient('Q', Material.QUARTZ),

            ShapedRecipe(
                denyHopperFilterRecipeKey,
                HopperFilterItem.toItem(HopperFilterItem.HopperFilterType.DENY),
            ).shape("IGI", "RHO", "IQI")
                .setIngredient('I', Material.IRON_INGOT)
                .setIngredient('G', Material.RED_DYE)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('H', Material.HOPPER)
                .setIngredient('O', Material.OBSERVER)
                .setIngredient('Q', Material.QUARTZ),

            ShapedRecipe(
                deleteHopperFilterRecipeKey,
                HopperFilterItem.toItem(HopperFilterItem.HopperFilterType.DELETE),
            ).shape("IGI", "RHC", "ITI")
                .setIngredient('I', Material.IRON_INGOT)
                .setIngredient('G', Material.PURPLE_DYE)
                .setIngredient('R', Material.REDSTONE)
                .setIngredient('H', Material.HOPPER)
                .setIngredient('C', Material.COMPARATOR)
                .setIngredient('T', Material.TNT),
        )

        // prevent using hopper filter items
        registerEvents(
            MakeHopperFilterUselessListener(logger),
            SmartHopperPlaceBreakListener(logger),
            HopperFilterLifecycleListener(),
            HopperFilterEditor(),
        )
    }

    fun registerEndStorageFeature() {
        logger.info("Registering ender storage feature...")

        val storage = EnderChestChannelStorage(this).apply {
            startAutosave(intervalSeconds = 300)
        }
        enderStorage = storage

        registerEvents(EnderChestPlaceBreakListener(logger, storage))
    }

}
