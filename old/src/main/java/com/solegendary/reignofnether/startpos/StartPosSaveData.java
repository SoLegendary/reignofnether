package com.solegendary.reignofnether.startpos;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class StartPosSaveData extends SavedData {

    public final ArrayList<StartPos> startPoses = new ArrayList<>();

    private static StartPosSaveData create() {
        return new StartPosSaveData();
    }

    @Nonnull
    public static StartPosSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld()
                .getDataStorage()
                .computeIfAbsent(StartPosSaveData::load, StartPosSaveData::create, "saved-start-pos-data");
    }

    public static StartPosSaveData load(CompoundTag tag) {
        ReignOfNether.LOGGER.info("StartPosSaveData.load");

        StartPosSaveData data = create();
        ListTag ltag = (ListTag) tag.get("startPoses");

        if (ltag != null) {
            for (Tag ctag : ltag) {
                CompoundTag btag = (CompoundTag) ctag;
                int x = btag.getInt("x");
                int y = btag.getInt("y");
                int z = btag.getInt("z");
                int colorId = btag.getInt("colorId");
                data.startPoses.add(new StartPos(new BlockPos(x,y,z), colorId));
                ReignOfNether.LOGGER.info("StartPosSaveData.load: " + x + "," + y + "," + z + "|" + colorId);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        //ReignOfNether.LOGGER.info("StartPosSaveData.save");

        ListTag list = new ListTag();
        this.startPoses.forEach(sp -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putInt("x", sp.pos.getX());
            cTag.putInt("y", sp.pos.getY());
            cTag.putInt("z", sp.pos.getZ());
            cTag.putInt("colorId", sp.colorId);
            list.add(cTag);
            //ReignOfNether.LOGGER.info("StartPosSaveData.save: " + sp.pos.getX() + "," + sp.pos.getY() + "," + sp.pos.getZ() + "|" + sp.colorId);
        });
        tag.put("startPoses", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
