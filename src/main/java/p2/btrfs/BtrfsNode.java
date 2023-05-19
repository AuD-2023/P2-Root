package p2.btrfs;

import p2.storage.Interval;

public class BtrfsNode {

    public int degree;
    public int size = 0;
    public Interval[] keys;
    public BtrfsNode[] children;
    public int[] childLengths;

    public BtrfsNode(int degree) {
        this.degree = degree;
        keys = new Interval[2 * degree - 1];
        children = new BtrfsNode[2 * degree];
        childLengths = new int[2 * degree];
    }

    public boolean isFull() {
        return size >= 2 * degree - 1;
    }

    public boolean isLeaf() {
        return children[0] == null;
    }

}
