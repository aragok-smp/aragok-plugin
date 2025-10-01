package io.d2a.ara.paper.survival.floo

import io.d2a.ara.paper.base.extension.noItalic
import io.d2a.ara.paper.survival.coal.NAMESPACE
import io.d2a.ara.paper.survival.extensions.setInt
import io.d2a.ara.paper.survival.extensions.setString
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.block.Campfire
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import java.util.concurrent.ThreadLocalRandom
import java.util.logging.Logger

class FlooListener(
    val logger: Logger
) : Listener {

    val glimmerItem: ItemStack by lazy { FlooItem.toGlimmersItem() }
    val unusedFlooItem: ItemStack by lazy { FlooItem.toUnusedPowderItem() }

    companion object {
        private val PLAIN = PlainTextComponentSerializer.plainText()

        // Soul fire name tag (on Campfire block state)
        val FLOO_SOUL_FIRE_NAME_KEY = NamespacedKey(NAMESPACE, "floo_soul_fire_name")

        // Powder destination tags
        val FLOO_POWDER_DESTINATION_KEY = NamespacedKey(NAMESPACE, "floo_powder_destination")
        val FLOO_POWDER_DESTINATION_X = NamespacedKey(NAMESPACE, "floo_powder_destination_x")
        val FLOO_POWDER_DESTINATION_Y = NamespacedKey(NAMESPACE, "floo_powder_destination_y")
        val FLOO_POWDER_DESTINATION_Z = NamespacedKey(NAMESPACE, "floo_powder_destination_z")

        const val DROP_CHANCE = 0.5f
    }

    @EventHandler
    fun onWitchDeath(event: EntityDeathEvent) {
        if (event.entity.type != EntityType.WITCH) return

        if (ThreadLocalRandom.current().nextFloat() < DROP_CHANCE) {
            event.drops.add(glimmerItem)
            // even more drops???
            if (ThreadLocalRandom.current().nextFloat() < 0.5) {
                event.drops.add(glimmerItem)
            }
        }
    }

    /**
     * When a soul campfire is placed with a custom name, we store that name inside the block's persistent data container.
     * This allows us to later identify the soul fire as a "Floo Network" connection
     */
    @EventHandler
    fun onSoulFirePlace(event: BlockPlaceEvent) {
        if (event.itemInHand.type != Material.SOUL_CAMPFIRE) return

        // make sure the soul campfire has a custom name
        if (!event.itemInHand.hasItemMeta()) return
        val name = event.itemInHand.itemMeta?.displayName() ?: return
        val plainName = plainTextComponentSerializer.serialize(name)
        if (plainName.isEmpty()) return

        val block = event.block

        // if the soul fire was renamed, it is probably a Floo-powered one,
        // so we store the name inside the soul fire block for later retrieval
        val blockState = block.state as? Campfire ?: return
        blockState.persistentDataContainer.setString(FLOO_SOUL_FIRE_NAME_KEY, plainName)
        if (!blockState.update()) {
            logger.info("Failed to update soul campfire block state with custom name: $plainName")
            return
        }

        // show some nice particles to indicate success
        block.world.spawnParticle(
            Particle.TOTEM_OF_UNDYING,
            block.location.add(0.5, 0.5, 0.5),
            20,
            0.3,
            0.3,
            0.3,
            0.1
        )

        logger.info("${event.player.name} placed a Floo Network soul fire with name: $plainName at ${block.location}")
    }

    fun getFlooNetworkName(block: Campfire): String? {
        // requirements fora floo network soul fire:
        // - must be a soul campfire
        // - must have a custom name (stored in PDC)
        // - must have a lodestone underneath
        if (block.type != Material.SOUL_CAMPFIRE) return null

        val belowBlock = block.location.block.getRelative(0, -1, 0)
        if (belowBlock.type != Material.LODESTONE) return null

        return block.persistentDataContainer.get(
            FLOO_SOUL_FIRE_NAME_KEY,
            PersistentDataType.STRING
        )
    }

    @EventHandler
    fun onSoulFireRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val hand = event.hand ?: return

        val block = event.clickedBlock ?: return
        if (block.type != Material.SOUL_CAMPFIRE) return

        // make sure we right-clicked with some unused floo powder
        val item = event.item ?: return
        if (!item.isSimilar(unusedFlooItem)) return

        val campfire = block.state as? Campfire ?: return

        // send a message to the player to indicate the connection status
        val flooNetworkName = getFlooNetworkName(campfire) ?: run {
            event.player.apply {
                sendRichMessage("<red>This soul fire is not connected to the Floo Network.")
                sendRichMessage("<red>Place a lodestone underneath and rename the soul fire.")
            }
            return
        }

        // convert the unused floo powder into regular floo powder
        // with information attached to the soul fire

        val powderItem = FlooItem.toPowderItem()
        powderItem.amount = item.amount

        powderItem.itemMeta = powderItem.itemMeta?.apply {
            lore(
                listOf(
                    Component.text("Destination: ")
                        .append(Component.text(flooNetworkName, NamedTextColor.YELLOW))
                        .noItalic()
                )
            )
            persistentDataContainer.apply {
                setString(FLOO_POWDER_DESTINATION_KEY, flooNetworkName)
                setInt(FLOO_POWDER_DESTINATION_X, block.x)
                setInt(FLOO_POWDER_DESTINATION_Y, block.y)
                setInt(FLOO_POWDER_DESTINATION_Z, block.z)
            }
        }
        event.player.inventory.setItem(hand, powderItem)

        block.world.spawnParticle(
            Particle.SOUL_FIRE_FLAME,
            block.location.add(0.5, 0.5, 0.5),
            20,
            0.3,
            0.3,
            0.3,
            0.1
        )
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        // make sure it's a floo powder item
        val item = event.itemDrop.itemStack
        if (!isPoweredFlooPowder(item)) return

        val pdc = item.itemMeta?.persistentDataContainer ?: run {
            logger.info("Floo powder has no persistent data container, ignoring")
            return
        }

        logger.info("${event.player.name} dropped floo powder at ${event.itemDrop.location}")

        // make sure the player is standing on a campfire
        val player = event.player
        val block = player.location.block

        // check preconditions for a floo network connection
        if (block.type != Material.SOUL_CAMPFIRE) {
            logger.info("But they are not standing on a soul campfire, ignoring")
            return
        }
        if (block.getRelative(0, -1, 0).type != Material.LODESTONE) {
            logger.info("But there is no lodestone underneath, ignoring")
            return
        }

        // get information about the connection
        val destinationName = pdc.get(FLOO_POWDER_DESTINATION_KEY, PersistentDataType.STRING)
            ?: return logger.info("Floo powder has no destination name, ignoring")
        val destinationX = pdc.get(FLOO_POWDER_DESTINATION_X, PersistentDataType.INTEGER)
            ?: return logger.info("Floo powder has no destination X, ignoring")
        val destinationY = pdc.get(FLOO_POWDER_DESTINATION_Y, PersistentDataType.INTEGER)
            ?: return logger.info("Floo powder has no destination Y, ignoring")
        val destinationZ = pdc.get(FLOO_POWDER_DESTINATION_Z, PersistentDataType.INTEGER)
            ?: return logger.info("Floo powder has no destination Z, ignoring")

        // get target soul fire and make sure it is valid and has the same name
        val destinationBlock = player.world.getBlockAt(destinationX, destinationY, destinationZ)
        val destinationCampfire = (destinationBlock.state as? Campfire) ?: run {
            player.sendRichMessage("<red>The destination was not found.")
            return logger.info("But the destination block is not a campfire, ignoring")
        }
        val destinationFlooName = getFlooNetworkName(destinationCampfire) ?: run {
            player.sendRichMessage("<red>The destination is not connected to the Floo Network.")
            return logger.info("But the destination campfire is not a floo network, ignoring")
        }
        if (destinationFlooName != destinationName) {
            player.sendRichMessage("<red>The destination Floo Network name does not match.")
            return logger.info("But the destination floo network name does not match, ignoring")
        }

        // now we can actually teleport the player :)
        // there should appear a lightning strike at the source and destination
        playTeleportEffects(block.location.add(0.5, 0.0, 0.5))
        playTeleportEffects(destinationBlock.location.add(0.5, 0.0, 0.5))

        player.teleport(destinationBlock.location.add(0.5, 1.0, 0.5))
        event.itemDrop.remove()
    }

    @EventHandler
    fun onSoulFireDamage(event: EntityDamageByBlockEvent) {
        val player = event.entity as? Player ?: return
        if (event.damager?.type != Material.SOUL_CAMPFIRE) return

        if (isPoweredFlooPowder(player.inventory.itemInMainHand) ||
            isPoweredFlooPowder(player.inventory.itemInOffHand)
        ) {
            event.isCancelled = true
        }
    }

    fun playTeleportEffects(location: Location) {
        location.world.strikeLightning(location)
        location.world.spawnParticle(
            Particle.SOUL_FIRE_FLAME,
            location.add(0.5, 0.5, 0.5),
            50,
            0.5,
            0.5,
            0.5,
            0.2
        )
    }

    fun isPoweredFlooPowder(stack: ItemStack?): Boolean {
        if (stack?.type != Material.STICK) return false
        if (!stack.hasItemMeta()) return false
        val itemModel = stack.itemMeta?.itemModel ?: return false
        return itemModel == FlooItem.POWDER_ITEM_MODEL
    }

}