package p2;

public class BtrfsNode {

    DiskSegment[] keys;
    BtrfsNode[] children;
    int[] index;

    public BtrfsNode(int n) {
        keys = new DiskSegment[n];
        children = new BtrfsNode[n + 1];
    }

}
