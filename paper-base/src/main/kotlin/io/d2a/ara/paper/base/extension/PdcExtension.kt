package io.d2a.ara.paper.base.extension

import io.papermc.paper.persistence.PersistentDataContainerView
import org.bukkit.NamespacedKey
import org.bukkit.block.TileState
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType
import java.util.*

/**
 * Sets a string value in the persistent data container.
 * @param value The string value to set.
 */
fun PersistentDataContainer.setString(key: NamespacedKey, value: String) =
    this.set(key, PersistentDataType.STRING, value)

/**
 * Gets a string value from the persistent data container.
 * @return The string value, or null if not present.
 */
fun PersistentDataContainer.getString(key: NamespacedKey): String? =
    this.get(key, PersistentDataType.STRING)

fun PersistentDataContainerView.getString(key: NamespacedKey): String? =
    this.get(key, PersistentDataType.STRING)

fun PersistentDataContainer.getUniqueId(key: NamespacedKey): UUID? =
    this.get(key, PersistentDataType.STRING)?.let { UUID.fromString(it) }

fun PersistentDataContainer.setUniqueId(key: NamespacedKey, value: UUID) =
    this.setString(key, value.toString())

/**
 * Sets an integer value in the persistent data container.
 * @param value The integer value to set.
 */
fun PersistentDataContainer.setInt(key: NamespacedKey, value: Int) =
    this.set(key, PersistentDataType.INTEGER, value)

/**
 * Gets an integer value from the persistent data container.
 * @return The integer value, or null if not present.
 */
fun PersistentDataContainer.getInt(key: NamespacedKey): Int? =
    this.get(key, PersistentDataType.INTEGER)

fun PersistentDataContainerView.getInt(key: NamespacedKey): Int? =
    this.get(key, PersistentDataType.INTEGER)

/**
 * Sets a byte value in the persistent data container.
 * @param value The byte value to set.
 */
fun PersistentDataContainer.setByte(key: NamespacedKey, value: Byte) =
    this.set(key, PersistentDataType.BYTE, value)

/**
 * Gets a byte value from the persistent data container.
 * @return The byte value, or null if not present.
 */
fun PersistentDataContainer.getByte(key: NamespacedKey): Byte? =
    this.get(key, PersistentDataType.BYTE)

fun PersistentDataContainerView.getByte(key: NamespacedKey): Byte? =
    this.get(key, PersistentDataType.BYTE)

/**
 * Gets a boolean value from the persistent data container.
 * @return The boolean value, or null if not present.
 */
fun PersistentDataContainer.setBoolean(key: NamespacedKey, value: Boolean) =
    this.setByte(key, if (value) 1 else 0)

fun PersistentDataContainer.setTrue(key: NamespacedKey) = this.setByte(key, 1)
fun PersistentDataContainer.setFalse(key: NamespacedKey) = this.setByte(key, 0)

/**
 * Gets a boolean value from the persistent data container.
 * @return The boolean value, or null if not present.
 */
fun PersistentDataContainer.getBoolean(key: NamespacedKey): Boolean? =
    this.getByte(key)?.let { it != 0.toByte() }

fun PersistentDataContainerView.getBoolean(key: NamespacedKey): Boolean? =
    this.getByte(key)?.let { it != 0.toByte() }

fun PersistentDataContainer.isTrue(key: NamespacedKey): Boolean = this.getBoolean(key) == true
fun PersistentDataContainer.isFalse(key: NamespacedKey): Boolean = this.getBoolean(key) == false

fun PersistentDataContainerView.isTrue(key: NamespacedKey): Boolean = this.getBoolean(key) == true
fun PersistentDataContainerView.isFalse(key: NamespacedKey): Boolean = this.getBoolean(key) == false


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

inline fun <reified T : Enum<T>> PersistentDataContainerView.getEnum(key: NamespacedKey): T? {
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
