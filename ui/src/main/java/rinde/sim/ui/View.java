/**
 * 
 */
package rinde.sim.ui;

import static com.google.common.base.Preconditions.checkState;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import rinde.sim.core.Simulator;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.ui.renderers.Renderer;
import rinde.sim.ui.utils.Sleak;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public final class View {

	/**
	 * Indicates whether using the SWT handles tracing mode. Default value is
	 * <code>false</code>.
	 */
	protected static boolean testingMode;

	/**
	 * Indicates whether to start the simulation immediatly after launch.
	 * Default value is <code>false</code>.
	 */
	protected static boolean autoPlay;

	/**
	 * Indicates whether to stop/close the app when the simulation has stopped.
	 * Default value is <code>false</code>.
	 */
	protected static boolean autoClose;

	private View() {}

	/**
	 * Define the SWT handles tracing mode. Disabled by default
	 */
	public static void setTestingMode(boolean pTestingMode) {
		View.testingMode = pTestingMode;
	}

	public static void setAutoPlay(boolean pAutoPlay) {
		View.autoPlay = pAutoPlay;
	}

	public static void setAutoClose(boolean pAutoClose) {
		View.autoClose = pAutoClose;
	}

	@SuppressWarnings("unused")
	public static void startGui(final Simulator simulator, final int speedup, Renderer... renderers) {
		checkState(simulator.isConfigured(), "Simulator needs to be configured before it can be visualized, see Simulator.configure()");
		Display.setAppName("RinSim");
		final Display display;
		if (testingMode) {
			final DeviceData data = new DeviceData();
			data.tracking = true;
			display = new Display(data);
			final Sleak sleak = new Sleak();
			sleak.open();
		} else {
			display = new Display();
		}

		final Shell shell = new Shell(display, SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
		shell.setText("RinSim - Simulator");
		shell.setSize(new org.eclipse.swt.graphics.Point(800, 600));

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
