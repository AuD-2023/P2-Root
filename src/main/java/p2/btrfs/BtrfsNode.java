package p2.btrfs;

import p2.storage.Interval;

public class BtrfsNode {

    public int size = 0;
    public Interval[] keys;
    public BtrfsNode[] children;
    public int[] childLength;

    public BtrfsNode(int degree) {
        keys = new Interval[2 * degree - 1];
        children = new BtrfsNode[2 * degree];
        childLength = new int[2 * degree];
    }
}
