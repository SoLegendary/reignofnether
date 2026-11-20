package com.solegendary.reignofnether.rtsmap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.blocks.RTSStartBlock;
import com.solegendary.reignofnether.building.BuildingPlacement;
import com.solegendary.reignofnether.building.BuildingServerEvents;
import com.solegendary.reignofnether.building.custombuilding.CustomBuilding;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingClientboundPacket;
import com.solegendary.reignofnether.building.custombuilding.CustomBuildingServerEvents;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.startpos.StartPosServerEvents;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.packets.UnitSyncClientboundPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * The only code that knows the old (v1) map format: rtsmap.json (or a ron/map.json without "format") and the start
 * blocks, custom buildings and scenario roles kept in data/. Nothing else reads it: a v1 world opens with none of it
 * loaded and can't be played (RTSMapInfoServerEvents.playRefusal) until the mapmaker's Convert writes it as format 2.
 */
public final class MapConverter {

    private static final String OLD_FILE = "rtsmap.json";
    private static final String START_POSES = "saved-start-pos-data";
    private static final String CUSTOM_BUILDINGS = "saved-custom-building-data";
    private static final String ROLES = "saved-scenario-data";
    private static final String BUILDINGS = "saved-building-data";
    // a v1 scenario unit's role index, kept in its persistent data (Unit.readUnitSaveData) until its seat claims it
    public static final String LEGACY_ROLE = "reignofnether_legacy_role";

    // a v1 scenario role as saved in data/saved-scenario-data.dat
    private record Role(int index, String name, String faction, int food, int wood, int ore, int team, boolean npc) {
        static Role of(CompoundTag tag) {
            return new Role(tag.getInt("index"), tag.getString("name"), tag.getString("faction"), tag.getInt("startingFood"),
                    tag.getInt("startingWood"), tag.getInt("startingOre"), tag.getInt("teamNumber"), tag.getBoolean("isNpc"));
        }

        boolean neutral() {
            return faction.isBlank() || faction.equalsIgnoreCase("NEUTRAL") || faction.endsWith(":neutral") || faction.endsWith(":none");
        }

        // changed from its defaults by the old scenario editor (every world saved the 8 default roles)
        boolean edited(List<Role> roles) {
            boolean sharesTeam = roles.stream().anyMatch(r -> r != this && r.team == team);
            return !name.equals("Player " + (index + 1)) || !neutral() || npc || team != index + 1 || sharesTeam
                    || food != 0 || wood != 0 || ore != 0;
        }
    }

    private MapConverter() { }

    // a world without a format 2 map.json holding rtsmap.json, or start blocks or custom buildings in
    // data/. Reads files only, so the world list can use it too
    static boolean hasOldData(Path world) {
        return Files.exists(world.resolve(OLD_FILE))
                || !savedData(world, START_POSES).getList("startPoses", Tag.TAG_COMPOUND).isEmpty()
                || !savedData(world, CUSTOM_BUILDINGS).getList("custombuildings", Tag.TAG_COMPOUND).isEmpty()
                || !editedRoles(world).isEmpty();
    }

    // The mapmaker's Convert (the Editor warns to back the world up first): the old files are left as they are, so
    // older mod versions still open the world as it was, then it's written as format 2 and loaded like any format 2 map
    public static void convert(ServerPlayer player) {
        MinecraftServer server = player.server;
        if (RTSMapInfoServerEvents.mapFormat() != 1) {
            Component refusal = RTSMapInfoServerEvents.writeRefusal();
            player.sendSystemMessage(SystemMessage.error(refusal != null ? refusal : Component.translatable("map.reignofnether.format.already")));
            return;
        }
        Path world = server.getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize();
        ServerLevel level = server.overworld();
        RTSMapInfo info;
        try {
            info = oldInfo(server, world);
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Map conversion failed, nothing was converted: " + e.getMessage());
            player.sendSystemMessage(SystemMessage.error(Component.translatable("map.reignofnether.format.convert_failed")));
            return;
        }
        if (info.getSeats().isEmpty()) // a map without seats played on its start blocks
            seatStartBlocks(info, world, level);
        addRoleSeats(info, world);
        RTSMapInfoServerEvents.format = RTSMapInfoServerEvents.SUPPORTED_FORMAT;
        if (!RTSMapInfoServerEvents.setAndSave(server, info)) {
            RTSMapInfoServerEvents.format = 0; // read from the files again
            player.sendSystemMessage(SystemMessage.error(Component.translatable("map.reignofnether.format.convert_failed")));
            return;
        }
        List<CustomBuilding> buildings = customBuildings(world);
        for (CustomBuilding building : buildings) {
            CustomBuildingServerEvents.deleteFiles(server, building);
            CustomBuildingServerEvents.saveFiles(server, building, true);
        }
        claimRoleBuildings(info, world);
        for (ServerLevel l : server.getAllLevels())
            for (Entity entity : l.getAllEntities())
                claimLegacyUnit(entity);
        CustomBuildingServerEvents.loadCustomBuildings(level);
        CustomBuildingServerEvents.customBuildings.forEach(CustomBuildingClientboundPacket::registerCustomBuilding);
        RTSMapInfoClientboundPacket.sendValue(RTSMapInfoAction.SET_FORMAT, String.valueOf(RTSMapInfoServerEvents.SUPPORTED_FORMAT));
        for (ServerPlayer p : server.getPlayerList().getPlayers())
            RonFilesServer.sendList(p);
        ReignOfNether.LOGGER.info("Map converted to format " + RTSMapInfoServerEvents.SUPPORTED_FORMAT + " (" + buildings.size()
                + " custom buildings)");
        player.sendSystemMessage(SystemMessage.info(Component.translatable("map.reignofnether.format.converted",
                RTSMapInfoServerEvents.SUPPORTED_FORMAT)));
        // their placements were kept in the world's save data (BuildingSaveData), they come back with their buildings
        if (!buildings.isEmpty())
            player.sendSystemMessage(SystemMessage.info(Component.translatable("map.reignofnether.format.converted_reopen")));
    }

    // the old map.json (or rtsmap.json) with its old names renamed, put in ron/map.json so setAndSave merges into it
    // and keeps its unknown keys; an empty map when there's none. Throws when it's broken
    private static RTSMapInfo oldInfo(MinecraftServer server, Path world) throws IOException {
        Path json = RTSMapInfoLoader.path(server);
        if (!Files.exists(json))
            json = world.resolve(OLD_FILE);
        if (!Files.exists(json))
            return new RTSMapInfo();
        JsonObject old = normalize(JsonParser.parseString(Files.readString(json)).getAsJsonObject());
        String text = RTSMapInfoLoader.toJson(old);
        Files.createDirectories(RTSMapInfoLoader.path(server).getParent());
        Files.writeString(RTSMapInfoLoader.path(server), text);
        return RTSMapInfoLoader.fromJson(text);
    }

    // rtsmap.json's "author" (a name or a list) and "startPositions" become "authors" and "seats"
    private static JsonObject normalize(JsonObject json) {
        JsonElement author = json.remove("author");
        if (author != null && !json.has("authors")) {
            JsonArray authors = new JsonArray();
            if (author.isJsonPrimitive())
                authors.add(author);
            json.add("authors", author.isJsonPrimitive() ? authors : author);
        }
        JsonElement startPositions = json.remove("startPositions");
        if (startPositions != null && !json.has("seats"))
            json.add("seats", startPositions);
        return json;
    }

    // every start block still in the world becomes a seat, block colour = team
    private static void seatStartBlocks(RTSMapInfo info, Path world, ServerLevel level) {
        for (Tag tag : savedData(world, START_POSES).getList("startPoses", Tag.TAG_COMPOUND)) {
            CompoundTag pos = (CompoundTag) tag;
            BlockPos bp = new BlockPos(pos.getInt("x"), pos.getInt("y"), pos.getInt("z"));
            if (level.getBlockState(bp).getBlock() instanceof RTSStartBlock && info.getSeat(bp) == null)
                info.getSeats().add(new RTSMapInfo.Seat(StartPosServerEvents.nextSeatId(info), bp));
        }
        StartPosServerEvents.groupBlockSeats(info, level);
    }

    // each gets a unique id for custom:<id>, its file name in ron/buildings
    private static List<CustomBuilding> customBuildings(Path world) {
        List<CustomBuilding> buildings = new ArrayList<>();
        Set<String> names = new HashSet<>(), ids = new HashSet<>();
        for (Tag tag : savedData(world, CUSTOM_BUILDINGS).getList("custombuildings", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) tag;
            if (!names.add(b.getString("buildingName")))
                continue;
            CustomBuilding building = new CustomBuilding(b.getString("buildingName"),
                    new Vec3i(b.getInt("structureSizeX"), b.getInt("structureSizeY"), b.getInt("structureSizeZ")), Blocks.COMMAND_BLOCK,
                    b.getCompound("structureNbt"), b.getCompound("attributesNbt"), b.getList("commandsNbt", Tag.TAG_COMPOUND));
            String baseId = building.id;
            for (int i = 2; !ids.add(building.id); i++)
                building.id = baseId + "_" + i;
            buildings.add(building);
        }
        return buildings;
    }

    // data/<name>.dat's contents as on disk, empty when there's none
    private static CompoundTag savedData(Path world, String name) {
        Path file = world.resolve("data").resolve(name + ".dat");
        try {
            return Files.isRegularFile(file) ? NbtIo.readCompressed(file.toFile()).getCompound("data") : new CompoundTag();
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Failed to read " + file + ": " + e.getMessage());
            return new CompoundTag();
        }
    }

    private static List<Role> editedRoles(Path world) {
        List<Role> roles = new ArrayList<>();
        for (Tag tag : savedData(world, ROLES).getList("scenarioRoles", Tag.TAG_COMPOUND))
            roles.add(Role.of((CompoundTag) tag));
        return roles.stream().filter(role -> role.edited(roles)).toList();
    }

    // every edited role becomes a seat named after it, at its buildings (the first one's corner), in the "scenario"
    // mode's teams. A role without buildings has nowhere to start, so it's left out
    private static void addRoleSeats(RTSMapInfo info, Path world) {
        List<List<Integer>> teams = new ArrayList<>();
        for (Role role : editedRoles(world)) {
            BlockPos pos = roleBuildings(world, role.index).stream().findFirst().orElse(null);
            if (pos == null) {
                ReignOfNether.LOGGER.warn("Map conversion: scenario role " + role.name + " has no buildings, left out");
                continue;
            }
            RTSMapInfo.Seat seat = info.getSeat(pos);
            if (seat == null) {
                seat = new RTSMapInfo.Seat(StartPosServerEvents.nextSeatId(info), pos);
                info.getSeats().add(seat);
            }
            seat.role = role.index;
            seat.name = role.name;
            seat.faction = role.neutral() ? null : role.faction.toLowerCase(Locale.ROOT).contains(":")
                    ? role.faction : role.faction.toLowerCase(Locale.ROOT);
            seat.food = role.food == 0 ? null : role.food;
            seat.wood = role.wood == 0 ? null : role.wood;
            seat.ore = role.ore == 0 ? null : role.ore;
            seat.control = role.npc ? "none" : null;
            while (teams.size() < Math.max(1, role.team))
                teams.add(new ArrayList<>());
            teams.get(Math.max(1, role.team) - 1).add(seat.id);
        }
        teams.removeIf(List::isEmpty);
        if (!teams.isEmpty()) {
            info.getModes().put("scenario", teams);
            ReignOfNether.LOGGER.info("Map conversion: " + teams.stream().mapToInt(List::size).sum() + " scenario roles became seats");
        }
    }

    // where the role's buildings are, from data/saved-building-data.dat as on disk (the game no longer reads roles)
    private static List<BlockPos> roleBuildings(Path world, int role) {
        List<BlockPos> list = new ArrayList<>();
        for (Tag tag : savedData(world, BUILDINGS).getList("buildings", Tag.TAG_COMPOUND)) {
            CompoundTag b = (CompoundTag) tag;
            if (b.contains("scenarioRoleIndex") && b.getInt("scenarioRoleIndex") == role)
                list.add(new BlockPos(b.getInt("x"), b.getInt("y"), b.getInt("z")));
        }
        return list;
    }

    // a role's buildings are owned by its seat's name, which commands resolve to whoever plays it
    private static void claimRoleBuildings(RTSMapInfo info, Path world) {
        for (RTSMapInfo.Seat seat : info.getSeats()) {
            if (seat.role == null)
                continue;
            List<BlockPos> poses = roleBuildings(world, seat.role);
            for (BuildingPlacement placement : BuildingServerEvents.getBuildings())
                if (poses.contains(placement.originPos))
                    placement.ownerName = seat.name;
        }
    }

    // a v1 scenario unit goes to its role's seat once the map is converted; on a v1 map it waits
    public static void claimLegacyUnit(Entity entity) {
        if (!(entity instanceof Unit unit) || !entity.getPersistentData().contains(LEGACY_ROLE) || !RTSMapInfoServerEvents.isEditable())
            return;
        RTSMapInfo info = RTSMapInfoServerEvents.rtsMapInfo;
        if (info == null) // not loaded yet: the world's first units join before ServerStartedEvent
            return;
        int role = entity.getPersistentData().getInt(LEGACY_ROLE);
        entity.getPersistentData().remove(LEGACY_ROLE);
        info.getSeats().stream().filter(seat -> seat.role != null && seat.role == role && seat.name != null).findFirst().ifPresent(seat -> {
            unit.setOwnerName(seat.name);
            UnitSyncClientboundPacket.sendSyncOwnerNamePacket(unit);
        });
    }
}
