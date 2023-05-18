package p2.btrfs;

class IndexedNodeLinkedList {

    IndexedNodeLinkedList parent;
    BtrfsNode node;
    int index;

    public IndexedNodeLinkedList(IndexedNodeLinkedList parent, BtrfsNode node, int index) {
        this.parent = parent;
        this.node = node;
        this.index = index;
    }
}
