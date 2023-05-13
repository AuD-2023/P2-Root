package p2.btrfs;

import p2.storage.StorageInterval;

public class BtrfsNode {

    public int size = 0;
    public StorageInterval[] keys;
    public BtrfsNode[] children;
    public int[] childLength;

    public BtrfsNode(int degree) {
        keys = new StorageInterval[2*degree - 1];
        children = new BtrfsNode[2*degree];
        childLength = new int[2*degree];
    }
}
