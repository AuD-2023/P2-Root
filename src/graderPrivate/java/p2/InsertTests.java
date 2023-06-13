package p2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.json.JsonClasspathSource;
import org.junitpioneer.jupiter.json.Property;
import org.sourcegrade.jagr.api.rubric.TestForSubmission;
import org.tudalgo.algoutils.tutor.general.assertions.Context;
import p2.btrfs.BtrfsFile;
import p2.btrfs.IndexedNodeLinkedList;
import p2.storage.Interval;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.*;
import static p2.TreeUtil.*;
import static p2.TreeUtil.getRoot;

@TestForSubmission
public class InsertTests {

    @ParameterizedTest
    @JsonClasspathSource(value = "InsertTests.json", data = "testEnoughSpace")
    public void testEnoughSpace(@Property("tree") List<Object> tree,
                                @Property("degree") int degree,
                                @Property("intervalsToInsert") List<String> intervalsToInsert,
                                @Property("index") int index,
                                @Property("expected") List<Object> expected) throws Throwable {
        testInsert(tree, degree, intervalsToInsert, index, expected);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "InsertTests.json", data = "testNotEnoughSpaceSameNode")
    public void testNotEnoughSpaceSameNode(@Property("tree") List<Object> tree,
                                @Property("degree") int degree,
                                @Property("intervalsToInsert") List<String> intervalsToInsert,
                                @Property("index") int index,
                                @Property("expected") List<Object> expected) throws Throwable {
        testInsert(tree, degree, intervalsToInsert, index, expected);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "InsertTests.json", data = "testNotEnoughSpaceNewNode")
    public void testNotEnoughSpaceNewNode(@Property("tree") List<Object> tree,
                                           @Property("degree") int degree,
                                           @Property("intervalsToInsert") List<String> intervalsToInsert,
                                           @Property("index") int index,
                                           @Property("expected") List<Object> expected) throws Throwable {
        testInsert(tree, degree, intervalsToInsert, index, expected);
    }

    private void testInsert(List<Object> tree,
                            int degree,
                            List<String> intervalsToInsert,
                            int index,
                            List<Object> expected) throws Throwable {

        Context.Builder<?> context = contextBuilder()
            .subject("BtrfsFile.insert()")
            .add("tree", treeToString(tree))
            .add("degree", degree)
            .add("intervalsToInsert", intervalsToInsert)
            .add("index", index)
            .add("expected", expected);

        TreeUtil.FileAndStorage actualFileAndStorage = constructTree(tree, degree);
        BtrfsFile actualTree = actualFileAndStorage.file();

        overrideSplit(actualTree);

        TreeUtil.FileAndStorage expectedFileAndStorage = constructTree(expected, degree);
        BtrfsFile expectedTree = expectedFileAndStorage.file();

        List<Interval> intervals = new ArrayList<>();

        for (String string : intervalsToInsert) {
            intervals.add(addToStorage(string, actualFileAndStorage.storage(), actualFileAndStorage.allocator()));
        }

        setSize(actualTree, actualTree.getSize() + getSize(intervals));

        call(() -> callInsert(actualTree, new ArrayList<>(intervals), new IndexedNodeLinkedList(null, getRoot(actualTree), index)), context.build(),
            TR -> "BtrfsFile.insert() should not throw an exception.");

        assertTreeEquals(context.build(), "The tree is not correct.",
            getRoot(expectedTree), getRoot(actualTree),
            expectedFileAndStorage.storage(), actualFileAndStorage.storage());
    }

    private void callInsert(BtrfsFile tree, List<Interval> intervals, IndexedNodeLinkedList indexedLeaf) throws Exception {

        Method method = BtrfsFile.class.getDeclaredMethod("insert", List.class, IndexedNodeLinkedList.class, int.class);
        method.setAccessible(true);
        try {
            method.invoke(tree, intervals, indexedLeaf, getSize(intervals));
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            } else {
                throw e;
            }
        }
    }

    private int getSize(List<Interval> intervals) {
        int size = 0;
        for (Interval interval : intervals) {
            size += interval.length();
        }
        return size;
    }

}
