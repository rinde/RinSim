/**
 * 
 */
package rinde.sim.ui;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;

import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
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
    boolean async;
    int speedUp;
    long sleep;
    @Nullable
    Display display;
    String title;
    Point screenSize;
    @Nullable
    Monitor monitor;
    final List<Renderer> rendererList;

    Listener callback;

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

    public Builder setCallback(Listener l) {
      callback = l;
      return this;
    }

    public Builder setSleep(long ms) {
      sleep = ms;
      return this;
    }

    public Builder setDisplay(Display d) {
      display = d;
      return this;
    }

    public Builder setAsync() {
      async = true;
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

    /**
     * @param monitor
     */
    public Builder displayOnMonitor(Monitor m) {
      monitor = m;
      return this;
    }

    public void show() {
      checkState(
          simulator.isConfigured(),
          "Simulator needs to be configured before it can be visualized, see Simulator.configure()");

      // if( )
      Display.setAppName("RinSim");
      final Display d = display != null ? display : Display.getCurrent();
      final boolean isDisplayOwner = d == null;
      final Display disp = isDisplayOwner ? new Display() : Display
          .getCurrent();

      int shellArgs = SWT.TITLE | SWT.CLOSE;
      if (allowResize) {
        shellArgs = shellArgs | SWT.RESIZE;
      }
      final Shell shell = new Shell(disp, shellArgs);
      if (monitor != null) {
        shell.setLocation(monitor.getBounds().x, monitor.getBounds().y);
      }

      shell.setText("RinSim - " + title);
      if (fullScreen) {
        shell.setFullScreen(true);
        shell.setMaximized(true);
      } else {
        shell.setSize(screenSize);
      }

      if (autoClose) {

        final Listener list = callback;

        simulator.getEventAPI().addListener(new Listener() {
          @Override
          public void handleEvent(final Event arg0) {
            if (list != null) {
              list.handleEvent(arg0);
            }

            if (shell.isDisposed()) {
              return;
            }
            disp.asyncExec(new Runnable() {
              @Override
              public void run() {
                shell.close();
              }
            });
          }
        }, Simulator.SimulatorEventType.STOPPED);
      }

      shell.addListener(SWT.Close, new org.eclipse.swt.widgets.Listener() {
        @SuppressWarnings("synthetic-access")
        @Override
        public void handleEvent(@Nullable org.eclipse.swt.widgets.Event event) {
          simulator.stop();
          while (simulator.isPlaying()) {
            // wait until simulator acutally stops (it finishes its
            // current tick first).
          }
          if (isDisplayOwner && !disp.isDisposed()) {
            disp.dispose();
          } else if (!isDisplayOwner && !shell.isDisposed()) {
            shell.dispose();
          }
        }
      });

      // simulator viewer is run in here
      new SimulationViewer(shell, simulator, speedUp, autoPlay, rendererList);
      shell.open();
      if (!async) {
        while (!shell.isDisposed()) {
          if (!disp.readAndDispatch()) {
            disp.sleep();
          }
        }
        if (shell.isDisposed()) {
          simulator.stop();
        }
      }
    }
  }

}
