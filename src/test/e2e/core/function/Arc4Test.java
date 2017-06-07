
// Generated by hack/generate-e2e-test.py
package test.e2e.core.function;

import static org.junit.Assert.fail;
import org.junit.Test;
import test.e2e.core.E2eTestBase;

public class Arc4Test extends E2eTestBase {

  public Arc4Test() {
    coreCodeTemplate = "PI <- 3.1415\nQUARTER_PI <- PI/4\nOPEN <- 1\nCHORD <- 2\nPIE <- 3\n\narc(50, 50, 80, 80, 0, PI + QUARTER_PI, as.integer(PIE))\n\nsaveFrame(\"%s\")\nexit()\n";
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
