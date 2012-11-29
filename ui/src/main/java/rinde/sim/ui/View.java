/**
 * 
 */
package rinde.sim.ui;

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
public class View {

	protected static boolean testingMode = false;
	protected static boolean autoPlay = false;
	protected static boolean autoClose = false;

	private View() {};

	/**
	 * Define the SWT handles tracing mode. Disabled by default
	 * @param testingMode
	 */
	public static void setTestingMode(boolean testingMode) {
		View.testingMode = testingMode;
	}

	public static void setAutoPlay(boolean autoPlay) {
		View.autoPlay = autoPlay;
	}

	public static void setAutoClose(boolean autoClose) {
		View.autoClose = autoClose;
	}

	public static void startGui(final Simulator simulator, final int speedup, Renderer... renderers) {
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
