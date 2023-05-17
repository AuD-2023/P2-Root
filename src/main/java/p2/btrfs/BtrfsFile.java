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

    public BtrfsFile(BtrfsNode root, int degree) {
        this.root = root;
        this.degree = degree;
    }

    public void write(int start, Interval interval) {
        //TODO
    }

    public void insert(int start, byte[] data) {

        // create intervals to insert
        List<Interval> intervals = storage.getAllocationStrategy().allocate(data.length);

        // fill the intervals with the data
        for (Interval interval : intervals) {
            storage.write(interval.start(), data, 0, interval.length());
        }

        //split if necessary
        if (isFull(root)) {
            split(null, root, 0);
        }


        //insert the intervals into the tree

        insert(start, intervals, root, new LinkedParentList(null, null, -1), 0, 0);

    }

    //TODO can be a lot easier:
    // 1. find leaf node to insert into
    // 2. insert into leaf node until full
    // 3. split leaf node
    // 4. repeat 2. with one of the two leaf nodes

    public int insert(int start, List<Interval> intervals, BtrfsNode node,
                      LinkedParentList parent, int cumulativeLength, int insertedLength) {

        //iterate over all keys and children
        for (int i = 0; i < 2 * degree - 1; i++) {
            //before i-th child and i-th key

            // if a child exists and the key should be inserted before the
            // i-th key, insert it (start == index of i-th key -> insert before)
            if (node.children[i] != null && start + insertedLength < cumulativeLength + node.childLength[i] + 1) {

                //split if necessary
                if (isFull(node)) {

                    LinkedParentList currentParent = new LinkedParentList(parent, node, i);

                    split(currentParent, node.children[i], 0);

                    //splitting might have changed the position where we currently are -> update via parent list
                    node = currentParent.node;
                    i = currentParent.index;

                    //check again where we have to insert
                    i--;
                    continue;
                }

                final int previousInsertedLength = insertedLength;
                LinkedParentList currentParent = new LinkedParentList(parent, node, i);

                //insert the interval into the child
                insertedLength = insert(start, intervals, node.children[i], currentParent, cumulativeLength, insertedLength);

                //splitting might have changed the position where we currently are -> update via parent list
                node = currentParent.node;
                i = currentParent.index;

                //update the length of the child we are inserting the node into
                node.childLength[i] += insertedLength - previousInsertedLength;

                //check if we have inserted everything
                if (intervals.isEmpty()) {
                    return insertedLength;
                }
            }

            cumulativeLength += node.childLength[i];

            // if we reached the end of the current node, and we are inserting it after the current position,
            // we are done for this node
            if (node.keys[i] == null && start + cumulativeLength > cumulativeLength) {
                return insertedLength;
            }

            // if we insert it at the position of the i-th key and there is no child,
            // move keys to the right and insert the key into the current node
            if (start + insertedLength == cumulativeLength) {

                // move keys to the right if they exist; children and childLengths are not affected because
                // they don't exist (we always insert into a leaf node)
                if (node.keys[i] != null) {
                    System.arraycopy(node.keys, i, node.keys, i + 1, node.size - i);
                }

                //update size of current node
                node.size++;

                //insert the interval into the node
                node.keys[i] = intervals.get(0);

                //remove the interval from the list
                intervals.remove(0);

                //check if we have inserted everything
                if (intervals.isEmpty()) {
                    return insertedLength;
                }

                //update cumulative and inserted length
                cumulativeLength += node.keys[i].length();
                insertedLength += node.keys[i].length();

                //continue with next child
                continue;
            }

            // if start is less than the length up to the end of the i-th key but didn't get
            // inserted before (-> start is inside the i-th key),
            // we have to split the key into two keys and insert the interval between them.
            if (node.keys[i] != null && start + insertedLength <= cumulativeLength + node.keys[i].length()) {

                //store the original interval
                Interval oldInterval = node.keys[i];

                //create new intervals for the left and right part of the old interval
                Interval newLeftInterval = new Interval(oldInterval.start(), (cumulativeLength + oldInterval.length()) - start);
                Interval newRightInterval = new Interval(newLeftInterval.start() + newLeftInterval.length(),
                    oldInterval.length() - newLeftInterval.length());

                //store the new left interval in the node
                node.keys[i] = newLeftInterval;

                //add the new right interval to the end of the list of intervals to insert
                intervals.add(newRightInterval);

                //update cumulative length
                cumulativeLength += newLeftInterval.length();

                //we don't have to change anything else because we didn't insert a completely new key
                continue;
            }

            //we either inserted an interval continued or already returned
            if (node.keys[i] == null) {
                throw new AssertionError();
            }

            //if start is greater than the length up to the end of the i-th key, we just continue with the next key
            cumulativeLength += node.keys[i].length();
        }

        // try to insert into the last child
        if (node.children[2 * degree - 1] != null
            && start + insertedLength < cumulativeLength + node.childLength[2 * degree - 1] + 1) {

            int previousInsertedLength = insertedLength;

            //insert the interval into the child
            insertedLength = insert(start, intervals, node.children[2 * degree - 1],
                new LinkedParentList(parent, node, 2 * degree - 1), cumulativeLength, insertedLength);

            //update the length of the child we are inserting the node into
            node.childLength[2 * degree - 1] += insertedLength - previousInsertedLength;
        }

        return insertedLength;

    }

    private boolean isFull(BtrfsNode node) {
        return node.size > 2 * degree - 1;
    }

    public void split(LinkedParentList parent, BtrfsNode node, int index) {


        if (!isFull(node)) {
            throw new IllegalArgumentException("node is not full when splitting");
        }

        BtrfsNode parentNode = parent.node;
        int parentIndex = parent.index;

        if (parentNode != null && isFull(parentNode)) {
            split(parent.parent, parentNode, parent.index);
        }

        //create new node
        BtrfsNode right = new BtrfsNode(degree);

        //calculate length of left node
        int leftLength = node.childLength[degree - 1];
        for (int i = 0; i < degree - 1; i++) {
            leftLength += node.childLength[i];
            leftLength += node.keys[i].length();
        }

        //calculate length of right node
        int rightLength = node.childLength[2 * degree - 1];
        for (int i = degree; i < node.size; i++) {
            rightLength += node.childLength[i];
            rightLength += node.keys[i].length();
        }

        //copy keys, children and childLengths to right node
        System.arraycopy(node.keys, degree, right.keys, 0, degree - 1);
        System.arraycopy(node.children, degree, right.children, 0, degree);
        System.arraycopy(node.childLength, degree, right.childLength, 0, degree);

        //update sizes of left and right
        node.size = degree - 1;
        right.size = degree - 1;

        //splitting the root
        if (parentNode == null) {

            //create new root
            BtrfsNode newRoot = new BtrfsNode(degree);

            //add middle key of node to parent
            newRoot.keys[0] = node.keys[degree - 1];

            //add left and right to children of parent
            newRoot.children[0] = node;
            newRoot.children[1] = right;

            //set childLengths of parent
            newRoot.childLength[0] = leftLength;
            newRoot.childLength[1] = rightLength;

            //set sizes of parent
            newRoot.size = 1;

            //set new root
            root = newRoot;

            //update LinkedParentList
            parent.node = newRoot;

        } else {

            //split parent if necessary
            if (isFull(parentNode)) {
                split(parent.parent, parentNode, parent.index);
            }

            //move keys of parent to the right
            for (int i = parentNode.size - 1; i >= parentIndex; i--) {
                parentNode.keys[i + 1] = parentNode.keys[i];
            }

            //move children and childrenLength of parent to the right
            for (int i = parentNode.size; i > parentIndex; i--) {
                parentNode.children[i + 1] = parentNode.children[i];
                parentNode.childLength[i + 1] = parentNode.childLength[i];
            }

            //add middle key of node to parent
            parentNode.keys[parentIndex] = node.keys[degree - 1];

            //add right to children of parent
            parentNode.children[parentIndex + 1] = right;

            //set childLengths of parent
            parentNode.childLength[parentIndex + 1] = rightLength;

            //update size of parent
            parentNode.size++;

            //update LinkedParentList
            if (index > degree - 1) {
                parent.node = right;
                parent.index = index - degree;
            }
        }

        //reset removed elements of node
        for (int i = degree; i < 2 * degree; i++) {
            node.children[i] = null;
            node.childLength[i] = 0;
            node.keys[i] = null;
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
                lengthRead += Math.max(node.childLength[i], length - lengthRead);
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
        remove(start, length, root, new LinkedParentList(null, null, -1), 0, 0);
    }

    public void remove(int start, int length, BtrfsNode node, LinkedParentList parent, int cumulativeLength, int removedLength) {

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
                remove(start, length, node, new LinkedParentList(parent, node, i), cumulativeLength, removedLength);

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
                        Interval removedKey = removeRightMostKey(new LinkedParentList(parent, node, i), node.children[i]);

                        //update childLength of current node
                        node.childLength[i] -= removedKey.length();

                        //update key
                        node.keys[i] = removedKey;

                        //update removedLength
                        removedLength += key.length();

                        //try to replace with leftmost key of right child
                    } else if (node.children[i + 1].size >= degree) {
                        Interval removedKey = removeLeftMostKey(new LinkedParentList(parent, node, i + 1), node.children[i + 1]);

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
                            new LinkedParentList(parent, node, i), cumulativeLength - leftNodeLength, removedLength);

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
            remove(start, length, node, new LinkedParentList(parent, node, node.size), cumulativeLength, removedLength);
        }

        //check if we have traversed the whole tree
        if (parent.node == null) {
            throw new IllegalArgumentException("start + length is out of bounds");
        }

    }

    private Interval removeRightMostKey(LinkedParentList parent, BtrfsNode node) {
        //node has at least degree keys

        if (parent.node.size < degree) {
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
            Interval key = removeRightMostKey(new LinkedParentList(parent, node, node.size), node.children[node.size]);

            //update childLength
            node.childLength[node.size] -= key.length();

            return key;
        }
    }

    private Interval removeLeftMostKey(LinkedParentList parent, BtrfsNode node) {
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
            Interval key = removeLeftMostKey(new LinkedParentList(parent, node, node.size), node.children[0]);

            //update childLength
            node.childLength[0] -= key.length();

            return key;
        }
    }

    private static class LinkedParentList {

        LinkedParentList parent;
        BtrfsNode node;
        int index;

        public LinkedParentList(LinkedParentList parent, BtrfsNode node, int index) {
            this.parent = parent;
            this.node = node;
            this.index = index;
        }
    }

    private void ensureSize(LinkedParentList parent, BtrfsNode node) {

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

}
