package io.d2a.ara.paper.survival.enderchest

import io.d2a.ara.paper.base.extension.*
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.DyeColor
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.block.EnderChest
import org.bukkit.entity.Interaction
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.logging.Logger

class EnderChestUseListener(
    val logger: Logger,
    val storage: EnderChestStorage
) : Listener {

    companion object {
        val PLAIN = PlainTextComponentSerializer.plainText()
    }

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

    fun updateStripeColor(enderChest: EnderChest, index: Int, stack: ItemStack, key: String) {
        val world = enderChest.world

        val displayUuid = when (index) {
            0 -> enderChest.persistentDataContainer.getString(EnderStorageKeys.leftDisplay)
            1 -> enderChest.persistentDataContainer.getString(EnderStorageKeys.middleDisplay)
            2 -> enderChest.persistentDataContainer.getString(EnderStorageKeys.rightDisplay)
            else -> null
        } ?: return

        val display = world.getEntity(UUID.fromString(displayUuid)) as? ItemDisplay ?: return
        display.setItemStack(stack)

        // also update the stored pattern in the ender chest
        when (index) {
            0 -> enderChest.persist { setString(EnderStorageKeys.left, key) }
            1 -> enderChest.persist { setString(EnderStorageKeys.middle, key) }
            2 -> enderChest.persist { setString(EnderStorageKeys.right, key) }
        }
    }

    // re-color the pattern
    @EventHandler(
        priority = EventPriority.HIGHEST,
        ignoreCancelled = true
    )
    fun onStripeClick(event: PlayerInteractAtEntityEvent) {
        val interaction = event.rightClicked as? Interaction ?: return
        val chestBlock = interaction.location.block
        if (chestBlock.type != Material.ENDER_CHEST) return
        val enderChest = chestBlock.state as? EnderChest ?: return

        val pdc = interaction.persistentDataContainer
        val patternPartIndex = pdc.getInt(EnderStorageKeys.hitboxStripeIndex) ?: return

        event.isCancelled = true

        val player = event.player
        if (!player.isSneaking) return

        // prevent re-coloring when locked
        // this should prevent abuse => infinite inventory space when re-coloring
        if (enderChest.persistentDataContainer.isTrue(EnderStorageKeys.locked)) {
            player.failActionBar(
                "This Ender Storage is locked.",
                sound = Sound.BLOCK_SNIFFER_EGG_PLOP.toAdventure()
                    .volume(1.0f)
                    .pitch(2.0f)
                    .build()
            )
            return
        }

        val item = player.inventory.itemInMainHand
        val dyeColor = materialToDye[item.type] ?: return
        val glassItem = coloredGlassByDye[dyeColor] ?: return

        updateStripeColor(enderChest, patternPartIndex, glassItem, dyeColor.name)

        enderChest.world.playSound(
            enderChest.location,
            Sound.AMBIENT_UNDERWATER_ENTER,
            1.0f, 2.0f
        )

        // consume the item
        if (item.amount > 1) {
            item.amount -= 1
        } else {
            player.inventory.setItemInMainHand(null)
        }

        logger.info("player ${player.name} re-colored ender chest at ${enderChest.location} stripe index $patternPartIndex to color ${dyeColor.name}")
    }

    fun patternKeyOf(vararg keys: String): String = keys.joinToString("-")

    @EventHandler(ignoreCancelled = true)
    fun onChestOpen(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return
        if (event.action != Action.RIGHT_CLICK_BLOCK) return

        val block = event.clickedBlock ?: return
        if (block.type != Material.ENDER_CHEST) return
        val enderChest = block.state as? EnderChest ?: return

        val patternKeyLeft = enderChest.persistentDataContainer.getString(EnderStorageKeys.left) ?: return
        val patternKeyMiddle = enderChest.persistentDataContainer.getString(EnderStorageKeys.middle) ?: return
        val patternKeyRight = enderChest.persistentDataContainer.getString(EnderStorageKeys.right) ?: return
        val key = patternKeyOf(patternKeyLeft, patternKeyMiddle, patternKeyRight)

        event.isCancelled = true

        if (event.player.isSneaking) return

        event.player.successActionBar("...")
        val sharedInventory = storage.getOrLoad(key)

        event.player.openInventory(sharedInventory)
        enderChest.world.playSound(enderChest.location, Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 0.9f)

        enderChest.persistentDataContainer.apply {
            // since the player tried to open the ender chest it is considered "complete",
            // so to prevent re-colorings in the future, we lock it
            if (!isTrue(EnderStorageKeys.locked)) {
                setTrue(EnderStorageKeys.locked)
                enderChest.update(true, false)

                enderChest.world.playSound(enderChest.location, Sound.BLOCK_CHEST_LOCKED, 1.0f, 1.0f)
                enderChest.world.spawnParticle(Particle.END_ROD, enderChest.location, 20, 0.5, 0.5, 0.5, 0.0)

                event.player.successActionBar("This Ender Chest is now locked.")

                logger.info("Player ${event.player.name} locked ender chest at ${enderChest.location} with pattern $key")
            }
        }
    }

    @EventHandler
    fun onEnderChestClose(event: InventoryCloseEvent) {
        val title = PLAIN.serialize(event.view.title())
        if (!title.startsWith(EnderChestStorage.INVENTORY_PREFIX)) return

        val key = title.removePrefix(EnderChestStorage.INVENTORY_PREFIX).trim()
        if (key.isEmpty()) return

        storage.markDirty(key)
        logger.info("Marked ender chest channel '$key' as dirty for saving.")
    }

}