package io.d2a.ara.paper.base.configuration

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.Plugin
import java.io.File
import java.util.logging.Level

class Configuration(
    plugin: Plugin,
    val name: String,
    val autoSave: Boolean = false,
) {

    private val file: File = plugin.dataFolder
        .also { it.mkdirs() } // create the plugin folder if it doesn't exist
        .resolve(name)
        .apply {
            if (!exists()) {
                runCatching {
                    createNewFile()
                }.onFailure {
                    plugin.logger.log(Level.SEVERE, "Failed to create file $name", it)
                }
            }
        }

    val state: YamlConfiguration = YamlConfiguration.loadConfiguration(file)

    // delegate functions
    fun getString(path: String, default: String? = null): String? = state.getString(path, default)
    fun getInt(path: String, default: Int = 0): Int = state.getInt(path, default)
    fun getBoolean(path: String, default: Boolean = false): Boolean = state.getBoolean(path, default)

    operator fun get(path: String): Any? = state.get(path)
    operator fun set(path: String, value: Any?) {
        state.set(path, value)
        if (autoSave) {
            save()
        }
    }

    /**
     * Gets a value from the configuration and casts it to the specified type [T].
     * If the value is not present, returns the provided [default].
     */
    inline fun <reified T> get(path: String, default: T? = null): T? =
        state.get(path, default) as? T ?: default

    /**
     * Gets a value from the configuration and casts it to the specified type [T].
     * If the value is not present, sets it to the provided [default] and returns
     * that.
     */
    inline fun <reified T> getOrSet(path: String, default: T): T {
        val existing = get<T>(path)
        if (existing != null) {
            return existing
        }
        set(path, default)
        return default
    }

    /**
     * Gets a value from the configuration and casts it to the specified type [T].
     * If the value is not present, calls the [default] function to obtain a default
     * value, sets it in the configuration, and returns it.
     */
    inline fun <reified T> getOrSetLazy(path: String, default: () -> T): T {
        val existing = get<T>(path)
        if (existing != null) {
            return existing
        }
        val value = default()
        set(path, value)
        return value
    }

    fun save() = state.save(file)

    fun <T> bind(path: String, default: T? = null): Bind<T> =
        Bind(this, path, default)

}