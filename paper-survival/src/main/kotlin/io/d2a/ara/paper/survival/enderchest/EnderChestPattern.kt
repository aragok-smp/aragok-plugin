package io.d2a.ara.paper.survival.enderchest

import org.bukkit.DyeColor

class EnderChestPattern(
    val left: DyeColor,
    val middle: DyeColor,
    val right: DyeColor,
) {

    companion object {

        fun default(): EnderChestPattern = EnderChestPattern(
            DyeColor.WHITE,
            DyeColor.WHITE,
            DyeColor.WHITE,
        )

        fun fromKey(key: String): EnderChestPattern? {
            val parts = key.split("-")
            if (parts.size != 3) return null
            val left = DyeColor.entries.find { it.name == parts[0] } ?: return null
            val middle = DyeColor.entries.find { it.name == parts[1] } ?: return null
            val right = DyeColor.entries.find { it.name == parts[2] } ?: return null
            return EnderChestPattern(left, middle, right)
        }

    }

}

enum class PatternPart(val displayName: String) {
    LEFT("Left Stripe"),
    MIDDLE("Middle Stripe"),
    RIGHT("Right Stripe");

    companion object {
        fun fromIndex(index: Int): PatternPart? = when (index) {
            0 -> LEFT
            1 -> MIDDLE
            2 -> RIGHT
            else -> null
        }
    }
}