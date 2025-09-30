package io.d2a.ara.paper.base.flair.listener

import io.d2a.ara.paper.base.extension.toComponent
import io.d2a.ara.paper.base.flair.PrefixSuffixProvider
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

class InjectFlairToChatListener(
    val provider: PrefixSuffixProvider
) : Listener {

    @EventHandler
    fun onChat(event: AsyncChatEvent) {
        val prefix = provider.prefix(event.player).toComponent()
        val suffix = provider.suffix(event.player).toComponent()

        event.renderer { _, displayName, message, _ ->
            Component.empty()
                .append(prefix)
                .append(displayName.style { it.colorIfAbsent(prefix.color()) })
                .append(suffix)
                .append(Component.text(": "))
                .append(message)
        }
    }

}