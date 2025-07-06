package com.solegendary.reignofnether.unit;

import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class HeroUnitSaveData extends SavedData {

    public final ArrayList<HeroUnitSave> heroUnits = new ArrayList<>();

    private static HeroUnitSaveData create() {
        return new HeroUnitSaveData();
    }

    @Nonnull
    public static HeroUnitSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld()
            .getDataStorage()
            .computeIfAbsent(HeroUnitSaveData::load, HeroUnitSaveData::create, "saved-herounit-data");
    }

    public static HeroUnitSaveData load(CompoundTag tag) {
        ReignOfNether.LOGGER.info("HeroUnitSaveData.load");

        HeroUnitSaveData data = create();
        ListTag ltag = (ListTag) tag.get("heroUnits");

        if (ltag != null) {
            for (Tag ctag : ltag) {
                CompoundTag htag = (CompoundTag) ctag;

                String uuid = htag.getString("uuid");
                String name = htag.getString("name");
                String ownerName = htag.getString("ownerName");
                int experience = htag.getInt("experience");
                int skillPoints = htag.getInt("skillPoints");
                int charges = htag.getInt("charges");
                int ability1Rank = htag.getInt("ability1Rank");
                int ability2Rank = htag.getInt("ability2Rank");
                int ability3Rank = htag.getInt("ability3Rank");
                int ability4Rank = htag.getInt("ability4Rank");

                data.heroUnits.add(new HeroUnitSave(uuid, name, ownerName, experience, skillPoints, charges, ability1Rank, ability2Rank, ability3Rank, ability4Rank));
                ReignOfNether.LOGGER.info("HeroUnitSaveData.load: " + uuid + "|" + experience + "|" + skillPoints + "|" + charges + "|" + ability1Rank + "|" + ability2Rank + "|" + ability3Rank + "|" + ability4Rank);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        //ReignOfNether.LOGGER.info("UnitSaveData.save");

        ListTag list = new ListTag();
        this.heroUnits.forEach(h -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("uuid", h.uuid);
            cTag.putString("name", h.name);
            cTag.putString("ownerName", h.ownerName);
            cTag.putInt("experience", h.experience);
            cTag.putInt("skillPoints", h.skillPoints);
            cTag.putInt("charges", h.charges);
            cTag.putInt("ability1Rank", h.ability1Rank);
            cTag.putInt("ability2Rank", h.ability2Rank);
            cTag.putInt("ability3Rank", h.ability3Rank);
            cTag.putInt("ability4Rank", h.ability4Rank);
            list.add(cTag);
        });
        tag.put("heroUnits", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }
}
