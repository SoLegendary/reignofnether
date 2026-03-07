package com.solegendary.reignofnether.hud.passives;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.ability.abilities.EnchantMaiming;
import com.solegendary.reignofnether.ability.abilities.EnchantVigor;
import com.solegendary.reignofnether.registrars.EnchantmentRegistrar;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.List;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;



public class PassiveIcons {

    private static ItemStack getEnchantedItemStack(Item item) {
        ItemStack itemStack = new ItemStack(item);
        itemStack.enchant(Enchantments.UNBREAKING, 1);
        return itemStack;
    }

    public static final EnchantmentIcon MULTISHOT = new EnchantmentIcon(
            Enchantments.MULTISHOT,
            EquipmentSlot.MAINHAND,
            ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/icons/abilities/multishot.png"),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.multishot"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.multishot.tooltip"))
            )
    );
    public static final EnchantmentIcon QUICK_CHARGE = new EnchantmentIcon(
            Enchantments.QUICK_CHARGE,
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.CROSSBOW),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.quickshot"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.quickshot.tooltip"))
            )
    );
    public static final EnchantmentIcon MAIMING = new EnchantmentIcon(
            EnchantmentRegistrar.MAIMING.get(),
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.IRON_AXE),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.maiming"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.maiming.tooltip"))
            )
    );
    public static final EnchantmentIcon SHARPNESS = new EnchantmentIcon(
            Enchantments.SHARPNESS,
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.IRON_SWORD),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.sharpness"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.sharpness.tooltip"))
            )
    );
    public static final EnchantmentIcon VIGOR = new EnchantmentIcon(
            EnchantmentRegistrar.VIGOR.get(),
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.STICK),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.vigor"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.vigor.tooltip"))
            )
    );
    public static final EnchantmentIcon EFFICIENCY = new EnchantmentIcon(
            Enchantments.BLOCK_EFFICIENCY,
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.IRON_PICKAXE),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.efficiency"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.efficiency.tooltip"))
            )
    );
    public static final EnchantmentIcon FORTIFYING = new EnchantmentIcon(
            EnchantmentRegistrar.FORTYIFYING.get(),
            EquipmentSlot.CHEST,
            getEnchantedItemStack(Items.IRON_CHESTPLATE),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.fortifying"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.fortifying.tooltip"))
            )
    );
    public static final EnchantmentIcon POWER = new EnchantmentIcon(
            Enchantments.POWER_ARROWS,
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.BOW),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.power"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.power.tooltip"))
            )
    );
    public static final EnchantmentIcon ZEAL = new EnchantmentIcon(
            EnchantmentRegistrar.ZEAL.get(),
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.NETHER_STAR),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.zeal"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.zeal.tooltip"))
            )
    );
    public static final EnchantmentIcon PIERCING = new EnchantmentIcon(
            Enchantments.PIERCING,
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.ARROW),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.piercing"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.piercing.tooltip"))
            )
    );
    public static final EnchantmentIcon BREACHING = new EnchantmentIcon(
            EnchantmentRegistrar.BREACHING.get(),
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.DIAMOND_AXE),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.breaching"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.breaching.tooltip"))
            )
    );
    public static final EnchantmentIcon THORNS = new EnchantmentIcon(
            Enchantments.THORNS,
            EquipmentSlot.CHEST,
            getEnchantedItemStack(Items.CHAINMAIL_CHESTPLATE),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.thorns"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.thorns.tooltip"))
            )
    );
    public static final EnchantmentIcon FIRE_ASPECT = new EnchantmentIcon(
            Enchantments.FIRE_ASPECT,
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.NETHERITE_SWORD),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.fire_aspect"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.fire_aspect.tooltip"))
            )
    );
    public static final EnchantmentIcon FLAME = new EnchantmentIcon(
            Enchantments.FLAMING_ARROWS,
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.TRIDENT),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.flame"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.flame.tooltip"))
            )
    );

    public static final EnchantmentIcon ENCHANTMENT_AMPLIFIER = new EnchantmentIcon(
            null,
            EquipmentSlot.MAINHAND,
            getEnchantedItemStack(Items.ENCHANTED_BOOK),
            List.of(
                    fcs(I18n.get("hud.enchant.reignofnether.march_of_progress"), true),
                    fcs(I18n.get("hud.enchant.reignofnether.march_of_progress.tooltip"))
            )
    );

    public static final List<EnchantmentIcon> ENCHANTMENT_ICONS = List.of(
            MULTISHOT,
            QUICK_CHARGE,
            MAIMING,
            SHARPNESS,
            VIGOR,
            ZEAL,
            //EFFICIENCY,
            FORTIFYING,
            POWER,
            PIERCING,
            BREACHING,
            THORNS,
            FIRE_ASPECT,
            FLAME
    );
}
