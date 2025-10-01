package io.d2a.ara.paper.survival.extensions

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

fun PersistentDataContainer.setString(key: NamespacedKey, value: String) =
    this.set(key, PersistentDataType.STRING, value)

fun PersistentDataContainer.setInt(key: NamespacedKey, value: Int) =
    this.set(key, PersistentDataType.INTEGER, value)