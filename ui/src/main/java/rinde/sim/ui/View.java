/**
 * 
 */
package rinde.sim.ui;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import rinde.sim.core.Simulator;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.ui.renderers.Renderer;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public final class View {

  /**
   * Creates a {@link View.Builder}.
   * @param simulator The {@link Simulator} to create the view for.
   * @return The {@link View.Builder}.
   */
  public static Builder create(Simulator simulator) {
    return new Builder(simulator);
  }

  public static class Builder {
    Simulator simulator;
    boolean autoPlay;
    boolean autoClose;
    boolean allowResize;
    boolean fullScreen;
    int speedUp;
    long sleep;
    String title;
    Point screenSize;
    final List<Renderer> rendererList;

    Builder(Simulator s) {
      simulator = s;
      autoPlay = false;
      autoClose = false;
      allowResize = true;
      fullScreen = false;
      title = "Simulator";
      speedUp = 1;
      screenSize = new Point(800, 600);
      rendererList = newArrayList();
    }

    public Builder with(Renderer... renderers) {
      rendererList.addAll(asList(renderers));
      return this;
    }

    public Builder enableAutoPlay() {
      autoPlay = true;
      return this;
    }

    public Builder enableAutoClose() {
      autoClose = true;
      return this;
    }

    public Builder setSpeedUp(int speed) {
      speedUp = speed;
      return this;
    }

    public Builder setSleep(long ms) {
      sleep = ms;
      return this;
    }

    public Builder setTitleAppendix(String titleAppendix) {
      title = titleAppendix;
      return this;
    }

    public Builder disallowResizing() {
      allowResize = false;
      return this;
    }

    /**
     * This takes precedence over any calls to {@link #setResolution(int, int)}.
     * @return This, as per the builder pattern.
     */
    public Builder setFullScreen() {
      fullScreen = true;
      return this;
    }

    public Builder setResolution(int width, int height) {
      checkArgument(width > 0 && height > 0,
          "Only positive dimensions are allowed, input: %s x %s.", width,
          height);
      screenSize = new Point(width, height);
      return this;
    }

    public void show() {
      checkState(
          simulator.isConfigured(),
          "Simulator needs to be configured before it can be visualized, see Simulator.configure()");
      Display.setAppName("RinSim");
      final Display display = new Display();

      int shellArgs = SWT.TITLE | SWT.CLOSE;
      if (allowResize) {
        shellArgs = shellArgs | SWT.RESIZE;
      }
      final Shell shell = new Shell(display, shellArgs);
      shell.setText("RinSim - " + title);
      if (fullScreen) {
        shell.setFullScreen(true);
        shell.setMaximized(true);
      } else {
        shell.setSize(screenSize);
      }

      if (autoClose) {
        simulator.getEventAPI().addListener(new Listener() {
          @Override
          public void handleEvent(final Event arg0) {
            if (display.isDisposed()) {
              return;
            }
            display.asyncExec(new Runnable() {
              @Override
              public void run() {
                shell.close();
              }
            });
          }
        }, Simulator.SimulatorEventType.STOPPED);
      }

      // simulator viewer is run in here
      new SimulationViewer(shell, simulator, speedUp, autoPlay, rendererList);
      shell.open();
      while (!shell.isDisposed()) {
        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }
      if (shell.isDisposed()) {
        simulator.stop();
      }
    }
  }
}
