package com.solegendary.reignofnether.hud.effecticons;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.hud.buttons.Button;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProfessionIcon extends Button {

    public static final int ICON_SIZE = 8;
    public final VillagerUnitProfession profession;
    public final boolean veteran;

    public ProfessionIcon(VillagerUnitProfession profession, boolean veteran, ItemStack iconItem, @Nullable List<FormattedCharSequence> tooltipLines) {
        super("Profession Icon", ICON_SIZE, null, (Keybinding) null, () -> false, () -> false, () -> true, null, null, tooltipLines);
        this.iconItem = iconItem;
        this.profession = profession;
        this.veteran = veteran;
        this.iconItemScale = 0.75f;
        if (veteran) {
            this.frameResource = ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, "textures/hud/icon_frame_diamond.png");
        }
    }
}
