package com.solegendary.reignofnether.building.buildings.neutral;

import com.solegendary.reignofnether.building.buildings.placements.HealingFountainPlacement;
import org.joml.Vector3d;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.hud.AbilityButton;
import com.solegendary.reignofnether.keybinds.Keybinding;
import com.solegendary.reignofnether.research.ResearchClient;
import com.solegendary.reignofnether.resources.ResourceCost;
import com.solegendary.reignofnether.time.TimeClientEvents;
import com.solegendary.reignofnether.util.Faction;
import com.solegendary.reignofnether.util.MiscUtil;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;

import java.util.List;

import static com.solegendary.reignofnether.building.BuildingUtils.getAbsoluteBlockData;

public class HealingFountain extends Building {

    public final static String buildingName = "Healing Fountain";
    public final static String structureName = "healing_fountain";
    public final static ResourceCost cost = ResourceCost.Building(0,0,0,0);
    public HealingFountain() {
        super(structureName, cost, false);
        this.name = buildingName;
        this.portraitBlock = Blocks.ROSE_BUSH;
        this.icon = new ResourceLocation("minecraft", "textures/block/rose_bush_bottom.png");

        this.buildTimeModifier = 0.8f;

        this.startingBlockTypes.add(Blocks.POLISHED_ANDESITE);
        this.startingBlockTypes.add(Blocks.STONE_BRICK_STAIRS);
        this.startingBlockTypes.add(Blocks.GRASS_BLOCK);
        this.startingBlockTypes.add(Blocks.SPRUCE_TRAPDOOR);

        this.selfBuilding = true;
        this.capturable = false;
        this.invulnerable = true;
        this.shouldDestroyOnReset = false;
    }

    public Faction getFaction() {return Faction.NONE;}

    @Override
    public BuildingPlacement createBuildingPlacement(Level level, BlockPos pos, Rotation rotation, String ownerName) {
        return new HealingFountainPlacement(this, level, pos, rotation, ownerName, getAbsoluteBlockData(getRelativeBlockData(level), level, pos, rotation), false);
    }

    public AbilityButton getBuildButton(Keybinding hotkey) {
        return new AbilityButton(
            HealingFountain.buildingName,
            new ResourceLocation("minecraft", "textures/block/rose_bush_bottom.png"),
            hotkey,
            () -> BuildingClientEvents.getBuildingToPlace() == this,
            () -> false,
            () -> true,
            () -> BuildingClientEvents.setBuildingToPlace(this),
            null,
            List.of(
                FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.healing_fountain"), Style.EMPTY.withBold(true)),
                FormattedCharSequence.forward("", Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.healing_fountain.tooltip1"), Style.EMPTY),
                FormattedCharSequence.forward(I18n.get("buildings.neutral.reignofnether.healing_fountain.tooltip2"), Style.EMPTY)
            ),
            null
        );
    }
}
