package io.d2a.ara.paper.survival.enderchest

import io.d2a.ara.paper.base.extension.italic
import io.d2a.ara.paper.base.extension.setTrue
import io.d2a.ara.paper.base.extension.text
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemRarity
import org.bukkit.inventory.ItemStack

class EnderStorageItem {

    companion object {
        fun toItem() = ItemStack.of(Material.ENDER_CHEST).apply {
            itemMeta = itemMeta.apply {
                displayName("Resonant Ender Chest".text().italic(false))
                setRarity(ItemRarity.RARE)
                lore(
                    listOf(
                        "A dimensional anchor fused with".text(NamedTextColor.GRAY).italic(false),
                        "amethyst resonance and nether heat.".text(NamedTextColor.GRAY).italic(false),
                        "Linked to others by colored harmony.".text(NamedTextColor.GRAY).italic(false)
                    )
                )
                persistentDataContainer.setTrue(EnderStorageKeys.item)
            }
        }
    }

}