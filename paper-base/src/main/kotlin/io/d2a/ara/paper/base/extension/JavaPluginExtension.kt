package io.d2a.ara.paper.base.extension

import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.event.Listener
import org.bukkit.plugin.PluginManager
import org.bukkit.plugin.ServicePriority
import org.bukkit.plugin.java.JavaPlugin
import java.io.Closeable
import java.util.logging.Level

/**
 * Logs the error message and disables the plugin.
 */
fun JavaPlugin.disableWithError(message: String) {
    logger.severe(message)
    server.pluginManager.disablePlugin(this)
}

/**
 * Provides a way to register commands using the Paper command registrar system.
 */
fun JavaPlugin.withCommandRegistrar(registrar: Commands.() -> Unit) =
    lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { commands ->
        logger.info("Registering commands...")
        registrar(commands.registrar())
    }

/**
 * Provides a way to interact with the plugin manager.
 */
fun JavaPlugin.withPluginManager(manager: PluginManager.() -> Unit) =
    manager(server.pluginManager)

/**
 * Registers multiple event listeners at once.
 */
fun JavaPlugin.registerEvents(vararg listeners: Listener) =
    listeners.forEach { listener ->
        server.pluginManager.registerEvents(listener, this)
    }

fun JavaPlugin.closeQuietly(resource: Closeable?, name: String) =
    resource?.runCatching { close() }
        ?.onFailure { logger.log(Level.WARNING, "Failed to close $name", it) }

/**
 * Registers a service with the server's service manager.
 */
inline fun <reified S : Any> JavaPlugin.registerService(
    service: S,
    priority: ServicePriority = ServicePriority.Normal,
) {
    logger.info("Registering service: ${service.javaClass.name} with priority $priority")
    server.servicesManager.register(
        S::class.java,
        service,
        this,
        priority
    )
}

/**
 * Retrieves a service from the server's service manager.
 */
inline fun <reified S> JavaPlugin.getService(): S? =
    server.servicesManager.load(S::class.java)