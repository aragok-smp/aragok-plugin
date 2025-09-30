package io.d2a.ara.paper.base.extension

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

private val MINI_MESSAGE: MiniMessage = MiniMessage.miniMessage()
private val LEGACY_SERIALIZER: LegacyComponentSerializer = LegacyComponentSerializer.legacyAmpersand()

enum class TextFormat {
    LEGACY, MINI_MESSAGE
}

fun String.toComponent(format: TextFormat): Component = when (format) {
    TextFormat.LEGACY -> LEGACY_SERIALIZER.deserialize(this)
    TextFormat.MINI_MESSAGE -> MINI_MESSAGE.deserialize(this)
}

fun String.toComponent(): Component =
    if (this.contains('<', true))
        toComponent(TextFormat.MINI_MESSAGE)
    else
        toComponent(TextFormat.LEGACY)

fun Component.namedColor(): NamedTextColor? =
    this.color()?.let { NamedTextColor.nearestTo(it) }

fun Component.noItalic(): Component =
    this.decoration(TextDecoration.ITALIC, false)