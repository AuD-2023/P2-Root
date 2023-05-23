package p2.btrfs;

import p2.storage.Interval;

/**
 * A node in a BtrfsTree. This class is used to represent both internal and leaf nodes.
 */
public class BtrfsNode {

    /**
     * The degree of the tree.
     */
    public final int degree;

    /**
     * The number of keys in this node.
     */
    public int size = 0;

    /**
     * The keys in this node.
     */
    public Interval[] keys;

    /**
     * The children of this node.
     */
    public BtrfsNode[] children;

    /**
     * The lengths of the children of this node.
     */
    public int[] childLengths;

    /**
     * Creates a new {@link BtrfsNode} instance.
     *
     * @param degree the degree of the tree.
     */
    public BtrfsNode(int degree) {
        this.degree = degree;
        keys = new Interval[2 * degree - 1];
        children = new BtrfsNode[2 * degree];
        childLengths = new int[2 * degree];
    }

    /**
     * Checks if this node is full.
     *
     * @return true if this node is full, false otherwise.
     */
    public boolean isFull() {
        return size >= 2 * degree - 1;
    }

    /**
     * Checks if this node is a leaf.
     *
     * @return true if this node is a leaf, false otherwise.
     */
    public boolean isLeaf() {
        return children[0] == null;
    }

}
