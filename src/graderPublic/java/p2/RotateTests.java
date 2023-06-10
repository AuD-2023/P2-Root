package p2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.json.JsonClasspathSource;
import org.junitpioneer.jupiter.json.Property;
import org.sourcegrade.jagr.api.rubric.TestForSubmission;
import org.tudalgo.algoutils.tutor.general.assertions.Context;
import p2.btrfs.BtrfsFile;
import p2.btrfs.BtrfsNode;
import p2.btrfs.IndexedNodeLinkedList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.assertEquals;
import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.call;
import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.contextBuilder;
import static p2.TreeUtil.assertTreeEquals;
import static p2.TreeUtil.constructTree;
import static p2.TreeUtil.getRoot;
import static p2.TreeUtil.treeToString;

@SuppressWarnings("DuplicatedCode")
@TestForSubmission
public class RotateTests {

    @ParameterizedTest
    @JsonClasspathSource(value = "RotateTests.json", data = "testRotateRight")
    public void testRotateRightOriginalNode(@Property("tree") List<Object> tree,
                                            @Property("degree") int degree,
                                            @Property("parentIndex") int parentIndex,
                                            @Property("childIndex") int childIndex,
                                            @Property("expected") List<Object> expected) throws Throwable {

        testRotate(tree, degree, parentIndex, childIndex, expected, file -> getRoot(file).children[parentIndex], true);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "RotateTests.json", data = "testRotateRight")
    public void testRotateRightRightNode(@Property("tree") List<Object> tree,
                                         @Property("degree") int degree,
                                         @Property("parentIndex") int parentIndex,
                                         @Property("childIndex") int childIndex,
                                         @Property("expected") List<Object> expected) throws Throwable {

        testRotate(tree, degree, parentIndex, childIndex, expected, file -> getRoot(file).children[parentIndex + 1], true);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "RotateTests.json", data = "testRotateRight")
    public void testRotateRightParentNode(@Property("tree") List<Object> tree,
                                          @Property("degree") int degree,
                                          @Property("parentIndex") int parentIndex,
                                          @Property("childIndex") int childIndex,
                                          @Property("expected") List<Object> expected) throws Throwable {

        testRotate(tree, degree, parentIndex, childIndex, expected, TreeUtil::getRoot, true);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "RotateTests.json", data = "testRotateLeft")
    public void testRotateLeftParentNode(@Property("tree") List<Object> tree,
                                          @Property("degree") int degree,
                                          @Property("parentIndex") int parentIndex,
                                          @Property("childIndex") int childIndex,
                                          @Property("expected") List<Object> expected) throws Throwable {

        testRotate(tree, degree, parentIndex, childIndex, expected, TreeUtil::getRoot, false);
    }

    private void testRotate(List<Object> tree,
                            int degree,
                            int parentIndex,
                            int childIndex,
                            List<Object> expected,
                            ThrowingFunction<BtrfsFile, BtrfsNode> testedNodeFunction,
                            boolean right) throws Throwable {

        Context.Builder<?> context = contextBuilder()
            .subject("BtrfsFile.rotateFrom*Sibling()")
            .add("tree", treeToString(tree))
            .add("degree", degree)
            .add("parentIndex", parentIndex)
            .add("childIndex", childIndex)
            .add("expected tree", treeToString(expected));

        TreeUtil.FileAndStorage actualFileAndStorage = constructTree(tree, degree);
        BtrfsFile actualTree = actualFileAndStorage.file();
        BtrfsNode root = getRoot(actualTree);
        BtrfsNode child = root.children[parentIndex];

        TreeUtil.FileAndStorage expectedFileAndStorage = constructTree(expected, degree);
        BtrfsFile expectedTree = expectedFileAndStorage.file();

        IndexedNodeLinkedList indexedRoot = new IndexedNodeLinkedList(null, root, parentIndex);
        IndexedNodeLinkedList indexedChild = new IndexedNodeLinkedList(indexedRoot, root.children[parentIndex], childIndex);

        if (right) {
            call(() -> callRotateRight(actualTree, indexedChild), context.build(),
                TR -> "rotateFromRightSibling should not throw an exception");
        } else {
            call(() -> callRotateLeft(actualTree, indexedChild), context.build(),
                TR -> "rotateFromLeftSibling should not throw an exception");
        }

        context.add("actual tree", treeToString(actualTree, actualFileAndStorage.storage()));

        assertTreeEquals(context.build(), "The tree is not correct.",
            testedNodeFunction.apply(expectedTree), testedNodeFunction.apply(actualTree),
            expectedFileAndStorage.storage(), actualFileAndStorage.storage());

        assertEquals(expectedTree.getSize(), actualTree.getSize(), context.build(),
            TR -> "The size of the tree should not change");

        assertEquals(parentIndex, indexedRoot.index, context.build(),
            TR -> "The index of the parent should not change");
        assertEquals(root, indexedRoot.node, context.build(),
            TR -> "The node of the parent should not change");
        assertEquals(null, indexedRoot.parent, context.build(),
            TR -> "The parent of the parent should not change");

        if (right) {
            assertEquals(childIndex, indexedChild.index, context.build(),
                TR -> "The index of the child should not change");
        } else {
            assertEquals(childIndex + 1, indexedChild.index, context.build(),
                TR -> "The index of the child is not correct");
        }
        assertEquals(child, indexedChild.node, context.build(),
            TR -> "The node of the child should not change");
        assertEquals(indexedRoot, indexedChild.parent, context.build(),
            TR -> "The parent of the child should not change");
    }

    private void callRotateRight(BtrfsFile tree, IndexedNodeLinkedList indexedNode) throws Exception {

        Method method = BtrfsFile.class.getDeclaredMethod("rotateFromRightSibling", IndexedNodeLinkedList.class);
        method.setAccessible(true);
        try {
            method.invoke(tree, indexedNode);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            } else {
                throw e;
            }
        }
    }

    private void callRotateLeft(BtrfsFile tree, IndexedNodeLinkedList indexedNode) throws Exception {

        Method method = BtrfsFile.class.getDeclaredMethod("rotateFromLeftSibling", IndexedNodeLinkedList.class);
        method.setAccessible(true);
        try {
            method.invoke(tree, indexedNode);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            } else {
                throw e;
            }
        }
    }

}

