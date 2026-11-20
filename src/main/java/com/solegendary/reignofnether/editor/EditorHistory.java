package com.solegendary.reignofnether.editor;

import com.solegendary.reignofnether.util.WorldState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Undo and redo for the editor's documents (the map.json copy), as text snapshots. Each tracked
 * document is captured a few times a second while the editor is open; a change becomes one step, and quick changes to
 * the same document (typing, clicking a stepper) merge into it. World edits (blocks, units, buildings) aren't covered.
 */
public final class EditorHistory {

    private EditorHistory() { }

    private record Doc(Supplier<String> capture, Consumer<String> restore) { }

    private record Step(String key, String before, String after, long ms) { }

    private static final int MAX_STEPS = 60;
    private static final long MERGE_MS = 800;
    private static final long POLL_MS = 100;

    private static final Map<String, Doc> docs = new LinkedHashMap<>();
    private static final Map<String, String> last = new HashMap<>();
    private static final Deque<Step> undo = new ArrayDeque<>();
    private static final Deque<Step> redo = new ArrayDeque<>();
    private static long lastPollMs = 0;

    static {
        WorldState.onClientReset(EditorHistory::clear);
    }

    public static void clear() {
        last.clear();
        undo.clear();
        redo.clear();
    }

    /** Starts tracking a document: capture returns it as text, restore puts a captured text back. */
    public static void track(String key, Supplier<String> capture, Consumer<String> restore) {
        docs.put(key, new Doc(capture, restore));
        last.remove(key);
    }

    /** The document changed by itself (synced from the server): take it as it is now, without an undo step. */
    public static void rebase(String key) {
        Doc doc = docs.get(key);
        if (doc != null)
            last.put(key, doc.capture().get());
    }

    /** Called every frame the editor draws; captures the documents a few times a second. */
    public static void poll() {
        long now = System.currentTimeMillis();
        if (now - lastPollMs >= POLL_MS)
            capture(now);
    }

    private static void capture(long now) {
        lastPollMs = now;
        docs.forEach((key, doc) -> {
            String text = doc.capture().get();
            String before = last.put(key, text);
            if (before == null || before.equals(text))
                return;
            Step top = undo.peekLast();
            boolean merge = top != null && top.key().equals(key) && now - top.ms() < MERGE_MS;
            if (merge)
                undo.pollLast();
            undo.addLast(new Step(key, merge ? top.before() : before, text, now));
            if (undo.size() > MAX_STEPS)
                undo.pollFirst();
            redo.clear();
        });
    }

    public static boolean canUndo() {
        return !undo.isEmpty();
    }

    public static void undo() {
        capture(System.currentTimeMillis()); // a change made since the last poll is a step of its own
        Step step = undo.pollLast();
        if (step == null)
            return;
        apply(step.key(), step.before());
        redo.addLast(step);
    }

    public static void redo() {
        Step step = redo.pollLast();
        if (step == null)
            return;
        apply(step.key(), step.after());
        undo.addLast(new Step(step.key(), step.before(), step.after(), 0)); // never merged into
    }

    private static void apply(String key, String text) {
        Doc doc = docs.get(key);
        if (doc == null)
            return;
        doc.restore().accept(text);
        last.put(key, doc.capture().get());
    }
}
