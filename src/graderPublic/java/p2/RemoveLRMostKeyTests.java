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
import java.util.List;

import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.callObject;
import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.contextBuilder;
import static p2.TreeUtil.assertIntervalEquals;
import static p2.TreeUtil.assertTreeEquals;
import static p2.TreeUtil.constructTree;
import static p2.TreeUtil.getRoot;
import static p2.TreeUtil.treeToString;

@SuppressWarnings("DuplicatedCode")
@TestForSubmission
public class RemoveLRMostKeyTests {

    @ParameterizedTest
    @JsonClasspathSource(value = "RemoveLRMostKeyTests.json", data = "testRemoveRightLeaf")
    public void testRemoveRightLeaf(@Property("tree") List<Object> tree,
                                    @Property("degree") int degree,
                                    @Property("key") String key,
                                    @Property("expected") List<Object> expected) throws Throwable {
        testRemoveLRMostKey(tree, degree, key, expected, true);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "RemoveLRMostKeyTests.json", data = "testRemoveRightNoCorrection")
    public void testRemoveRightNoCorrection(@Property("tree") List<Object> tree,
                                    @Property("degree") int degree,
                                    @Property("key") String key,
                                    @Property("expected") List<Object> expected) throws Throwable {
        testRemoveLRMostKey(tree, degree, key, expected, true);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "RemoveLRMostKeyTests.json", data = "testRemoveRightWithCorrection")
    public void testRemoveRightWithCorrection(@Property("tree") List<Object> tree,
                                            @Property("degree") int degree,
                                            @Property("key") String key,
                                            @Property("expected") List<Object> expected) throws Throwable {
        testRemoveLRMostKey(tree, degree, key, expected, true);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "RemoveLRMostKeyTests.json", data = "testRemoveLeft")
    public void testRemoveLeft(@Property("tree") List<Object> tree,
                                            @Property("degree") int degree,
                                            @Property("key") String key,
                                            @Property("expected") List<Object> expected) throws Throwable {
        testRemoveLRMostKey(tree, degree, key, expected, false);
    }

    public void testRemoveLRMostKey(List<Object> tree,
                                    int degree,
                                    String key,
                                    List<Object> expected, boolean right) throws Throwable {

        Context.Builder<?> context = contextBuilder()
            .subject("BtrfsFile.rotateFromRightSibling()")
            .add("tree", treeToString(tree))
            .add("degree", degree)
            .add("expected tree", treeToString(expected));

        TreeUtil.FileAndStorage actualFileAndStorage = constructTree(tree, degree);
        BtrfsFile actualTree = actualFileAndStorage.file();
        BtrfsNode root = getRoot(actualTree);

        TreeUtil.FileAndStorage expectedFileAndStorage = constructTree(expected, degree);
        BtrfsFile expectedTree = expectedFileAndStorage.file();

        IndexedNodeLinkedList indexedRoot = new IndexedNodeLinkedList(null, root, 0);

        if (right) {
            Interval actual = callObject(() -> callRemoveRightMostKey(actualTree, indexedRoot), context.build(),
                TR -> "removeRightMostKey should not throw an exception");
            context.add("actual tree", treeToString(actualTree, actualFileAndStorage.storage()));

            assertIntervalEquals(context.build(), key, actual, actualFileAndStorage.storage(),
                "removeRightMostKey did not return the correct interval. Expected %s, but was %s");
        } else {
            Interval actual = callObject(() -> callRemoveLeftMostKey(actualTree, indexedRoot), context.build(),
                TR -> "callRemoveLeftMostKey should not throw an exception");
            context.add("actual tree", treeToString(actualTree, actualFileAndStorage.storage()));

            assertIntervalEquals(context.build(), key, actual, actualFileAndStorage.storage(),
                "callRemoveLeftMostKey did not return the correct interval. Expected %s, but was %s");
        }


        assertTreeEquals(context.build(), "The tree is not correct.", getRoot(expectedTree), getRoot(actualTree),
            expectedFileAndStorage.storage(), actualFileAndStorage.storage());
    }

    private Interval callRemoveRightMostKey(BtrfsFile tree, IndexedNodeLinkedList indexedNode) throws Exception {

        Method method = BtrfsFile.class.getDeclaredMethod("removeRightMostKey", IndexedNodeLinkedList.class);
        method.setAccessible(true);
        try {
            return (Interval) method.invoke(tree, indexedNode);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            } else {
                throw e;
            }
        }
    }

    private Interval callRemoveLeftMostKey(BtrfsFile tree, IndexedNodeLinkedList indexedNode) throws Exception {

        Method method = BtrfsFile.class.getDeclaredMethod("removeLeftMostKey", IndexedNodeLinkedList.class);
        method.setAccessible(true);
        try {
            return (Interval) method.invoke(tree, indexedNode);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            } else {
                throw e;
            }
        }
    }

}
