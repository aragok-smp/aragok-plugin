package io.d2a.ara.paper.survival.enderchest

import org.bukkit.NamespacedKey
import org.bukkit.plugin.Plugin

object EnderStorageKeys {

    // Stripe data
    lateinit var left: NamespacedKey
    lateinit var middle: NamespacedKey
    lateinit var right: NamespacedKey
    lateinit var locked: NamespacedKey

    // UUIDs of visuals
    lateinit var leftDisplay: NamespacedKey
    lateinit var middleDisplay: NamespacedKey
    lateinit var rightDisplay: NamespacedKey
    lateinit var leftHitbox: NamespacedKey
    lateinit var middleHitbox: NamespacedKey
    lateinit var rightHitbox: NamespacedKey

    // Hitbox metadata
    lateinit var hitboxStripeIndex: NamespacedKey
    lateinit var hitboxOwner: NamespacedKey

    fun init(plugin: Plugin) {
        fun k(id: String) = NamespacedKey(plugin, id)

        left = k("ender_left_key")
        middle = k("ender_middle_key")
        right = k("ender_right_key")
        locked = k("ender_locked")

        leftDisplay = k("ender_left_display")
        middleDisplay = k("ender_middle_display")
        rightDisplay = k("ender_right_display")
        leftHitbox = k("ender_left_hitbox")
        middleHitbox = k("ender_middle_hitbox")
        rightHitbox = k("ender_right_hitbox")

        hitboxStripeIndex = k("ender_index")
        hitboxOwner = k("ender_owner")
    }

}