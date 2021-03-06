
// Generated by hack/generate-e2e-test.py
package test.e2e.core.function;

import static org.junit.Assert.fail;
import org.junit.Test;
import test.e2e.core.E2eTestBase;

public class Arc4Test extends E2eTestBase {

  public Arc4Test() {
    coreCodeTemplate = "# arc 4 https://processing.org/reference/arc_.html\n\n# TODO: replace with built-in constants\nPI <- 3.1415\nHALF_PI <- PI/2\nQUARTER_PI <- PI/4\nTWO_PI <- PI * 2\n\n# TODO: Fix double to int casts\nOPEN <- as.integer(1)\nCHORD <- as.integer(2)\nPIE <- as.integer(3)\n\narc(50, 50, 80, 80, 0, PI + QUARTER_PI, PIE)\n\nsaveFrame(\"%s\")\nexit()\n";
    referenceURI = "https://processing.org/reference/images/arc_4.png";
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
