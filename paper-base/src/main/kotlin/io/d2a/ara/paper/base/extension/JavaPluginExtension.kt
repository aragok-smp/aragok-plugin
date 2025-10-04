package io.d2a.ara.paper.base.extension

import com.mojang.brigadier.tree.LiteralCommandNode
import io.d2a.ara.paper.base.commands.CommandBuilder
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Keyed
import org.bukkit.event.Listener
import org.bukkit.inventory.Recipe
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

fun <T : Listener> JavaPlugin.withListenerRegistration(listener: T): T = listener.also {
    server.pluginManager.registerEvents(it, this)
}

/**
 * Registers multiple crafting recipes at once.
 */
fun <T> JavaPlugin.registerRecipe(vararg recipe: T) where T : Recipe, T : Keyed =
    recipe.forEach {
        logger.info("Registering recipe: ${it.key}")
        server.addRecipe(it)
    }

/**
 * Registers multiple commands at once.
 */
fun JavaPlugin.registerCommands(vararg nodes: LiteralCommandNode<CommandSourceStack>) =
    withCommandRegistrar {
        nodes.forEach { node ->
            logger.info("Registering command: ${node.literal}")
            register(node)
        }
    }

/**
 * Registers multiple commands using command builders at once.
 */
fun JavaPlugin.registerCommands(vararg builders: CommandBuilder) =
    withCommandRegistrar {
        builders.forEach { builder ->
            val node = builder.build()
            val description = builder.description()
            logger.info("Registering command: ${node.literal} ($description)")
            register(node, description)
        }
    }

/**
 * Closes a [Closeable] resource quietly, logging any exceptions that occur during the close operation.
 */
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