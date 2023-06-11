package p2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.json.JsonClasspathSource;
import org.junitpioneer.jupiter.json.Property;
import org.sourcegrade.jagr.api.rubric.TestForSubmission;
import org.tudalgo.algoutils.tutor.general.assertions.Context;
import p2.btrfs.BtrfsFile;
import p2.btrfs.BtrfsNode;
import p2.storage.StorageView;
import p2.storage.StringEncoder;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.tudalgo.algoutils.tutor.general.assertions.Assertions2.*;
import static p2.TreeUtil.*;

@TestForSubmission
public class ReadTests {

    @ParameterizedTest
    @JsonClasspathSource(value = "ReadTests.json", data = "testReadNoChildren")
    public void testReadNoChildren(@Property("tree") List<Object> tree,
                                @Property("degree") int degree,
                                @Property("start") int start,
                                @Property("length") int length,
                                @Property("expected") String expected) throws Throwable {
        testRead(tree, degree, start, length, expected);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "ReadTests.json", data = "testReadWholeIntervals")
    public void testReadWholeIntervals(@Property("tree") List<Object> tree,
                                   @Property("degree") int degree,
                                   @Property("start") int start,
                                   @Property("length") int length,
                                   @Property("expected") String expected) throws Throwable {
        testRead(tree, degree, start, length, expected);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "ReadTests.json", data = "testReadStartIntervalPartially")
    public void testReadStartIntervalPartially(@Property("tree") List<Object> tree,
                                   @Property("degree") int degree,
                                   @Property("start") int start,
                                   @Property("length") int length,
                                   @Property("expected") String expected) throws Throwable {
        testRead(tree, degree, start, length, expected);
    }

    @ParameterizedTest
    @JsonClasspathSource(value = "ReadTests.json", data = "testReadStartAndEndIntervalPartially")
    public void testReadStartAndEndIntervalPartially(@Property("tree") List<Object> tree,
                                   @Property("degree") int degree,
                                   @Property("start") int start,
                                   @Property("length") int length,
                                   @Property("expected") String expected) throws Throwable {
        testRead(tree, degree, start, length, expected);
    }

    private void testRead(List<Object> tree,
                          int degree,
                          int start,
                          int length,
                          String expected) throws Throwable {

        Context.Builder<?> context = contextBuilder()
            .subject("BtrfsFile.read()")
            .add("tree", treeToString(tree))
            .add("degree", degree)
            .add("start", start)
            .add("length", length)
            .add("expected", expected);

        TreeUtil.FileAndStorage actualFileAndStorage = constructTree(tree, degree);
        BtrfsFile actualTree = actualFileAndStorage.file();
        BtrfsNode root = getRoot(actualTree);

        TreeUtil.FileAndStorage expectedFileAndStorage = constructTree(tree, degree);
        BtrfsFile expectedTree = expectedFileAndStorage.file();

        StorageView actualView = callObject(() -> callRead(actualTree, start, length, root), context.build(),
            TR -> "BtrfsFile.read() should not throw an exception.");

        String actual = StringEncoder.INSTANCE.decode(actualView);

        assertEquals(expected, actual, context.build(),
            TR -> "BtrfsFile.read() did not return the correct value.");

        assertTreeEquals(context.build(), "The tree is not correct.",
            getRoot(expectedTree), getRoot(actualTree),
            expectedFileAndStorage.storage(), actualFileAndStorage.storage());
    }

    private StorageView callRead(BtrfsFile tree, int start, int length, BtrfsNode root) throws Exception {

        Method method = BtrfsFile.class.getDeclaredMethod("read", int.class, int.class, BtrfsNode.class, int.class, int.class);
        method.setAccessible(true);
        try {
            return (StorageView) method.invoke(tree, start, length, root, 0, 0);
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof Exception exception) {
                throw exception;
            } else {
                throw e;
            }
        }
    }

}
