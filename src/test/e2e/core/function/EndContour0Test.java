
// Generated by hack/generate-e2e-test.py
package test.e2e.core.function;

import static org.junit.Assert.fail;
import org.junit.Test;
import test.e2e.core.E2eTestBase;

public class EndContour0Test extends E2eTestBase {

  public EndContour0Test() {
    coreCodeTemplate = "CLOSE <- as.integer(2)\n\nsize(100, 100)\ntranslate(50, 50)\nstroke(255, 0, 0)\nbeginShape()\n# Exterior part of shape, clockwise winding\nvertex(-40, -40)\nvertex(40, -40)\nvertex(40, 40)\nvertex(-40, 40)\n# Interior part of shape, counter-clockwise winding\nbeginContour()\nvertex(-20, -20)\nvertex(-20, 20)\nvertex(20, 20)\nvertex(20, -20)\nendContour()\nendShape(CLOSE)\n\nsaveFrame(\"%s\")\nexit()\n";
    referenceURI = "https://processing.org/reference/images/beginContour_0.png";
  }

  @Test
  public void test() {
    try {
      defaultOperation();
    } catch (Exception exception) {
      System.err.println(exception);
      fail("Should not have thrown any exception");
    }
  }
}
