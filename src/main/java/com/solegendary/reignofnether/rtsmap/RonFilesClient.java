package com.solegendary.reignofnether.rtsmap;

import com.solegendary.reignofnether.chat.SystemMessage;
import com.solegendary.reignofnether.util.WorldState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

// what the client knows of the ron/ .mcfunction files (ops only): paths are relative to ron/ without ".mcfunction".
// contents is the text as saved on the server; drafts belong to the UI. version goes up on every change, so UIs can
// redraw when it differs from the one they last saw
public class RonFilesClient {

    public static final Map<String, Integer> files = WorldState.client(new TreeMap<>()); // path -> command count
    public static final Set<String> others = WorldState.client(new TreeSet<>()); // other ron/ files, with their extension
    public static final Map<String, String> contents = WorldState.client(new HashMap<>()); // files asked for with open()
    public static final Map<String, Map<Integer, String>> problems = WorldState.client(new HashMap<>()); // path -> line (1-based) -> message
    public static final List<LogLine> log = WorldState.client(new ArrayList<>()); // the Editor's Console, oldest first
    private static final Set<String> saving = WorldState.client(new HashSet<>()); // saved, waiting for their problems
    private static final int MAX_LOG = 300;
    private static int version = 0;

    public record LogLine(String text, boolean error) { }

    public static void log(String text, boolean error) {
        log.add(new LogLine(text, error));
        if (log.size() > MAX_LOG)
            log.remove(0);
    }

    private static String fileName(String path) {
        return "ron/" + normalize(path) + ".mcfunction";
    }

    public static int version() {
        return version;
    }

    public static boolean exists(String path) {
        return files.containsKey(path);
    }

    public static String normalize(String path) {
        path = path.strip().replace('\\', '/');
        if (path.startsWith("/"))
            path = path.substring(1);
        return path.endsWith(".mcfunction") ? path.substring(0, path.length() - ".mcfunction".length()) : path;
    }

    public static void refresh() {
        RonFileServerboundPacket.send(RonFileAction.LIST, "", "");
    }

    // asks for the file's text and problems (a file that doesn't exist yet comes back empty)
    public static void open(String path) {
        RonFileServerboundPacket.send(RonFileAction.READ, normalize(path), "");
    }

    /** @return false (and tells the player) if it's too big to send */
    public static boolean save(String path, String text) {
        if (!fits(path, text))
            return false;
        RonFileServerboundPacket.send(RonFileAction.WRITE, normalize(path), text);
        saving.add(normalize(path));
        return true;
    }

    public static boolean validate(String path, String text) {
        if (!fits(path, text))
            return false;
        RonFileServerboundPacket.send(RonFileAction.VALIDATE, normalize(path), text);
        return true;
    }

    public static void delete(String path) {
        RonFileServerboundPacket.send(RonFileAction.DELETE, normalize(path), "");
        log(I18n.get("editor.reignofnether.console.deleted", fileName(path)), false);
    }

    public static void run(String path) {
        RonFileServerboundPacket.send(RonFileAction.RUN, normalize(path), "");
        log(I18n.get("editor.reignofnether.files.ran", fileName(path)), false);
    }

    private static boolean fits(String path, String text) {
        if (text.getBytes(StandardCharsets.UTF_8).length <= RonFileServerboundPacket.MAX_BYTES)
            return true;
        if (Minecraft.getInstance().player != null)
            Minecraft.getInstance().player.sendSystemMessage(SystemMessage.error(
                    Component.translatable("editor.reignofnether.files.too_big", "ron/" + normalize(path) + ".mcfunction")));
        return false;
    }

    static void received(RonFileAction action, String path, String text) {
        switch (action) {
            case LIST -> {
                files.clear();
                others.clear();
                for (String line : text.split("\n")) {
                    int tab = line.indexOf('\t');
                    if (tab <= 0)
                        continue;
                    int count = Integer.parseInt(line.substring(tab + 1));
                    if (count < 0)
                        others.add(line.substring(0, tab));
                    else
                        files.put(line.substring(0, tab), count);
                }
            }
            case CONTENT -> contents.put(path, text);
            case PROBLEMS -> {
                Map<Integer, String> map = new LinkedHashMap<>();
                for (String line : text.split("\n")) {
                    String[] parts = line.split("\t");
                    if (parts.length < 2)
                        continue;
                    String msg = I18n.exists(parts[1]) ? I18n.get(parts[1], parts.length > 2 ? parts[2] : "") : parts[1];
                    map.merge(Integer.parseInt(parts[0]), msg, (a, b) -> a + " " + b);
                }
                problems.put(path, map);
                if (saving.remove(path)) {
                    log(map.isEmpty() ? I18n.get("editor.reignofnether.console.saved", fileName(path))
                            : I18n.get("editor.reignofnether.console.saved_problems", fileName(path), map.size()), !map.isEmpty());
                    map.forEach((line, msg) -> log("  " + I18n.get("editor.reignofnether.code.line", line) + ": " + msg, true));
                }
            }
            case LOG -> log(text, true);
            default -> { }
        }
        version++;
    }
}
