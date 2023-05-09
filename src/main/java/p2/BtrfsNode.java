package p2;

import p2.storage.StorageInterval;

public class BtrfsNode {

    StorageInterval[] keys;
    BtrfsNode[] children;
    int[] index;

    public BtrfsNode(int n) {
        keys = new StorageInterval[n];
        children = new BtrfsNode[n + 1];
    }
}
