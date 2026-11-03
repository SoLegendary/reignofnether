package com.solegendary.reignofnether.unit.targeting;

/** Incremental, height-balanced point BVH. Bounds are loose so small movements do not rebuild branches. */
final class DynamicTargetTree<T> {
    private static final double MARGIN = 0.5;
    Node<T> root;

    Node<T> insert(T value, double x, double y, double z, long section, long order) {
        Node<T> leaf = new Node<>(value);
        leaf.x = x;
        leaf.y = y;
        leaf.z = z;
        leaf.section = section;
        leaf.order = order;
        leaf.expandPoint();
        insertLeaf(leaf, null);
        return leaf;
    }

    boolean move(Node<T> leaf, double x, double y, double z) {
        if (leaf.x == x && leaf.y == y && leaf.z == z) return false;
        leaf.x = x;
        leaf.y = y;
        leaf.z = z;
        if (x >= leaf.minX && x <= leaf.maxX && y >= leaf.minY && y <= leaf.maxY &&
                z >= leaf.minZ && z <= leaf.maxZ) return true;
        Node<T> spare = removeLeaf(leaf);
        leaf.expandPoint();
        insertLeaf(leaf, spare); // Reuse the detached parent: relocation allocates no new tree nodes.
        return true;
    }

    void setOrder(Node<T> leaf, long section, long order) {
        leaf.section = section;
        leaf.order = order;
        for (Node<T> node = leaf.parent; node != null; node = node.parent) refit(node);
    }

    void remove(Node<T> leaf) {
        removeLeaf(leaf);
    }

    private void insertLeaf(Node<T> leaf, Node<T> spare) {
        if (root == null) {
            root = leaf;
            leaf.parent = null;
            return;
        }
        Node<T> sibling = root;
        while (!sibling.isLeaf()) {
            double combined = unionArea(sibling, leaf);
            double inherited = 2 * (combined - sibling.area());
            double leftCost = insertionCost(sibling.left, leaf, inherited);
            double rightCost = insertionCost(sibling.right, leaf, inherited);
            // Descend to a leaf so insertion changes ancestor heights by at most one; local rotations
            // can then keep a strict height balance even after adversarial insertion/movement orders.
            sibling = leftCost < rightCost ? sibling.left : sibling.right;
        }
        Node<T> parent = spare != null ? spare : new Node<>(null);
        Node<T> previousParent = sibling.parent;
        parent.parent = previousParent;
        parent.left = sibling;
        parent.right = leaf;
        sibling.parent = parent;
        leaf.parent = parent;
        refit(parent);
        replaceChild(previousParent, sibling, parent);
        repair(parent);
    }

    private static <T> double insertionCost(Node<T> node, Node<T> leaf, double inherited) {
        double combined = unionArea(node, leaf);
        return (node.isLeaf() ? combined : combined - node.area()) + inherited;
    }

    private Node<T> removeLeaf(Node<T> leaf) {
        if (leaf == root) {
            root = null;
            leaf.parent = null;
            return null;
        }
        Node<T> parent = leaf.parent;
        Node<T> grandparent = parent.parent;
        Node<T> sibling = parent.left == leaf ? parent.right : parent.left;
        replaceChild(grandparent, parent, sibling);
        sibling.parent = grandparent;
        leaf.parent = null;
        if (grandparent != null) repair(grandparent);
        parent.parent = parent.left = parent.right = null;
        return parent;
    }

    private void replaceChild(Node<T> parent, Node<T> oldChild, Node<T> newChild) {
        if (parent == null) root = newChild;
        else if (parent.left == oldChild) parent.left = newChild;
        else parent.right = newChild;
    }

    private void repair(Node<T> node) {
        while (node != null) {
            refit(node);
            node = balance(node);
            node = node.parent;
        }
    }

    private Node<T> balance(Node<T> a) {
        if (a.isLeaf() || a.height < 2) return a;
        Node<T> b = a.left, c = a.right;
        if (c.height - b.height > 1) {
            Node<T> f = c.left, g = c.right;
            c.parent = a.parent;
            replaceChild(a.parent, a, c);
            c.left = a;
            a.parent = c;
            if (f.height > g.height) {
                c.right = f;
                a.right = g;
                f.parent = c;
                g.parent = a;
            } else {
                c.right = g;
                a.right = f;
                g.parent = c;
                f.parent = a;
            }
            refit(a);
            refit(c);
            return c;
        }
        if (b.height - c.height > 1) {
            Node<T> d = b.left, e = b.right;
            b.parent = a.parent;
            replaceChild(a.parent, a, b);
            b.right = a;
            a.parent = b;
            if (d.height > e.height) {
                b.left = d;
                a.left = e;
                d.parent = b;
                e.parent = a;
            } else {
                b.left = e;
                a.left = d;
                e.parent = b;
                d.parent = a;
            }
            refit(a);
            refit(b);
            return b;
        }
        return a;
    }

    private static <T> void refit(Node<T> node) {
        Node<T> a = node.left, b = node.right;
        node.minX = Math.min(a.minX, b.minX);
        node.minY = Math.min(a.minY, b.minY);
        node.minZ = Math.min(a.minZ, b.minZ);
        node.maxX = Math.max(a.maxX, b.maxX);
        node.maxY = Math.max(a.maxY, b.maxY);
        node.maxZ = Math.max(a.maxZ, b.maxZ);
        node.height = 1 + Math.max(a.height, b.height);
        Node<T> first = compareOrder(a, b) <= 0 ? a : b;
        node.section = first.section;
        node.order = first.order;
    }

    static <T> int compareOrder(Node<T> a, Node<T> b) {
        int section = Long.compare(a.section, b.section);
        return section != 0 ? section : Long.compare(a.order, b.order);
    }

    private static double unionArea(Node<?> a, Node<?> b) {
        double x = Math.max(a.maxX, b.maxX) - Math.min(a.minX, b.minX);
        double y = Math.max(a.maxY, b.maxY) - Math.min(a.minY, b.minY);
        double z = Math.max(a.maxZ, b.maxZ) - Math.min(a.minZ, b.minZ);
        return 2 * (x * y + y * z + z * x);
    }

    static final class Node<T> {
        final T value;
        Node<T> parent, left, right;
        int height;
        long section, order;
        double x, y, z;
        double minX, minY, minZ, maxX, maxY, maxZ;

        Node(T value) { this.value = value; }
        boolean isLeaf() { return value != null; }

        void expandPoint() {
            minX = x - MARGIN;
            minY = y - MARGIN;
            minZ = z - MARGIN;
            maxX = x + MARGIN;
            maxY = y + MARGIN;
            maxZ = z + MARGIN;
        }

        double area() {
            double x = maxX - minX, y = maxY - minY, z = maxZ - minZ;
            return 2 * (x * y + y * z + z * x);
        }

        double minimumDistance(double x, double y, double z) {
            double dx = Math.max(Math.max(minX - x, x - maxX), 0);
            double dy = Math.max(Math.max(minY - y, y - maxY), 0);
            double dz = Math.max(Math.max(minZ - z, z - maxZ), 0);
            return Math.sqrt(dx * dx + dy * dy + dz * dz);
        }
    }
}
