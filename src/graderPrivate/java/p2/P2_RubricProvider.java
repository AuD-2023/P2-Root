package p2;

import org.sourcegrade.jagr.api.rubric.*;
import org.sourcegrade.jagr.api.testing.RubricConfiguration;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

@SuppressWarnings("unused")
public class P2_RubricProvider implements RubricProvider {

    private static Criterion createUntestedCriterion(String shortDescription) {
        return Criterion.builder()
            .shortDescription(shortDescription)
            .grader((testCycle, criterion) ->
                GradeResult.of(criterion.getMinPoints(), criterion.getMaxPoints(), "Not graded by public grader"))
            .maxPoints(1)
            .build();
    }

    @SafeVarargs
    private static Criterion createCriterion(String shortDescription, int maxPoints, Callable<Method>... methodReferences) {

        return createCriterion(shortDescription, maxPoints, Arrays.stream(methodReferences).map(JUnitTestRef::ofMethod).toArray(JUnitTestRef[]::new));
    }

    @SafeVarargs
    private static Criterion createCriterion(String shortDescription, Callable<Method>... methodReferences) {

        return createCriterion(shortDescription, 1, Arrays.stream(methodReferences).map(JUnitTestRef::ofMethod).toArray(JUnitTestRef[]::new));
    }

    private static Criterion createCriterion(String shortDescription, JUnitTestRef... testReferences) {
        return createCriterion(shortDescription, 1, testReferences);
    }

    private static Criterion createCriterion(String shortDescription, int maxPoints, JUnitTestRef... testReferences) {

        if (testReferences.length == 0) {
            return Criterion.builder()
                .shortDescription(shortDescription)
                .maxPoints(1)
                .build();
        }

        Grader.TestAwareBuilder graderBuilder = Grader.testAwareBuilder();

        for (JUnitTestRef reference : testReferences) {
            graderBuilder.requirePass(reference);
        }

        return Criterion.builder()
            .shortDescription(shortDescription)
            .grader(graderBuilder
                .pointsFailedMin()
                .pointsPassedMax()
                .build())
            .maxPoints(maxPoints)
            .build();
    }

    private static Criterion createParentCriterion(String task, String shortDescription, Criterion... children) {
        return Criterion.builder()
            .shortDescription("H" + task + " | " + shortDescription)
            .addChildCriteria(children)
            .build();
    }

    public static final Criterion H1_1 = createCriterion("Die Methode [[[read]]] funktioniert korrekt, wenn der Wurzelknoten keine Kinder hat und nur ein Intervall eingelesen wird.", 7,
        () -> ReadTests.class.getDeclaredMethod("testReadNoChildren", List.class, int.class, int.class, int.class, String.class));

    public static final Criterion H1_4 = createCriterion("Die Methode [[[read]]] funktioniert korrekt.",
        () -> ReadTests.class.getDeclaredMethod("testReadStartIntervalPartially", List.class, int.class, int.class, int.class, String.class),
        () -> ReadTests.class.getDeclaredMethod("testReadStartAndEndIntervalPartially", List.class, int.class, int.class, int.class, String.class));

    public static final Criterion H1 = createParentCriterion("1", "Lesen", H1_1, H1_4);

    public static final Criterion H2_1_1 = createCriterion("Die Methode [[[split]]] splittet den Knoten in normalen Fällen korrekt.", 2,
        () -> SplitTests.class.getDeclaredMethod("testSimpleSplit", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H2_1_2 = createCriterion("Die Methode [[[split]]] aktualisiert die Werte in der übergebenen IndexedNodeLinkedList korrekt.", 5,
        () -> SplitTests.class.getDeclaredMethod("testIndexedNodeLinkedList", List.class, int.class, int.class, int.class, int.class, int.class));

    public static final Criterion H2_1_4 = createCriterion("Die Methode [[[split]]] funktioniert zusätzlich korrekt, wenn rekursive mehrere Knoten gesplittet werden müssen oder der Wurzelknoten gesplittet wird.",
        () -> SplitTests.class.getDeclaredMethod("testSplitRoot", List.class, int.class, int.class, int.class, int.class, List.class),
        () -> SplitTests.class.getDeclaredMethod("testSplitRecursive", List.class, int.class, int.class, int.class, int.class, int.class, int.class, List.class));

    public static final Criterion H2_1 = createParentCriterion("2 a)", "Split", H2_1_1, H2_1_2, H2_1_4);

    public static final Criterion H2_2_1 = createCriterion("Die Methode [[[findInsertionPosition]]] funktioniert korrekt, wenn der WurzelKnoten keine Kinder hat und kein Intervall aufgeteilt werden muss.", 6,
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> FindInsertionPositionTests.class.getDeclaredMethod("testNoChildNoSplitting", List.class, int.class, int.class, int.class, List.class)),
            JUnitTestRef.ofMethod(() -> FindInsertionPositionTestsPublic.class.getDeclaredMethod("testNoChildNoSplitting", List.class, int.class, int.class, int.class, List.class))));

    public static final Criterion H2_2_2 = createCriterion("Die Methode [[[findInsertionPosition]]] funktioniert korrekt, wenn der Wurzelknoten Kinder hat und kein Intervall aufgeteilt werden muss.",
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> FindInsertionPositionTests.class.getDeclaredMethod("testWithChildNoSplitting", List.class, int.class, int.class, int.class, List.class)),
            JUnitTestRef.ofMethod(() -> FindInsertionPositionTestsPublic.class.getDeclaredMethod("testWithChildNoSplitting", List.class, int.class, int.class, int.class, List.class))));

    public static final Criterion H2_2_3 = createCriterion("Die Methode [[[findInsertionPosition]]] funktioniert korrekt, wenn Intervalle bei vollen und nicht vollen Blattknoten aufgeteilt werden müssen .",
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> FindInsertionPositionTests.class.getDeclaredMethod("testWithKeySplitting", List.class, int.class, int.class, int.class, List.class, List.class)),
            JUnitTestRef.ofMethod(() -> FindInsertionPositionTestsPublic.class.getDeclaredMethod("testWithKeySplitting", List.class, int.class, int.class, int.class, List.class, List.class))),
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> FindInsertionPositionTests.class.getDeclaredMethod("testWithLeafSplitting", List.class, int.class, int.class, int.class, List.class, List.class)),
            JUnitTestRef.ofMethod(() -> FindInsertionPositionTestsPublic.class.getDeclaredMethod("testWithLeafSplitting", List.class, int.class, int.class, int.class, List.class, List.class))));

    public static final Criterion H2_2 = createParentCriterion("2 b)", "findInsertionPosition", H2_2_1, H2_2_2, H2_2_3);

    public static final Criterion H2_3_1 = createCriterion("Die Methode [[[insert]]] funktioniert korrekt, wenn alle Intervalle in den momentanen Knoten passen.", 5,
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> InsertTests.class.getDeclaredMethod("testEnoughSpace", List.class, int.class, List.class, int.class, List.class)),
            JUnitTestRef.ofMethod(() -> InsertTestsPublic.class.getDeclaredMethod("testEnoughSpace", List.class, int.class, List.class, int.class, List.class))));

    public static final Criterion H2_3_2 = createCriterion("Die Methode [[[insert]]] funktioniert korrekt, wenn nicht alle Intervalle in den momentanen Knoten passen.",
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> InsertTests.class.getDeclaredMethod("testNotEnoughSpaceSameNode", List.class, int.class, List.class, int.class, List.class)),
            JUnitTestRef.ofMethod(() -> InsertTestsPublic.class.getDeclaredMethod("testNotEnoughSpaceSameNode", List.class, int.class, List.class, int.class, List.class))),
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> InsertTests.class.getDeclaredMethod("testNotEnoughSpaceNewNode", List.class, int.class, List.class, int.class, List.class)),
            JUnitTestRef.ofMethod(() -> InsertTestsPublic.class.getDeclaredMethod("testNotEnoughSpaceNewNode", List.class, int.class, List.class, int.class, List.class))));

    public static final Criterion H2_3 = createParentCriterion("2 c)", "insert", H2_3_1, H2_3_2);

    public static final Criterion H2 = createParentCriterion("2", "Einfügen", H2_1, H2_2, H2_3);

    public static final Criterion H3_1_1 = createCriterion("Nach dem Aufrufen der Methode [[[rotateFromRightSibling]]] ist der Knoten, in welchen hineinrotiert wird, korrekt und an der richtigen Position.", 7,
        () -> RotateTests.class.getDeclaredMethod("testRotateRightOriginalNode", List.class, int.class, int.class, int.class, List.class));
    public static final Criterion H3_1_3 = createCriterion("Nach dem Aufrufen der Methode [[[rotateFromRightSibling]]] ist der Elternknoten und der rechte Knoten korrekt und die Methode [[[rotateFromLeftSibling]]] ist vollständig korrekt.",
        () -> RotateTests.class.getDeclaredMethod("testRotateRightRightNode", List.class, int.class, int.class, int.class, List.class),
        () -> RotateTests.class.getDeclaredMethod("testRotateRightParentNode", List.class, int.class, int.class, int.class, List.class),
        () -> RotateTests.class.getDeclaredMethod("testRotateLeftParentNode", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_1 = createParentCriterion("3 a)", "Rotieren", H3_1_1, H3_1_3);

    public static final Criterion H3_2_1 = createCriterion("Nach dem Aufrufen der Methode [[[mergeWithRightSibling]]] ist der Kindknoten korrekt und an der korrekten Position.", 5,
        () -> MergeTests.class.getDeclaredMethod("testMergeRightOriginalNode", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_2_2 = createCriterion("Nach dem Aufrufen der Methode [[[mergeWithRightSibling]]] ist der Elternknoten korrekt und die Methode [[[rotateFromLeftSibling]]] ist vollständig korrekt.",
        () -> MergeTests.class.getDeclaredMethod("testMergeRightParentNode", List.class, int.class, int.class, int.class, List.class),
        () -> MergeTests.class.getDeclaredMethod("testMergeLeftParentNode", List.class, int.class, int.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_2 = createParentCriterion("3 b)", "Mergen", H3_2_1, H3_2_2);

    public static final Criterion H3_3_1 = createCriterion("Die Methode [[[ensureSize]]] funktioniert korrekt, wenn rotateFromRightSibling aufgerufen werden muss.", 2,
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> EnsureSizeTests.class.getDeclaredMethod("testRotateRight", List.class, int.class, int.class, int.class, int.class, int.class, List.class)),
            JUnitTestRef.ofMethod(() -> EnsureSizeTestsPublic.class.getDeclaredMethod("testRotateRight", List.class, int.class, int.class, int.class, int.class, int.class, List.class))));

    public static final Criterion H3_3_2 = createCriterion("Die Methode [[[ensureSize]]] funktioniert korrekt, wenn rotateFromLeftSibling aufgerufen werden muss, oder beide aufgerufen werden können.", 2,
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> EnsureSizeTests.class.getDeclaredMethod("testRotateLeft", List.class, int.class, int.class, int.class, int.class, int.class, List.class)),
            JUnitTestRef.ofMethod(() -> EnsureSizeTestsPublic.class.getDeclaredMethod("testRotateLeft", List.class, int.class, int.class, int.class, int.class, int.class, List.class))),
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> EnsureSizeTests.class.getDeclaredMethod("testRotateBoth", List.class, int.class, int.class, int.class, int.class, int.class, int.class, List.class, List.class)),
            JUnitTestRef.ofMethod(() -> EnsureSizeTestsPublic.class.getDeclaredMethod("testRotateBoth", List.class, int.class, int.class, int.class, int.class, int.class, int.class, List.class, List.class))));

    public static final Criterion H3_3_3 = createCriterion("Die Methode [[[ensureSize]]] funktioniert korrekt, wenn mergeWithRightSibling aufgerufen werden muss.", 2,
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> EnsureSizeTests.class.getDeclaredMethod("testMergeRight", List.class, int.class, int.class, int.class, int.class, int.class, List.class)),
            JUnitTestRef.ofMethod(() -> EnsureSizeTestsPublic.class.getDeclaredMethod("testMergeRight", List.class, int.class, int.class, int.class, int.class, int.class, List.class))));

    public static final Criterion H3_3_4 = createCriterion("Die Methode [[[ensureSize]]] funktioniert korrekt, wenn mergeWithLeftSibling aufgerufen werden kann, beide Methoden aufgerufen werden können, oder gar keine Methode aufgerufen werden muss.", 2,
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> EnsureSizeTests.class.getDeclaredMethod("testMergeLeft", List.class, int.class, int.class, int.class, int.class, int.class, List.class)),
            JUnitTestRef.ofMethod(() -> EnsureSizeTestsPublic.class.getDeclaredMethod("testMergeLeft", List.class, int.class, int.class, int.class, int.class, int.class, List.class))),
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> EnsureSizeTests.class.getDeclaredMethod("testMergeBoth", List.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, List.class, List.class)),
            JUnitTestRef.ofMethod(() -> EnsureSizeTestsPublic.class.getDeclaredMethod("testMergeBoth", List.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, List.class, List.class))),
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> EnsureSizeTests.class.getDeclaredMethod("testNoChange", List.class, int.class, int.class, int.class)),
            JUnitTestRef.ofMethod(() -> EnsureSizeTestsPublic.class.getDeclaredMethod("testNoChange", List.class, int.class, int.class, int.class))));

    public static final Criterion H3_3 = createParentCriterion("3 c)", "ensureSize", H3_3_1, H3_3_2, H3_3_3, H3_3_4);

    public static final Criterion H3_4_1 = createCriterion("Die Methode [[[removeRightMostKey]]] funktioniert korrekt, wenn der Knoten ein Blattknoten ist und mehr als die Mindestanzahl an Schlüsselwerten besitzt.", 7,
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> RemoveLRMostKeyTests.class.getDeclaredMethod("testRemoveRightLeaf", List.class, int.class, String.class, List.class)),
            JUnitTestRef.ofMethod(() -> RemoveLRMostKeyTestsPublic.class.getDeclaredMethod("testRemoveRightLeaf", List.class, int.class, String.class, List.class))));
    public static final Criterion H3_4_3 = createCriterion("Die Methoden [[[removeRightMostKey]]] und [[[removeLeftMostKey]]] funktionieren korrekt.",
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> RemoveLRMostKeyTests.class.getDeclaredMethod("testRemoveRightNoCorrection", List.class, int.class, String.class, List.class)),
            JUnitTestRef.ofMethod(() -> RemoveLRMostKeyTestsPublic.class.getDeclaredMethod("testRemoveRightNoCorrection", List.class, int.class, String.class, List.class))),
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> RemoveLRMostKeyTests.class.getDeclaredMethod("testRemoveRightWithCorrection", List.class, int.class, String.class, List.class)),
            JUnitTestRef.ofMethod(() -> RemoveLRMostKeyTestsPublic.class.getDeclaredMethod("testRemoveRightWithCorrection", List.class, int.class, String.class, List.class))),
        JUnitTestRef.or(
            JUnitTestRef.ofMethod(() -> RemoveLRMostKeyTests.class.getDeclaredMethod("testRemoveLeft", List.class, int.class, String.class, List.class)),
            JUnitTestRef.ofMethod(() -> RemoveLRMostKeyTestsPublic.class.getDeclaredMethod("testRemoveLeft", List.class, int.class, String.class, List.class))));

    public static final Criterion H3_4 = createParentCriterion("3 d)", "removeRightMostKey und removeLeftMostKey", H3_4_1, H3_4_3);

    public static final Criterion H3 = createParentCriterion("3", "Löschen", H3_1, H3_2, H3_3, H3_4);

    public static final Rubric RUBRIC = Rubric.builder()
        .title("P2")
        .addChildCriteria(H1, H2, H3)
        .build();

    @Override
    public Rubric getRubric() {
        return RUBRIC;
    }

    @Override
    public void configure(RubricConfiguration configuration) {
        configuration.addTransformer(new AccessTransformer());
    }
}
