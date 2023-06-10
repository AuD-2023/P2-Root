package p2.btrfs;

import p2.storage.EmptyStorageView;
import p2.storage.Interval;
import p2.storage.Storage;
import p2.storage.StorageView;

import java.util.ArrayList;
import java.util.List;

/**
 * A file in a Btrfs file system. it uses a B-tree to store the intervals that hold the file's data.
 */
public class BtrfsFile {

    /**
     * The storage in which the file is stored.
     */
    private final Storage storage;

    /**
     * The name of the file.
     */
    private final String name;

    /**
     * The degree of the B-tree.
     */
    private final int degree;

    private final int maxKeys;

    /**
     * The root node of the B-tree.
     */
    private BtrfsNode root;

    /**
     * The total size of the file.
     */
    private int size;

    /**
     * Creates a new {@link BtrfsFile} instance.
     *
     * @param name    the name of the file.
     * @param storage the storage in which the file is stored.
     * @param degree  the degree of the B-tree.
     */
    public BtrfsFile(String name, Storage storage, int degree) {
        this.name = name;
        this.storage = storage;
        this.degree = degree;
        maxKeys = 2 * degree - 1;
        root = new BtrfsNode(degree);
    }

    /**
     * Reads all data from the file.
     *
     * @return a {@link StorageView} containing all data that is stored in this file.
     */
    public StorageView readAll() {
        return readAll(root);
    }

    /**
     * Reads all data from the given node.
     *
     * @param node the node to read from.
     * @return a {@link StorageView} containing all data that is stored in this file.
     */
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

    /**
     * Reads the given amount of data from the file starting at the given start position.
     *
     * @param start  the start position.
     * @param length the amount of data to read.
     * @return a {@link StorageView} containing the data that was read.
     */
    public StorageView read(int start, int length) {
        return read(start, length, root, 0, 0);
    }

    /**
     * Reads the given amount of data from the given node starting at the given start position.
     *
     * @param start            the start position.
     * @param length           the amount of data to read.
     * @param node             the current node to read from.
     * @param cumulativeLength the cumulative length of the intervals that have been visited so far.
     * @param lengthRead       the amount of data that has been read so far.
     * @return a {@link StorageView} containing the data that was read.
     */
    private StorageView read(int start, int length, BtrfsNode node, int cumulativeLength, int lengthRead) {

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
                throw new IllegalStateException("Read more keys than wanted"); // sanity check
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
                throw new IllegalStateException("Read more keys than wanted"); // sanity check
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

    /**
     * Insert the given data into the file starting at the given start position.
     *
     * @param start     the start position.
     * @param intervals the intervals to write to.
     * @param data      the data to write into the storage.
     */
    public void insert(int start, List<Interval> intervals, byte[] data) {

        // fill the intervals with the data
        int dataPos = 0;
        for (Interval interval : intervals) {
            storage.write(interval.start(), data, dataPos, interval.length());
            dataPos += interval.length();
        }

        size += data.length;

        int insertionSize = data.length;

        // findInsertionIndex assumes that the current node is not full
        if (root.isFull()) {
            split(new IndexedNodeLinkedList(null, root, 0));
        }

        insert(intervals, findInsertionPosition(new IndexedNodeLinkedList(
            null, root, 0), start, 0, insertionSize, null), insertionSize);

    }

    /**
     * Inserts the given data into the given leaf at the given index.
     *
     * @param intervals       the intervals to insert.
     * @param indexedLeaf     The node and index to insert at.
     * @param remainingLength the remaining length of the data to insert.
     */
    private void insert(List<Interval> intervals, IndexedNodeLinkedList indexedLeaf, int remainingLength) {

        int amountToInsert = Math.min(intervals.size(), maxKeys - indexedLeaf.node.size);

        if (indexedLeaf.index < indexedLeaf.node.size) {
            System.arraycopy(indexedLeaf.node.keys, indexedLeaf.index, indexedLeaf.node.keys,
                indexedLeaf.index + amountToInsert, indexedLeaf.node.size - indexedLeaf.index);
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

    /**
     * Finds the leaf node and index at which new intervals should be inserted given a start position.
     * It ensures that the start position is not in the middle of an existing interval
     * and updates the childLengths of the visited nodes.
     *
     * @param indexedNode      The current Position in the tree.
     * @param start            The start position of the intervals to insert.
     * @param cumulativeLength The length of the intervals in the tree up to the current node and index.
     * @param insertionSize    The total size of the intervals to insert.
     * @param splitKey         The right half of the interval that had to be split to ensure that the start position
     *                         is not in the middle of an interval. It will be inserted once the leaf node is reached.
     *                         If no split was necessary, this is null.
     * @return The leaf node and index, as well as the path to it, at which the intervals should be inserted.
     */
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

            // we already split before going into child -> current node is not full
            System.arraycopy(indexedNode.node.keys, indexedNode.index, indexedNode.node.keys,
                indexedNode.index + 1, indexedNode.node.size - indexedNode.index);
            indexedNode.node.keys[indexedNode.index] = splitKey;
            indexedNode.node.size++;

            return indexedNode;
        }

        for (; indexedNode.index < indexedNode.node.size; indexedNode.index++) {

            if (!indexedNode.node.isLeaf() && start <= cumulativeLength + indexedNode.node.childLengths[indexedNode.index]) {

                // split if child is full
                if (indexedNode.node.children[indexedNode.index].isFull()) {
                    split(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.index], 0));

                    // check again where we should insert
                    indexedNode.index--;
                    continue;
                }

                indexedNode.node.childLengths[indexedNode.index] += insertionSize;

                return findInsertionPosition(new IndexedNodeLinkedList(indexedNode,
                    indexedNode.node.children[indexedNode.index], 0), start, cumulativeLength, insertionSize, splitKey);
            }

            cumulativeLength += indexedNode.node.childLengths[indexedNode.index];

            if (start == cumulativeLength) {
                return indexedNode;
            }

            // if we insert it in the middle of the key -> split the key
            if (start < cumulativeLength + indexedNode.node.keys[indexedNode.index].length()) {

                Interval oldInterval = indexedNode.node.keys[indexedNode.index];

                // create new intervals for the left and right part of the old interval
                Interval newLeftInterval = new Interval(oldInterval.start(), start - cumulativeLength);
                Interval newRightInterval = new Interval(newLeftInterval.start() + newLeftInterval.length(),
                    oldInterval.length() - newLeftInterval.length());

                // store the new left interval in the node
                indexedNode.node.keys[indexedNode.index] = newLeftInterval;

                insertionSize += newRightInterval.length();
                splitKey = newRightInterval;

                if (indexedNode.node.isLeaf()) {
                    indexedNode.index++;
                    return findInsertionPosition(indexedNode, start, cumulativeLength, insertionSize, splitKey);
                }
            }

            cumulativeLength += indexedNode.node.keys[indexedNode.index].length();
        }

        if (indexedNode.node.isLeaf()) {
            return indexedNode;
        }

        //if last child is full, split it
        if (indexedNode.node.children[indexedNode.node.size].isFull()) {
            split(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.node.size], 0));

            // check again where we should insert
            indexedNode.index--;
            return findInsertionPosition(indexedNode, start, cumulativeLength, insertionSize, splitKey);
        }

        indexedNode.node.childLengths[indexedNode.node.size] += insertionSize;

        return findInsertionPosition(new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.node.size], 0),
            start, cumulativeLength, insertionSize, splitKey);
    }

    /**
     * Splits the given node at the given index.
     * The method ensures that the given indexedNode points to correct node and index after the split.
     *
     * @param indexedNode The node to split.
     */
    private void split(IndexedNodeLinkedList indexedNode) {

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
        int rightLength = indexedNode.node.childLengths[maxKeys];
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
            System.arraycopy(parentNode.keys, parentIndex, parentNode.keys, parentIndex + 1,
                parentNode.size - parentIndex);

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
        for (int i = degree - 1; i < maxKeys; i++) {
            originalNode.children[i + 1] = null;
            originalNode.childLengths[i + 1] = 0;
            originalNode.keys[i] = null;
        }

        originalNode.children[maxKeys] = null;
        originalNode.childLengths[maxKeys] = 0;
    }

    /**
     * Writes the given data to the given intervals and stores them in the file starting at the given start position.
     * This method will override existing data starting at the given start position.
     *
     * @param start     the start position.
     * @param intervals the intervals to write to.
     * @param data      the data to write into the storage.
     */
    public void write(int start, List<Interval> intervals, byte[] data) {

        // fill the intervals with the data
        int dataPos = 0;
        for (Interval interval : intervals) {
            storage.write(interval.start(), data, dataPos, interval.length());
            dataPos += interval.length();
        }

        WriteResult result = write(start, intervals, new IndexedNodeLinkedList(null, root, 0), 0, 0, data.length, data.length);

        size += result.lengthDiff;

        //insert remaining intervals
        if (!intervals.isEmpty()) {
            // findInsertionIndex assumes that the current node is not full
            if (root.isFull()) {
                split(new IndexedNodeLinkedList(null, root, 0));
            }

            insert(intervals, findInsertionPosition(new IndexedNodeLinkedList(
                null, root, 0), size, 0, getSize(intervals), null), getSize(intervals));
        }
    }

    private record WriteResult(int removedLength, int lengthDiff) {
    }

    public WriteResult write(int start, List<Interval> intervals, IndexedNodeLinkedList indexedNode, int cumulativeLength, int removedLength, int insertionSize, int remainingLength) {

        int lengthDiff = 0;

        for (; indexedNode.index < indexedNode.node.size; indexedNode.index++) {
            // before i-th key and i-th child.

            // write into i-th child if start is in front of or in the i-th child, and it exists
            if (indexedNode.node.children[indexedNode.index] != null &&
                start < cumulativeLength + indexedNode.node.childLengths[indexedNode.index]) {

                WriteResult result = write(start, intervals, new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.index], 0),
                    cumulativeLength, removedLength, insertionSize, remainingLength);

                indexedNode.node.childLengths[indexedNode.index] += result.lengthDiff;
                removedLength += result.removedLength;
            }


            // check if we have written everything into the file
            if (intervals.isEmpty()) {
                return new WriteResult(removedLength, lengthDiff);
            } else if (removedLength == insertionSize) {

                //if we have removed as much as we can we have to insert the rest
                insert(intervals,
                    findInsertionPosition(indexedNode, cumulativeLength, cumulativeLength,
                        getSize(intervals), null), getSize(intervals));

                return new WriteResult(removedLength, lengthDiff);
            }

            cumulativeLength += indexedNode.node.childLengths[indexedNode.index];

            Interval key = indexedNode.node.keys[indexedNode.index];

            // if we would overwrite too much with the next key we have to split it
            if (intervals.get(0).length() + removedLength > insertionSize) {
                Interval oldInterval = intervals.remove(0);
                int maxInsertionSize = insertionSize - removedLength;
                Interval newLeftInterval = new Interval(oldInterval.start(), maxInsertionSize);
                Interval newRightInterval = new Interval(oldInterval.start() + maxInsertionSize, oldInterval.length() - maxInsertionSize);
                intervals.add(0, newRightInterval);
                intervals.add(0, newLeftInterval);
            }

            // start is inside the i-th key
            if (start < cumulativeLength + key.length()) {

                // start is inside the i-th key, but not at the beginning
                if (start > cumulativeLength) {

                    // split the key
                    Interval newInterval = new Interval(key.start(), key.length() - (start - cumulativeLength));

                    //add new interval
                    indexedNode.node.keys[indexedNode.index] = newInterval;

                    // if we don't overwrite the whole interval we just have to insert the missing Intervals
                    if (remainingLength < key.length() - newInterval.length()) {
                        Interval endInterval = new Interval(newInterval.start() + newInterval.length() + insertionSize, key.length() - newInterval.length() - insertionSize);

                        intervals.add(endInterval);

                        insert(intervals,
                            findInsertionPosition(indexedNode, key.start() + newInterval.length(),
                                cumulativeLength - indexedNode.node.childLengths[indexedNode.index], getSize(intervals), null),
                            getSize(intervals));

                        return new WriteResult(removedLength, lengthDiff);
                    }

                    //otherwise we insert the end Interval
                    Interval endInterval = new Interval(key.start() + newInterval.length(), key.length() - newInterval.length());
                    insert(new ArrayList<>(List.of(endInterval)),
                        findInsertionPosition(indexedNode, cumulativeLength + newInterval.length(),
                            cumulativeLength - indexedNode.node.childLengths[indexedNode.index], endInterval.length(), null), endInterval.length());

                    cumulativeLength += newInterval.length();
                    continue;
                }

                // start is now in front of current key

                // if we don't overwrite the whole interval we have to insert the end of the interval afterward
                if (remainingLength < key.length()) {

                    indexedNode.node.keys[indexedNode.index] = intervals.remove(0);

                    Interval endInterval = new Interval(key.start() + remainingLength, key.length() - remainingLength);
                    intervals.add(endInterval);

                    insert(intervals,
                        findInsertionPosition(indexedNode, cumulativeLength + remainingLength,
                            cumulativeLength - indexedNode.node.childLengths[indexedNode.index], getSize(intervals), null),
                        getSize(intervals));

                    return new WriteResult(removedLength, lengthDiff);
                }

                //otherwise we overwrite the whole interval
                indexedNode.node.keys[indexedNode.index] = intervals.remove(0);

                removedLength += key.length();
                lengthDiff += indexedNode.node.keys[indexedNode.index].length() - key.length();

            }

            cumulativeLength += key.length();

            // check if we have written everything into the file
            if (intervals.isEmpty()) {
                return new WriteResult(removedLength, lengthDiff);
            } else if (removedLength == insertionSize) {

                //if we have removed as much as we can we have to insert the rest
                insert(intervals,
                    findInsertionPosition(indexedNode, cumulativeLength, cumulativeLength,
                        getSize(intervals), null), getSize(intervals));

                return new WriteResult(removedLength, lengthDiff);
            }
        }

        // write into last child if it exists
        if (indexedNode.node.children[indexedNode.node.size] != null) {
            return write(start, intervals, new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.node.size], 0),
                cumulativeLength, removedLength, insertionSize, remainingLength);
        }

        return new WriteResult(removedLength, lengthDiff);

    }

    private int getSize(List<Interval> intervals) {
        int size = 0;
        for (Interval interval : intervals) {
            size += interval.length();
        }
        return size;
    }

    /**
     * Removes the given number of bytes starting at the given position from this file.
     *
     * @param start  the start position of the bytes to remove
     * @param length the amount of bytes to remove
     */
    public void remove(int start, int length) {
        size -= length;
        int removed = remove(start, length, new IndexedNodeLinkedList(null, root, 0), 0, 0);

        // check if we have traversed the whole tree
        if (removed < length) {
            throw new IllegalArgumentException("start + length is out of bounds");
        } else if (removed > length) {
            throw new IllegalStateException("Removed more keys than wanted"); // sanity check
        }
    }

    /**
     * Removes the given number of bytes starting at the given position from the given node.
     *
     * @param start            the start position of the bytes to remove
     * @param length           the amount of bytes to remove
     * @param indexedNode      the current node to remove from
     * @param cumulativeLength the length of the intervals up to the current node and index
     * @param removedLength    the length of the intervals that have already been removed
     * @return the number of bytes that have been removed
     */
    private int remove(int start, int length, IndexedNodeLinkedList indexedNode, int cumulativeLength, int removedLength) {

        int initiallyRemoved = removedLength;
        boolean visitNextChild = true;

        // iterate over all children and keys
        for (; indexedNode.index < indexedNode.node.size; indexedNode.index++) {
            // before i-th child and i-th child.

            // check if we have removed enough
            if (removedLength > length) {
                throw new IllegalStateException("Removed more keys than wanted"); // sanity check
            } else if (removedLength == length) {
                return removedLength - initiallyRemoved;
            }

            // check if we have to visit the next child
            // we don't want to visit the child if we have already visited it but had to go back because the previous
            // key changed
            if (visitNextChild) {

                // remove from i-th child if start is in front of or in the i-th child, and it exists
                if (indexedNode.node.children[indexedNode.index] != null &&
                    start < cumulativeLength + indexedNode.node.childLengths[indexedNode.index]) {

                    // remove from child
                    final int removedInChild = remove(start, length,
                        new IndexedNodeLinkedList(indexedNode, indexedNode.node.children[indexedNode.index], 0),
                        cumulativeLength, removedLength);

                    // update removedLength
                    removedLength += removedInChild;

                    // update childLength of parent accordingly
                    indexedNode.node.childLengths[indexedNode.index] -= removedInChild;

                    // check if we have removed enough
                    if (removedLength == length) {
                        return removedLength - initiallyRemoved;
                    } else if (removedLength > length) {
                        throw new IllegalStateException("Removed more keys than wanted"); // sanity check
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
                final int newLength = start - cumulativeLength;

                // check if we are only removing the middle of a single interval
                if (key.length() - newLength > length) {

                    // update the key with the start of the interval
                    Interval startInterval = new Interval(key.start(), newLength);
                    indexedNode.node.keys[indexedNode.index] = startInterval;

                    // create a new key for the end of the interval
                    Interval endInterval = new Interval(key.start() + newLength + length, key.length() - newLength - length);

                    // findInsertionIndex assumes that the current node is not full
                    if (indexedNode.node.isFull()) {
                        split(indexedNode);
                    }

                    // insert the new key for the end of the interval
                    insert(new ArrayList<>(List.of(endInterval)),
                        findInsertionPosition(indexedNode, start + startInterval.length() + 1, cumulativeLength + startInterval.length(), endInterval.length(), null),
                        endInterval.length());

                    //update removedLength
                    removedLength += key.length() - startInterval.length() - endInterval.length();

                    return removedLength - initiallyRemoved;
                }

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

                    final int newLength = key.length() - (length - removedLength);
                    final int newStart = key.start() + (key.length() - newLength);

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
                    System.arraycopy(indexedNode.node.keys, indexedNode.index + 1,
                        indexedNode.node.keys, indexedNode.index, indexedNode.node.size - indexedNode.index - 1);

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
                        final Interval removedKey = removeRightMostKey(new IndexedNodeLinkedList(indexedNode,
                            indexedNode.node.children[indexedNode.index], 0));

                        // update childLength of current node
                        indexedNode.node.childLengths[indexedNode.index] -= removedKey.length();

                        // update key
                        indexedNode.node.keys[indexedNode.index] = removedKey;

                        // update removedLength
                        removedLength += key.length();

                        // try to replace with leftmost key of right child
                    } else if (indexedNode.node.children[indexedNode.index + 1].size >= degree) {
                        final Interval removedKey = removeLeftMostKey(new IndexedNodeLinkedList(indexedNode,
                            indexedNode.node.children[indexedNode.index + 1], 0));

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
                        mergeWithRightSibling(new IndexedNodeLinkedList(indexedNode,
                            indexedNode.node.children[indexedNode.index], 0));

                        // remove the key from the merged node
                        int removedInChild = remove(start, length, new IndexedNodeLinkedList(indexedNode,
                                indexedNode.node.children[indexedNode.index], degree - 1),
                            cumulativeLength, removedLength);

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
            throw new IllegalStateException("Removed more keys than wanted"); // sanity check
        } else if (removedLength == length) {
            return removedLength - initiallyRemoved;
        }

        // remove from the last child if start is in front of or in the i-th child, and it exists
        if (indexedNode.node.children[indexedNode.node.size] != null &&
            start <= cumulativeLength + indexedNode.node.childLengths[indexedNode.node.size]) {

            // remove from child
            int removedInChild = remove(start, length, new IndexedNodeLinkedList(indexedNode,
                indexedNode.node.children[indexedNode.node.size], 0), cumulativeLength, removedLength);

            // update childLength of parent accordingly
            indexedNode.node.childLengths[indexedNode.node.size] -= removedInChild;

            // update removedLength
            removedLength += removedInChild;
        }

        return removedLength - initiallyRemoved;
    }

    /**
     * Removes the rightmost key of the given node if it is a leaf.
     * Otherwise, it will remove the rightmost key of the last child.
     *
     * @param indexedNode the node to remove the rightmost key from.
     * @return the removed key.
     */
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

    /**
     * Removes the leftmost key of the given node if it is a leaf.
     * Otherwise, it will remove the leftmost key of the first child.
     *
     * @param indexedNode the node to remove the leftmost key from.
     * @return the removed key.
     */
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

    /**
     * Ensures that the given node has at least degree keys if it is not the root.
     * If the node has less than degree keys, it will try to rotate a key from a sibling or merge with a sibling.
     *
     * @param indexedNode the node to ensure the size of.
     */
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
            rotateFromLeftSibling(indexedNode);
        } else if (rightSibling != null && rightSibling.size >= degree) {
            rotateFromRightSibling(indexedNode);
        } else { // if we can't rotate, merge with left or right node

            // recursively fix size of parent
            ensureSize(indexedNode.parent);

            if (parentIndex > 0) {
                mergeWithLeftSibling(indexedNode);
            } else {
                mergeWithRightSibling(indexedNode);
            }

            // if the root has no keys left after merging, set the only node as the new root
            if (root.size == 0 && root.children[0] != null) {
                root = root.children[0];
            }
        }
    }

    /**
     * Merges the given node with its left sibling.
     * The method ensures that the given indexedNode points to correct node and index after the split.
     *
     * @param indexedNode the node to merge with its left sibling.
     */
    private void mergeWithLeftSibling(IndexedNodeLinkedList indexedNode) {

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
        middleChild.size = maxKeys;

        // we moved one to the left in the parent
        indexedNode.parent.index--;

        // the original position moved to the right
        indexedNode.index += degree;
    }

    /**
     * Merges the given node with its right sibling.
     * The method ensures that the given indexedNode points to correct node and index after the split.
     *
     * @param indexedNode the node to merge with its right sibling.
     */
    private void mergeWithRightSibling(IndexedNodeLinkedList indexedNode) {

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
        middleChild.size = maxKeys;
    }

    /**
     * Rotates an interval from the left sibling via the parent to the given node.
     *
     * @param indexedNode the node to rotate to.
     */
    private void rotateFromLeftSibling(IndexedNodeLinkedList indexedNode) {

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

    /**
     * Rotates an interval from the right sibling via the parent to the given node.
     *
     * @param indexedNode the node to rotate to.
     */
    private void rotateFromRightSibling(IndexedNodeLinkedList indexedNode) {

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

    /**
     * Checks if there are any adjacent intervals that are also point to adjacent bytes in the storage.
     * If there are such intervals, they are merged into a single interval.
     */
    public void shrink() {
        //TODO
    }

    /**
     * Returns the size of the file.
     * This is the sum of the length of all intervals or the amount of bytes used in the storage.
     *
     * @return the size of the file.
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the name of the file.
     *
     * @return the name of the file.
     */
    public String getName() {
        return name;
    }
}
