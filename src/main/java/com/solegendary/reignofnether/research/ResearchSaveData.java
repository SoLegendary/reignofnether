package com.solegendary.reignofnether.research;

import com.mojang.datafixers.util.Pair;
import com.solegendary.reignofnether.ReignOfNether;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nonnull;
import java.util.ArrayList;

public class ResearchSaveData extends SavedData {

    public final ArrayList<Pair<String, ResourceLocation>> researchItems = new ArrayList<>();

    private static ResearchSaveData create() {
        return new ResearchSaveData();
    }

    @Nonnull
    public static ResearchSaveData getInstance(LevelAccessor level) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return create();
        }
        return server.overworld()
            .getDataStorage()
            .computeIfAbsent(ResearchSaveData::load, ResearchSaveData::create, "saved-research-data");
    }

    public static ResearchSaveData load(CompoundTag tag) {
        ReignOfNether.LOGGER.info("ResearchSaveData.load");

        ResearchSaveData data = create();
        ListTag ltag = (ListTag) tag.get("researchItems");

        if (ltag != null) {
            for (Tag ctag : ltag) {
                CompoundTag btag = (CompoundTag) ctag;
                String ownerName = btag.getString("ownerName");
                ResourceLocation researchKey;
                if (btag.contains("researchKey")) {
                    researchKey = ResourceLocation.tryParse(btag.getString("researchKey"));
                }else {
                    String researchName = btag.getString("researchName");
                    researchKey = translateOldData(researchName);
//                    researchKey = new ResourceLocation(ReignOfNether.MOD_ID, researchName.toLowerCase().replace(' ', '_'));
                }
                data.researchItems.add(new Pair<>(ownerName, researchKey));
                ReignOfNether.LOGGER.info("ResearchSaveData.load: " + ownerName + "|" + researchKey.toString());
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        //ReignOfNether.LOGGER.info("ResearchSaveData.save");

        ListTag list = new ListTag();
        this.researchItems.forEach(b -> {
            CompoundTag cTag = new CompoundTag();
            cTag.putString("ownerName", b.getFirst());
            cTag.putString("researchKey", b.getSecond().toString());
            list.add(cTag);
            //ReignOfNether.LOGGER.info("ResearchSaveData.save: " + b.getFirst() + "|" + b.getSecond());
        });
        tag.put("researchItems", list);
        return tag;
    }

    public void save() {
        this.setDirty();
    }

    private static ResourceLocation translateOldData(String researchName) {
        String newName = switch (researchName) {
            case "Iron Beacon": yield "beacon_level_1";
            case "Gold Beacon": yield "beacon_level_2";
            case "Emerald Beacon": yield "beacon_level_3";
            case "Diamond Beacon": yield "beacon_level_4";
            case "Netherite Beacon": yield "beacon_level_5";
            case "Walls of Fire": yield "blaze_firewall";
            case "Shield Tactics": yield "brute_shields";
            case "Officer's Quarters": yield "castle_flag";
            case "Drowned Zombies": yield "drowned";
            case "Vexing Summons": yield "evoker_vexes";
            case "Husk Zombies": yield "husks";
            case "Lightning Rod": yield "lab_lightning_rod";
            case "Multishot Crossbows": yield "pillager_crossbows";
            case "Civilian Portal": yield "portal_for_civilian";
            case "Military Portal": yield "portal_for_military";
            case "Transport Portal": yield "portal_for_transport";
            case "Ravager Artillery": yield "ravager_cavalry";
            case "Worker Carry Bags": yield "resource_capacity";
            case "Infested Defences": yield "silverfish";
            case "Slimy Conversion": yield "slime_conversion";
            case "Sticky Webbing": yield "spider_webs";
            case "Stray Skeletons": yield "strays";
            case "Diamond Axes": yield "vindicator_axes";
            case "Wither Death Clouds": yield "wither_clouds";
            default: yield researchName.toLowerCase().replace(' ', '_');
        };

        return ResourceLocation.fromNamespaceAndPath(ReignOfNether.MOD_ID, newName);
    }
}
