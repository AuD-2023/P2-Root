package p2;

import org.junit.jupiter.params.ParameterizedTest;
import org.junitpioneer.jupiter.json.JsonClasspathSource;
import org.junitpioneer.jupiter.json.Property;

import java.util.List;

public class ReadTests {

    @ParameterizedTest
    @JsonClasspathSource(value = "ReadTests.json", data = "readNoChild")
    public void testNoChild(@Property("tree") List<Object> tree, @Property("degree") int degree) {




        System.out.println("test");
    }
}
