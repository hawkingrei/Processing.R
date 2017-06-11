package rprocessing;

import java.awt.Component;
import java.awt.Window;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.renjin.eval.EvalException;
import org.renjin.parser.RParser;
import org.renjin.script.RenjinScriptEngine;
import org.renjin.sexp.Closure;
import org.renjin.sexp.ExpressionVector;
import org.renjin.sexp.FunctionCall;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.Symbol;

import com.jogamp.newt.opengl.GLWindow;

import processing.awt.PSurfaceAWT;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PSurface;
import processing.javafx.PSurfaceFX;
import processing.opengl.PSurfaceJOGL;
import rprocessing.applet.BuiltinApplet;
import rprocessing.exception.NotFoundException;
import rprocessing.exception.RSketchError;
import rprocessing.util.Constant;
import rprocessing.util.Printer;
import rprocessing.util.RScriptReader;

/**
 * RlangPApplet PApplet for R language, powered by Renjin.
 * 
 * @author github.com/gaocegege
 */
public class RLangPApplet extends BuiltinApplet {

  private static final boolean VERBOSE = Boolean.parseBoolean(System.getenv("VERBOSE_RLANG_MODE"));

  // A static-mode sketch must be interpreted from within the setup() method.
  // All others are interpreted during construction in order to harvest method
  // definitions, which we then invoke during the run loop.
  private final Mode mode;

  /** Program code */
  private final String programText;

  /** Engine to interpret R code */
  private final RenjinScriptEngine renjinEngine;

  private static final String CORE_TEXT =
      RScriptReader.readResourceAsText(Runner.class, "r/core.R");

  private final Printer stdout;

  private final CountDownLatch finishedLatch = new CountDownLatch(1);

  private RSketchError terminalException = null;

  /**
   * Mode for Processing.
   * 
   * @author github.com/gaocegege
   */
  private enum Mode {
    STATIC, ACTIVE, MIXED
  }

  private static void log(String msg) {
    if (!VERBOSE) {
      return;
    }
    System.err.println(RLangPApplet.class.getSimpleName() + ": " + msg);
  }

  public RLangPApplet(final String programText, final Printer stdout) throws NotFoundException {
    // Create a script engine manager.
    ScriptEngineManager manager = new ScriptEngineManager();
    // Create a Renjin engine.
    ScriptEngine engine = manager.getEngineByName("Renjin");
    // Check if the engine has loaded correctly.
    if (engine == null) {
      throw new NotFoundException("Renjin Script Engine not found on the classpath.");
    }
    this.renjinEngine = (RenjinScriptEngine) engine;
    this.programText = programText;
    this.stdout = stdout;
    this.prePassCode();
    // Detect the mode after pre-pass program code.
    this.mode = this.detectMode();
  }

  public void evaluateCoreCode() throws ScriptException {
    this.renjinEngine.eval(CORE_TEXT);
  }

  /**
   * Evaluate all the function calls.
   */
  public void prePassCode() {
    SEXP source = RParser.parseSource(this.programText + "\n", "inline-string");
    if (isSameClass(source, ExpressionVector.class)) {
      ExpressionVector ev = (ExpressionVector) source;
      for (int i = 0; i < ev.length(); ++i) {
        /**
         * There is a bug, see https://github.com/gaocegege/Processing.R/issues/7 For example,
         * processing$line() is also a function call in renjin engine. To solve this problem, add a
         * hack to make sure the function is "<-".
         */
        if (isSameClass(ev.get(i), FunctionCall.class)
            && isSameClass(((FunctionCall) ev.get(i)).getFunction(), Symbol.class)
            && ((Symbol) ((FunctionCall) ev.get(i)).getFunction()).getPrintName().equals("<-")) {
          this.renjinEngine.getTopLevelContext().evaluate(ev.get(i),
              this.renjinEngine.getTopLevelContext().getEnvironment());
        }
      }
    }
  }

  /**
   * Detect the mode. After: prePassCode()
   */
  private Mode detectMode() {
    if (isActiveMode()) {
      if (isMixMode()) {
        return Mode.MIXED;
      }
      return Mode.ACTIVE;
    }
    return Mode.STATIC;
  }

  /**
   * Add PApplet instance to R top context Notice: DO NOT do it in constructor.
   */
  public void addPAppletToRContext() {
    this.renjinEngine.put(Constant.PROCESSING_VAR_NAME, this);
    // This is a trick to be deprecated. It is used to print
    // messages in Processing app console by stdout$print(msg).
    this.renjinEngine.put("stdout", stdout);
  }

  public void runBlock(final String[] arguments) throws RSketchError {
    log("runBlock");
    PApplet.runSketch(arguments, this);
    try {
      finishedLatch.await();
      log("Down");
    } catch (final InterruptedException interrupted) {
      // Treat an interruption as a request to the applet to terminate.
      exit();
      try {
        finishedLatch.await();
        log("Down");
      } catch (final InterruptedException exception) {
        log(exception.toString());
      }
    } finally {
      Thread.setDefaultUncaughtExceptionHandler(null);
      if (PApplet.platform == PConstants.MACOSX
          && Arrays.asList(arguments).contains("fullScreen")) {
        // Frame should be OS-X fullscreen, and it won't stop being that unless the jvm
        // exits or we explicitly tell it to minimize.
        // (If it's disposed, it'll leave a gray blank window behind it.)
        log("Disabling fullscreen.");
        macosxFullScreenToggle(frame);
      }
      if (surface instanceof PSurfaceFX) {
        // Sadly, JavaFX is an abomination, and there's no way to run an FX sketch more than once,
        // so we must actually exit.
        log("JavaFX requires SketchRunner to terminate. Farewell!");
        System.exit(0);
      }
      final Object nativeWindow = surface.getNative();
      if (nativeWindow instanceof com.jogamp.newt.Window) {
        ((com.jogamp.newt.Window) nativeWindow).destroy();
      } else {
        surface.setVisible(false);
      }
    }
    if (terminalException != null) {
      throw terminalException;
    }
  }

  private static void macosxFullScreenToggle(final Window window) {
    try {
      final Class<?> appClass = Class.forName("com.apple.eawt.Application");
      final Method getAppMethod = appClass.getMethod("getApplication");
      final Object app = getAppMethod.invoke(null);
      final Method requestMethod = appClass.getMethod("requestToggleFullScreen", Window.class);
      requestMethod.invoke(app, window);
    } catch (final ClassNotFoundException cnfe) {
      // ignored
    } catch (final Exception exception) {
      exception.printStackTrace();
    }
  }

  /**
   * 
   * @see processing.core.PApplet#initSurface()
   */
  @Override
  protected PSurface initSurface() {
    final PSurface s = super.initSurface();
    this.frame = null; // eliminate a memory leak from 2.x compat hack
    // s.setTitle(pySketchPath.getFileName().toString().replaceAll("\\..*$", ""));
    if (s instanceof PSurfaceAWT) {
      final PSurfaceAWT surf = (PSurfaceAWT) s;
      final Component c = (Component) surf.getNative();
      c.addComponentListener(new ComponentAdapter() {
        @Override
        public void componentHidden(final ComponentEvent e) {
          log("initSurface");
          finishedLatch.countDown();
        }
      });
    } else if (s instanceof PSurfaceJOGL) {
      final PSurfaceJOGL surf = (PSurfaceJOGL) s;
      final GLWindow win = (GLWindow) surf.getNative();
      win.addWindowListener(new com.jogamp.newt.event.WindowAdapter() {
        @Override
        public void windowDestroyed(final com.jogamp.newt.event.WindowEvent arg0) {
          log("initSurface");
          finishedLatch.countDown();
        }
      });
    } else if (s instanceof PSurfaceFX) {
      System.err.println("I don't know how to watch FX2D windows for close.");
    }
    return s;
  }

  @Override
  public void exitActual() {
    log("exitActual");
    finishedLatch.countDown();
  }

  /**
   * @see processing.core.PApplet#start()
   */
  @Override
  public void start() {
    // I want to quit on runtime exceptions.
    // Processing just sits there by default.
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(final Thread t, final Throwable e) {
        terminalException = toSketchException(e);
        try {
          log("There is an unexpected exception.");
          handleMethods("dispose");
        } catch (final Exception noop) {
          // give up
        }
        finishedLatch.countDown();
      }
    });
    super.start();
  }

  /**
   * @see processing.core.PApplet#settings()
   */
  @Override
  public void settings() {
    Object obj = this.renjinEngine.get(Constant.SETTINGS_NAME);
    if (obj.getClass().equals(Closure.class)) {
      ((Closure) obj).doApply(this.renjinEngine.getTopLevelContext());
    } else if (mode == Mode.STATIC) {
      // TODO: Implement Static Mode.
      // Set size and something else.
    }
  }

  /**
   * Evaluate the program code.
   * 
   * @see processing.core.PApplet#setup()
   */
  @Override
  public void setup() {
    // I don't know why I put it there. Now I think it should be in constructor.
    // But I ...
    wrapProcessingVariables();
    if (this.mode == Mode.STATIC) {
      try {
        log("The mode is static, run the program directly.");
        this.renjinEngine.eval(this.programText);
        log("Evaluate the code in static mode.");
      } catch (final Exception exception) {
        log("There is exception when evaluate the code in static mode.");
        terminalException = toSketchException(exception);
        exitActual();
      }
    } else if (this.mode == Mode.ACTIVE) {
      Object obj = this.renjinEngine.get(Constant.SETUP_NAME);
      if (obj.getClass().equals(Closure.class)) {
        ((Closure) obj).doApply(this.renjinEngine.getTopLevelContext());
      }
    } else {
      System.out.println("The program is in mix mode now.");
    }
    log("Setup done");
  }

  /**
   * Call the draw function in R script.
   * 
   * @see processing.core.PApplet#draw()
   */
  @Override
  public void draw() {
    Object obj = this.renjinEngine.get(Constant.DRAW_NAME);
    if (obj.getClass().equals(Closure.class)) {
      ((Closure) obj).doApply(this.renjinEngine.getTopLevelContext());
    }
  }

  /*
   * Helper functions
   */

  /**
   * Detect whether the program is in active mode.
   * 
   * @return
   */
  @SuppressWarnings("rawtypes")
  private boolean isActiveMode() {
    Class closureClass = Closure.class;
    return isSameClass(this.renjinEngine.get(Constant.SETTINGS_NAME), closureClass)
        || isSameClass(this.renjinEngine.get(Constant.SETUP_NAME), closureClass)
        || isSameClass(this.renjinEngine.get(Constant.DRAW_NAME), closureClass);
  }

  /**
   * Detect whether the program is in mix mode. After: isActiveMode()
   * 
   * @return
   */
  @SuppressWarnings("rawtypes")
  private boolean isMixMode() {
    Class closureClass = Closure.class;
    return isSameClass(this.renjinEngine.get(Constant.SIZE_NAME), closureClass);
  }

  /**
   * Set Environment variables in R top context.
   */
  protected void wrapProcessingVariables() {
    log("Wrap Processing built-in variables into R top context.");
    // TODO: Find some ways to push constants into R.
    this.renjinEngine.put("width", width);
    this.renjinEngine.put("height", height);
    this.renjinEngine.put("displayWidth", displayWidth);
    this.renjinEngine.put("displayHeight", displayHeight);
    // this.renjinEngine.put("focused", focused);
    // this.renjinEngine.put("keyPressed", keyPressed);
  }

  /**
   * Return whether the object has same class with clazz.
   * 
   * @param obj
   * @param clazz
   * @return
   */
  @SuppressWarnings("rawtypes")
  private static boolean isSameClass(Object obj, Class clazz) {
    return obj.getClass().equals(clazz);
  }

  private static RSketchError toSketchException(Throwable t) {
    if (t instanceof RuntimeException && t.getCause() != null) {
      t = t.getCause();
    }
    if (t instanceof RSketchError) {
      return (RSketchError) t;
    }
    if (t instanceof EvalException) {
      final RSketchError e = (RSketchError) t;
      return e;
    }
    if (t instanceof ClassCastException) {
      final RSketchError e = (RSketchError) t;
      return e;
    }
    log("No exception type detected.");
    return null;
  }
}
