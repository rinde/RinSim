/**
 * 
 */
package rinde.sim.ui;

import static com.google.common.base.Preconditions.checkState;

import org.eclipse.swt.SWT;
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

  private static boolean autoPlay;
  private static boolean autoClose;

  private View() {}

  /**
   * Indicates whether to start the simulation immediatly after launch. Default
   * value is <code>false</code>.
   * @param pAutoPlay auto play.
   */
  public static void setAutoPlay(boolean pAutoPlay) {
    View.autoPlay = pAutoPlay;
  }

  /**
   * Indicates whether to stop/close the app when the simulation has stopped.
   * Default value is <code>false</code>.
   * @param pAutoClose auto close.
   */
  public static void setAutoClose(boolean pAutoClose) {
    View.autoClose = pAutoClose;
  }

  /**
   * Immediately starts the gui for the specified simulator.
   * @param simulator The simulator to visualize.
   * @param speedup The speedup to use.
   * @param renderers Any additional renderers to use.
   */
  @SuppressWarnings("unused")
  public static void startGui(final Simulator simulator, final int speedup,
      Renderer... renderers) {
    checkState(
        simulator.isConfigured(),
        "Simulator needs to be configured before it can be visualized, see Simulator.configure()");
    Display.setAppName("RinSim");
    final Display display = new Display();

    final Shell shell = new Shell(display, SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
    shell.setText("RinSim - Simulator");
    shell.setSize(new org.eclipse.swt.graphics.Point(1024, 768));

    if (autoClose) {
      simulator.getEventAPI().addListener(new Listener() {
        @Override
        public void handleEvent(final Event arg0) {
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
    new SimulationViewer(shell, simulator, speedup, autoPlay, renderers);

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
