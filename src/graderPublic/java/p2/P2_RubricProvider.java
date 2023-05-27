package p2;

import org.sourcegrade.jagr.api.rubric.Criterion;
import org.sourcegrade.jagr.api.rubric.GradeResult;
import org.sourcegrade.jagr.api.rubric.Grader;
import org.sourcegrade.jagr.api.rubric.JUnitTestRef;
import org.sourcegrade.jagr.api.rubric.Rubric;
import org.sourcegrade.jagr.api.rubric.RubricProvider;

import java.lang.reflect.Method;
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
    private static Criterion createCriterion(String shortDescription, Callable<Method>... methodReferences) {

        if (methodReferences.length == 0) {
            return Criterion.builder()
                .shortDescription(shortDescription)
                .maxPoints(1)
                .build();
        }

        Grader.TestAwareBuilder graderBuilder = Grader.testAwareBuilder();

        for (Callable<Method> reference : methodReferences) {
            graderBuilder.requirePass(JUnitTestRef.ofMethod(reference));
        }

        return Criterion.builder()
            .shortDescription(shortDescription)
            .grader(graderBuilder
                .pointsFailedMin()
                .pointsPassedMax()
                .build())
            .maxPoints(1)
            .build();
    }

    private static Criterion createParentCriterion(String task, String shortDescription, Criterion... children) {
        return Criterion.builder()
            .shortDescription("H" + task + " | " + shortDescription)
            .addChildCriteria(children)
            .build();
    }

    public static final Criterion H1_1 = createUntestedCriterion("Die Methode [[[read]]] funktioniert korrekt, wenn der Wurzelknoten keine Kinder hat.");

    public static final Criterion H1_2 = createUntestedCriterion("Die Methode [[[read]]] funktioniert korrekt, wenn nur alle Intervalle komplett gelesen werden.");

    public static final Criterion H1_3 = createUntestedCriterion("Die Methode [[[read]]] funktioniert korrekt, wenn das Startintervall nur zum Teil gelesen wird.");

    public static final Criterion H1_4 = createUntestedCriterion("Die Methode [[[read]]] funktioniert korrekt, wenn das Start- und Endintervall nur zum Teil gelesen wird.");

    public static final Criterion H1 = createParentCriterion("1", "Lesen", H1_1, H1_2, H1_3, H1_4);

    public static final Criterion H2_1_1 = createCriterion("Die Methode [[[split]]] splittet den Knoten in normalen Fällen korrekt.",
        () -> SplitTests.class.getDeclaredMethod("testSimpleSplit", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H2_1_2 = createCriterion("Die Methode [[[split]]] aktualisiert die Werte in der übergebenen IndexedNodeLinkedList korrekt.",
        () -> SplitTests.class.getDeclaredMethod("testIndexedNodeLinkedList", List.class, int.class, int.class, int.class, int.class, int.class));

    public static final Criterion H2_1_3 = createCriterion("Die Methode [[[split]]] funktioniert korrekt, wenn der Wurzelknoten gesplittet wird.",
        () -> SplitTests.class.getDeclaredMethod("testSplitRoot", List.class, int.class, int.class, int.class, int.class, List.class));

    public static final Criterion H2_1_4 = createCriterion("Die Methode [[[split]]] funktioniert zusätzlich korrekt, wenn rekursive mehrere Knoten gesplittet werden müssen.",
        () -> SplitTests.class.getDeclaredMethod("testSimpleSplit", List.class, int.class, int.class, int.class, List.class),
        () -> SplitTests.class.getDeclaredMethod("testIndexedNodeLinkedList", List.class, int.class, int.class, int.class, int.class, int.class),
        () -> SplitTests.class.getDeclaredMethod("testSplitRoot", List.class, int.class, int.class, int.class, int.class, List.class),
        () -> SplitTests.class.getDeclaredMethod("testSplitRecursive", List.class, int.class, int.class, int.class, int.class, int.class, int.class, List.class));

    public static final Criterion H2_1 = createParentCriterion("2 a)", "Split", H2_1_1, H2_1_2, H2_1_3, H2_1_4);

    public static final Criterion H2_2_1 = createCriterion("Die Methode [[[findInsertionPosition]]] funktioniert korrekt, wenn der WurzelKnoten keine Kinder hat und kein Intervall aufgeteilt werden muss.",
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testNoChildNoSplitting", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H2_2_2 = createCriterion("Die Methode [[[findInsertionPosition]]] funktioniert zusätzlich korrekt, wenn der Wurzelknoten Kinder hat und kein Intervall aufgeteilt werden muss.",
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testNoChildNoSplitting", List.class, int.class, int.class, int.class, List.class),
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testWithChildNoSplitting", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H2_2_3 = createCriterion("Die Methode [[[findInsertionPosition]]] funktioniert zusätzlich korrekt, wenn Intervalle aufgeteilt werden müssen, aber der Blattknoten nicht voll ist.",
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testNoChildNoSplitting", List.class, int.class, int.class, int.class, List.class),
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testWithChildNoSplitting", List.class, int.class, int.class, int.class, List.class),
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testWithKeySplitting", List.class, int.class, int.class, int.class, List.class, List.class));

    public static final Criterion H2_2_4 = createCriterion("Die Methode [[[findInsertionPosition]]] funktioniert zusätzlich korrekt, wenn Intervalle aufgeteilt werden müssen und der Blattknoten voll ist.",
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testNoChildNoSplitting", List.class, int.class, int.class, int.class, List.class),
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testWithChildNoSplitting", List.class, int.class, int.class, int.class, List.class),
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testWithKeySplitting", List.class, int.class, int.class, int.class, List.class, List.class),
        () -> FindInsertionPositionTests.class.getDeclaredMethod("testWithLeafSplitting", List.class, int.class, int.class, int.class, List.class, List.class));

    public static final Criterion H2_2 = createParentCriterion("2 b)", "findInsertionPosition", H2_2_1, H2_2_2, H2_2_3, H2_2_4);

    public static final Criterion H2_3_1 = createUntestedCriterion("Die Methode [[[insert]]] funktioniert korrekt, wenn alle Intervalle in den momentanen Knoten passen.");

    public static final Criterion H2_3_2 = createUntestedCriterion("Die Methode [[[insert]]] funktioniert korrekt, wenn nicht alle Intervalle in den momentanen Knoten passen und nach dem Splitten aber weiterhin in den selben Knoten eingefügt wird.");

    public static final Criterion H2_3_3 = createUntestedCriterion("Die Methode [[[insert]]] funktioniert korrekt, wenn nicht alle Intervalle in den momentanen Knoten passen und nach dem Splitten in einen anderen Knoten eingefügt wird.");

    public static final Criterion H2_3 = createParentCriterion("2 c)", "insert", H2_3_1, H2_3_2, H2_3_3);

    public static final Criterion H2 = createParentCriterion("2", "Einfügen", H2_1, H2_2, H2_3);

    public static final Criterion H3_1_1 = createCriterion("Nach dem Aufrufen der Methode [[[rotateFromRightChild]]] ist der Knoten, in welchen hineinrotiert wird, korrekt und an der richtigen Position.",
        () -> RotateTests.class.getDeclaredMethod("testRotateRightOriginalNode", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_1_2 = createCriterion("Nach dem Aufrufen der Methode [[[rotateFromRightChild]]] ist der rechte Knoten korrekt und an der richtigen Position.",
        () -> RotateTests.class.getDeclaredMethod("testRotateRightRightNode", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_1_3 = createCriterion("Nach dem Aufrufen der Methode [[[rotateFromRightChild]]] ist der gesamte Baum korrekt.",
        () -> RotateTests.class.getDeclaredMethod("testRotateRightParentNode", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_1_4 = createCriterion("Nach dem Aufrufen der Methode [[[rotateFromLeftChild]]] ist der gesamte Baum korrekt.",
        () -> RotateTests.class.getDeclaredMethod("testRotateLeftParentNode", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_1 = createParentCriterion("3 a)", "Rotieren", H3_1_1, H3_1_2, H3_1_3, H3_1_4);

    public static final Criterion H3_2_1 = createCriterion("Nach dem Aufrufen der Methode [[[mergeWithRightChild]]] ist der Kindknoten korrekt und an der korrekten Position.",
        () -> MergeTests.class.getDeclaredMethod("testMergeRightOriginalNode", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_2_2 = createCriterion("Nach dem Aufrufen der Methode [[[mergeWithRightChild]]] ist der gesamte Baum korrekt.",
        () -> MergeTests.class.getDeclaredMethod("testMergeRightParentNode", List.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_2_3 = createCriterion("Nach dem Aufrufen der Methode [[[mergeWithLeftChild]]] ist der gesamte Baum korrekt.",
        () -> MergeTests.class.getDeclaredMethod("testMergeLeftParentNode", List.class, int.class, int.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_2 = createParentCriterion("3 b)", "Mergen", H3_2_1, H3_2_2, H3_2_3);

    public static final Criterion H3_3_1 = createCriterion("Die Methode [[[ensureSize]]] funktioniert korrekt, wenn rotateFromRightChild aufgerufen werden muss.",
        () -> EnsureSizeTests.class.getDeclaredMethod("testRotateRight", List.class, int.class, int.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_3_2 = createCriterion("Die Methode [[[ensureSize]]] funktioniert korrekt, wenn rotateFromLeftChild aufgerufen werden muss, oder beide aufgerufen werden können.",
        () -> EnsureSizeTests.class.getDeclaredMethod("testRotateLeft", List.class, int.class, int.class, int.class, int.class, int.class, List.class),
        () -> EnsureSizeTests.class.getDeclaredMethod("testRotateBoth", List.class, int.class, int.class, int.class, int.class, int.class, int.class, List.class, List.class));

    public static final Criterion H3_3_3 = createCriterion("Die Methode [[[ensureSize]]] funktioniert korrekt, wenn mergeWithRightChild aufgerufen werden muss.",
        () -> EnsureSizeTests.class.getDeclaredMethod("testMergeRight", List.class, int.class, int.class, int.class, int.class, int.class, List.class));

    public static final Criterion H3_3_4 = createCriterion("Die Methode [[[ensureSize]]] funktioniert korrekt, wenn mergeWithLeftChild aufgerufen werden kann, beide Methoden aufgerufen werden können, oder gar keine Methode aufgerufen werden muss.",
        () -> EnsureSizeTests.class.getDeclaredMethod("testMergeLeft", List.class, int.class, int.class, int.class, int.class, int.class, List.class),
        () -> EnsureSizeTests.class.getDeclaredMethod("testMergeBoth", List.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class, List.class, List.class),
        () -> EnsureSizeTests.class.getDeclaredMethod("testNoChange", List.class, int.class, int.class, int.class));

    public static final Criterion H3_3 = createParentCriterion("3 c)", "ensureSize", H3_3_1, H3_3_2, H3_3_3, H3_3_4);

    public static final Criterion H3_4_1 = createCriterion("Die Methode [[[removeRightMostKey]]] funktioniert korrekt, wenn der Knoten ein Blattknoten ist und mehr als die Mindestanzahl an Schlüsselwerten besitzt.",
        () -> RemoveLRMostKeyTests.class.getDeclaredMethod("testRemoveRightLeaf", List.class, int.class, String.class, List.class));

    public static final Criterion H3_4_2 = createCriterion("Die Methode [[[removeRightMostKey]]] funktioniert korrekt, wenn der Knoten kein Blattknoten ist und der zugehörige Blattknoten mehr als die Mindestanzahl an Schlüsselwerten besitzt.",
        () -> RemoveLRMostKeyTests.class.getDeclaredMethod("testRemoveRightNoCorrection", List.class, int.class, String.class, List.class));

    public static final Criterion H3_4_3 = createCriterion("Die Methode [[[removeRightMostKey]]] funktioniert korrekt, wenn der Knoten kein Blattknoten ist und der zugehörige Blattknoten die Mindestanzahl an Schlüsselwerten besitzt.",
        () -> RemoveLRMostKeyTests.class.getDeclaredMethod("testRemoveRightWithCorrection", List.class, int.class, String.class, List.class));

    public static final Criterion H3_4_4 = createCriterion("Die Methode [[[removeLeftMostKey]]] funktioniert korrekt.",
        () -> RemoveLRMostKeyTests.class.getDeclaredMethod("testRemoveLeft", List.class, int.class, String.class, List.class));

    public static final Criterion H3_4 = createParentCriterion("3 d)", "removeRightMostKey und removeLeftMostKey", H3_4_1, H3_4_2, H3_4_3, H3_4_4);

    public static final Criterion H3 = createParentCriterion("3", "Löschen", H3_1, H3_2, H3_3, H3_4);

    public static final Rubric RUBRIC = Rubric.builder()
        .title("P2")
        .addChildCriteria(H1, H2, H3)
        .build();

    @Override
    public Rubric getRubric() {
        return RUBRIC;
    }
}
