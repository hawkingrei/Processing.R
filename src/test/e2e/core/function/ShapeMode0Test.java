
// Generated by hack/generate-e2e-test.py
package test.e2e.core.function;

import static org.junit.Assert.fail;
import org.junit.Test;
import test.e2e.core.E2eTestBase;

public class ShapeMode0Test extends E2eTestBase {

  public ShapeMode0Test() {
    coreCodeTemplate = "CENTER <- as.integer(3)\nCORNER <- as.integer(0)\n\nsetup <- function() {\n    size(100, 100)\n    bot = loadShape(\"https://raw.githubusercontent.com/processing/processing-docs/master/content/examples/Basics/Shape/LoadDisplaySVG/data/bot1.svg\")\n}\n\ndraw <- function() {\n    shapeMode(CENTER)\n    shape(bot, 35, 35, 50, 50)\n    shapeMode(CORNER)\n    shape(bot, 35, 35, 50, 50)\n    saveFrame(\"%s\")\n    exit()\n}\n";
    referenceURI = "https://processing.org/reference/images/shapeMode_.png";
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
