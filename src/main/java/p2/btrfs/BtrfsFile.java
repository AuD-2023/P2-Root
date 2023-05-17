package p2.btrfs;

import p2.storage.Storage;
import p2.storage.Interval;
import p2.storage.StorageView;

import java.util.List;

public class BtrfsFile {

    BtrfsNode root;
    Storage storage;

    int degree;

    int size;

    public BtrfsFile(char[] data, int degree) {
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
            storage.write(interval.start(), data, interval.length());
        }

        //split if necessary
        if (isFull(root)) {
            split(null, root, 0);
        }

        //insert the intervals into the tree
        for (int i = 0; i < data.length; i++) {
            insert(start + i, intervals.get(i), root);
        }

    }

    public void insert(int start, Interval interval, BtrfsNode node) {

        //information about parent for splitting
        BtrfsNode previousNode;
        int previousIndex;

        // descend into the tree
        outer: while (true) {

            //the length of all keys and children up to the currently considered position
            int cumulativeLength = 0;

            //iterate over all keys and children
            for (int i = 0; i < node.size; i++) {
                //before i-th child and i-th key

                //if a child exists and the key should be inserted before the i-th key (start == index of i-th key -> insert before)
                if (node.children[i] != null && start <= cumulativeLength + node.childLength[i] + 1) {

                    //update current and previous node
                    previousNode = node;
                    node = node.children[i];
                    previousIndex = i;

                    //split if necessary
                    if (isFull(node)) {
                        split(previousNode, node, previousIndex);

                        //check again where we have to insert
                        i--;
                        continue;
                    }

                    //update the length of the child we are inserting the node into
                    node.childLength[i] += interval.length();


                    continue outer;
                }

                cumulativeLength += node.childLength[i];

                //if we insert it at the position of the i-th key and there is no child, move keys to the right and insert the key into the current node
                if (start == cumulativeLength + 1) {

                    //move keys to the right; children and childLengths are not affected because they don't exist
                    System.arraycopy(node.keys, i, node.keys, i + 1, node.size - i);

                    //update size of current node
                    node.size++;

                    //insert the interval into the node
                    node.keys[i] = interval;

                    //we are done
                    return;
                }

                //if start is less than the length up to the end of the i-th key but didn't get inserted before (-> start is inside the i-th key),
                //we have to split the key into two keys and insert the interval between them.
                if (start < cumulativeLength + node.keys[i].length()) {
                    Interval oldInterval = node.keys[i];

                    //create new intervals for the left and right part of the old interval
                    Interval newLeftInterval = new Interval(oldInterval.start(), start - oldInterval.start());
                    Interval newRightInterval = new Interval(newLeftInterval.start() + interval.length(), oldInterval.length() - newLeftInterval.length());

                    //store the new left interval in the node
                    node.keys[i] = newLeftInterval;

                    //insert the new right interval behind the new left interval
                    insert(start - cumulativeLength + newLeftInterval.length() + 1, newRightInterval, node);

                    //insert the original interval between the two new intervals
                    //because we split a 2 * degree - 2 we can still insert the new node
                    insert(start - cumulativeLength + newLeftInterval.length() + 1, interval, node);

                    //we are done
                    return;
                }

            }

            //The key does not fit into any child and came after every node, so we insert it at the end of the current node
            node.keys[node.size] = interval;
            node.size++;
            break;
        }

    }

    private boolean isFull(BtrfsNode node) {
        return node.size >= 2 * degree - 2;
    }

    public void split(BtrfsNode parent, BtrfsNode child, int parentIndex) {

        // we already split when there is only one free space left in the node (2 * degree - 2 keys)
        // because we might need to split an existing key when inserting a new one
        if (child.size < 2 * degree - 2) {
            throw new IllegalArgumentException("Node must have at least 2*degree - 2 keys when splitting");
        }

        if (parent != null && parent.size == 2 * degree - 1) {
            throw new IllegalArgumentException("Parent is full");
        }

        //create new node
        BtrfsNode right = new BtrfsNode(degree);

        //calculate length of left child
        int leftLength = child.childLength[degree - 1];
        for (int i = 0; i < degree - 1; i++) {
            leftLength += child.childLength[i];
            leftLength += child.keys[i].length();
        }

        //calculate length of right child
        int rightLength = child.childLength[2 * degree - 1];
        for (int i = degree; i < child.size; i++) {
            rightLength += child.childLength[i];
            rightLength += child.keys[i].length();
        }

        //copy keys, children and childLengths to right
        System.arraycopy(child.keys, degree, right.keys, 0, degree - 1);
        System.arraycopy(child.children, degree, right.children, 0, degree);
        System.arraycopy(child.childLength, degree, right.childLength, 0, degree);

        //update sizes of left and right
        child.size = degree - 1;
        right.size = child.size - degree;

        //splitting the root
        if (parent == null) {

            //create new root
            parent = new BtrfsNode(degree);

            //add middle key of child to parent
            parent.keys[0] = child.keys[degree - 1];

            //add left and right to children of parent
            parent.children[0] = child;
            parent.children[1] = right;

            //set childLengths of parent
            parent.childLength[0] = leftLength;
            parent.childLength[1] = rightLength;

            //set sizes of parent
            parent.size = 1;

            //set new root
            root = parent;

        } else {
            //move keys of parent to the right
            for (int i = parent.size - 1; i >= parentIndex; i--) {
                parent.keys[i + 1] = parent.keys[i];
            }

            //add middle key of child to parent
            parent.keys[parentIndex] = child.keys[degree - 1];

            //move children and childrenLength of parent to the right
            for (int i = parent.size; i > parentIndex; i--) {
                parent.children[i + 1] = parent.children[i];
                parent.childLength[i + 1] = parent.childLength[i];
            }

            //add right to children of parent
            parent.children[parentIndex + 1] = right;

            //set childLengths of parent
            parent.childLength[parentIndex + 1] = rightLength;

            //update size of parent
            parent.size++;
        }

        //remove unused children from old node to allow garbage collection
        for (int i = degree; i < 2 * degree; i++) {
            child.children[i] = null;
        }
    }

    public StorageView read(int start, int length) {
        return read(start, length, root);
    }

    public StorageView read(int start, int length, BtrfsNode node) {

        int cumulativeLength = 0;
        int lengthRead = 0;

        StorageView view = StorageView.EMPTY;

        for (int i = 0; i < node.size; i++) {
            //before i-th key and i-th child.

            //read from i-th child if start is in front of or in the i-th child, and it exists
            if (start < cumulativeLength + node.childLength[i] && node.children[i] != null) {
                view = view.plus(read(start - cumulativeLength, length - lengthRead, node.children[i]));
                lengthRead += Math.max(node.childLength[i], length - lengthRead);
            }

            cumulativeLength += node.childLength[i];

            //check if we have read enough
            if (lengthRead == length) {
                return view;
            }

            //read from i-th key if start is in front of or in the i-th key
            if (start < cumulativeLength + node.keys[i].length()) {

                int viewStart = node.keys[i].start() + Math.max(0, start - cumulativeLength);
                int viewLength = Math.min(node.keys[i].length(), length - lengthRead);

                view = view.plus(storage.createView(new Interval(viewStart, viewLength)));
                lengthRead += viewLength;
            }

            cumulativeLength += node.keys[i].length();

            //check if we have read enough
            if (lengthRead == length) {
                return view;
            }
        }

        //we reached the last child and haven't read enough -> if it doesn't exist we hope that this isn't the root
        if (node.children[node.size] == null) {
            return view;
        }

        //read from last child if it exists, and we haven't read enough
        view = view.plus(read(start - cumulativeLength, length - lengthRead, node.children[node.size]));

        return view;
    }

    public void remove(int start, int length) {
        remove(start, length, root);
    }

    public void remove(int start, int length, BtrfsNode node) {

        LinkedParentList parent = new LinkedParentList(null, null, -1);
        int removedLength = 0;
        int cumulativeLength = 0;
        int startIndex = 0;

        // descend into the tree
        outer: while (true) {

            //iterate over all children and keys
            for (int i = startIndex; i < node.size; i++) {
                //before i-th child and i-th child.

                //check if we have removed enough
                if (removedLength > length) {
                    throw new AssertionError(); //sanity check
                } else if (removedLength == length) {
                    return;
                }

                //remove from i-th child if start is in front of or in the i-th child, and it exists
                if (node.children[i] != null && start <= cumulativeLength + node.childLength[i]) {

                    ensureChildSize(parent, node.children[i], i);

                    //calculate how much we will remove from the child
                    int removedInChild = Math.min(length - removedLength, node.childLength[i] - (start - cumulativeLength));

                    //update childLength of parent accordingly
                    node.childLength[i] -= removedInChild;

                    //descend into child
                    parent = new LinkedParentList(parent, node, i);
                    node = node.children[i];

                    //set start index for next loop
                    startIndex = 0;

                    continue outer;
                }

                cumulativeLength += node.childLength[i];

                //get the i-th key
                Interval key = node.keys[i];
                int keyStart = cumulativeLength + 1;

                //if start is in the i-th key we just have to shorten the interval
                if (start > keyStart && start < cumulativeLength + 1 + key.length()) {

                    //calculate the new length of the key
                    int newLength = start - (keyStart);

                    //update the key
                    node.keys[i] = new Interval(key.start(), newLength);

                    //update removedLength
                    removedLength += key.length() - newLength;

                    //update cumulativeLength
                    cumulativeLength += key.length();

                    //continue with next key
                    continue;
                }

                //if start is in front of or the start of the i-th key we have to remove the key
                if (start <= keyStart) {

                    //if we are in a leaf node we can just remove the key
                    if (node.children[0] == null) {

                        //move all keys after the removed key to the left
                        System.arraycopy(node.keys, i + 1, node.keys, i, node.size - i - 1);

                        //remove (duplicated) last key
                        node.keys[node.size - 1] = null;

                        //update size
                        node.size--;

                        //update removedLength
                        removedLength += key.length();

                        //update cumulativeLength
                        cumulativeLength += key.length();

                        //the next key moved one index to the left
                        i--;

                        //continue with next key
                        continue;
                    }

                    //remove key from inner node

                    //try to replace with rightmost key of left child
                    if (node.children[i].size >= degree) {
                        Interval removedKey = removeRightMostKey(new LinkedParentList(parent, node, i), node.children[i]);

                        //update childLength of current node
                        node.childLength[i] -= removedKey.length();

                        //update key
                        node.keys[i] = removedKey;

                        continue;
                    }

                    //try to replace with leftmost key of right child
                    if (node.children[i + 1].size >= degree) {
                        Interval removedKey = removeLeftMostKey(new LinkedParentList(parent, node, i + 1), node.children[i + 1]);

                        //update childLength of current node
                        node.childLength[i + 1] -= removedKey.length();

                        //update key
                        node.keys[i] = removedKey;

                        //update cumulativeLength
                        cumulativeLength += key.length();

                        //update removedLength
                        removedLength += key.length();

                        //set start index for next loop
                        startIndex = 0;

                        continue;
                    }

                    //if both children have only degree - 1 keys we have to merge them and remove the key from the merged node
                    int leftNodeLength = lengthOfNode(node.children[i]);
                    mergeWithRightChild(node, i);
                    remove(leftNodeLength + 1, node.keys[i].length(), node.children[i]);

                    //update childLength of current node
                    node.childLength[i] -= key.length();

                    //update cumulativeLength
                    cumulativeLength += key.length();

                    //update removedLength
                    removedLength += key.length();

                    //if the key we removed merged with the children was the last key of the node we have to update the root
                    if (node.size == 0) {
                        root = node.children[0];
                    }

                    continue;

                }

                //otherwise (if start is behind the i-th key) we just continue
                cumulativeLength += key.length();
            }

            //only the last child is left

            //check if we have removed enough
            if (removedLength > length) {
                throw new AssertionError(); //sanity check
            } else if (removedLength == length) {
                return;
            }

            //remove from the last child if start is in front of or in the i-th child, and it exists
            if (node.children[node.size] != null && start <= cumulativeLength + node.childLength[node.size]) {

                ensureChildSize(parent, node.children[node.size], node.size);

                //calculate how much we will remove from the child
                int removedInChild = Math.min(length - removedLength, node.childLength[node.size] - (start - cumulativeLength));

                //update childLength of parent accordingly
                node.childLength[node.size] -= removedInChild;

                //descend into child
                parent = new LinkedParentList(parent, node, node.size);
                node = node.children[node.size];

                //set start index for next loop
                startIndex = 0;

                continue;
            }

            //if we did not descend into the last child we have to continue at the parent at the index where we last stopped

            //set start index for next loop
            startIndex = parent.index;

            //load parent
            node = parent.node;
            parent = parent.parent;

            //check if we have traversed the whole tree
            if (node == null) {
                throw new IllegalArgumentException("start + length is out of bounds");
            }

        }

    }

    private int lengthOfNode(BtrfsNode node) {
        int length = 0;

        for (int i = 0; i < node.size; i++) {
            length += node.childLength[i];
            length += node.keys[i].length();
        }

        length += node.childLength[node.size];

        return length;
    }

    private Interval removeRightMostKey(LinkedParentList parent, BtrfsNode node) {
        //node has at least degree keys

        if (parent.node.size < degree) {
            throw new AssertionError(); //sanity check
        }

        //check if node is a leaf
        if (node.children[0] == null) {

            //get right most key
            Interval key = node.keys[node.size - 1];

            //remove key
            node.keys[node.size - 1] = null;

            //update size
            node.size--;

            return key;
        } else { //if node is an inner node continue downward

            //update parent
            parent = new LinkedParentList(parent, node, node.size);

            ensureChildSize(parent, node.children[node.size], node.size);

            Interval key = removeRightMostKey(parent, node.children[node.size]);

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

            //get left most key
            Interval key = node.keys[0];

            //move all other keys to the left
            System.arraycopy(node.keys, 1, node.keys, 0, node.size - 1);

            //remove (duplicated) last key
            node.keys[node.size - 1] = null;

            //update size
            node.size--;

            return key;
        } else { //if node is an inner node continue downward

            //update parent
            parent = new LinkedParentList(parent, node, node.size);

            ensureChildSize(parent, node.children[0], node.size);

            Interval key = removeLeftMostKey(parent, node.children[0]);

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

    private void ensureChildSize(LinkedParentList parent, BtrfsNode child, int index) {

        //check if child has at least degree keys
        if (child.size < degree) {

            //get parent node
            BtrfsNode parentNode = parent.node;

            //get left and right sibling
            BtrfsNode leftSibling = index > 0 ? parentNode.children[index - 1] : null;
            BtrfsNode rightSibling = index < parentNode.size - 1? parentNode.children[index + 1] : null;

            //rotate a key from left or right child if possible
            if (leftSibling != null && leftSibling.size >= degree) {
                rotateFromLeftChild(parentNode, index);
            } else if (rightSibling != null && rightSibling.size >= degree) {
                rotateFromRightChild(parentNode, index);
            } else {//if we can't rotate, merge with left or right child

                //ensure that the parent has at least degree keys before merging
                if (parentNode.size < degree) {

                    //the root does not have to have at least degree keys
                    if (parentNode != root) {
                        //recursively fix size of parent
                        ensureChildSize(parent.parent, parentNode, parent.index);
                    }
                }

                if (index > 0) {
                    mergeWithLeftChild(child, index);
                } else {
                    mergeWithRightChild(child, index);
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
        middleChild.size += 2*degree - 1;
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
        middleChild.size += 2*degree - 1;
    }

    private void rotateFromLeftChild(BtrfsNode parent, int index) {

        //get left child
        BtrfsNode leftChild = parent.children[index - 1];

        //store and remove last key and child of left child
        Interval key = leftChild.keys[leftChild.size - 1];
        leftChild.keys[leftChild.size - 1] = null;

        BtrfsNode lastChild = leftChild.children[leftChild.size];
        leftChild.children[leftChild.size] = null;

        int lastChildLength = leftChild.childLength[leftChild.size];
        leftChild.childLength[leftChild.size] = 0;

        //update size of left child
        leftChild.size--;

        //update childLength of parent (childLength of the parent of the parent doesn't change)
        parent.childLength[index - 1] -= key.length() + lastChildLength;

        //store and replace key of parent
        Interval parentKey = parent.keys[index];
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
        BtrfsNode rightChild = parent.children[index + 1];

        //store and remove first key and child of right child
        Interval key = rightChild.keys[0];
        rightChild.keys[0] = null;

        BtrfsNode lastChild = rightChild.children[0];
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
