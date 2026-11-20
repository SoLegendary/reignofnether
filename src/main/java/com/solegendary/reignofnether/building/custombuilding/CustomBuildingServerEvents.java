package com.solegendary.reignofnether.building.custombuilding;

import com.google.gson.*;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.blocks.RTSStructureBlockEntity;
import com.solegendary.reignofnether.building.*;
import com.solegendary.reignofnether.building.addon.ItemShopAddon;
import com.solegendary.reignofnether.building.buildings.placements.CustomBuildingPlacement;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import com.solegendary.reignofnether.rtsmap.RonFiles;
import com.solegendary.reignofnether.rtsmap.RTSMapInfoServerEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class CustomBuildingServerEvents {

    // since every custom building has a different structure, we need to maintain a list of them here
    public static final Set<CustomBuilding> customBuildings = new HashSet<>();

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static boolean retainBuildings = false; // retain custom buildiongs across singleplayer worlds in the same client session

    public static CustomBuilding getCustomBuilding(String name) {
        for (CustomBuilding building : customBuildings)
            if (building.name.equals(name))
                return building;
        return null;
    }

    public static CustomBuilding getCustomBuildingById(String id) {
        for (CustomBuilding building : customBuildings)
            if (building.id.equals(id))
                return building;
        return null;
    }

    public static void deregisterCustomBuilding(String buildingName) {
        CustomBuilding building = getCustomBuilding(buildingName);
        customBuildings.removeIf(b -> b.name.equals(buildingName));
        BuildingServerEvents.getBuildings().removeIf(b -> b.getBuilding().name.equals(buildingName));
        ServerLevel level = BuildingServerEvents.getServerLevel();
        if (building != null && level != null)
            deleteFiles(level.getServer(), building);
    }


    // registers and places a new custom building on server and client
    public static boolean createAndRegisterNewCustomBuilding(ResourceLocation structureRL, String structureName, ServerLevel level,
                                                             BlockPos pos, Vec3i structureSize) {
        Component refusal = RTSMapInfoServerEvents.writeRefusal(); // an old or newer format map is never changed
        if (refusal != null) {
            PlayerServerEvents.players.forEach(p -> p.sendSystemMessage(SystemMessage.error(refusal)));
            return false;
        }
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> template = manager.get(structureRL);
        CompoundTag structureNbt = null;
        if (template.isPresent()) {
            structureNbt = template.get().save(new CompoundTag());
        }
        if (structureNbt != null) {
            BuildingServerEvents.getBuildings().removeIf(b -> b.originPos.equals(pos));

            ArrayList<BuildingBlock> blocks = BuildingUtils.getAbsoluteBlockData(
                    BuildingBlockData.getBuildingBlocksFromNbt(structureNbt),
                    level, pos, Rotation.NONE
            );
            int numSolidBlocks = 0;
            Block portraitBlock = Blocks.COMMAND_BLOCK;
            for (BuildingBlock bb : blocks) {
                BlockState bs = bb.getBlockState();
                if (!bs.isAir() && bs.getFluidState().isEmpty() &&
                    !CustomBuilding.INVULNERABLE_BLOCKS.contains(bb.getBlockState().getBlock())) {
                    numSolidBlocks += 1;
                    portraitBlock = bs.getBlock();
                }
            }
            if (numSolidBlocks == 0) {
                PlayerServerEvents.sendMessageToAllPlayers("ERROR (server): cannot registerBuilding custom building with no solid blocks");
            } else {
                CustomBuilding building = new CustomBuilding(structureName, structureSize, portraitBlock, structureNbt);
                for (CustomBuilding customBuilding : customBuildings) {
                    if (customBuilding.name.equals(building.name)) {
                        PlayerServerEvents.sendMessageToAllPlayers("ERROR (server): custom building " + building.name + " already exists");
                        return false;
                    }
                }
                if (!building.productions.get().isEmpty()) {
                    PlayerServerEvents.sendMessageToAllPlayers(
                            "sandbox.reignofnether.custom_buildings.trainable_units.save_success",
                            false, building.productions.get().size()
                    );
                }
                if (!building.stockedItems.isEmpty()) {
                    PlayerServerEvents.sendMessageToAllPlayers(
                            "sandbox.reignofnether.custom_buildings.buyable_items.save_success",
                            false, building.stockedItems.size()
                    );
                }

                String baseId = building.id;
                for (int i = 2; getCustomBuildingById(building.id) != null || Files.exists(dir(level.getServer()).resolve(building.id + ".json")); i++)
                    building.id = baseId + "_" + i;

                customBuildings.add(building);
                BuildingPlacement placement = building.createBuildingPlacement(level, pos, Rotation.NONE, "");
                BuildingServerEvents.getBuildings().add(placement);
                CustomBuildingClientboundPacket.registerCustomBuilding(building);
                saveFiles(level.getServer(), building, true);
                BuildingServerEvents.saveBuildings(level);
                BuildingServerEvents.placeBuildingClientside(pos);
                return true;
            }
        }
        return false;
    }

    // each custom building is kept as files in ron/buildings/ so mapmakers can share and hand-edit them:
    // <id>.nbt is the vanilla structure, <id>.json the name and attributes,
    // <id>/<trigger>.mcfunction the commands run when that trigger happens (read fresh every time)
    public static Path dir(MinecraftServer server) {
        return RonFiles.dir(server).resolve("buildings");
    }

    // commands=false leaves the .mcfunction files alone, so hand edits aren't overwritten by unrelated edits
    public static void saveFiles(MinecraftServer server, CustomBuilding building, boolean commands) {
        saveFiles(server, building, commands ? List.of(BuildingCommand.TriggerCondition.values()) : List.of());
    }

    // the lines the editor's list makes of one trigger's file
    public static List<String> commandLines(CustomBuilding building, BuildingCommand.TriggerCondition trigger) {
        return building.commands.stream()
                .filter(c -> c.condition == trigger && !c.commandStr.isBlank())
                .map(c -> c.commandStr.startsWith("/") ? c.commandStr.substring(1) : c.commandStr)
                .toList();
    }

    // only the given triggers' .mcfunction files are rewritten, so hand edits to the others are kept
    // ponytail: a rewritten file comes from the editor's list, one command per line, so its comments and hand edits
    // made since the world opened are lost, and commands sharing a trigger share its cooldown
    public static void saveFiles(MinecraftServer server, CustomBuilding building, Collection<BuildingCommand.TriggerCondition> commandTriggers) {
        Path dir = dir(server);
        RTSMapInfoServerEvents.claimFormat(server);
        try {
            Files.createDirectories(dir);
            building.packAttributesNbt();
            building.packCommandsNbt();

            JsonObject attributes = new JsonObject();
            for (String key : new TreeSet<>(building.attributesNbt.getAllKeys())) {
                Tag tag = building.attributesNbt.get(key);
                if (tag instanceof ByteTag byteTag)
                    attributes.addProperty(key, byteTag.getAsByte() != 0);
                else if (tag instanceof NumericTag numericTag)
                    attributes.addProperty(key, numericTag.getAsNumber());
                else if (tag != null)
                    attributes.addProperty(key, tag.getAsString());
            }
            JsonObject cooldowns = new JsonObject();
            for (BuildingCommand command : building.commands)
                if (command.hasCooldownCondition() && !cooldowns.has(command.triggerFileName()))
                    cooldowns.addProperty(command.triggerFileName(), command.tickCooldownMax);

            JsonObject json = new JsonObject();
            json.addProperty("name", building.name);
            json.add("attributes", attributes);
            json.add("cooldowns", cooldowns);
            Files.writeString(dir.resolve(building.id + ".json"), GSON.toJson(json));

            Path nbtPath = dir.resolve(building.id + ".nbt");
            if (!Files.exists(nbtPath)) // the structure never changes after creation, a hand-replaced one is kept
                NbtIo.writeCompressed(building.structureNbt, nbtPath.toFile());

            Path commandDir = dir.resolve(building.id);
            for (BuildingCommand.TriggerCondition trigger : commandTriggers) {
                if (trigger == BuildingCommand.TriggerCondition.NONE)
                    continue;
                List<String> lines = commandLines(building, trigger);
                Path file = commandDir.resolve(trigger.name().toLowerCase() + ".mcfunction");
                if (lines.isEmpty()) {
                    Files.deleteIfExists(file);
                } else {
                    Files.createDirectories(commandDir);
                    Files.write(file, lines);
                }
            }
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Failed to save custom building " + building.id + ": " + e.getMessage());
        }
    }

    public static void deleteFiles(MinecraftServer server, CustomBuilding building) {
        Path dir = dir(server);
        try {
            Files.deleteIfExists(dir.resolve(building.id + ".json"));
            Files.deleteIfExists(dir.resolve(building.id + ".nbt"));
            Path commandDir = dir.resolve(building.id);
            if (Files.isDirectory(commandDir))
                try (Stream<Path> paths = Files.walk(commandDir)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
                }
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Failed to delete custom building " + building.id + ": " + e.getMessage());
        }
    }

    private static CustomBuilding loadFiles(Path dir, String id) throws Exception {
        JsonObject json = JsonParser.parseString(Files.readString(dir.resolve(id + ".json"))).getAsJsonObject();
        CompoundTag structureNbt = NbtIo.readCompressed(dir.resolve(id + ".nbt").toFile());
        ListTag size = structureNbt.getList("size", Tag.TAG_INT);
        JsonObject cooldowns = json.has("cooldowns") ? json.getAsJsonObject("cooldowns") : new JsonObject();

        ListTag commandsNbt = new ListTag();
        for (BuildingCommand.TriggerCondition trigger : BuildingCommand.TriggerCondition.values()) {
            Path file = dir.resolve(id).resolve(trigger.name().toLowerCase() + ".mcfunction");
            if (trigger == BuildingCommand.TriggerCondition.NONE || !Files.isRegularFile(file))
                continue;
            int cooldown = cooldowns.has(trigger.name().toLowerCase()) ? cooldowns.get(trigger.name().toLowerCase()).getAsInt() : 100;
            for (String line : Files.readAllLines(file)) {
                line = line.strip();
                if (line.isEmpty() || line.startsWith("#"))
                    continue;
                CompoundTag ctag = new CompoundTag();
                ctag.putInt("tickCooldown", cooldown);
                ctag.putInt("tickCooldownMax", cooldown);
                ctag.putString("commandStr", line);
                ctag.putString("condition", trigger.name());
                commandsNbt.add(ctag);
            }
        }
        String name = json.has("name") ? json.get("name").getAsString() : id;
        CustomBuilding building = new CustomBuilding(name, new Vec3i(size.getInt(0), size.getInt(1), size.getInt(2)),
                Blocks.COMMAND_BLOCK, structureNbt, null, commandsNbt);
        building.id = id;

        // attributes left out of the file keep their defaults
        if (json.has("attributes")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("attributes").entrySet()) {
                JsonPrimitive value = entry.getValue().getAsJsonPrimitive();
                if (value.isBoolean())
                    building.attributesNbt.putBoolean(entry.getKey(), value.getAsBoolean());
                else if (value.isNumber())
                    building.attributesNbt.putInt(entry.getKey(), value.getAsInt());
                else
                    building.attributesNbt.putString(entry.getKey(), value.getAsString());
            }
            building.unpackAttributesNbt();
            if (building.maxHealth <= 0)
                building.setToDefaultMaxHealth();
        }
        return building;
    }

    public static void loadCustomBuildings(ServerLevel level) {
        if (!retainBuildings)
            customBuildings.clear();

        MinecraftServer server = level.getServer();
        if (!RTSMapInfoServerEvents.isEditable()) // an old or newer format map loads none
            return;
        Path dir = dir(server);

        Set<CustomBuilding> retained = new HashSet<>(customBuildings);
        if (Files.isDirectory(dir)) {
            try (Stream<Path> paths = Files.list(dir)) {
                for (Path path : paths.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                    String id = path.getFileName().toString().replaceAll("\\.json$", "");
                    try {
                        CustomBuilding building = loadFiles(dir, id);
                        boolean buildingExists = false;
                        for (CustomBuilding customBuilding : customBuildings) {
                            if (customBuilding.name.equals(building.name) || customBuilding.id.equals(building.id)) {
                                buildingExists = true;
                                break;
                            }
                        }
                        if (!buildingExists)
                            customBuildings.add(building);
                        ReignOfNether.LOGGER.info("loaded custom building ron/buildings/" + id);
                    } catch (Exception e) {
                        ReignOfNether.LOGGER.error("Failed to load custom building ron/buildings/" + id + ": " + e.getMessage());
                    }
                }
            } catch (Exception e) {
                ReignOfNether.LOGGER.error("Failed to read " + dir + ": " + e.getMessage());
            }
        }
        // buildings kept from the previous world by /rtsapi-export-custom-buildings become part of this one
        for (CustomBuilding building : retained)
            if (!Files.exists(dir.resolve(building.id + ".json")))
                saveFiles(server, building, true);
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (!PlayerServerEvents.rtsSyncingEnabled) {
            return;
        }
        MinecraftServer server = evt.getEntity().level().getServer();
        if (server == null || !server.isDedicatedServer()) {
            CompletableFuture.delayedExecutor(1000,  TimeUnit.MILLISECONDS).execute(() -> syncCustomBuildings(evt.getEntity().getName().getString()));
        } else {
            syncCustomBuildings(evt.getEntity().getName().getString());
        }
        //ReignOfNether.LOGGER.info("Synced " + buildings.size() + " custom buildings with player logged in");
    }

    private static void syncCustomBuildings(String playerName) {
        for (CustomBuilding customBuilding : customBuildings) {
            CustomBuildingClientboundPacket.registerCustomBuilding(playerName, customBuilding);
        }
    }
}
