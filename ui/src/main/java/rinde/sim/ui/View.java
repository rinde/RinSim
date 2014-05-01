/**
 * 
 */
package rinde.sim.ui;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static java.util.Arrays.asList;

import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.ui.renderers.Renderer;

/**
 * The view class is the main GUI class. For creating a view, see
 * {@link #create(Simulator)}.
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public final class View {

  private View() {}

  /**
   * Creates a {@link View.Builder} for a specific simulator. The returned
   * builder allows to configure the visualization.
   * @param simulator The {@link Simulator} to create the view for.
   * @return The {@link View.Builder}.
   */
  public static Builder create(Simulator simulator) {
    return new Builder(simulator);
  }

  /**
   * A builder that creates a visualization for {@link Simulator} instances.
   * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
   */
  public static class Builder {
    /**
     * The default window size: 800x600.
     */
    public static final Point DEFAULT_WINDOW_SIZE = new Point(800, 600);

    Simulator simulator;
    boolean autoPlay;
    boolean autoClose;
    boolean allowResize;
    boolean fullScreen;
    boolean async;
    int speedUp;
    long stopTime;
    long sleep;
    @Nullable
    Display display;
    String title;
    Point screenSize;
    @Nullable
    Monitor monitor;
    final List<Renderer> rendererList;
    Map<MenuItems, Integer> accelerators;
    @Nullable
    Listener callback;

    Builder(Simulator s) {
      simulator = s;
      autoPlay = false;
      autoClose = false;
      allowResize = true;
      fullScreen = false;
      title = "Simulator";
      speedUp = 1;
      stopTime = -1;
      screenSize = DEFAULT_WINDOW_SIZE;
      rendererList = newArrayList();
      accelerators = newHashMap();
      accelerators.putAll(MenuItems.QWERTY_ACCELERATORS);
    }

    /**
     * @param renderers The {@link Renderer}s to add to the view.
     * @return This as per the builder pattern.
     */
    public Builder with(Renderer... renderers) {
      rendererList.addAll(asList(renderers));
      return this;
    }

    /**
     * When <i>auto play</i> is enabled the {@link Simulator} will be started
     * directly when {@link #show()} is called. Default: <code>disabled</code>.
     * @return This as per the builder pattern.
     */
    public Builder enableAutoPlay() {
      autoPlay = true;
      return this;
    }

    /**
     * When <i>auto close</i> is enabled the view will be closed as soon as the
     * {@link Simulator} is stopped. This is useful for creating automated GUIs.
     * Default: <code>disabled</code>.
     * @return This as per the builder pattern.
     */
    public Builder enableAutoClose() {
      autoClose = true;
      return this;
    }

    /**
     * Stops the simulator at the specified time.
     * @param simulationTime The time to stop, must be positive.
     * @return This as per the builder pattern.
     */
    public Builder stopSimulatorAtTime(long simulationTime) {
      checkArgument(simulationTime > 0);
      stopTime = simulationTime;
      return this;
    }

    /**
     * Speed up defines the simulation time between to respective GUI draw
     * operations. Default: <code>1</code>.
     * @param speed The speed to use.
     * @return This as per the builder pattern.
     */
    public Builder setSpeedUp(int speed) {
      speedUp = speed;
      return this;
    }

    /**
     * Should be used in case there is already an SWT application running that
     * was launched from the same VM as the current GUI that is created.
     * @param d The existing {@link Display} to use as display for the view.
     * @return This as per the builder pattern.
     */
    public Builder setDisplay(Display d) {
      display = d;
      return this;
    }

    /**
     * Changes the title appendix of the view. The default title is <i>RinSim -
     * Simulator</i>, the title appendix is everything after the dash.
     * @param titleAppendix The new appendix to use.
     * @return This as per the builder pattern.
     */
    public Builder setTitleAppendix(String titleAppendix) {
      title = titleAppendix;
      return this;
    }

    /**
     * Don't allow the user to resize the application window. Default:
     * <i>allowed</i>.
     * @return This as per the builder pattern.
     */
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

    /**
     * Change the resolution of the window. Default resolution:
     * {@link #DEFAULT_WINDOW_SIZE}.
     * @param width The new width to use.
     * @param height The new height to use.
     * @return This, as per the builder pattern.
     */
    public Builder setResolution(int width, int height) {
      checkArgument(width > 0 && height > 0,
          "Only positive dimensions are allowed, input: %s x %s.", width,
          height);
      screenSize = new Point(width, height);
      return this;
    }

    /**
     * Specify on which monitor the application should be positioned. If this
     * method is not called SWT decides where the screen is positioned, usually
     * on the primary monitor.
     * @param m The monitor.
     * @return This, as per the builder pattern.
     */
    public Builder displayOnMonitor(Monitor m) {
      monitor = m;
      return this;
    }

    /**
     * Allows to change the accelerators (aka shortcuts) of the menu items. Each
     * accelerator is set to its respective menu item via
     * {@link org.eclipse.swt.widgets.MenuItem#setAccelerator(int)}. By default
     * the {@link MenuItems#QWERTY_ACCELERATORS} are used.
     * @param acc The accelerators to set.
     * @return This, as per the builder pattern.
     */
    public Builder setAccelerators(Map<MenuItems, Integer> acc) {
      accelerators.putAll(acc);
      return this;
    }

    public Builder setAsync() {
      async = true;
      return this;
    }

    public Builder setCallback(Listener l) {
      callback = l;
      return this;
    }

    /**
     * Show the view.
     */
    public void show() {
      checkState(
          simulator.isConfigured(),
          "Simulator needs to be configured before it can be visualized, see Simulator.configure()");

      checkArgument(!rendererList.isEmpty(),
          "At least one renderer needs to be defined.");

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

      if (stopTime > 0) {
        simulator.addTickListener(new TickListener() {
          @Override
          public void tick(TimeLapse time) {}

          @Override
          public void afterTick(TimeLapse time) {
            if (time.getTime() >= stopTime) {
              simulator.stop();
            }
          }
        });
      }

      if (autoClose) {

        final Listener list = callback;

        simulator.getEventAPI().addListener(new Listener() {
          @Override
          public void handleEvent(final Event arg0) {
            if (!shell.isDisposed()) {
              disp.asyncExec(new Runnable() {
                @Override
                public void run() {
                  shell.close();
                }
              });
            }
            if (list != null) {
              list.handleEvent(arg0);
            }
          }
        }, Simulator.SimulatorEventType.STOPPED);
      }

      shell.addListener(SWT.Close, new org.eclipse.swt.widgets.Listener() {
        @Override
        public void handleEvent(@Nullable org.eclipse.swt.widgets.Event event) {
          simulator.stop();
          while (simulator.isPlaying()) {
            // wait until simulator actually stops (it finishes its
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
      new SimulationViewer(shell, simulator, speedUp, autoPlay, rendererList,
          accelerators);
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
