package io.d2a.ara.paper.survival.enderchest

import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import io.d2a.ara.paper.base.extension.*
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.block.EnderChest
import org.bukkit.entity.Display
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.logging.Logger

// TODO: as soon as the first ender chest was opened
//   The user should not be able to re-color it to prevent abuse (infinite inventory space)
class EnderChestPlaceBreakListener(
    val logger: Logger,
    val storage: EnderChestChannelStorage
) : Listener {

    companion object {
        val PLAIN = PlainTextComponentSerializer.plainText()
    }

    private object Stripes {
        //        val PDC_INTERACTION_TARGET_ENDER_CHEST = NamespacedKey(NAMESPACE, "ender_interaction_target_ender_chest")
        val PDC_INTERACTION_TARGET_PATTERN = NamespacedKey(NAMESPACE, "ender_interaction_target_pattern")

        // make sure we cannot re-color when locked
        val PDC_LOCKED = NamespacedKey(NAMESPACE, "ender_locked")

        val PDC_LEFT_KEY = NamespacedKey(NAMESPACE, "ender_left_key")
        val PDC_MIDDLE_KEY = NamespacedKey(NAMESPACE, "ender_middle_key")
        val PDC_RIGHT_KEY = NamespacedKey(NAMESPACE, "ender_right_key")

        val PDC_LEFT_DISPLAY_UUID = NamespacedKey(NAMESPACE, "ender_left_display_uuid")
        val PDC_MIDDLE_DISPLAY_UUID = NamespacedKey(NAMESPACE, "ender_middle_display_uuid")
        val PDC_RIGHT_DISPLAY_UUID = NamespacedKey(NAMESPACE, "ender_right_display_uuid")

        val PDC_LEFT_INTERACTION_UUID = NamespacedKey(NAMESPACE, "ender_left_interaction_uuid")
        val PDC_MIDDLE_INTERACTION_UUID = NamespacedKey(NAMESPACE, "ender_middle_interaction_uuid")
        val PDC_RIGHT_INTERACTION_UUID = NamespacedKey(NAMESPACE, "ender_right_interaction_uuid")

        // TODO: store the pattern in the enderchest!

        // pre-built glass ItemStacks by dye color
        val coloredGlassByDye = EnumMap<DyeColor, ItemStack>(DyeColor::class.java).apply {
            for (dyeColor in DyeColor.entries) {
                val material = Material.valueOf("${dyeColor.name}_STAINED_GLASS")
                put(dyeColor, ItemStack.of(material))
            }
        }

        // pre-built mapping of materials to dye colors
        val materialToDye = EnumMap<Material, DyeColor>(Material::class.java).apply {
            for (material in Material.entries) {
                if (material.name.endsWith("_DYE")) {
                    val dye = DyeColor.entries.find { it.name == material.name.removeSuffix("_DYE") }
                    if (dye != null) {
                        put(material, dye)
                    }
                }
            }
        }

        // X offsets for left, center and right stripes on the chest lid
        val stripeXOffsets = doubleArrayOf(-0.20, 0.0, 0.20)

        // transformation to make a thin rectangular plate
        val sharedTransform = Transformation(
            Vector3f(0f, 0f, 0f),
            Quaternionf(AxisAngle4f(0f, 0f, 1f, 0f)),
            Vector3f(0.10f, 0.05f, 0.30f),
            Quaternionf()
        )
    }

    fun spawnEnderChestWithStripes(enderChest: EnderChest, pattern: EnderChestPattern) {
        val world = enderChest.world
        val lidCenter = enderChest.location.clone().add(0.5, 0.9, 0.5)

        var leftDisplayUuid: UUID? = null
        var middleDisplayUuid: UUID? = null
        var rightDisplayUuid: UUID? = null

        var leftInteractionUuid: UUID? = null
        var middleInteractionUuid: UUID? = null
        var rightInteractionUuid: UUID? = null

        Stripes.stripeXOffsets.forEachIndexed { index, xOffset ->
            val stripeLocation = lidCenter.clone().add(xOffset, 0.0, 0.0)

            val dyeColor = when (index) {
                0 -> pattern.left
                1 -> pattern.middle
                2 -> pattern.right
                else -> throw IllegalStateException("Index out of bounds: $index")
            }
            val glassItem = Stripes.coloredGlassByDye[dyeColor] ?: return@forEachIndexed

            val itemDisplay = world.spawn(stripeLocation, ItemDisplay::class.java) { display ->
                display.isPersistent = true
                display.isInvisible = true
                display.isGlowing = false
                display.setItemStack(glassItem)
                display.billboard = Display.Billboard.FIXED
                display.setGravity(false)
                display.isInvulnerable = true
                display.viewRange = 32f
                display.transformation = Stripes.sharedTransform
            }
            when (index) {
                0 -> leftDisplayUuid = itemDisplay.uniqueId
                1 -> middleDisplayUuid = itemDisplay.uniqueId
                2 -> rightDisplayUuid = itemDisplay.uniqueId
            }

            val interaction = world.spawn(stripeLocation, Interaction::class.java) { hitbox ->
                hitbox.isPersistent = true
                hitbox.isInvisible = true
                hitbox.setGravity(false)
                hitbox.isInvulnerable = true
                hitbox.interactionWidth = 0.3f
                hitbox.interactionHeight = 0.1f
                hitbox.isResponsive = false

                hitbox.persistentDataContainer.apply {
                    // TODO: check if this is required
                    // setString(Stripes.PDC_INTERACTION_TARGET_ENDER_CHEST, blockLocationToString(enderChest.location))
                    setInt(Stripes.PDC_INTERACTION_TARGET_PATTERN, index)
                }
            }
            when (index) {
                0 -> leftInteractionUuid = interaction.uniqueId
                1 -> middleInteractionUuid = interaction.uniqueId
                2 -> rightInteractionUuid = interaction.uniqueId
            }
        }

        if (leftDisplayUuid == null || middleDisplayUuid == null || rightDisplayUuid == null) {
            throw IllegalStateException("Failed to spawn all display entities for ender chest at ${enderChest.location}")
        }
        // note: interactions cannot be null at this point

        // store the UUIDs of the displays and interactions in the ender chest's persistent data container
        enderChest.persist {
            setString(Stripes.PDC_LEFT_DISPLAY_UUID, leftDisplayUuid.toString())
            setString(Stripes.PDC_MIDDLE_DISPLAY_UUID, middleDisplayUuid.toString())
            setString(Stripes.PDC_RIGHT_DISPLAY_UUID, rightDisplayUuid.toString())

            setString(Stripes.PDC_LEFT_INTERACTION_UUID, leftInteractionUuid.toString())
            setString(Stripes.PDC_MIDDLE_INTERACTION_UUID, middleInteractionUuid.toString())
            setString(Stripes.PDC_RIGHT_INTERACTION_UUID, rightInteractionUuid.toString())

            // store the pattern
            setString(Stripes.PDC_LEFT_KEY, pattern.left.name)
            setString(Stripes.PDC_MIDDLE_KEY, pattern.middle.name)
            setString(Stripes.PDC_RIGHT_KEY, pattern.right.name)
        }
    }

    fun updateStripeColor(enderChest: EnderChest, index: Int, stack: ItemStack, key: String) {
        val world = enderChest.world

        val displayUuid = when (index) {
            0 -> enderChest.persistentDataContainer.getString(Stripes.PDC_LEFT_DISPLAY_UUID)
            1 -> enderChest.persistentDataContainer.getString(Stripes.PDC_MIDDLE_DISPLAY_UUID)
            2 -> enderChest.persistentDataContainer.getString(Stripes.PDC_RIGHT_DISPLAY_UUID)
            else -> null
        } ?: return

        val display = world.getEntity(UUID.fromString(displayUuid)) as? ItemDisplay ?: return
        display.setItemStack(stack)

        // also update the stored pattern in the ender chest
        when (index) {
            0 -> enderChest.persist { setString(Stripes.PDC_LEFT_KEY, key) }
            1 -> enderChest.persist { setString(Stripes.PDC_MIDDLE_KEY, key) }
            2 -> enderChest.persist { setString(Stripes.PDC_RIGHT_KEY, key) }
        }
    }

    fun removeAllEntities(enderChest: EnderChest) {
        val world = enderChest.world
        enderChest.persistentDataContainer.apply {
            // remove displays
            getUniqueId(Stripes.PDC_LEFT_DISPLAY_UUID)?.let { world.getEntity(it)?.remove() }
            getUniqueId(Stripes.PDC_MIDDLE_DISPLAY_UUID)?.let { world.getEntity(it)?.remove() }
            getUniqueId(Stripes.PDC_RIGHT_DISPLAY_UUID)?.let { world.getEntity(it)?.remove() }

            // remove interactions
            getUniqueId(Stripes.PDC_LEFT_INTERACTION_UUID)?.let { world.getEntity(it)?.remove() }
            getUniqueId(Stripes.PDC_MIDDLE_INTERACTION_UUID)?.let { world.getEntity(it)?.remove() }
            getUniqueId(Stripes.PDC_RIGHT_INTERACTION_UUID)?.let { world.getEntity(it)?.remove() }
        }
    }

    // add interactions when the ender chest is placed
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onEnderChestPlace(event: BlockPlaceEvent) {
        if (event.block.type != Material.ENDER_CHEST) return
        // TODO: check if it's a special ender chest

        val enderChest = event.block.state as? EnderChest ?: return
        spawnEnderChestWithStripes(enderChest, EnderChestPattern.default())
    }

    // remove interactions when the ender chest is broken
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onEnderChestBreak(event: BlockBreakEvent) {
        if (event.block.type != Material.ENDER_CHEST) return
        // TODO: check if it's a special ender chest

        val enderChest = event.block.state as? EnderChest ?: return
        removeAllEntities(enderChest)
    }

    // re-color the pattern
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onStripeClick(event: PlayerInteractAtEntityEvent) {
        val interaction = event.rightClicked as? Interaction ?: return
        println("player ${event.player.name} clicked interaction entity ${interaction.uniqueId}")

        val chestBlock = interaction.location.block
        if (chestBlock.type != Material.ENDER_CHEST) return
        val enderChest = chestBlock.state as? EnderChest ?: return
        println("and it's on top of an ender chest at ${enderChest.location}")

        val player = event.player

        val pdc = interaction.persistentDataContainer
        val patternPartIndex = pdc.getInt(Stripes.PDC_INTERACTION_TARGET_PATTERN) ?: return
        println("player ${player.name} clicked interaction for index $patternPartIndex")

        event.isCancelled = true // prevent interaction from doing anything else

        if (!player.isSneaking) return

        // prevent re-coloring when locked
        // this should prevent abuse => infinite inventory space when re-coloring
        if (enderChest.persistentDataContainer.isTrue(Stripes.PDC_LOCKED)) {
            player.failActionBar("This Ender Chest is locked and cannot be re-colored.", sound = VILLAGER_NO_SOUND)
            return
        }

        val item = player.inventory.itemInMainHand
        val dyeColor = Stripes.materialToDye[item.type] ?: return
        println("player ${player.name} is holding dye color ${dyeColor.name}")

        val glassItem = Stripes.coloredGlassByDye[dyeColor] ?: return
        println("which corresponds to glass item ${glassItem.type.name}")

        updateStripeColor(enderChest, patternPartIndex, glassItem, dyeColor.name)
        println("updated ender chest at ${enderChest.location} stripe index $patternPartIndex to color ${dyeColor.name}")

        // consume the item
        if (item.amount > 1) {
            item.amount -= 1
        } else {
            player.inventory.setItemInMainHand(null)
        }
    }

    fun patternKeyOf(vararg keys: String): String = keys.joinToString("-")

    @EventHandler(
        ignoreCancelled = true
    )
    fun onChestOpen(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return
        if (block.type != Material.ENDER_CHEST) return

        val enderChest = block.state as? EnderChest ?: return
        val patternKeyLeft = enderChest.persistentDataContainer.getString(Stripes.PDC_LEFT_KEY) ?: return
        val patternKeyMiddle = enderChest.persistentDataContainer.getString(Stripes.PDC_MIDDLE_KEY) ?: return
        val patternKeyRight = enderChest.persistentDataContainer.getString(Stripes.PDC_RIGHT_KEY) ?: return
        val key = patternKeyOf(patternKeyLeft, patternKeyMiddle, patternKeyRight)

        event.isCancelled = true

        if (event.player.isSneaking) return

        event.player.successActionBar("Opening Ender Storage $key...")
        val sharedInventory = storage.getOrCreateInventory(key)

        event.player.openInventory(sharedInventory)

        enderChest.persistentDataContainer.apply {
            if (!isTrue(Stripes.PDC_LOCKED)) {
                setTrue(Stripes.PDC_LOCKED)
                enderChest.update(true, false)

                enderChest.world.playSound(
                    enderChest.location,
                    Sound.BLOCK_CHEST_LOCKED,
                    1.0f, 1.0f
                )

                event.player.successActionBar("This Ender Chest is now locked.")

                logger.info("Locked ender chest at ${enderChest.location} with pattern $key")
            }
        }
    }

    @EventHandler
    fun onEnderChestClose(event: InventoryCloseEvent) {
        val title = PLAIN.serialize(event.view.title())
        if (!title.startsWith(EnderChestChannelStorage.INVENTORY_PREFIX)) return

        val key = title.removePrefix(EnderChestChannelStorage.INVENTORY_PREFIX).trim()
        if (key.isEmpty()) return

        storage.markDirty(key)
        logger.info("Marked ender chest channel '$key' as dirty for saving.")
    }

}