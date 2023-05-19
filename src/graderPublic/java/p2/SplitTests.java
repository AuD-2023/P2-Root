package p2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.json.JsonClasspathSource;
import org.junitpioneer.jupiter.json.Property;
import org.tudalgo.algoutils.tutor.general.assertions.Context;
import p2.btrfs.BtrfsFile;
import p2.btrfs.BtrfsNode;
import p2.btrfs.IndexedNodeLinkedList;

import java.util.List;

import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.assertEquals;
import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.call;
import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.contextBuilder;
import static p2.TreeUtil.assertTreeEquals;
import static p2.TreeUtil.constructTree;
import static p2.TreeUtil.getRoot;
import static p2.TreeUtil.treeToString;
import static p2.TreeUtil.FileAndStorage;

public class SplitTests {

    @ParameterizedTest
    @JsonClasspathSource(value = "SplitTests.json", data = "testSimpleSplit")
    public void testSimpleSplit(@Property("tree") List<Object> tree,
                                 @Property("degree") int degree,
                                 @Property("parentIndex") int parentIndex,
                                 @Property("childIndex") int childIndex,
                                 @Property("expected") List<Object> expected) throws NoSuchFieldException, IllegalAccessException {

        Context.Builder<?> context = contextBuilder()
            .add("original Tree", treeToString(tree))
            .add("degree", degree)
            .add("parentIndex", parentIndex)
            .add("childIndex", childIndex)
            .add("expected tree", treeToString(expected));

        FileAndStorage expectedFileAndStorage = constructTree(expected, degree);
        BtrfsFile expectedTree = expectedFileAndStorage.file();

        FileAndStorage actualFileAndStorage = constructTree(tree, degree);
        BtrfsFile actualTree = actualFileAndStorage.file();
        BtrfsNode root = getRoot(actualTree);
        BtrfsNode child = root.children[parentIndex];

        IndexedNodeLinkedList indexedRoot = new IndexedNodeLinkedList(null, root, parentIndex);
        IndexedNodeLinkedList indexedChild = new IndexedNodeLinkedList(indexedRoot, child, childIndex);

        call(() -> actualTree.split(indexedChild), context.build(), TR -> "split() should not throw an exception");

        context.add("actual tree", treeToString(actualTree, actualFileAndStorage.storage()));

        assertTreeEquals(context.build(), "The tree is not correct.",
            getRoot(expectedTree), getRoot(actualTree), expectedFileAndStorage.storage(), actualFileAndStorage.storage());

        assertEquals(root, getRoot(actualTree), context.build(), TR -> "The root should not change.");
        assertEquals(expectedTree.getSize(), actualTree.getSize(), context.build(), TR -> "The size should not change.");
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "SplitTests.json", data = "testIndexedNodeLinkedList")
    public void testIndexedNodeLinkedList(@Property("tree") List<Object> tree,
                                          @Property("degree") int degree,
                                          @Property("parentIndex") int parentIndex,
                                          @Property("childIndex") int childIndex,
                                          @Property("expectedParentIndex") int expectedParentIndex,
                                          @Property("expectedChildIndex") int expectedChildIndex) throws NoSuchFieldException, IllegalAccessException {

        Context.Builder<?> context = contextBuilder()
            .add("original Tree", treeToString(tree))
            .add("degree", degree)
            .add("parentIndex", parentIndex)
            .add("childIndex", childIndex)
            .add("expectedParentIndex", expectedParentIndex)
            .add("expectedChildIndex", expectedChildIndex);

        FileAndStorage actualFileAndStorage = constructTree(tree, degree);
        BtrfsFile actualTree = actualFileAndStorage.file();
        BtrfsNode root = getRoot(actualTree);
        BtrfsNode child = root.children[parentIndex];

        IndexedNodeLinkedList indexedRoot = new IndexedNodeLinkedList(null, root, parentIndex);
        IndexedNodeLinkedList indexedChild = new IndexedNodeLinkedList(indexedRoot, child, childIndex);

        call(() -> actualTree.split(indexedChild), context.build(), TR -> "split() should not throw an exception");

        context.add("actual tree", treeToString(actualTree, actualFileAndStorage.storage()));

        assertEquals(root, indexedRoot.node, context.build(),
            TR -> "indexedNode.parent.node should be equal to the old value.");
        assertEquals(expectedParentIndex, indexedRoot.index, context.build(),
            TR -> "indexedNode.parent.index is not correct.");
        assertEquals(root.children[expectedParentIndex], indexedChild.node, context.build(),
            TR -> "indexedNode.node is not equal to root.children[parentIndex].");
        assertEquals(expectedChildIndex, indexedChild.index, context.build(),
            TR -> "indexedNode.index is not correct.");
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "SplitTests.json", data = "testSplitRoot")
    public void testSplitRoot(@Property("tree") List<Object> tree,
                              @Property("degree") int degree,
                              @Property("index") int index,
                              @Property("expectedParentIndex") int expectedParentIndex,
                              @Property("expectedChildIndex") int expectedChildIndex,
                              @Property("expected") List<Object> expected) throws NoSuchFieldException, IllegalAccessException {

        Context.Builder<?> context = contextBuilder()
            .add("original Tree", treeToString(tree))
            .add("degree", degree)
            .add("index", index)
            .add("expectedParentIndex", expectedParentIndex)
            .add("expectedChildIndex", expectedChildIndex)
            .add("expected tree", treeToString(expected));

        FileAndStorage expectedFileAndStorage = constructTree(expected, degree);
        BtrfsFile expectedTree = expectedFileAndStorage.file();

        FileAndStorage actualFileAndStorage = constructTree(tree, degree);
        BtrfsFile actualTree = actualFileAndStorage.file();

        IndexedNodeLinkedList indexedRoot = new IndexedNodeLinkedList(null, getRoot(actualTree), index);

        call(() -> actualTree.split(indexedRoot), context.build(), TR -> "split() should not throw an exception");

        context.add("actual tree", treeToString(actualTree, actualFileAndStorage.storage()));

        assertTreeEquals(context.build(), "The tree is not correct.",
            getRoot(expectedTree), getRoot(actualTree), expectedFileAndStorage.storage(), actualFileAndStorage.storage());

        assertEquals(expectedTree.getSize(), actualTree.getSize(), context.build(), TR -> "The size should not change.");

        assertEquals(getRoot(actualTree), indexedRoot.parent.node, context.build(),
            TR -> "indexedNode.parent.node should be equal to the root attribute of the tree.");
        assertEquals(expectedParentIndex, indexedRoot.parent.index, context.build(),
            TR -> "indexedNode.parent.index is not correct.");
        assertEquals(getRoot(actualTree).children[expectedParentIndex], indexedRoot.node, context.build(),
            TR -> "indexedNode.node is not equal to root.children[parentIndex].");
        assertEquals(expectedChildIndex, indexedRoot.index, context.build(),
            TR -> "indexedNode.index is not correct.");
    }
}
