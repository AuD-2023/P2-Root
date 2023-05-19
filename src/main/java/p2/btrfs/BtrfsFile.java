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
        // TODO
    }

    public StorageView readAll() {
        return readAll(root);
    }

    private StorageView readAll(BtrfsNode node) {

        StorageView view = new EmptyStorageView(storage);

        for (int i = 0; i < node.size; i++) {
            // before i-th key and i-th child.

            // read from i-th child if it exists
            if (node.children[i] != null) {
                view = view.plus(readAll(node.children[i]));
            }

            Interval key = node.keys[i];

            // read from i-th key
            view = view.plus(storage.createView(new Interval(key.start(), key.length())));
        }

        // read from last child if it exists
        if (node.children[node.size] != null) {
            view = view.plus(readAll(node.children[node.size]));
        }

        return view;
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

    public void insert(List<Interval> intervals, IndexedNodeLinkedList indexedLeaf, int remainingLength) {

        int amountToInsert = Math.min(intervals.size(), 2 * degree - 1 - indexedLeaf.node.size);

        if (indexedLeaf.index < indexedLeaf.node.size) {
            System.arraycopy(indexedLeaf.node.keys, indexedLeaf.index, indexedLeaf.node.keys, indexedLeaf.index + amountToInsert, indexedLeaf.node.size - indexedLeaf.index);
        }

        int insertionEnd = indexedLeaf.index + amountToInsert;

        for (; indexedLeaf.index < insertionEnd; indexedLeaf.index++) {
            indexedLeaf.node.keys[indexedLeaf.index] = intervals.get(0);
            intervals.remove(0);
            remainingLength -= indexedLeaf.node.keys[indexedLeaf.index].length();
            indexedLeaf.node.size++;
        }

        if (intervals.isEmpty()) {
            return;
        }

        int previousIndex = indexedLeaf.index;

        split(indexedLeaf);

        // update childLength if we will not be inserting into the same node
        if (previousIndex > degree - 1) {
            indexedLeaf.parent.node.childLengths[indexedLeaf.parent.index - 1] -= remainingLength;
            indexedLeaf.parent.node.childLengths[indexedLeaf.parent.index] += remainingLength;
        }

        insert(intervals, indexedLeaf, remainingLength);
    }

    private IndexedNodeLinkedList findInsertionPosition(IndexedNodeLinkedList indexedNode,
                                                        int start,
                                                        int cumulativeLength,
                                                        int insertionSize,
                                                        Interval splitKey) {

        if (indexedNode.node.size == 0) {
            return indexedNode;
        }

        // if we had to split the key, we have to insert it into the leaf node
        if (indexedNode.node.isLeaf() && splitKey != null) {

            // split if necessary
            if (indexedNode.node.isFull()) {
                BtrfsNode previousNode = indexedNode.node;

                split(indexedNode);

                if (previousNode != indexedNode.node) {
                    indexedNode.parent.node.childLengths[indexedNode.parent.index - 1] -= insertionSize;
                    indexedNode.parent.node.childLengths[indexedNode.parent.index] += insertionSize;
                }
            }

            System.arraycopy(indexedNode.node.keys, indexedNode.index, indexedNode.node.keys, indexedNode.index + 1, indexedNode.node.size - indexedNode.index);
            indexedNode.node.keys[indexedNode.index] = splitKey;
            indexedNode.node.size++;

            return indexedNode;
        }

        for (int i = indexedNode.index; i < indexedNode.node.size; i++, indexedNode.index++) {

            if (!indexedNode.node.isLeaf() && start <= cumulativeLength + indexedNode.node.childLengths[i]) {

                indexedNode.node.childLengths[i] += insertionSize;

                return findInsertionPosition(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[i], 0), start, cumulativeLength, insertionSize, splitKey);
            }

            cumulativeLength += indexedNode.node.childLengths[i];

            if (start == cumulativeLength) {
                return indexedNode;
            }

            // if we insert it in the middle of the key -> split the key
            if (start < cumulativeLength + indexedNode.node.keys[i].length()) {

                Interval oldInterval = indexedNode.node.keys[i];

                // create new intervals for the left and right part of the old interval
                Interval newLeftInterval = new Interval(oldInterval.start(), (cumulativeLength + oldInterval.length()) - start);
                Interval newRightInterval = new Interval(newLeftInterval.start() + newLeftInterval.length(),
                    oldInterval.length() - newLeftInterval.length());

                // store the new left interval in the node
                indexedNode.node.keys[i] = newLeftInterval;

                insertionSize += newRightInterval.length();
                splitKey = newRightInterval;

                if (indexedNode.node.isLeaf()) {
                    indexedNode.index++;
                    return findInsertionPosition(indexedNode, start, cumulativeLength, insertionSize, splitKey);
                }
            }

            cumulativeLength += indexedNode.node.keys[i].length();
        }

        indexedNode.node.childLengths[indexedNode.node.size] += insertionSize;

        return findInsertionPosition(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.node.size], 0),
            start, cumulativeLength, insertionSize, splitKey);
    }

    public void split(IndexedNodeLinkedList indexedNode) {

        if (!indexedNode.node.isFull()) {
            throw new IllegalArgumentException("node is not full when splitting");
        }

        BtrfsNode originalNode = indexedNode.node;
        IndexedNodeLinkedList parent = indexedNode.parent;
        BtrfsNode parentNode = parent == null ? null : parent.node;

        if (parentNode != null && parentNode.isFull()) {
            split(parent);

            // parentNode might have changed after splitting
            parentNode = parent.node;
        }

        // create new node
        BtrfsNode right = new BtrfsNode(degree);

        // calculate length of right node
        int rightLength = indexedNode.node.childLengths[2 * degree - 1];
        for (int i = degree; i < indexedNode.node.size; i++) {
            rightLength += indexedNode.node.childLengths[i];
            rightLength += indexedNode.node.keys[i].length();
        }

        // copy keys, children and childLengths to right node
        System.arraycopy(indexedNode.node.keys, degree, right.keys, 0, degree - 1);
        System.arraycopy(indexedNode.node.children, degree, right.children, 0, degree);
        System.arraycopy(indexedNode.node.childLengths, degree, right.childLengths, 0, degree);

        // update sizes of left and right
        indexedNode.node.size = degree - 1;
        right.size = degree - 1;

        // splitting the root
        if (parentNode == null) {

            // create new root
            BtrfsNode newRoot = new BtrfsNode(degree);

            // add middle key of node to parent
            newRoot.keys[0] = indexedNode.node.keys[degree - 1];

            // add left and right to children of parent
            newRoot.children[0] = indexedNode.node;
            newRoot.children[1] = right;

            // set childLengths of parent
            newRoot.childLengths[0] = size - rightLength - indexedNode.node.keys[degree - 1].length();
            newRoot.childLengths[1] = rightLength;

            // set sizes of parent
            newRoot.size = 1;

            // set new root
            root = newRoot;

            // update LinkedParentList
            indexedNode.parent = new IndexedNodeLinkedList(null, newRoot, indexedNode.index > degree - 1 ? 1 : 0);

            if (indexedNode.index > degree - 1) {
                indexedNode.node = right;
                indexedNode.index -= degree;
            }

        } else {

            int parentIndex = parent.index;

            // move keys of parent to the right
            System.arraycopy(parentNode.keys, parentIndex, parentNode.keys, parentIndex + 1, parentNode.size - parentIndex);

            // move children and childrenLength of parent to the right
            System.arraycopy(parentNode.children, parentIndex + 1, parentNode.children, parentIndex + 2, parentNode.size - parentIndex);
            System.arraycopy(parentNode.childLengths, parentIndex + 1, parentNode.childLengths, parentIndex + 2, parentNode.size - parentIndex);

            // add middle key of node to parent
            parentNode.keys[parentIndex] = indexedNode.node.keys[degree - 1];

            // add right to children of parent
            parentNode.children[parentIndex + 1] = right;

            // set childLengths of parent
            parentNode.childLengths[parentIndex + 1] = rightLength;
            parentNode.childLengths[parentIndex] -= rightLength + indexedNode.node.keys[degree - 1].length();

            // update size of parent
            parentNode.size++;

            // update link to parent if necessary
            if (indexedNode.index > degree - 1) {
                parent.index++;

                indexedNode.node = right;
                indexedNode.index -= degree;
            }
        }

        // reset removed elements of node
        for (int i = degree - 1; i < 2 * degree - 1; i++) {
            originalNode.children[i + 1] = null;
            originalNode.childLengths[i + 1] = 0;
            originalNode.keys[i] = null;
        }

        originalNode.children[2 * degree - 1] = null;
        originalNode.childLengths[2 * degree - 1] = 0;
    }

    public StorageView read(int start, int length) {
        return read(start, length, root, 0, 0);
    }

    public StorageView read(int start, int length, BtrfsNode node, int cumulativeLength, int lengthRead) {

        StorageView view = new EmptyStorageView(storage);

        for (int i = 0; i < node.size; i++) {
            // before i-th key and i-th child.

            // read from i-th child if start is in front of or in the i-th child, and it exists
            if (node.children[i] != null && start < cumulativeLength + node.childLengths[i]) {
                view = view.plus(read(start, length, node.children[i], cumulativeLength, lengthRead));
                lengthRead += Math.min(node.childLengths[i], length - lengthRead);
            }

            cumulativeLength += node.childLengths[i];

            // check if we have read enough
            if (lengthRead == length) {
                return view;
            } else if (lengthRead > length) {
                throw new AssertionError(); // sanity check
            }


            Interval key = node.keys[i];

            // if there is no next key we are done for this node
            if (key == null) {
                return view;
            }

            // read from i-th key if and start is in front of or in the i-th key
            if (start < cumulativeLength + key.length()) {

                int viewStart = key.start() + Math.max(0, start - cumulativeLength);
                int viewLength = Math.min(key.length() - (viewStart - key.start()), length - lengthRead);

                view = view.plus(storage.createView(new Interval(viewStart, viewLength)));
                lengthRead += viewLength;
            }

            cumulativeLength += key.length();

            // check if we have read enough
            if (lengthRead == length) {
                return view;
            } else if (lengthRead > length) {
                throw new AssertionError(); // sanity check
            }
        }

        // we reached the last child; read from it if it exists and return
        if (node.children[node.size] == null) {
            return view;
        }

        // read from last child if it exists, because we have not read enough if we are here
        view = view.plus(read(start, length, node.children[node.size], cumulativeLength, lengthRead));

        return view;
    }

    public void remove(int start, int length) {
        size -= length;
        int removed = remove(start, length, new IndexedNodeLinkedList(null, root, 0), 0, 0);

        // check if we have traversed the whole tree
        if (removed < length) {
            throw new IllegalArgumentException("start + length is out of bounds");
        } else if (removed > length) {
            throw new AssertionError(); // sanity check
        }
    }

    public int remove(int start, int length, IndexedNodeLinkedList indexedNode, int cumulativeLength, int removedLength) {

        int initiallyRemoved = removedLength;
        boolean visitNextChild = true;

        // iterate over all children and keys
        for (; indexedNode.index < indexedNode.node.size; indexedNode.index++) {
            // before i-th child and i-th child.

            // check if we have removed enough
            if (removedLength > length) {
                throw new AssertionError(); // sanity check
            } else if (removedLength == length) {
                return removedLength - initiallyRemoved;
            }

            // check if we have to visit the next child
            // we don't want to visit the child if we have already visited it but had to go back because the previous
            // key changed
            if (visitNextChild) {

                // remove from i-th child if start is in front of or in the i-th child, and it exists
                if (indexedNode.node.children[indexedNode.index] != null && start < cumulativeLength + indexedNode.node.childLengths[indexedNode.index]) {

                    // remove from child
                    final int removedInChild = remove(start, length, new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.index], 0), cumulativeLength, removedLength);

                    // update removedLength
                    removedLength += removedInChild;

                    // update childLength of parent accordingly
                    indexedNode.node.childLengths[indexedNode.index] -= removedInChild;

                    // check if we have removed enough
                    if (removedLength == length) {
                        return removedLength - initiallyRemoved;
                    } else if (removedLength > length) {
                        throw new AssertionError(); // sanity check
                    }
                }

                cumulativeLength += indexedNode.node.childLengths[indexedNode.index];
            } else {
                visitNextChild = true;
            }

            // get the i-th key
            Interval key = indexedNode.node.keys[indexedNode.index];

            // the key might not exist anymore
            if (key == null) {
                return removedLength - initiallyRemoved;
            }

            // if start is in the i-th key we just have to shorten the interval
            if (start > cumulativeLength && start < cumulativeLength + key.length()) {

                // calculate the new length of the key
                int newLength = start - cumulativeLength;

                // update cumulativeLength before updating the key
                cumulativeLength += key.length();

                // update the key
                indexedNode.node.keys[indexedNode.index] = new Interval(key.start(), newLength);

                // update removedLength
                removedLength += key.length() - newLength;

                // continue with next key
                continue;
            }

            // if start is in front of or at the start of the i-th key we have to remove the key
            if (start <= cumulativeLength) {

                // if the key is longer than the length to be removed we just have to shorten the key
                if (key.length() > length - removedLength) {

                    int newLength = key.length() - (length - removedLength);
                    int newStart = key.start() + (key.length() - newLength);

                    // update the key
                    indexedNode.node.keys[indexedNode.index] = new Interval(newStart, newLength);

                    // update removedLength
                    removedLength += key.length() - newLength;

                    // we are done
                    return removedLength - initiallyRemoved;
                }

                // if we are in a leaf node we can just remove the key
                if (indexedNode.node.isLeaf()) {

                    ensureSize(indexedNode);

                    // move all keys after the removed key to the left
                    System.arraycopy(indexedNode.node.keys, indexedNode.index + 1, indexedNode.node.keys, indexedNode.index, indexedNode.node.size - indexedNode.index - 1);

                    // remove (duplicated) last key
                    indexedNode.node.keys[indexedNode.node.size - 1] = null;

                    // update size
                    indexedNode.node.size--;

                    // update removedLength
                    removedLength += key.length();

                    // the next key moved one index to the left
                    indexedNode.index--;

                } else { // remove key from inner node

                    // try to replace with rightmost key of left child
                    if (indexedNode.node.children[indexedNode.index].size >= degree) {
                        Interval removedKey = removeRightMostKey(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.index], 0));

                        // update childLength of current node
                        indexedNode.node.childLengths[indexedNode.index] -= removedKey.length();

                        // update key
                        indexedNode.node.keys[indexedNode.index] = removedKey;

                        // update removedLength
                        removedLength += key.length();

                        // try to replace with leftmost key of right child
                    } else if (indexedNode.node.children[indexedNode.index + 1].size >= degree) {
                        Interval removedKey = removeLeftMostKey(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.index + 1], 0));

                        // update childLength of current node
                        indexedNode.node.childLengths[indexedNode.index + 1] -= removedKey.length();

                        // update key
                        indexedNode.node.keys[indexedNode.index] = removedKey;

                        // update removedLength
                        removedLength += key.length();

                        cumulativeLength += removedKey.length();

                        // we might have to remove the new key as well -> go back
                        indexedNode.index--;
                        visitNextChild = false; // we don't want to remove from the previous child again

                        continue;

                        // if both children have only degree - 1 keys we have to merge them and remove the key from the merged node
                    } else {

                        // save the length of the right child before merging because we have to add it to the
                        // cumulative length later
                        final int rightNodeLength = indexedNode.node.childLengths[indexedNode.index + 1];

                        ensureSize(indexedNode);

                        // merge the two children
                        mergeWithRightChild(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.index], 0));

                        // remove the key from the merged node
                        int removedInChild = remove(start, length, new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.index], degree - 1), cumulativeLength, removedLength);

                        // update childLength of current node
                        indexedNode.node.childLengths[indexedNode.index] -= removedInChild;

                        // update removedLength
                        removedLength += removedInChild;

                        // add the right child to the cumulative length
                        cumulativeLength += rightNodeLength;

                        // merging with right child shifted the keys to the left -> we have to visit the previous key again
                        indexedNode.index--;
                        visitNextChild = false; // we don't want to remove from the previous child again
                    }

                }

            }

            // update cumulativeLength after visiting the i-th key
            cumulativeLength += key.length();

        } // only the last child is left

        // check if we have removed enough
        if (removedLength > length) {
            throw new AssertionError(); // sanity check
        } else if (removedLength == length) {
            return removedLength - initiallyRemoved;
        }

        // remove from the last child if start is in front of or in the i-th child, and it exists
        if (indexedNode.node.children[indexedNode.node.size] != null && start <= cumulativeLength + indexedNode.node.childLengths[indexedNode.node.size]) {

            // remove from child
            int removedInChild = remove(start, length, new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.node.size], 0), cumulativeLength, removedLength);

            // update childLength of parent accordingly
            indexedNode.node.childLengths[indexedNode.node.size] -= removedInChild;

            // update removedLength
            removedLength += removedInChild;
        }

        return removedLength - initiallyRemoved;
    }

    private Interval removeRightMostKey(IndexedNodeLinkedList indexedNode) {

        indexedNode.index = indexedNode.node.size;

        // check if node is a leaf
        if (indexedNode.node.isLeaf()) {

            ensureSize(indexedNode);

            // get right most key
            final Interval key = indexedNode.node.keys[indexedNode.node.size - 1];

            // remove key
            indexedNode.node.keys[indexedNode.node.size - 1] = null;

            // update size
            indexedNode.node.size--;

            return key;
        } else { // if node is an inner node continue downward

            // recursively remove from rightmost child
            final Interval key = removeRightMostKey(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.node.size], 0));

            // update childLength
            indexedNode.node.childLengths[indexedNode.node.size] -= key.length();

            return key;
        }
    }

    private Interval removeLeftMostKey(IndexedNodeLinkedList indexedNode) {

        indexedNode.index = 0;

        // check if node is a leaf
        if (indexedNode.node.children[0] == null) {

            ensureSize(indexedNode);

            // get left most key
            final Interval key = indexedNode.node.keys[0];

            // move all other keys to the left
            System.arraycopy(indexedNode.node.keys, 1, indexedNode.node.keys, 0, indexedNode.node.size - 1);

            // remove (duplicated) last key
            indexedNode.node.keys[indexedNode.node.size - 1] = null;

            // update size
            indexedNode.node.size--;

            return key;
        } else { // if node is an inner node continue downward

            // recursively remove from leftmost child
            final Interval key = removeLeftMostKey(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[0], 0));

            // update childLength
            indexedNode.node.childLengths[0] -= key.length();

            return key;
        }
    }

    private void ensureSize(IndexedNodeLinkedList indexedNode) {

        // check if node has at least degree keys
        if (indexedNode.node.size >= degree || indexedNode.node == root) {
            return;
        }

        // get parent node and index
        BtrfsNode parentNode = indexedNode.parent.node;
        int parentIndex = indexedNode.parent.index;

        // get left and right sibling
        BtrfsNode leftSibling = parentIndex > 0 ? parentNode.children[parentIndex - 1] : null;
        BtrfsNode rightSibling = parentIndex < parentNode.size ? parentNode.children[parentIndex + 1] : null;

        // rotate a key from left or right node if possible
        if (leftSibling != null && leftSibling.size >= degree) {
            rotateFromLeftChild(indexedNode);
        } else if (rightSibling != null && rightSibling.size >= degree) {
            rotateFromRightChild(indexedNode);
        } else { // if we can't rotate, merge with left or right node

            // ensure that the parent has at least degree keys before merging
            if (parentNode.size < degree) {

                // the root does not have to have at least degree keys
                if (parentNode != root) {
                    // recursively fix size of parent
                    ensureSize(indexedNode.parent);
                }
            }

            if (parentIndex > 0) {
                mergeWithLeftChild(indexedNode);
            } else {
                mergeWithRightChild(indexedNode);
            }

            // if the root has no keys left after merging, set the only node as the new root
            if (root.size == 0 && root.children[0] != null) {
                root = root.children[0];
            }
        }
    }

    private void mergeWithLeftChild(IndexedNodeLinkedList indexedNode) {

        BtrfsNode parentNode = indexedNode.parent.node;
        int parentIndex = indexedNode.parent.index;
        BtrfsNode middleChild = indexedNode.node;
        BtrfsNode leftChild = parentNode.children[parentIndex - 1];

        // move keys and children of middle child to the right
        System.arraycopy(middleChild.keys, 0, middleChild.keys, degree, middleChild.size);
        System.arraycopy(middleChild.children, 0, middleChild.children, degree, middleChild.size + 1);
        System.arraycopy(middleChild.childLengths, 0, middleChild.childLengths, degree, middleChild.size + 1);

        // move key and children of left child to the middle child
        System.arraycopy(leftChild.keys, 0, middleChild.keys, 0, leftChild.size);
        System.arraycopy(leftChild.children, 0, middleChild.children, 0, leftChild.size + 1);
        System.arraycopy(leftChild.childLengths, 0, middleChild.childLengths, 0, leftChild.size + 1);

        // move key of parent into middle child
        middleChild.keys[degree - 1] = parentNode.keys[parentIndex - 1];

        // update childLength of parent
        parentNode.childLengths[parentIndex] += parentNode.keys[parentIndex - 1].length() + parentNode.childLengths[parentIndex - 1];

        // move keys and children of parent to the left
        System.arraycopy(parentNode.keys, parentIndex, parentNode.keys, parentIndex - 1, parentNode.size - parentIndex);
        System.arraycopy(parentNode.children, parentIndex, parentNode.children, parentIndex - 1, parentNode.size - parentIndex + 1);
        System.arraycopy(parentNode.childLengths, parentIndex, parentNode.childLengths, parentIndex - 1, parentNode.size - parentIndex + 1);

        // remove (duplicated) last key and child
        parentNode.keys[parentNode.size - 1] = null;
        parentNode.children[parentNode.size] = null;
        parentNode.childLengths[parentNode.size] = 0;

        // update size of parent and middle child
        parentNode.size--;
        middleChild.size = 2 * degree - 1;

        // we moved one to the left in the parent
        indexedNode.parent.index--;

        // the original position moved to the right
        indexedNode.index += degree;
    }

    private void mergeWithRightChild(IndexedNodeLinkedList indexedNode) {

        BtrfsNode parentNode = indexedNode.parent.node;
        int parentIndex = indexedNode.parent.index;
        BtrfsNode middleChild = indexedNode.node;
        BtrfsNode rightChild = parentNode.children[parentIndex + 1];

        // move key and children of right child to the middle child
        System.arraycopy(rightChild.keys, 0, middleChild.keys, degree, rightChild.size);
        System.arraycopy(rightChild.children, 0, middleChild.children, degree, rightChild.size + 1);
        System.arraycopy(rightChild.childLengths, 0, middleChild.childLengths, degree, rightChild.size + 1);

        // move key of parent into middle child
        middleChild.keys[degree - 1] = parentNode.keys[parentIndex];

        // update childLength of parent
        parentNode.childLengths[parentIndex] += parentNode.keys[parentIndex].length() + parentNode.childLengths[parentIndex + 1];

        // move keys and children of parent to the left
        System.arraycopy(parentNode.keys, parentIndex + 1, parentNode.keys, parentIndex, parentNode.size - parentIndex - 1);
        System.arraycopy(parentNode.children, parentIndex + 2, parentNode.children, parentIndex + 1, parentNode.size - parentIndex - 1);
        System.arraycopy(parentNode.childLengths, parentIndex + 2, parentNode.childLengths, parentIndex + 1, parentNode.size - parentIndex - 1);

        // remove (duplicated) last key and child
        parentNode.keys[parentNode.size - 1] = null;
        parentNode.children[parentNode.size] = null;
        parentNode.childLengths[parentNode.size] = 0;

        // update size of parent and middle child
        parentNode.size--;
        middleChild.size = 2 * degree - 1;
    }

    private void rotateFromLeftChild(IndexedNodeLinkedList indexedNode) {

        BtrfsNode parentNode = indexedNode.parent.node;
        int parentIndex = indexedNode.parent.index;

        // get left child
        BtrfsNode leftChild = parentNode.children[parentIndex - 1];

        // store and remove last key and child of left child
        final Interval key = leftChild.keys[leftChild.size - 1];
        leftChild.keys[leftChild.size - 1] = null;

        final BtrfsNode lastChild = leftChild.children[leftChild.size];
        leftChild.children[leftChild.size] = null;

        final int lastChildLength = leftChild.childLengths[leftChild.size];
        leftChild.childLengths[leftChild.size] = 0;

        // update size of left child
        leftChild.size--;

        // update childLength of parent (childLength of the parent of the parent doesn't change)
        parentNode.childLengths[parentIndex - 1] -= key.length() + lastChildLength;

        // store and replace key of parent
        final Interval parentKey = parentNode.keys[parentIndex - 1];
        parentNode.keys[parentIndex - 1] = key;

        // get middle child
        BtrfsNode middleChild = parentNode.children[parentIndex];

        // move entries of middle child to the right
        System.arraycopy(middleChild.keys, 0, middleChild.keys, 1, middleChild.size);
        System.arraycopy(middleChild.children, 0, middleChild.children, 1, middleChild.size + 1);
        System.arraycopy(middleChild.childLengths, 0, middleChild.childLengths, 1, middleChild.size + 1);

        // add key and child to middle child
        middleChild.keys[0] = parentKey;
        middleChild.children[0] = lastChild;
        middleChild.childLengths[0] = lastChildLength;

        // update size of middle child
        middleChild.size++;

        // update childLength of parent
        parentNode.childLengths[parentIndex] += parentKey.length() + lastChildLength;

        // the original position moved to the right
        indexedNode.index++;
    }

    private void rotateFromRightChild(IndexedNodeLinkedList indexedNode) {

        BtrfsNode parentNode = indexedNode.parent.node;
        int parentIndex = indexedNode.parent.index;

        // get right child
        final BtrfsNode rightChild = parentNode.children[parentIndex + 1];

        // store first key and child of right child
        final Interval key = rightChild.keys[0];
        final BtrfsNode lastChild = rightChild.children[0];
        int lastChildLength = rightChild.childLengths[0];

        // update size of right child
        rightChild.size--;

        // move entries of right child to the left
        System.arraycopy(rightChild.keys, 1, rightChild.keys, 0, rightChild.size);
        System.arraycopy(rightChild.children, 1, rightChild.children, 0, rightChild.size + 1);
        System.arraycopy(rightChild.childLengths, 1, rightChild.childLengths, 0, rightChild.size + 1);

        // remove (duplicated) last key and child of right child
        rightChild.keys[rightChild.size] = null;
        rightChild.children[rightChild.size + 1] = null;
        rightChild.childLengths[rightChild.size + 1] = 0;

        // update childLength of parent (childLength of the parent of the parent doesn't change)
        parentNode.childLengths[parentIndex + 1] -= key.length() + lastChildLength;

        // store and replace key of parent
        Interval parentKey = parentNode.keys[parentIndex];
        parentNode.keys[parentIndex] = key;

        // get middle child
        BtrfsNode middleChild = parentNode.children[parentIndex];

        // add key and child to middle child
        middleChild.keys[middleChild.size] = parentKey;
        middleChild.children[middleChild.size + 1] = lastChild;
        middleChild.childLengths[middleChild.size + 1] = lastChildLength;

        // update size of middle child
        middleChild.size++;

        // update childLength of parent
        parentNode.childLengths[parentIndex] += parentKey.length() + lastChildLength;
    }

    public int getSize() {
        return size;
    }

}
