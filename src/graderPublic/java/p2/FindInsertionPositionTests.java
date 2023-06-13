package p2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.json.JsonClasspathSource;
import org.junitpioneer.jupiter.json.Property;
import org.sourcegrade.jagr.api.rubric.TestForSubmission;
import org.tudalgo.algoutils.tutor.general.assertions.Context;
import p2.btrfs.BtrfsFile;
import p2.btrfs.BtrfsNode;
import p2.btrfs.IndexedNodeLinkedList;
import p2.storage.Interval;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.assertEquals;
import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.callObject;
import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.contextBuilder;
import static p2.TreeUtil.FileAndStorage;
import static p2.TreeUtil.assertIndexedNodeLinkedListEquals;
import static p2.TreeUtil.assertTreeEquals;
import static p2.TreeUtil.constructTree;
import static p2.TreeUtil.getRoot;
import static p2.TreeUtil.treeToString;

@TestForSubmission
public class FindInsertionPositionTests {

    @ParameterizedTest
    @JsonClasspathSource(value = "FindInsertionPositionTests.json", data = "testNoChildNoSplitting")
    public void testNoChildNoSplitting(@Property("tree") List<Object> tree,
                                       @Property("degree") int degree,
                                       @Property("start") int start,
                                       @Property("insertionSize") int insertionSize,
                                       @Property("expectedIndices") List<Integer> expectedIndices) throws Throwable {

        testFindInsertionPosition(tree, degree, start, insertionSize, expectedIndices, tree);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "FindInsertionPositionTests.json", data = "testWithChildNoSplitting")
    public void testWithChildNoSplitting(@Property("tree") List<Object> tree,
                                         @Property("degree") int degree,
                                         @Property("start") int start,
                                         @Property("insertionSize") int insertionSize,
                                         @Property("expectedIndices") List<Integer> expectedIndices) throws Throwable {

        testFindInsertionPosition(tree, degree, start, insertionSize, expectedIndices, tree);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "FindInsertionPositionTests.json", data = "testWithKeySplit")
    public void testWithKeySplitting(@Property("tree") List<Object> tree,
                                     @Property("degree") int degree,
                                     @Property("start") int start,
                                     @Property("insertionSize") int insertionSize,
                                     @Property("expectedIndices") List<Integer> expectedIndices,
                                     @Property("expectedTree") List<Object> expectedTree) throws Throwable {

        testFindInsertionPosition(tree, degree, start, insertionSize, expectedIndices, expectedTree);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "FindInsertionPositionTests.json", data = "testWithLeafSplitting")
    public void testWithLeafSplitting(@Property("tree") List<Object> tree,
                                      @Property("degree") int degree,
                                      @Property("start") int start,
                                      @Property("insertionSize") int insertionSize,
                                      @Property("expectedIndices") List<Integer> expectedIndices,
                                      @Property("expectedTree") List<Object> expectedTree) throws Throwable {

        testFindInsertionPosition(tree, degree, start, insertionSize, expectedIndices, expectedTree);
    }

    private void testFindInsertionPosition(List<Object> tree,
                                           int degree,
                                           int start,
                                           int insertionSize,
                                           List<Integer> expectedIndices,
                                           List<Object> expected) throws Throwable {

        Context.Builder<?> context = contextBuilder()
            .subject("BtrfsFile.findInsertionPosition()")
            .add("tree", treeToString(tree))
            .add("degree", degree)
            .add("start", start)
            .add("InsertionSize", insertionSize)
            .add("expectedIndices", expectedIndices)
            .add("expected tree", treeToString(expected));

        FileAndStorage actualFileAndStorage = constructTree(tree, degree);
        BtrfsFile actualTree = actualFileAndStorage.file();

        FileAndStorage expectedFileAndStorage = constructTree(expected, degree);
        BtrfsFile expectedTree = expectedFileAndStorage.file();

        IndexedNodeLinkedList actualIndexedNode = callObject(() -> callFindInsertionPosition(
            actualTree, new IndexedNodeLinkedList(null, getRoot(actualTree), 0), start, insertionSize),
            context.build(), TR -> "findInsertionPosition() should not throw an exception");

        context.add("actual tree", treeToString(actualTree, actualFileAndStorage.storage()));

        IndexedNodeLinkedList expectedIndexedNode = createIndexedNode(null, getRoot(actualTree), new ArrayList<>(expectedIndices));
        updateChildLengths(createIndexedNode(null, getRoot(expectedTree), new ArrayList<>(expectedIndices)), insertionSize);

        assertTreeEquals(context.build(), "The tree is not correct", getRoot(expectedTree),
            getRoot(actualTree), expectedFileAndStorage.storage(), actualFileAndStorage.storage());

        assertEquals(expectedTree.getSize(), actualTree.getSize(), context.build(),
            TR -> "The size of the tree should not change");

        assertIndexedNodeLinkedListEquals(context.build(), expectedIndexedNode, actualIndexedNode,
            actualFileAndStorage.storage());
    }

    private void updateChildLengths(IndexedNodeLinkedList indexedNode, int insertionSize) {
        if (indexedNode.parent != null) {
            indexedNode.parent.node.childLengths[indexedNode.parent.index] += insertionSize;
            updateChildLengths(indexedNode.parent, insertionSize);
        }
    }

    private IndexedNodeLinkedList createIndexedNode(IndexedNodeLinkedList parent, BtrfsNode node, List<Integer> indices) {

        IndexedNodeLinkedList indexedNode = new IndexedNodeLinkedList(parent, node, indices.get(0));
        indices.remove(0);

        if (indices.isEmpty()) {
            return indexedNode;
        }

        return createIndexedNode(indexedNode, node.children[indexedNode.index], indices);
    }

    private IndexedNodeLinkedList callFindInsertionPosition(BtrfsFile tree,
                                                            IndexedNodeLinkedList indexedNode,
                                                            int start,
                                                            int insertionSize) throws Exception {

        Method method = BtrfsFile.class.getDeclaredMethod("findInsertionPosition", IndexedNodeLinkedList.class, int.class, int.class, int.class, Interval.class);
        method.setAccessible(true);
        try {
            return (IndexedNodeLinkedList) method.invoke(tree, indexedNode, start, 0, insertionSize, null);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            } else {
                throw e;
            }
        }
    }
}
