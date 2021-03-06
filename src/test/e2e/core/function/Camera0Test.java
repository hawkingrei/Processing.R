
// Generated by hack/generate-e2e-test.py
package test.e2e.core.function;

import static org.junit.Assert.fail;
import org.junit.Test;
import test.e2e.core.E2eTestBase;

public class Camera0Test extends E2eTestBase {

  public Camera0Test() {
    coreCodeTemplate = "P3D <- \"processing.opengl.PGraphics3D\"\nPI <- pi\n\nsettings <- function() {\n    size(100, 100, P3D)\n}\n\ndraw <- function() {\n    size(100, 100, P3D)\n    noFill()\n    background(204)\n    camera(70, 35, 120, 50, 50, 0, 0, 1, 0)\n    translate(50, 50, 0)\n    rotateX(-PI/6)\n    rotateY(PI/3)\n    box(45)\n    saveFrame(\"%s\")\n    exit()\n}\n";
    referenceURI = "https://processing.org/reference/images/camera_.png";
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
