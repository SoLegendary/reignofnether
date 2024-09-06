package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.resources.Resources;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class UnitSaveData extends SavedData {

    public final ArrayList<UnitSave> units = new ArrayList<>();

    private static UnitSaveData create() {
        return new UnitSaveData();
    }

    @Nonnull
    public static UnitSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld().getDataStorage().computeIfAbsent(UnitSaveData::load, UnitSaveData::create, "saved-unit-data");
    }

    public static UnitSaveData load(CompoundTag tag) {
        System.out.println("SavedUnitData.load");

        UnitSaveData data = create();
        ListTag ltag = (ListTag) tag.get("units");

        for (Tag ctag : ltag) {
            CompoundTag utag = (CompoundTag) ctag;

            String name = utag.getString("name");
            String ownerName = utag.getString("ownerName");
            int id = utag.getInt("id");
            Resources resources = new Resources("", utag.getInt("food"), utag.getInt("wood"), utag.getInt("ore"));

            data.units.add(new UnitSave(name, ownerName, id, resources));
            System.out.println("SavedUnitData.load: " + ownerName + "|" + name + "|" + id);
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        System.out.println("SavedUnitData.save");

        ListTag list = new ListTag();
        this.units.forEach(u -> {
            CompoundTag cTag = new CompoundTag();

            cTag.putString("name", u.name);
            cTag.putString("ownerName", u.ownerName);
            cTag.putInt("id", u.id);
            cTag.putInt("food", u.resources.food);
            cTag.putInt("wood", u.resources.wood);
            cTag.putInt("ore", u.resources.ore);
            list.add(cTag);

            System.out.println("SavedUnitData.save: " + u.ownerName + "|" + u.name + "|" + u.id);
        });
        tag.put("units", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
