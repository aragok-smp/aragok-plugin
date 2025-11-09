package io.d2a.ara.paper.survival.telekinesis

import io.d2a.ara.paper.survival.Constants
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.inventory.ItemStack

object TelekinesisUtil {

    private val registryAccess = RegistryAccess
        .registryAccess()
        .getRegistry(RegistryKey.ENCHANTMENT)

    val telekinesisEnchantment = registryAccess
        .get(Constants.TELEKINESIS_TYPED_KEY)

    /**
     * Returns the level of the telekinesis enchantment on the given tool,
     * or null if the player is not in survival mode or the tool is null.
     */
    fun getTelekinesisLevel(player: Player, tool: ItemStack?): Int? {
        if (player.gameMode != GameMode.SURVIVAL || tool == null) {
            return null
        }
        return tool.enchantments[telekinesisEnchantment]
    }

    fun pickupFromItemEntity(player: Player, item: Item): ItemStack? {
        val stack = item.itemStack
        if (stack.isEmpty) return null

        // we call the pickup event here to allow other plugins to cancel it (for some reason)
        @Suppress("UnstableApiUsage") val attempt = EntityPickupItemEvent(
            player,
            item,
            stack.amount
        )
        Bukkit.getPluginManager().callEvent(attempt)
        if (attempt.isCancelled) return stack

        val leftover = player.inventory.addItem(stack)
        if (leftover.isEmpty()) {
            playPickupAnimationUsingClone(player, stack, item.location)
            return null
        }

        val remainder = leftover.values.first()
        val pickedUp = stack.amount - remainder.amount
        if (pickedUp > 0) {
            playPickupAnimationUsingClone(player, stack.clone().apply {
                amount = pickedUp
            }, item.location)
        }
        return remainder
    }

    /**
     * Spawns a fake item stack at the given location that is invisible and cannot be picked up.
     * This is used to play the pickup animation without actually dropping an item.
     *
     * Also, we need this when using block break, because the item entity does not exist yet.
     */
    fun createFakeItemStack(original: ItemStack, at: Location): Item {
        val world = at.world ?: throw IllegalStateException("Location has no world: $at")
        return world.spawn(at, Item::class.java) { spawned ->
            spawned.itemStack = original.clone()
            spawned.isInvisible = true
            spawned.setWillAge(false)
            spawned.setCanMobPickup(false)
            spawned.setCanPlayerPickup(false)
        }
    }

    fun playPickupAnimationUsingClone(player: Player, original: ItemStack, at: Location) {
        if (original.isEmpty) return

        val fakeItem = createFakeItemStack(original, at)
        try {
            player.playPickupItemAnimation(fakeItem, original.amount)
            TelekinesisEffects.attractRibbonToPlayer(fakeItem.location, player)
        } finally {
            fakeItem.remove()
        }
    }

    fun pickupStack(player: Player, original: ItemStack, at: Location): ItemStack? {
        if (original.isEmpty) return null

        val fakeItem = createFakeItemStack(original, at)
        try {
            return pickupFromItemEntity(player, fakeItem)
        } finally {
            fakeItem.remove()
        }
    }

}