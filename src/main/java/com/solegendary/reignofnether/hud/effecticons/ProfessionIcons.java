package com.solegendary.reignofnether.hud.effecticons;

import com.solegendary.reignofnether.unit.units.villagers.VillagerUnitProfession;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;

import static com.solegendary.reignofnether.util.MiscUtil.fcs;

public class ProfessionIcons {

    private static ItemStack glintingStack(Item item) {
        ItemStack itemStack = new ItemStack(item);
        itemStack.enchant(Enchantments.UNBREAKING, 1);
        return itemStack;
    }

    private static ProfessionIcon create(VillagerUnitProfession profession, boolean veteran, ItemStack stack) {
        String key = "units.reignofnether." + (veteran ? "veteran_" : "")
                + profession.name().toLowerCase();
        return new ProfessionIcon(
                profession,
                veteran,
                stack,
                List.of(
                        fcs(I18n.get(key), true),
                        fcs(I18n.get(key + ".tooltip"))
                )
        );
    }

    public static final ProfessionIcon FARMER = create(VillagerUnitProfession.FARMER, false, new ItemStack(Items.IRON_HOE));
    public static final ProfessionIcon VETERAN_FARMER = create(VillagerUnitProfession.FARMER, true, glintingStack(Items.DIAMOND_HOE));

    public static final ProfessionIcon LUMBERJACK = create(VillagerUnitProfession.LUMBERJACK, false, new ItemStack(Items.IRON_AXE));
    public static final ProfessionIcon VETERAN_LUMBERJACK = create(VillagerUnitProfession.LUMBERJACK, true, glintingStack(Items.DIAMOND_AXE));

    public static final ProfessionIcon MINER = create(VillagerUnitProfession.MINER, false, new ItemStack(Items.IRON_PICKAXE));
    public static final ProfessionIcon VETERAN_MINER = create(VillagerUnitProfession.MINER, true, glintingStack(Items.DIAMOND_PICKAXE));

    public static final ProfessionIcon MASON = create(VillagerUnitProfession.MASON, false, new ItemStack(Items.IRON_SHOVEL));
    public static final ProfessionIcon VETERAN_MASON = create(VillagerUnitProfession.MASON, true, glintingStack(Items.DIAMOND_SHOVEL));

    public static final ProfessionIcon HUNTER = create(VillagerUnitProfession.HUNTER, false, new ItemStack(Items.IRON_SWORD));
    public static final ProfessionIcon VETERAN_HUNTER = create(VillagerUnitProfession.HUNTER, true, glintingStack(Items.DIAMOND_SWORD));

    @Nullable
    public static ProfessionIcon getProfessionIcon(VillagerUnitProfession profession, boolean veteran) {
        return switch (profession) {
            case FARMER -> veteran ? VETERAN_FARMER : FARMER;
            case LUMBERJACK -> veteran ? VETERAN_LUMBERJACK : LUMBERJACK;
            case MINER -> veteran ? VETERAN_MINER : MINER;
            case MASON -> veteran ? VETERAN_MASON : MASON;
            case HUNTER -> veteran ? VETERAN_HUNTER : HUNTER;
            case NONE -> null;
        };
    }
}