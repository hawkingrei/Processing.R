package rprocessing;

import javax.script.ScriptException;

import org.renjin.eval.EvalException;

import rprocessing.exception.NotFoundException;
import rprocessing.exception.REvalException;
import rprocessing.lancher.StandaloneSketch;
import rprocessing.util.Printer;
import rprocessing.util.StreamPrinter;

/**
 * R script runner
 * 
 * @author github.com/gaocegege
 */
public class Runner {

  public static RunnableSketch sketch;

  private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("VERBOSE_RLANG_MODE"));

  private static void log(final Object... objs) {
    if (!VERBOSE) {
      return;
    }
    for (final Object o : objs) {
      System.err.print(Runner.class.getSimpleName() + ": " + String.valueOf(o));
    }
    System.err.println();
  }

  public static void main(final String[] args) throws Exception {
    if (args.length < 1) {
      throw new NotFoundException("The path of your R script is needed as an argument.");
    }
    try {
      sketch = new StandaloneSketch(args);
      runSketchBlocking(sketch, new StreamPrinter(System.out), new StreamPrinter(System.err));

      // See https://github.com/gaocegege/Processing.R/issues/89
      // It can't be reproduced, so comment the statement.
      // System.exit(0);
    } catch (final Throwable t) {
      System.err.println(t);
      System.exit(-1);
    }
  }

  public static synchronized void runSketchBlocking(final RunnableSketch sketch,
      final Printer stdout, final Printer stderr)
      throws REvalException, NotFoundException, ScriptException {
    runSketchBlocking(sketch, stdout, stderr, null);
  }

  public static synchronized void runSketchBlocking(final RunnableSketch sketch,
      final Printer stdout, final Printer stderr,
      final SketchPositionListener sketchPositionListener)
      throws REvalException, NotFoundException, ScriptException {
    final String[] args = sketch.getPAppletArguments();

    log("Tring to initialize RLangPApplet.");
    RLangPApplet rp = new RLangPApplet(sketch.getMainCode(), stdout);
    log("Adding processing variable into R top context.");
    rp.addPAppletToRContext();
    rp.evaluateCoreCode();

    try {
      // Run Sketch.
      rp.runBlock(args);
      log("Down");
    } catch (EvalException ee) {
      throw new REvalException(ee.getMessage());
    }
  }
}
