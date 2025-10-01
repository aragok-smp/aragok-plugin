package io.d2a.ara.paper.survival.floo

import io.d2a.ara.paper.base.custom.CustomItems
import io.d2a.ara.paper.base.custom.CustomItems.Companion.NAMESPACE
import io.d2a.ara.paper.base.extension.*
import net.kyori.adventure.sound.Sound.Emitter
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.*
import org.bukkit.block.Campfire
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByBlockEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.logging.Logger

class FlooUseListeners(
    val logger: Logger
) : Listener {


    companion object {
        private val PLAIN = PlainTextComponentSerializer.plainText()

        // Powder destination tags
        val FLOO_POWDER_DESTINATION_NAME = NamespacedKey(NAMESPACE, "floo_destination_name")
        val FLOO_POWDER_DESTINATION_X = NamespacedKey(NAMESPACE, "floo_destination_x")
        val FLOO_POWDER_DESTINATION_Y = NamespacedKey(NAMESPACE, "floo_destination_y")
        val FLOO_POWDER_DESTINATION_Z = NamespacedKey(NAMESPACE, "floo_destination_z")
        val FLOO_POWDER_DESTINATION_WORLD = NamespacedKey(NAMESPACE, "floo_destination_world")

        const val XP_PER_REGISTRATION = 0.5
    }

    fun isPoweredFlooPowder(stack: ItemStack?) = CustomItems.isCustomItem(stack, FlooItem.POWDER_ITEM_MODEL)
    fun isUnusedFlooPowder(stack: ItemStack?) = CustomItems.isCustomItem(stack, FlooItem.UNUSED_POWDER_ITEM_MODEL)

    /**
     * When a soul campfire is placed with a custom name, we store that name inside the block's persistent data container.
     * This allows us to later identify the soul fire as a "Floo Network" connection
     */
    @EventHandler
    fun onSoulFirePlace(event: BlockPlaceEvent) {
        if (event.itemInHand.type != Material.SOUL_CAMPFIRE) return
        if (!event.itemInHand.hasItemMeta()) return
        val meta = event.itemInHand.itemMeta ?: return
        val nameComponent = meta.displayName() ?: return
        val plainName = PLAIN.serialize(nameComponent)
        if (plainName.isEmpty()) return

        val state = event.block.state as? Campfire ?: return
        state.persist {
            setString(FLOO_POWDER_DESTINATION_NAME, plainName)
        } ?: return logger.info("Failed to persist floo soul fire name on place, aborting")

        val location = event.block.location.toCenterLocation()
        event.block.world.spawnParticle(Particle.TOTEM_OF_UNDYING, location, 20, 0.5, 0.5, 0.5, 0.1)

        logger.info("${event.player.name} placed a Floo Network soul fire with name: $plainName at $location")
    }

    /**
     * Checks whether the given location is a valid floo soul fire location
     * (soul campfire with lodestone underneath)
     * @param location the location to check
     * @return true if the location is a valid floo soul fire location, false otherwise
     */
    fun isValidFlooSoulFireLocation(location: Location): Boolean {
        val block = location.block
        if (block.type != Material.SOUL_CAMPFIRE) return false

        val belowBlock = block.getRelative(0, -1, 0)
        return belowBlock.type == Material.LODESTONE
    }

    @EventHandler
    fun onSoulFireRightClick(event: PlayerInteractEvent) {
        if (event.action != Action.RIGHT_CLICK_BLOCK) return
        val hand = event.hand ?: return
        val clicked = event.clickedBlock ?: return
        if (clicked.type != Material.SOUL_CAMPFIRE) return

        // make sure we right-clicked with some unused floo powder
        val item = event.item ?: return
        if (!isUnusedFlooPowder(item)) return

        val campfire = clicked.state as? Campfire ?: return
        val flooNetworkName = campfire.persistentDataContainer.getString(FLOO_POWDER_DESTINATION_NAME)
            ?: return event.player.fail("This soul fire is not connected to the Floo Network.")

        val currentLevel = event.player.level
        val cost = (XP_PER_REGISTRATION * item.amount).toInt().coerceAtLeast(1)
        if (currentLevel < cost) {
            val remaining = cost - currentLevel
            return event.player.fail("You need $cost levels to register this connection (need $remaining more).")
        }
        event.player.level = currentLevel - cost

        val powderItem = FlooItem.toPowderItem().apply {
            amount = item.amount
            itemMeta = itemMeta.apply {
                lore(
                    listOf(
                        Component.text("Destination: ", NamedTextColor.GRAY)
                            .append(Component.text(flooNetworkName, NamedTextColor.YELLOW))
                            .italic(false)
                    )
                )
                persistentDataContainer.apply {
                    setString(FLOO_POWDER_DESTINATION_NAME, flooNetworkName)
                    setInt(FLOO_POWDER_DESTINATION_X, clicked.x)
                    setInt(FLOO_POWDER_DESTINATION_Y, clicked.y)
                    setInt(FLOO_POWDER_DESTINATION_Z, clicked.z)
                    setString(FLOO_POWDER_DESTINATION_WORLD, clicked.world.uid.toString())
                }
            }
        }

        event.player.inventory.setItem(hand, powderItem)
        event.player.playSound(
            Sound.BLOCK_AMETHYST_BLOCK_CHIME.toAdventure()
                .volume(1f)
                .pitch(1.5f)
                .build(), Emitter.self()
        )

        val location = clicked.location.toCenterLocation()
        clicked.world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 30, 0.5, 0.5, 0.5, 0.1)
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        val item = event.itemDrop.itemStack
        // only allow dropping a single item
        if (item.amount != 1) return
        if (!isPoweredFlooPowder(item)) return

        val pdc = item.itemMeta?.persistentDataContainer ?: return
        val player = event.player

        if (!isValidFlooSoulFireLocation(player.location)) return

        // get information about the connection
        val destinationName = pdc.getString(FLOO_POWDER_DESTINATION_NAME) ?: return
        val destinationX = pdc.getInt(FLOO_POWDER_DESTINATION_X) ?: return
        val destinationY = pdc.getInt(FLOO_POWDER_DESTINATION_Y) ?: return
        val destinationZ = pdc.getInt(FLOO_POWDER_DESTINATION_Z) ?: return
        val destinationWorldUID = pdc.getString(FLOO_POWDER_DESTINATION_WORLD) ?: return

        val world = Bukkit.getWorld(UUID.fromString(destinationWorldUID))
            ?: return player.fail("The destination world was not found.")

        // get target soul fire and make sure it is valid and has the same name
        val destinationBlock = world.getBlockAt(destinationX, destinationY, destinationZ)
        val destinationCampfire = destinationBlock.state as? Campfire
            ?: return player.fail("The destination was not found.")
        val actualName = destinationCampfire.persistentDataContainer.getString(FLOO_POWDER_DESTINATION_NAME)
            ?: return player.fail("The destination is not connected to the Floo Network.")
        if (destinationName != actualName) {
            return player.fail("The destination has changed and is no longer valid.")
        }

        // we can remove this item since we are ready to teleport!
        event.itemDrop.setCanPlayerPickup(false)
        event.itemDrop.setCanMobPickup(false)

        val destinationLocation = destinationBlock.location.clone().add(0.5, 1.0, 0.5)

        player.sendActionBar(
            Component.text("Preparing to teleport to ", NamedTextColor.GRAY)
                .append(Component.text(destinationName, NamedTextColor.YELLOW))
                .append(Component.text("...", NamedTextColor.GRAY))
        )

        player.teleportAsync(destinationLocation)
            .thenAccept { result ->
                if (result) {
                    event.itemDrop.remove() // remove the dropped item

                    player.sendActionBar(
                        Component.text("Teleported to ", NamedTextColor.GRAY)
                            .append(Component.text(destinationName, NamedTextColor.YELLOW))
                    )

                    playTeleportEffects(player.location)
                    playTeleportEffects(destinationLocation)
                } else {
                    // allow the player to pick the item back up
                    event.itemDrop.setCanPlayerPickup(true)
                    event.itemDrop.setCanMobPickup(true)

                    player.sendActionBar(
                        Component.text("Teleportation to ", NamedTextColor.RED)
                            .append(Component.text(destinationName, NamedTextColor.YELLOW))
                            .append(Component.text(" failed.", NamedTextColor.RED))
                    )
                }
            }
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
        location.world.spawnParticle(Particle.SOUL_FIRE_FLAME, location, 50, 0.5, 0.5, 0.5, 0.2)
    }

}