package p2.btrfs;

public class IndexedNodeLinkedList {

    public IndexedNodeLinkedList parent;
    public BtrfsNode node;
    public int index;

    public IndexedNodeLinkedList(IndexedNodeLinkedList parent, BtrfsNode node, int index) {
        this.parent = parent;
        this.node = node;
        this.index = index;
    }
}
