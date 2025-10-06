package io.d2a.ara.paper.survival.enderchest

import io.d2a.ara.paper.base.extension.*
import org.bukkit.*
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

class EnderStoragePlaceBreakListener(
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
            throw IllegalStateException("Failed to spawn all display entities for ender storage at ${enderChest.location}")
        }
        // note: interactions cannot be null at this point

        // store the UUIDs of the displays and interactions in the ender chest's persistent data container
        enderChest.persist {
            setTrue(EnderStorageKeys.item)

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

        // make sure it's our special ender storage item
        if (!event.itemInHand.persistentDataContainer.has(EnderStorageKeys.item)) return

        val enderChest = event.block.state as? EnderChest ?: return

        spawnEnderChestWithStripes(enderChest)
        playEnderStoragePlaceEffect(enderChest)

        logger.info("Player ${event.player.name} placed an ender storage at ${enderChest.location}")
    }

    // remove interactions when the ender storage is broken
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onEnderChestBreak(event: BlockBreakEvent) {
        if (event.block.type != Material.ENDER_CHEST) return

        val enderChest = event.block.state as? EnderChest ?: return
        if (!enderChest.persistentDataContainer.has(EnderStorageKeys.item)) return
        removeAllEntities(enderChest)

        // prevent abuse -> the chest gets destroyed
        event.isDropItems = false
        event.expToDrop = 15

        playEnderStorageBreakEffect(enderChest)

        logger.info("Player ${event.player.name} broke an ender storage at ${enderChest.location}")
    }

    // if you can see this code, this means ChatGPT cooked
    fun playEnderStoragePlaceEffect(enderChest: EnderChest) {
        val world = enderChest.world
        val loc = enderChest.location.clone().add(0.5, 0.9, 0.5)

        val facing = (enderChest.blockData as? Directional)?.facing ?: BlockFace.NORTH
        val left = when (facing) {
            BlockFace.NORTH -> Vector(-1.0, 0.0,  0.0)
            BlockFace.SOUTH -> Vector( 1.0, 0.0,  0.0)
            BlockFace.WEST  -> Vector( 0.0, 0.0,  1.0)
            BlockFace.EAST  -> Vector( 0.0, 0.0, -1.0)
            else -> Vector(-1.0, 0.0, 0.0)
        }

        // teal Dust
        val teal = Particle.DustOptions(Color.fromRGB(0x20, 0xFF, 0xCF), 1.1f)

        // base flash
        world.spawnParticle(Particle.FLASH, loc, 1)
        world.spawnParticle(Particle.DUST, loc, 20, 0.4, 0.06, 0.4, 0.0, teal)
        world.spawnParticle(Particle.PORTAL, loc, 12, 0.25, 0.05, 0.25, 0.2)

        // line of electric sparks along lid
        for (t in -4..4) {
            val p = loc.clone().add(left.clone().multiply(t / 8.0))
            world.spawnParticle(Particle.ELECTRIC_SPARK, p, 1, 0.0, 0.0, 0.0, 0.0)
        }

        // gentle rising END_ROD twinkles
        for (i in 0..10) {
            val p = loc.clone().add(0.0, i * 0.06, 0.0)
            world.spawnParticle(Particle.END_ROD, p, 2, 0.15, 0.00, 0.15, 0.0)
        }

        // sounds
        world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.8f, 1.5f)
        world.playSound(loc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.8f, 1.2f)
        world.playSound(loc, Sound.BLOCK_ENDER_CHEST_OPEN, 0.8f, 1.0f)
    }

    fun playEnderStorageBreakEffect(enderChest: EnderChest) {
        val world = enderChest.world
        val loc = enderChest.location.clone().add(0.5, 0.9, 0.5)

        val facing = (enderChest.blockData as? Directional)?.facing ?: BlockFace.NORTH
        val forward = when (facing) {
            BlockFace.NORTH -> Vector(0.0, 0.0, -1.0)
            BlockFace.SOUTH -> Vector(0.0, 0.0, 1.0)
            BlockFace.WEST  -> Vector(-1.0, 0.0, 0.0)
            BlockFace.EAST  -> Vector(1.0, 0.0, 0.0)
            else -> Vector(0.0, 0.0, 1.0)
        }

        // Dust color (teal → purple)
        val teal = Particle.DustOptions(Color.fromRGB(0x20, 0xFF, 0xCF), 1.2f)
        val purple = Particle.DustOptions(Color.fromRGB(0x9B, 0x5D, 0xE5), 1.0f)

        // Base puff + implosion sparks
        world.spawnParticle(Particle.FLASH, loc, 1)
        world.spawnParticle(Particle.PORTAL, loc, 25, 0.4, 0.1, 0.4, 0.2)
        world.spawnParticle(Particle.END_ROD, loc, 15, 0.25, 0.2, 0.25, 0.01)
        world.spawnParticle(Particle.REVERSE_PORTAL, loc, 20, 0.3, 0.2, 0.3, 0.0)
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, loc, 25, 0.3, 0.1, 0.3, 0.0,
            Particle.DustTransition(teal.color, purple.color, 1.0f)
        )

        // Directional shockwave (particles moving outwards)
        for (i in -3..3) {
            val offset = forward.clone().multiply(i * 0.1)
            val p = loc.clone().add(offset)
            world.spawnParticle(Particle.ELECTRIC_SPARK, p, 2, 0.02, 0.02, 0.02, 0.0)
        }

        // Rising obsidian tears (slow)
        world.spawnParticle(Particle.FALLING_OBSIDIAN_TEAR, loc, 8, 0.3, 0.2, 0.3, 0.02)

        // Sounds: deep implosion → crystal crack → chest break
        world.playSound(loc, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 0.9f)
        world.playSound(loc, Sound.BLOCK_AMETHYST_BLOCK_BREAK, 0.8f, 1.0f)
        world.playSound(loc, Sound.BLOCK_ENDER_CHEST_CLOSE, 0.7f, 1.0f)
    }


}