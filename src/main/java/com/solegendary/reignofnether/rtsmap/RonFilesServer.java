package com.solegendary.reignofnether.rtsmap;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.commands.rtsapi.RonIds;
import com.solegendary.reignofnether.guide.GuideServer;
import com.solegendary.reignofnether.player.PlayerServerEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// the Editor's Scripts drawer: lists, reads, writes, deletes, runs and checks the .mcfunction files in ron/ for ops
public class RonFilesServer {

    private static final String EXT = ".mcfunction";
    private static final Pattern PATH = Pattern.compile("[A-Za-z0-9_\\-./]{1,200}");
    // rtsapi restrict <who> deny|allow <id>: group 1 is the id
    private static final Pattern RESTRICT = Pattern.compile("^rtsapi\\s+restrict\\s+\\S+\\s+(?:deny|allow)\\s+(\\S+)\\s*$");

    static void handle(ServerPlayer player, RonFileAction action, String path, String text) {
        MinecraftServer server = player.server;
        if (action == RonFileAction.LIST) {
            sendList(player);
            return;
        }
        Path file = file(server, path);
        if (file == null) {
            player.sendSystemMessage(SystemMessage.error(Component.translatable("editor.reignofnether.files.bad_path", path)));
            return;
        }
        String rel = rel(server, file);
        switch (action) {
            case READ -> read(player, file, rel);
            case WRITE, DELETE -> {
                Component refusal = RTSMapInfoServerEvents.writeRefusal();
                if (refusal != null)
                    player.sendSystemMessage(SystemMessage.error(refusal));
                else if (action == RonFileAction.WRITE)
                    write(player, file, rel, text);
                else
                    delete(player, file, rel);
                // a new events/unit_selected... file: clients start sending that input
                GuideServer.syncListened(server, server.getPlayerList().getPlayers());
            }
            case VALIDATE -> RonFileClientboundPacket.send(player, RonFileAction.PROBLEMS, rel, validate(server, text));
            case RUN -> {
                RonFiles.run(server, rel, player.createCommandSourceStack());
                player.sendSystemMessage(SystemMessage.info(Component.translatable("editor.reignofnether.files.ran", "ron/" + rel + EXT)));
            }
            default -> { }
        }
    }

    // ron/<path>.mcfunction, or null if the path isn't a plain relative one inside ron/
    @Nullable
    private static Path file(MinecraftServer server, String path) {
        if (path.endsWith(EXT))
            path = path.substring(0, path.length() - EXT.length());
        if (!PATH.matcher(path).matches() || path.contains(".."))
            return null;
        Path dir = RonFiles.dir(server);
        Path file = dir.resolve(path + EXT).normalize();
        return file.startsWith(dir) ? file : null;
    }

    private static String rel(MinecraftServer server, Path file) {
        String rel = RonFiles.dir(server).relativize(file).toString().replace(File.separatorChar, '/');
        return rel.substring(0, rel.length() - EXT.length());
    }

    private static boolean tooBig(String text) {
        return text.getBytes(StandardCharsets.UTF_8).length > RonFileServerboundPacket.MAX_BYTES;
    }

    private static int commandCount(List<String> lines) {
        return (int) lines.stream().map(String::strip).filter(l -> !l.isEmpty() && !l.startsWith("#")).count();
    }

    // "path\tcommands" per .mcfunction file, and "path.ext\t-1" per other file (map.json, nbt...), sorted
    private static String list(MinecraftServer server) {
        Path dir = RonFiles.dir(server);
        TreeMap<String, Integer> files = new TreeMap<>();
        if (Files.isDirectory(dir)) {
            try (Stream<Path> walk = Files.walk(dir)) {
                walk.filter(Files::isRegularFile).forEach(p -> {
                    if (!p.toString().endsWith(EXT)) {
                        files.put(dir.relativize(p).toString().replace(File.separatorChar, '/'), -1);
                        return;
                    }
                    try {
                        files.put(rel(server, p), commandCount(Files.readAllLines(p)));
                    } catch (Exception e) {
                        files.put(rel(server, p), 0); // not UTF-8 or unreadable: still listed
                    }
                });
            } catch (Exception e) {
                ReignOfNether.LOGGER.error("Failed to list " + dir + ": " + e.getMessage());
            }
        }
        StringBuilder sb = new StringBuilder();
        files.forEach((path, count) -> sb.append(path).append('\t').append(count).append('\n'));
        return sb.toString();
    }

    public static void sendList(ServerPlayer player) {
        if (PlayerServerEvents.isOpOrHost(player))
            RonFileClientboundPacket.send(player, RonFileAction.LIST, "", list(player.server));
    }

    // a file changed: every op gets the new list and the file's text as it is now
    public static void changed(MinecraftServer server, String rel) {
        String list = list(server);
        Path file = RonFiles.dir(server).resolve(rel + EXT);
        String text;
        try {
            text = Files.isRegularFile(file) ? Files.readString(file) : "";
        } catch (Exception e) {
            text = null;
        }
        for (ServerPlayer op : server.getPlayerList().getPlayers()) {
            if (!PlayerServerEvents.isOpOrHost(op))
                continue;
            RonFileClientboundPacket.send(op, RonFileAction.LIST, "", list);
            if (text != null && !tooBig(text))
                RonFileClientboundPacket.send(op, RonFileAction.CONTENT, rel, text);
        }
    }

    private static void read(ServerPlayer player, Path file, String rel) {
        String text;
        try {
            text = Files.isRegularFile(file) ? Files.readString(file) : "";
        } catch (Exception e) {
            player.sendSystemMessage(SystemMessage.error(Component.literal("ron/" + rel + EXT + ": " + e.getMessage())));
            return;
        }
        if (tooBig(text)) {
            player.sendSystemMessage(SystemMessage.error(Component.translatable("editor.reignofnether.files.too_big", "ron/" + rel + EXT)));
            return;
        }
        RonFileClientboundPacket.send(player, RonFileAction.CONTENT, rel, text);
        RonFileClientboundPacket.send(player, RonFileAction.PROBLEMS, rel, validate(player.server, text));
    }

    private static void write(ServerPlayer player, Path file, String rel, String text) {
        if (tooBig(text)) {
            player.sendSystemMessage(SystemMessage.error(Component.translatable("editor.reignofnether.files.too_big", "ron/" + rel + EXT)));
            return;
        }
        RTSMapInfoServerEvents.claimFormat(player.server);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, text);
            RonFiles.forgetErrors();
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Failed to write " + file + ": " + e.getMessage());
            player.sendSystemMessage(SystemMessage.error(Component.translatable("editor.reignofnether.files.write_failed", "ron/" + rel + EXT)));
            return;
        }
        changed(player.server, rel);
        RonFileClientboundPacket.send(player, RonFileAction.PROBLEMS, rel, validate(player.server, text));
    }

    private static void delete(ServerPlayer player, Path file, String rel) {
        try {
            Files.deleteIfExists(file);
            // folders left empty go too, so the Editor's file tree doesn't keep them
            Path dir = RonFiles.dir(player.server);
            for (Path parent = file.getParent(); parent != null && parent.startsWith(dir) && !parent.equals(dir); parent = parent.getParent()) {
                try (Stream<Path> left = Files.list(parent)) {
                    if (left.findAny().isPresent())
                        break;
                }
                Files.delete(parent);
            }
        } catch (Exception e) {
            ReignOfNether.LOGGER.error("Failed to delete " + file + ": " + e.getMessage());
            player.sendSystemMessage(SystemMessage.error(Component.translatable("editor.reignofnether.files.write_failed", "ron/" + rel + EXT)));
            return;
        }
        changed(player.server, rel);
    }

    /**
     * Parses each command line as RonFiles.run would (op level, a leading / allowed) without running it:
     * "line\tmessage[\targ]" per problem; the message is a lang key when it's one of ours
     */
    public static String validate(MinecraftServer server, String text) {
        CommandSourceStack source = server.createCommandSourceStack().withPermission(2).withSuppressedOutput();
        List<String> problems = new ArrayList<>();
        String[] lines = text.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].strip();
            if (line.isEmpty() || line.startsWith("#"))
                continue;
            if (line.startsWith("?")) // a quiet line (RonFiles.run) still has to parse
                line = line.substring(1).strip();
            if (line.startsWith("/"))
                line = line.substring(1);
            ParseResults<CommandSourceStack> parse = server.getCommands().getDispatcher().parse(line, source);
            CommandSyntaxException e = Commands.getParseException(parse);
            if (e != null) {
                problems.add((i + 1) + "\t" + e.getMessage().replaceAll("[\t\n]", " "));
                continue;
            }
            CommandContextBuilder<CommandSourceStack> context = parse.getContext();
            while (context.getChild() != null)
                context = context.getChild();
            if (context.getCommand() == null) {
                problems.add((i + 1) + "\teditor.reignofnether.files.incomplete");
                continue;
            }
            Matcher m = RESTRICT.matcher(line);
            if (m.matches() && RonIds.restrictable(m.group(1)) == null)
                problems.add((i + 1) + "\teditor.reignofnether.files.unknown_id\t" + m.group(1));
        }
        return String.join("\n", problems);
    }
}
