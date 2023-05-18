package p2.btrfs;

import p2.storage.EmptyStorageView;
import p2.storage.Interval;
import p2.storage.Storage;
import p2.storage.StorageView;

import java.util.List;

public class BtrfsFile {

    Storage storage;
    BtrfsNode root;

    int degree;

    int size;

    public BtrfsFile(Storage storage, int degree) {
        this.storage = storage;
        this.degree = degree;
        root = new BtrfsNode(degree);
    }

    public void write(int start, Interval interval) {
        //TODO
    }

    public void insert(int start, List<Interval> intervals, byte[] data) {

        // fill the intervals with the data
        int dataPos = 0;
        for (Interval interval : intervals) {
            storage.write(interval.start(), data, dataPos, interval.length());
            dataPos += interval.length();
        }

        size += data.length;

        int insertionSize = data.length;

        insert(intervals, findInsertionPosition(new IndexedNodeLinkedList(
            null, root, 0), start, 0, insertionSize, null), insertionSize);

    }

    public void insert(List<Interval> intervals, IndexedNodeLinkedList leaf, int remainingLength) {

        BtrfsNode leafNode = leaf.node;

        int amountToInsert = Math.min(intervals.size(), 2 * degree - 1 - leafNode.size);

        if (leaf.index < leafNode.size) {
            System.arraycopy(leafNode.keys, leaf.index, leafNode.keys, leaf.index + amountToInsert, leafNode.size - leaf.index);
        }

        int insertionEnd = leaf.index + amountToInsert;

        for (; leaf.index < insertionEnd; leaf.index++) {
            leafNode.keys[leaf.index] = intervals.get(0);
            intervals.remove(0);
            remainingLength -= leafNode.keys[leaf.index].length();
            leafNode.size++;
        }

        if (intervals.isEmpty()) {
            return;
        }

        int previousIndex = leaf.index;

        split(leaf);

        //update childLength if we will not be inserting into the same node
        if (previousIndex > degree - 1) {
            leaf.parent.node.childLength[leaf.parent.index - 1] -= remainingLength;
            leaf.parent.node.childLength[leaf.parent.index] += remainingLength;
        }

        insert(intervals, leaf, remainingLength);
    }

    private IndexedNodeLinkedList findInsertionPosition(IndexedNodeLinkedList node,
                                                        int start,
                                                        int cumulativeLength,
                                                        int insertionSize,
                                                        Interval splitKey) {

        BtrfsNode currentNode = node.node;

        if (currentNode.size == 0) {
            return node;
        }

        // if we had to split the key, we have to insert it into the leaf node
        if (currentNode.isLeaf() && splitKey != null) {

            // split if necessary
            if (currentNode.isFull()) {
                split(node);

                if (currentNode != node.node) {
                    node.parent.node.childLength[node.parent.index - 1] -= insertionSize;
                    node.parent.node.childLength[node.parent.index] += insertionSize;
                    currentNode = node.node;
                }
            }

            System.arraycopy(currentNode.keys, node.index, currentNode.keys, node.index + 1, currentNode.size - node.index);
            currentNode.keys[node.index] = splitKey;
            currentNode.size++;
        }

        for (int i = 0; i < currentNode.size; i++, node.index++) {

            if (!currentNode.isLeaf() && start <= cumulativeLength + currentNode.childLength[i]) {

                currentNode.childLength[i] += insertionSize;

                return findInsertionPosition(new IndexedNodeLinkedList(node, currentNode.children[i], 0), start, cumulativeLength, insertionSize, splitKey);
            }

            cumulativeLength += currentNode.childLength[i];

            if (start == cumulativeLength) {
                return node;
            }

            // if we insert it in the middle of the key -> split the key
            if (start < cumulativeLength + currentNode.keys[i].length()) {

                Interval oldInterval = currentNode.keys[i];

                //create new intervals for the left and right part of the old interval
                Interval newLeftInterval = new Interval(oldInterval.start(), (cumulativeLength + oldInterval.length()) - start);
                Interval newRightInterval = new Interval(newLeftInterval.start() + newLeftInterval.length(),
                    oldInterval.length() - newLeftInterval.length());

                //store the new left interval in the node
                currentNode.keys[i] = newLeftInterval;

                insertionSize += newRightInterval.length();
                splitKey = newRightInterval;

                if (currentNode.isLeaf()) {
                    return findInsertionPosition(node, start, cumulativeLength, insertionSize, splitKey);
                }
            }

            cumulativeLength += currentNode.keys[i].length();
        }

        currentNode.childLength[currentNode.size] += insertionSize;

        return findInsertionPosition(new IndexedNodeLinkedList(node, currentNode.children[currentNode.size], 0),
            start, cumulativeLength, insertionSize, splitKey);

    }

    public void split(IndexedNodeLinkedList node) {

        BtrfsNode currentNode = node.node;

        if (!currentNode.isFull()) {
            throw new IllegalArgumentException("node is not full when splitting");
        }

        IndexedNodeLinkedList parent = node.parent;
        BtrfsNode parentNode = parent == null ? null : parent.node;

        if (parentNode != null && parentNode.isFull()) {
            split(parent);

            // parentNode might have changed after splitting
            parentNode = parent.node;
        }

        //create new node
        BtrfsNode right = new BtrfsNode(degree);

        //calculate length of right node
        int rightLength = currentNode.childLength[2 * degree - 1];
        for (int i = degree; i < currentNode.size; i++) {
            rightLength += currentNode.childLength[i];
            rightLength += currentNode.keys[i].length();
        }

        //copy keys, children and childLengths to right node
        System.arraycopy(currentNode.keys, degree, right.keys, 0, degree - 1);
        System.arraycopy(currentNode.children, degree, right.children, 0, degree);
        System.arraycopy(currentNode.childLength, degree, right.childLength, 0, degree);

        //update sizes of left and right
        currentNode.size = degree - 1;
        right.size = degree - 1;

        //splitting the root
        if (parentNode == null) {

            //create new root
            BtrfsNode newRoot = new BtrfsNode(degree);

            //add middle key of node to parent
            newRoot.keys[0] = currentNode.keys[degree - 1];

            //add left and right to children of parent
            newRoot.children[0] = currentNode;
            newRoot.children[1] = right;

            //set childLengths of parent
            newRoot.childLength[0] = size - rightLength - currentNode.keys[degree - 1].length();
            newRoot.childLength[1] = rightLength;

            //set sizes of parent
            newRoot.size = 1;

            //set new root
            root = newRoot;

            //update LinkedParentList
            node.parent = new IndexedNodeLinkedList(null, newRoot, node.index > degree - 1 ? 1 : 0);

            if (node.index > degree - 1) {
                node.node = right;
                node.index -= degree;
            }

        } else {

            int parentIndex = parent.index;

            // move keys of parent to the right
            System.arraycopy(parentNode.keys, parentIndex, parentNode.keys, parentIndex + 1, parentNode.size - parentIndex);

            //move children and childrenLength of parent to the right
            for (int i = parentNode.size; i > parentIndex; i--) {
                parentNode.children[i + 1] = parentNode.children[i];
                parentNode.childLength[i + 1] = parentNode.childLength[i];
            }
            System.arraycopy(parentNode.children, parentIndex + 1, parentNode.children, parentIndex + 2, parentNode.size - parentIndex);

            //add middle key of node to parent
            parentNode.keys[parentIndex] = currentNode.keys[degree - 1];

            //add right to children of parent
            parentNode.children[parentIndex + 1] = right;

            //set childLengths of parent
            parentNode.childLength[parentIndex + 1] = rightLength;
            parentNode.childLength[parentIndex] -= rightLength + currentNode.keys[degree - 1].length();

            //update size of parent
            parentNode.size++;

            //update link to parent if necessary
            if (node.index > degree - 1) {
                parent.node = right;
                parent.index++;

                node.node = right;
                node.index -= degree;
            }
        }

        //reset removed elements of node
        for (int i = degree - 1; i < 2 * degree - 1; i++) {
            currentNode.children[i] = null;
            currentNode.childLength[i] = 0;
            currentNode.keys[i] = null;
        }
    }

    public StorageView read(int start, int length) {
        return read(start, length, root, 0, 0);
    }

    public StorageView read(int start, int length, BtrfsNode node, int cumulativeLength, int lengthRead) {

        StorageView view = new EmptyStorageView(storage);

        for (int i = 0; i < node.size; i++) {
            //before i-th key and i-th child.

            //read from i-th child if start is in front of or in the i-th child, and it exists
            if (node.children[i] != null && start < cumulativeLength + node.childLength[i]) {
                view = view.plus(read(start, length, node.children[i], cumulativeLength, lengthRead));
                lengthRead += Math.min(node.childLength[i], length - lengthRead);
            }

            cumulativeLength += node.childLength[i];

            //check if we have read enough
            if (lengthRead == length) {
                return view;
            } else if (lengthRead > length) {
                throw new AssertionError(); //sanity check
            }


            Interval key = node.keys[i];

            //if there is no next key we are done for this node
            if (key == null) {
                return view;
            }

            //read from i-th key if and start is in front of or in the i-th key
            if (start < cumulativeLength + key.length()) {

                int viewStart = key.start() + Math.max(0, start - cumulativeLength);
                int viewLength = Math.min(key.length() - (viewStart - key.start()), length - lengthRead);

                view = view.plus(storage.createView(new Interval(viewStart, viewLength)));
                lengthRead += viewLength;
            }

            cumulativeLength += key.length();

            //check if we have read enough
            if (lengthRead == length) {
                return view;
            } else if (lengthRead > length) {
                throw new AssertionError(); //sanity check
            }
        }

        //we reached the last child; read from it if it exists and return
        if (node.children[node.size] == null) {
            return view;
        }

        //read from last child if it exists, because we have not read enough if we are here
        view = view.plus(read(start, length, node.children[node.size], cumulativeLength, lengthRead));

        return view;
    }

    public void remove(int start, int length) {
        remove(start, length, root, new IndexedNodeLinkedList(null, null, -1), 0, 0);
    }

    public void remove(int start, int length, BtrfsNode node, IndexedNodeLinkedList parent, int cumulativeLength, int removedLength) {

        //iterate over all children and keys
        for (int i = 0; i < node.size; i++) {
            //before i-th child and i-th child.

            //check if we have removed enough
            if (removedLength > length) {
                throw new AssertionError(); //sanity check
            } else if (removedLength == length) {
                return;
            }

            //remove from i-th child if start is in front of or in the i-th child, and it exists
            if (node.children[i] != null && start <= cumulativeLength + node.childLength[i]) {

                //calculate how much we will remove from the child
                int removedInChild = Math.min(length - removedLength, node.childLength[i] - (start - cumulativeLength));

                //update childLength of parent accordingly
                node.childLength[i] -= removedInChild;

                //update removedLength
                removedLength += removedInChild;

                //remove from child
                remove(start, length, node, new IndexedNodeLinkedList(parent, node, i), cumulativeLength, removedLength);

                //check if we have removed enough
                if (removedLength == length) {
                    return;
                } else if (removedLength > length) {
                    throw new AssertionError(); //sanity check
                }
            }

            cumulativeLength += node.childLength[i];

            //get the i-th key
            Interval key = node.keys[i];
            int keyStart = cumulativeLength + 1;

            //the key might have been removed through merging
            if (key == null) {
                return;
            }

            //if start is in the i-th key we just have to shorten the interval
            if (start > keyStart && start < cumulativeLength + 1 + key.length()) {

                //calculate the new length of the key
                int newLength = start - keyStart;

                //update the key
                node.keys[i] = new Interval(key.start(), newLength);

                //update removedLength
                removedLength += key.length() - newLength;

                //continue with next key
                continue;
            }

            //if start is in front of or at the start of the i-th key we have to remove the key
            if (start <= keyStart) {

                //if we are in a leaf node we can just remove the key
                if (node.children[0] == null) {

                    ensureSize(parent, node);

                    //move all keys after the removed key to the left
                    System.arraycopy(node.keys, i + 1, node.keys, i, node.size - i - 1);

                    //remove (duplicated) last key
                    node.keys[node.size - 1] = null;

                    //update size
                    node.size--;

                    //update removedLength
                    removedLength += key.length();

                    //the next key moved one index to the left
                    i--;

                } else { //remove key from inner node

                    //try to replace with rightmost key of left child
                    if (node.children[i].size >= degree) {
                        Interval removedKey = removeRightMostKey(new IndexedNodeLinkedList(parent, node, i), node.children[i]);

                        //update childLength of current node
                        node.childLength[i] -= removedKey.length();

                        //update key
                        node.keys[i] = removedKey;

                        //update removedLength
                        removedLength += key.length();

                        //try to replace with leftmost key of right child
                    } else if (node.children[i + 1].size >= degree) {
                        Interval removedKey = removeLeftMostKey(new IndexedNodeLinkedList(parent, node, i + 1), node.children[i + 1]);

                        //update childLength of current node
                        node.childLength[i + 1] -= removedKey.length();

                        //update key
                        node.keys[i] = removedKey;

                        //update removedLength
                        removedLength += key.length();

                        //if both children have only degree - 1 keys we have to merge them and remove the key from the merged node
                    } else {

                        // save the length of the left and right child before merging because we need them later
                        final int leftNodeLength = node.childLength[i];
                        final int rightNodeLength = node.childLength[i + 1];

                        //merge the two children
                        mergeWithRightChild(node, i);

                        //calculate how much we will remove from the child
                        int removedInChild = Math.min(length - removedLength,
                            node.childLength[i] - (start - cumulativeLength - leftNodeLength));

                        //remove the key from the merged node
                        remove(leftNodeLength + 1, node.keys[i].length(), node.children[i],
                            new IndexedNodeLinkedList(parent, node, i), cumulativeLength - leftNodeLength, removedLength);

                        //update childLength of current node
                        node.childLength[i] -= removedInChild;

                        //update removedLength
                        removedLength += removedInChild;

                        //add the right child to the cumulative length
                        cumulativeLength += rightNodeLength;
                    }

                }

            }

            // update cumulativeLength after visiting the i-th key
            cumulativeLength += key.length();

        } // only the last child is left

        // check if we have removed enough
        if (removedLength > length) {
            throw new AssertionError(); //sanity check
        } else if (removedLength == length) {
            return;
        }

        // remove from the last child if start is in front of or in the i-th child, and it exists
        if (node.children[node.size] != null && start <= cumulativeLength + node.childLength[node.size]) {

            // calculate how much we will remove from the child
            int removedInChild = Math.min(length - removedLength, node.childLength[node.size] - (start - cumulativeLength));

            // update childLength of parent accordingly
            node.childLength[node.size] -= removedInChild;

            // update removedLength
            removedLength += removedInChild;

            //remove from child
            remove(start, length, node, new IndexedNodeLinkedList(parent, node, node.size), cumulativeLength, removedLength);
        }

        //check if we have traversed the whole tree
        if (parent.node == null) {
            throw new IllegalArgumentException("start + length is out of bounds");
        }

    }

    private Interval removeRightMostKey(IndexedNodeLinkedList parent, BtrfsNode node) {
        //node has at least degree keys

        if (parent.node.size < node.degree) {
            throw new AssertionError(); //sanity check
        }

        //check if node is a leaf
        if (node.children[0] == null) {

            ensureSize(parent, node);

            //get right most key
            Interval key = node.keys[node.size - 1];

            //remove key
            node.keys[node.size - 1] = null;

            //update size
            node.size--;

            return key;
        } else { //if node is an inner node continue downward

            //recursively remove from rightmost child
            Interval key = removeRightMostKey(new IndexedNodeLinkedList(parent, node, node.size), node);

            //update childLength
            node.childLength[node.size] -= key.length();

            return key;
        }
    }

    private Interval removeLeftMostKey(IndexedNodeLinkedList parent, BtrfsNode node) {
        //node has at least degree keys

        if (parent.node.size < degree) {
            throw new AssertionError(); //sanity check
        }

        //check if node is a leaf
        if (node.children[0] == null) {

            ensureSize(parent, node);

            //get left most key
            final Interval key = node.keys[0];

            //move all other keys to the left
            System.arraycopy(node.keys, 1, node.keys, 0, node.size - 1);

            //remove (duplicated) last key
            node.keys[node.size - 1] = null;

            //update size
            node.size--;

            return key;
        } else { //if node is an inner node continue downward

            //recursively remove from leftmost child
            Interval key = removeLeftMostKey(new IndexedNodeLinkedList(parent, node, node.size), node.children[0]);

            //update childLength
            node.childLength[0] -= key.length();

            return key;
        }
    }

    private void ensureSize(IndexedNodeLinkedList parent, BtrfsNode node) {

        //check if node has at least degree keys
        if (node.size < degree) {

            //get parent node and index
            BtrfsNode parentNode = parent.node;
            int parentIndex = parent.index;

            //get left and right sibling
            BtrfsNode leftSibling = parentIndex > 0 ? parentNode.children[parentIndex - 1] : null;
            BtrfsNode rightSibling = parentIndex < parentNode.size - 1 ? parentNode.children[parentIndex + 1] : null;

            //rotate a key from left or right node if possible
            if (leftSibling != null && leftSibling.size >= degree) {
                rotateFromLeftChild(parentNode, parentIndex);
            } else if (rightSibling != null && rightSibling.size >= degree) {
                rotateFromRightChild(parentNode, parentIndex);
            } else { // if we can't rotate, merge with left or right node

                // ensure that the parent has at least degree keys before merging
                if (parentNode.size < degree) {

                    // the root does not have to have at least degree keys
                    if (parentNode != root) {
                        // recursively fix size of parent
                        ensureSize(parent.parent, parentNode);
                    }
                }

                if (parentIndex > 0) {
                    mergeWithLeftChild(parentNode, parentIndex);
                } else {
                    mergeWithRightChild(parentNode, parentIndex);
                }

                //if the root has no keys left after merging, set the only node as the new root
                if (root.size == 0) {
                    root = root.children[0];
                }
            }
        }
    }

    private void mergeWithLeftChild(BtrfsNode parent, int index) {

        //get middle and left child
        BtrfsNode middleChild = parent.children[index];
        BtrfsNode leftChild = parent.children[index - 1];

        //move keys and children of middle child to the right
        System.arraycopy(middleChild.keys, 0, middleChild.keys, degree, middleChild.size);
        System.arraycopy(middleChild.children, 0, middleChild.children, degree, middleChild.size + 1);
        System.arraycopy(middleChild.childLength, 0, middleChild.childLength, degree, middleChild.size + 1);

        //move key and children of left child to the middle child
        System.arraycopy(leftChild.keys, 0, middleChild.keys, 0, leftChild.size);
        System.arraycopy(leftChild.children, 0, middleChild.children, 0, leftChild.size + 1);
        System.arraycopy(leftChild.childLength, 0, middleChild.childLength, 0, leftChild.size + 1);

        //move key of parent into middle child
        middleChild.keys[degree - 1] = parent.keys[index - 1];

        //update childLength of parent
        parent.childLength[index] += parent.keys[index - 1].length() + parent.childLength[index - 1];

        //move keys and children of parent to the left
        System.arraycopy(parent.keys, index, parent.keys, index - 1, parent.size - index);
        System.arraycopy(parent.children, index + 1, parent.children, index, parent.size - index + 1);
        System.arraycopy(parent.childLength, index + 1, parent.childLength, index, parent.size - index + 1);

        //update size of parent and middle child
        parent.size--;
        middleChild.size += 2 * degree - 1;
    }

    private void mergeWithRightChild(BtrfsNode parent, int index) {

        //get middle and right child
        BtrfsNode middleChild = parent.children[index];
        BtrfsNode rightChild = parent.children[index + 1];

        //move key and children of right child to the middle child
        System.arraycopy(rightChild.keys, 0, middleChild.keys, degree, rightChild.size);
        System.arraycopy(rightChild.children, 0, middleChild.children, degree, rightChild.size + 1);
        System.arraycopy(rightChild.childLength, 0, middleChild.childLength, degree, rightChild.size + 1);

        //move key of parent into middle child
        middleChild.keys[degree - 1] = parent.keys[index];

        //update childLength of parent
        parent.childLength[index] += parent.keys[index].length() + parent.childLength[index + 1];

        //move keys and children of parent to the left
        System.arraycopy(parent.keys, index + 1, parent.keys, index, parent.size - index - 1);
        System.arraycopy(parent.children, index + 2, parent.children, index + 1, parent.size - index);
        System.arraycopy(parent.childLength, index + 2, parent.childLength, index + 1, parent.size - index);

        //update size of parent and middle child
        parent.size--;
        middleChild.size += 2 * degree - 1;
    }

    private void rotateFromLeftChild(BtrfsNode parent, int index) {

        //get left child
        BtrfsNode leftChild = parent.children[index - 1];

        //store and remove last key and child of left child
        final Interval key = leftChild.keys[leftChild.size - 1];
        leftChild.keys[leftChild.size - 1] = null;

        final BtrfsNode lastChild = leftChild.children[leftChild.size];
        leftChild.children[leftChild.size] = null;

        final int lastChildLength = leftChild.childLength[leftChild.size];
        leftChild.childLength[leftChild.size] = 0;

        // update size of left child
        leftChild.size--;

        // update childLength of parent (childLength of the parent of the parent doesn't change)
        parent.childLength[index - 1] -= key.length() + lastChildLength;

        //store and replace key of parent
        final Interval parentKey = parent.keys[index];
        parent.keys[index] = key;

        //get middle child
        BtrfsNode middleChild = parent.children[index];

        //move entries of middle child to the right
        System.arraycopy(middleChild.keys, 0, middleChild.keys, 1, middleChild.size);
        System.arraycopy(middleChild.children, 0, middleChild.children, 1, middleChild.size + 1);
        System.arraycopy(middleChild.childLength, 0, middleChild.childLength, 1, middleChild.size + 1);

        //add key and child to middle child
        middleChild.keys[0] = parentKey;
        middleChild.children[0] = lastChild;
        middleChild.childLength[0] = lastChildLength;

        //update size of middle child
        middleChild.size++;

        //update childLength of parent
        parent.childLength[index] += parentKey.length() + lastChildLength;
    }

    private void rotateFromRightChild(BtrfsNode parent, int index) {

        //get right child
        final BtrfsNode rightChild = parent.children[index + 1];

        //store and remove first key and child of right child
        final Interval key = rightChild.keys[0];
        rightChild.keys[0] = null;

        final BtrfsNode lastChild = rightChild.children[0];
        rightChild.children[0] = null;

        int lastChildLength = rightChild.childLength[0];
        rightChild.childLength[0] = 0;

        //update size of right child
        rightChild.size--;

        //update childLength of parent (childLength of the parent of the parent doesn't change)
        parent.childLength[index + 1] -= key.length() + lastChildLength;

        //store and replace key of parent
        Interval parentKey = parent.keys[index];
        parent.keys[index] = key;

        //get middle child
        BtrfsNode middleChild = parent.children[index];

        //add key and child to middle child
        middleChild.keys[parent.size] = parentKey;
        middleChild.children[parent.size + 1] = lastChild;
        middleChild.childLength[parent.size + 1] = lastChildLength;

        //update size of middle child
        middleChild.size++;

        //update childLength of parent
        parent.childLength[index] += parentKey.length() + lastChildLength;
    }

    public int getSize() {
        return size;
    }
}
