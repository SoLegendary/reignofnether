package com.solegendary.reignofnether.building;

import net.minecraft.ResourceLocationException;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.ArrayList;
import java.util.Optional;

public class Building {

    // players shouldn't have to destroy every single block so building collapses at a certain % blocks remaining
    public String structureName;
    public final float minBlocksPercent = 0.25f;
    public int health;
    public int maxHealth;
    public boolean isBuilt; // set true when health reaches 100% the first time
    public boolean isBuilding; // a builder is assigned and actively building or repairing
    public float buildRate; // rate at which health increases each tick when building or repairing
    // chance for a mini explosion to destroy extra blocks if a player is breaking it
    // should be higher for large fragile buildings so players don't take ages to destroy it
    public float explodeChance;
    public ArrayList<BuildingBlock> blocks = new ArrayList<>();
    public ArrayList<BlockState> palette = new ArrayList<>();

    private class BuildingBlock {
        public BlockPos blockPos;
        public Integer paletteIndex;
        public Boolean isPlaced = false;

        public BuildingBlock(BlockPos blockPos, Integer paletteIndex) {
            this.blockPos = blockPos;
            this.paletteIndex = paletteIndex;
        }

        public BlockState getBlockState() {
            return palette.get(paletteIndex);
        }

        public void place() {

        }

        public void destroy() {

        }
    }

    public Building(String structureName) {
        this.structureName = structureName;
    }

    public void loadBlocks(Minecraft MC) {
        System.out.println("loading NBT for: " + structureName);
        try {
            ResourceLocation fullRl = new ResourceLocation("reignofnether", "structures/" + structureName + ".nbt");
            Resource rs = MC.resourceManager.getResource(fullRl);
            CompoundTag nbt = NbtIo.readCompressed(rs.getInputStream());

            // load in palette (list of unique blockstates)
            ListTag paletteNbt = nbt.getList("palette", 10);
            for(int i = 0; i < paletteNbt.size(); ++i)
                palette.add(NbtUtils.readBlockState(paletteNbt.getCompound(i)));

            // load in blocks (list of blockPos and their palette index)
            ListTag blocksNbt = nbt.getList("blocks", 10);
            for(int i = 0; i < blocksNbt.size(); ++i)
                blocks.add(new BuildingBlock(
                        NbtUtils.readBlockPos(blocksNbt.getCompound(i)),
                        blocksNbt.getCompound(i).getInt("state")
                ));
        }
        catch (Exception e) {
            System.out.println(e);
        }
    }

    private StructureTemplate getTemplate(ServerLevel serverLevel)  {
        Optional<StructureTemplate> optional;
        StructureTemplate template;
        ResourceLocation rl = ResourceLocation.tryParse(structureName);
        try {
            optional = serverLevel.getStructureManager().get(rl);
            template = optional.orElse(null);
        } catch (ResourceLocationException resourcelocationexception) {
            template = null;
        }
        if (template == null)
            throw new Error("Failed to initialise structure: " + structureName);

        return template;
    }

    public float getBlocksPercent() {
        return 1f;
    }

    public float getHealthPercent() {
        return ((float) health / (float) maxHealth);
    }

    public boolean isFunctional() {
        return this.isBuilt && this.getHealthPercent() >= 0.5f;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent evt) {
        // when a player breaks a block that's part of the building:
        // - roll explodeChance to cause explosion effects and destroy more blocks
        // - cause fire if < 50% health
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent evt) {
        // match healthPercent to (blocksBuiltPercent + minBlocksPercent)

        // if health <= 0: destroy the building in big explosion

        // if builder is assigned, increase health by buildRate
        // if fires exist, put them out one by one (or remove them all if healthPercent > 50%)

        // calculate number of blocks to place based on new healthPercent and blocksBuiltPercent
        // eg. if there are 100/200 blocks built, and health raised from 50% -> 60%, place 20 blocks to match

        // place blocks as required from bottom to top (obeying gravity if possible)

    }
}
