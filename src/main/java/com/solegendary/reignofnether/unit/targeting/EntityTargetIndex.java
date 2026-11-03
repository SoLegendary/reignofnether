package com.solegendary.reignofnether.unit.targeting;

import com.solegendary.reignofnether.ReignOfNether;
import com.solegendary.reignofnether.alliance.AlliancesServerEvents;
import com.solegendary.reignofnether.faction.Faction;
import com.solegendary.reignofnether.faction.Factions;
import com.solegendary.reignofnether.unit.NonUnitServerEvents;
import com.solegendary.reignofnether.unit.Relationship;
import com.solegendary.reignofnether.unit.interfaces.Unit;
import com.solegendary.reignofnether.unit.units.monsters.PhantomSummon;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/** Server-thread-only spatial index of tracked living entities, grouped before target searches run. */
@Mod.EventBusSubscriber(modid = ReignOfNether.MOD_ID)
public final class EntityTargetIndex {
    private static final Map<ServerLevel, EntityTargetIndex> LEVELS = new IdentityHashMap<>();
    private final Map<Entity, Entry> entities = new IdentityHashMap<>();
    private final Map<TargetGroup, DynamicTargetTree<Entry>> trees = new HashMap<>();
    private final ServerLevel level;
    private long nextOrder;
    private long version;

    private EntityTargetIndex(ServerLevel level) { this.level = level; }

    public static EntityTargetIndex get(ServerLevel level) {
        if (!level.getServer().isSameThread())
            throw new IllegalStateException("Entity target queries must run on the server thread");
        return LEVELS.computeIfAbsent(level, ignored -> new EntityTargetIndex(level));
    }

    /** Called after vanilla starts tracking, including entities loaded from disk and players. */
    public static void startTracking(Entity entity) {
        if (entity instanceof LivingEntity living && entity.level() instanceof ServerLevel level) {
            EntityTargetIndex index = get(level);
            if (!index.entities.containsKey(entity)) {
                Entry entry = new Entry(living, classify(living), ++index.nextOrder);
                index.entities.put(entity, entry);
                index.attach(entry);
                index.version++;
            }
        }
    }

    public static void stopTracking(Entity entity) {
        if (entity.level() instanceof ServerLevel level) {
            EntityTargetIndex index = LEVELS.get(level);
            if (index == null) return;
            Entry entry = index.entities.remove(entity);
            if (entry != null) {
                index.detach(entry);
                index.version++;
            }
        }
    }

    /** Section callbacks also preserve vanilla's tie order when the entity changes sections. */
    public static void sectionChanged(Entity entity) {
        positionChanged(entity);
    }

    /** Every actual position write updates the point; most writes fit the leaf's existing loose bounds. */
    public static void positionChanged(Entity entity) {
        if (entity instanceof LivingEntity && entity.level() instanceof ServerLevel level) {
            EntityTargetIndex index = LEVELS.get(level);
            if (index == null) return;
            Entry entry = index.entities.get(entity);
            if (entry == null) return; // Constructors and untracked entities do not create index entries.
            Vec3 pos = entity.position();
            boolean moved = entry.tree.move(entry.leaf, pos.x, pos.y, pos.z);
            long section = SectionPos.asLong(entity.blockPosition());
            if (section != entry.section) {
                entry.section = section;
                entry.order = ++index.nextOrder;
                entry.tree.setOrder(entry.leaf, section, entry.order);
                moved = true;
            }
            if (moved) index.version++;
        }
    }

    /** Called after Unit.onSyncedDataUpdated has refreshed the shared owner-name cache. */
    public static void ownerMayHaveChanged(Entity entity) {
        if (entity instanceof Unit unit && entity.level() instanceof ServerLevel level) {
            EntityTargetIndex index = LEVELS.get(level);
            if (index == null) return;
            Entry entry = index.entities.get(entity);
            if (entry != null && !Objects.equals(entry.group.ownerName(), unit.getOwnerName())) {
                index.detach(entry);
                entry.group = classify(entry.entity);
                index.attach(entry); // Owner changes retain vanilla's within-section order.
                index.version++;
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            EntityTargetIndex index = LEVELS.remove(level);
            if (index != null) index.clear();
        }
    }

    @SubscribeEvent
    public static void onServerStop(ServerStoppedEvent event) {
        for (EntityTargetIndex index : LEVELS.values()) index.clear();
        LEVELS.clear();
    }

    private void clear() {
        entities.clear();
        trees.clear();
        version++;
    }

    public record TargetGroup(String ownerName, boolean unit, Faction faction, boolean neutralAttackable) {
        public Relationship relationshipTo(String owner) {
            if (ownerName == null) return Relationship.NEUTRAL;
            if (AlliancesServerEvents.isAllied(owner, ownerName) || owner.equals(ownerName))
                return Relationship.FRIENDLY;
            return owner.isBlank() || ownerName.isBlank() ? Relationship.NEUTRAL : Relationship.HOSTILE;
        }
    }

    private static TargetGroup classify(LivingEntity entity) {
        boolean isUnit = entity instanceof Unit;
        String owner = isUnit ? ((Unit) entity).getOwnerName() :
                entity instanceof Player player ? player.getName().getString() : null;
        Faction faction = isUnit ? Factions.getFaction((Unit) entity) : NonUnitServerEvents.getNonUnitFaction(entity);
        boolean passive = !isUnit && (entity instanceof Animal || entity instanceof AbstractFish || entity instanceof Villager);
        boolean neutralAttackable = !(entity instanceof Vex || entity instanceof ArmorStand ||
                entity instanceof PhantomSummon) && !passive;
        return new TargetGroup(owner, isUnit, faction, neutralAttackable);
    }

    private void attach(Entry entry) {
        entry.tree = trees.computeIfAbsent(entry.group, ignored -> new DynamicTargetTree<>());
        Vec3 pos = entry.entity.position();
        entry.leaf = entry.tree.insert(entry, pos.x, pos.y, pos.z, entry.section, entry.order);
    }

    private void detach(Entry entry) {
        entry.tree.remove(entry.leaf);
        if (entry.tree.root == null) trees.remove(entry.group);
    }

    /**
     * Repeatable, lazy nearest-first traversal by true distance. Consume each iterator within one server
     * call; position/ownership/lifecycle changes invalidate existing iterators instead of returning stale order.
     * No candidate list or complete distance sort is built before iteration.
     */
    public Iterable<LivingEntity> nearest(Vec3 origin, float range, Predicate<TargetGroup> groupCheck) {
        return ranked(origin, range, groupCheck, entity -> true, entity -> 0);
    }

    /** Lazy distance + non-negative penalty ordering; ties retain vanilla section/insertion order. */
    public Iterable<LivingEntity> ranked(Vec3 origin, float range, Predicate<TargetGroup> groupCheck,
                                         Predicate<LivingEntity> candidateCheck,
                                         ToDoubleFunction<LivingEntity> penalty) {
        return () -> new NearestIterator(origin, range, groupCheck, candidateCheck, penalty);
    }

    /** Convenience consumer preserving the original preferred pass followed by its ordinary fallback. */
    public LivingEntity findClosest(Vec3 origin, float range, Predicate<TargetGroup> groupCheck,
                                    Predicate<LivingEntity> preferred, ToDoubleFunction<LivingEntity> penalty,
                                    Predicate<LivingEntity> finalCheck) {
        if (preferred == null) {
            for (LivingEntity entity : ranked(origin, range, groupCheck, entity -> true, penalty))
                if (finalCheck.test(entity)) return entity;
            return null;
        }
        Map<TargetGroup, Boolean> accepted = new HashMap<>();
        Predicate<TargetGroup> groups = group -> accepted.computeIfAbsent(group, groupCheck::test);
        for (LivingEntity entity : ranked(origin, range, groups, preferred, penalty)) {
            if (finalCheck.test(entity)) return entity;
        }
        // Every preferred candidate already failed finalCheck. The fallback only needs ordinary ones.
        for (LivingEntity entity : ranked(origin, range, groups, entity -> !preferred.test(entity), penalty))
            if (finalCheck.test(entity)) return entity;
        return null;
    }

    private final class NearestIterator implements Iterator<LivingEntity> {
        private final long expectedVersion = version;
        private final Vec3 origin;
        private final float range;
        private final AABB bounds;
        private final Predicate<LivingEntity> candidateCheck;
        private final ToDoubleFunction<LivingEntity> penalty;
        private final NodeHeap queue = new NodeHeap();
        private LivingEntity next;

        NearestIterator(Vec3 origin, float range, Predicate<TargetGroup> groupCheck,
                        Predicate<LivingEntity> candidateCheck, ToDoubleFunction<LivingEntity> penalty) {
            checkValid();
            this.origin = origin;
            this.range = range;
            this.candidateCheck = candidateCheck;
            this.penalty = penalty;
            bounds = new AABB(origin.x - range, origin.y - range, origin.z - range,
                    origin.x + range, origin.y + range, origin.z + range);
            if (!(range >= 0)) return;
            for (var tree : trees.entrySet())
                if (groupCheck.test(tree.getKey())) enqueue(tree.getValue().root);
        }

        private void checkValid() {
            if (!level.getServer().isSameThread())
                throw new IllegalStateException("Entity target iteration must run on the server thread");
            if (expectedVersion != version)
                throw new ConcurrentModificationException("Target index changed; create a new nearest iterator");
        }

        private void enqueue(DynamicTargetTree.Node<Entry> node) {
            if (node == null) return;
            if (!node.isLeaf()) {
                double bound = node.minimumDistance(origin.x, origin.y, origin.z);
                if (bound <= range) queue.add(node, bound);
                return;
            }
            LivingEntity entity = node.value.entity;
            if (!entity.getBoundingBox().intersects(bounds)) return;
            double dx = node.x - origin.x, dy = node.y - origin.y, dz = node.z - origin.z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (!(distance <= range) || !candidateCheck.test(entity)) return;
            queue.add(node, distance + penalty.applyAsDouble(entity));
        }

        @Override
        public boolean hasNext() {
            checkValid();
            while (next == null && !queue.isEmpty()) {
                DynamicTargetTree.Node<Entry> node = queue.poll();
                if (node.isLeaf()) next = node.value.entity;
                else {
                    enqueue(node.left);
                    enqueue(node.right);
                }
            }
            return next != null;
        }

        @Override
        public LivingEntity next() {
            if (!hasNext()) throw new NoSuchElementException();
            LivingEntity result = next;
            next = null;
            return result;
        }
    }

    private static final class Entry {
        final LivingEntity entity;
        TargetGroup group;
        DynamicTargetTree<Entry> tree;
        DynamicTargetTree.Node<Entry> leaf;
        long section, order;
        Entry(LivingEntity entity, TargetGroup group, long order) {
            this.entity = entity;
            this.group = group;
            this.section = SectionPos.asLong(entity.blockPosition());
            this.order = order;
        }
    }

    /** Parallel arrays avoid allocating a temporary priority-queue object for each visited tree node. */
    private static final class NodeHeap {
        @SuppressWarnings("unchecked")
        private DynamicTargetTree.Node<Entry>[] nodes =
                (DynamicTargetTree.Node<Entry>[]) new DynamicTargetTree.Node<?>[16];
        private double[] scores = new double[16];
        private int size;

        boolean isEmpty() { return size == 0; }

        void add(DynamicTargetTree.Node<Entry> node, double score) {
            if (size == nodes.length) {
                nodes = Arrays.copyOf(nodes, size * 2);
                scores = Arrays.copyOf(scores, size * 2);
            }
            int at = size++;
            while (at > 0) {
                int parent = (at - 1) >>> 1;
                if (compare(node, score, nodes[parent], scores[parent]) >= 0) break;
                nodes[at] = nodes[parent];
                scores[at] = scores[parent];
                at = parent;
            }
            nodes[at] = node;
            scores[at] = score;
        }

        DynamicTargetTree.Node<Entry> poll() {
            DynamicTargetTree.Node<Entry> result = nodes[0];
            int last = --size;
            DynamicTargetTree.Node<Entry> node = nodes[last];
            double score = scores[last];
            nodes[last] = null;
            if (last == 0) return result;
            int at = 0;
            while (at < size / 2) {
                int child = at * 2 + 1;
                if (child + 1 < size && compare(nodes[child + 1], scores[child + 1], nodes[child], scores[child]) < 0)
                    child++;
                if (compare(node, score, nodes[child], scores[child]) <= 0) break;
                nodes[at] = nodes[child];
                scores[at] = scores[child];
                at = child;
            }
            nodes[at] = node;
            scores[at] = score;
            return result;
        }

        // Subtree tie minima allow coincident points to be yielded without expanding every tied subtree.
        private static int compare(DynamicTargetTree.Node<Entry> a, double aScore,
                                   DynamicTargetTree.Node<Entry> b, double bScore) {
            int score = Double.compare(aScore, bScore);
            return score != 0 ? score : DynamicTargetTree.compareOrder(a, b);
        }
    }
}
