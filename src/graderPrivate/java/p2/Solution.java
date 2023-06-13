package p2;

import p2.btrfs.BtrfsFile;
import p2.btrfs.BtrfsNode;
import p2.btrfs.IndexedNodeLinkedList;
import p2.storage.Interval;

import static p2.TreeUtil.*;

@SuppressWarnings("DuplicatedCode")
public class Solution {

    public static void split(IndexedNodeLinkedList indexedNode, BtrfsFile tree) throws NoSuchFieldException, IllegalAccessException {
        if (!indexedNode.node.isFull()) {
            throw new IllegalArgumentException("node is not full when splitting");
        }

        BtrfsNode originalNode = indexedNode.node;
        IndexedNodeLinkedList parent = indexedNode.parent;
        BtrfsNode parentNode = parent == null ? null : parent.node;

        if (parentNode != null && parentNode.isFull()) {
            split(parent, tree);

            // parentNode might have changed after splitting
            parentNode = parent.node;
        }

        int degree = getDegree(tree);
        int maxKeys = degree * 2 - 1;

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
            newRoot.childLengths[0] = tree.getSize() - rightLength - indexedNode.node.keys[degree - 1].length();
            newRoot.childLengths[1] = rightLength;

            // set sizes of parent
            newRoot.size = 1;

            // set new root
            setRoot(tree, newRoot);

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

    public static void rotateFromLeftSibling(IndexedNodeLinkedList indexedNode) {

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

    public static void rotateFromRightSibling(IndexedNodeLinkedList indexedNode) {

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

    public static void mergeWithLeftSibling(IndexedNodeLinkedList indexedNode) throws NoSuchFieldException, IllegalAccessException {

        BtrfsNode parentNode = indexedNode.parent.node;
        int parentIndex = indexedNode.parent.index;
        BtrfsNode middleChild = indexedNode.node;
        BtrfsNode leftChild = parentNode.children[parentIndex - 1];

        int degree = getDegree(indexedNode.node);
        int maxKeys = degree * 2 - 1;

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

    public static void mergeWithRightSibling(IndexedNodeLinkedList indexedNode) throws NoSuchFieldException, IllegalAccessException {

        BtrfsNode parentNode = indexedNode.parent.node;
        int parentIndex = indexedNode.parent.index;
        BtrfsNode middleChild = indexedNode.node;
        BtrfsNode rightChild = parentNode.children[parentIndex + 1];

        int degree = getDegree(indexedNode.node);
        int maxKeys = degree * 2 - 1;

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

    public static void ensureSize(IndexedNodeLinkedList indexedNode, BtrfsFile tree) throws NoSuchFieldException, IllegalAccessException {

        int degree = getDegree(indexedNode.node);

        // check if node has at least degree keys
        if (indexedNode.node.size >= degree || indexedNode.node == getRoot(tree)) {
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
            ensureSize(indexedNode.parent, tree);

            if (parentIndex > 0) {
                mergeWithLeftSibling(indexedNode);
            } else {
                mergeWithRightSibling(indexedNode);
            }

            // if the root has no keys left after merging, set the only node as the new root
            if (getRoot(tree).size == 0 && getRoot(tree).children[0] != null) {
                setRoot(tree, getRoot(tree).children[0]);
            }
        }
    }

}
