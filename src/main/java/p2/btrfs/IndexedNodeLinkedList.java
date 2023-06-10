package p2.btrfs;

/**
 * A linked list of nodes that are indexed.
 * This class is used to keep track of the path to a node in the tree.
 */
public class IndexedNodeLinkedList {

    /**
     * The parent of this node.
     */
    public IndexedNodeLinkedList parent;

    /**
     * The current node.
     */
    public BtrfsNode node;

    /**
     * The index in the current node.
     */
    public int index;

    /**
     * Creates a new {@link IndexedNodeLinkedList} instance.
     *
     * @param parent the parent of this node.
     * @param node the current node.
     * @param index the index in the current node.
     */
    public IndexedNodeLinkedList(IndexedNodeLinkedList parent, BtrfsNode node, int index) {
        this.parent = parent;
        this.node = node;
        this.index = index;
    }
}
