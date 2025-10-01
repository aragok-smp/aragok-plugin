package io.d2a.ara.paper.base.extension

import org.bukkit.NamespacedKey
import org.bukkit.block.TileState
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

/**
 * Sets a string value in the persistent data container.
 */
fun PersistentDataContainer.setString(key: NamespacedKey, value: String) =
    this.set(key, PersistentDataType.STRING, value)

/**
 * Gets a string value from the persistent data container.
 * @return The string value, or null if not present.
 */
fun PersistentDataContainer.getString(key: NamespacedKey): String? =
    this.get(key, PersistentDataType.STRING)

/**
 * Sets an integer value in the persistent data container.
 */
fun PersistentDataContainer.setInt(key: NamespacedKey, value: Int) =
    this.set(key, PersistentDataType.INTEGER, value)

/**
 * Gets an integer value from the persistent data container.
 * @return The integer value, or null if not present.
 */
fun PersistentDataContainer.getInt(key: NamespacedKey): Int? =
    this.get(key, PersistentDataType.INTEGER)

/**
 * Sets an enum value in the persistent data container by storing its name as a string.
 * @param value The enum value to store.
 */
fun PersistentDataContainer.setEnum(key: NamespacedKey, value: Enum<*>) =
    this.setString(key, value.name)

/**
 * Gets an enum value from the persistent data container by reading its name as a string.
 * @return The enum value, or null if not present or invalid.
 */
inline fun <reified T : Enum<T>> PersistentDataContainer.getEnum(key: NamespacedKey): T? {
    val name = this.getString(key) ?: return null
    return T::class.java.enumConstants.firstOrNull { it.name == name }
}

/**
 * Applies the given block to the tile state's persistent data container and updates the tile state.
 * @return True if the update was successful, null otherwise. This is useful for chaining calls.
 */
fun TileState.persist(block: PersistentDataContainer.() -> Unit): Boolean? {
    block(this.persistentDataContainer)
    if (this.update(true, false)) {
        return true
    }
    return null
}
