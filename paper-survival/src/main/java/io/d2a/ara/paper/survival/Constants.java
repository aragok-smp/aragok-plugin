package io.d2a.ara.paper.survival;

import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import io.papermc.paper.registry.keys.tags.ItemTypeTagKeys;
import io.papermc.paper.registry.tag.TagKey;
import net.kyori.adventure.key.Key;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemType;

@SuppressWarnings("NullableProblems")
public class Constants {

    // Green Thumb

    public static final Key GREEN_THUMB = Key.key("aragok", "green_thumb");

    public static final TypedKey<Enchantment> GREEN_THUMB_TYPED_KEY =
            TypedKey.create(RegistryKey.ENCHANTMENT, Constants.GREEN_THUMB);

    public static final String GREEN_THUMB_ENCHANTMENT_KEY = "enchantment.green-thumb";

    // Telekinesis

    public static final Key TELEKINESIS = Key.key("aragok", "telekinesis");

    public static final TypedKey<Enchantment> TELEKINESIS_TYPED_KEY =
            TypedKey.create(RegistryKey.ENCHANTMENT, Constants.TELEKINESIS);

    public static final String TELEKINESIS_ENCHANTMENT_KEY = "enchantment.telekinesis";

    public static final Key ENCHANTABLE_WEAPON_MINING_KEY = Key.key("aragok", "enchantable_weapon_mining");
    public static final TagKey<ItemType> ENCHANTABLE_WEAPON_MINING = ItemTypeTagKeys.create(ENCHANTABLE_WEAPON_MINING_KEY);


}
