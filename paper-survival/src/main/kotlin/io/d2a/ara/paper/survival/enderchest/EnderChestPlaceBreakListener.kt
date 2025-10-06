package io.d2a.ara.paper.survival.enderchest

import io.d2a.ara.paper.base.extension.getUniqueId
import io.d2a.ara.paper.base.extension.persist
import io.d2a.ara.paper.base.extension.setInt
import io.d2a.ara.paper.base.extension.setString
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.block.BlockFace
import org.bukkit.block.EnderChest
import org.bukkit.block.data.Directional
import org.bukkit.entity.Display
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*
import java.util.logging.Logger

class EnderChestPlaceBreakListener(
    val logger: Logger,
) : Listener {

    // pre-built glass ItemStacks by dye color
    val coloredGlassByDye = EnumMap<DyeColor, ItemStack>(DyeColor::class.java).apply {
        for (dyeColor in DyeColor.entries) {
            val material = Material.valueOf("${dyeColor.name}_STAINED_GLASS")
            put(dyeColor, ItemStack.of(material))
        }
    }

    private fun leftVector(face: BlockFace): Vector = when (face) {
        BlockFace.NORTH -> Vector(-1.0, 0.0, 0.0)
        BlockFace.SOUTH -> Vector(1.0, 0.0, 0.0)
        BlockFace.WEST -> Vector(0.0, 0.0, 1.0)
        BlockFace.EAST -> Vector(0.0, 0.0, -1.0)
        else -> Vector(-1.0, 0.0, 0.0)
    }

    private fun isEastWest(face: BlockFace): Boolean =
        face == BlockFace.EAST || face == BlockFace.WEST

    fun spawnEnderChestWithStripes(enderChest: EnderChest) {
        val world = enderChest.world
        val lidCenter = enderChest.location.clone().add(0.5, 0.9, 0.5)

        val face = (enderChest.blockData as? Directional)?.facing ?: return
        val left = leftVector(face)

        val offsets = doubleArrayOf(-0.20, 0.0, 0.20)
        val (scaleX, scaleZ) = if (isEastWest(face)) 0.3f to 0.1f else 0.1f to 0.3f
        val transform = Transformation(
            Vector3f(0f, 0f, 0f),
            Quaternionf(AxisAngle4f(0f, 0f, 1f, 0f)),
            Vector3f(scaleX, 0.05f, scaleZ),
            Quaternionf()
        )

        var leftDisplayUuid: UUID? = null
        var middleDisplayUuid: UUID? = null
        var rightDisplayUuid: UUID? = null

        var leftInteractionUuid: UUID? = null
        var middleInteractionUuid: UUID? = null
        var rightInteractionUuid: UUID? = null

        offsets.forEachIndexed { index, offset ->
            val stripeLocation = lidCenter.clone().add(left.clone().multiply(offset))

            val dyeColor = DyeColor.WHITE
            val glassItem = coloredGlassByDye[dyeColor] ?: return@forEachIndexed

            val itemDisplay = world.spawn(stripeLocation, ItemDisplay::class.java) { display ->
                display.isPersistent = true
                display.isInvisible = true
                display.isGlowing = false
                display.setItemStack(glassItem)
                display.billboard = Display.Billboard.FIXED
                display.setGravity(false)
                display.isInvulnerable = true
                display.viewRange = 32f
                display.transformation = transform
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
                    setInt(EnderStorageKeys.hitboxStripeIndex, index)
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
            setString(EnderStorageKeys.left, DyeColor.WHITE.name)
            setString(EnderStorageKeys.middle, DyeColor.WHITE.name)
            setString(EnderStorageKeys.right, DyeColor.WHITE.name)

            setString(EnderStorageKeys.leftDisplay, leftDisplayUuid.toString())
            setString(EnderStorageKeys.middleDisplay, middleDisplayUuid.toString())
            setString(EnderStorageKeys.rightDisplay, rightDisplayUuid.toString())

            setString(EnderStorageKeys.leftHitbox, leftInteractionUuid.toString())
            setString(EnderStorageKeys.middleHitbox, middleInteractionUuid.toString())
            setString(EnderStorageKeys.rightHitbox, rightInteractionUuid.toString())
        }
    }


    fun removeAllEntities(enderChest: EnderChest) {
        val world = enderChest.world
        enderChest.persistentDataContainer.apply {
            // remove displays
            getUniqueId(EnderStorageKeys.leftDisplay)?.let { world.getEntity(it)?.remove() }
            getUniqueId(EnderStorageKeys.middleDisplay)?.let { world.getEntity(it)?.remove() }
            getUniqueId(EnderStorageKeys.rightDisplay)?.let { world.getEntity(it)?.remove() }

            // remove interactions
            getUniqueId(EnderStorageKeys.leftHitbox)?.let { world.getEntity(it)?.remove() }
            getUniqueId(EnderStorageKeys.middleHitbox)?.let { world.getEntity(it)?.remove() }
            getUniqueId(EnderStorageKeys.rightHitbox)?.let { world.getEntity(it)?.remove() }
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

        spawnEnderChestWithStripes(enderChest)

        // TODO: play particles
        // TODO: play sound

        logger.info("Player ${event.player.name} placed an ender chest at ${enderChest.location}")
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

        // prevent abuse -> the chest gets destroyed
        event.isDropItems = false
        event.expToDrop = 15

        logger.info("Player ${event.player.name} broke an ender chest at ${enderChest.location}")
    }

}