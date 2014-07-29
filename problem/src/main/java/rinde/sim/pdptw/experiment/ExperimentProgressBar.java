package rinde.sim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.Thread.State;
import java.math.RoundingMode;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;

import rinde.sim.pdptw.experiment.Experiment.SimulationResult;

import com.google.common.base.Optional;
import com.google.common.math.DoubleMath;

public class ExperimentProgressBar implements ResultListener {

  GuiRunner guiRunner;
  Thread t;
  int counter;

  public ExperimentProgressBar() {
    counter = 0;
  }

  @Override
  public void startComputing(int numberOfSimulations) {
    counter = 0;
    guiRunner = new GuiRunner(numberOfSimulations);
    t = new Thread(guiRunner);
    t.start();
  }

  @Override
  public void receive(SimulationResult result) {
    // should wait while runner is not yet initialized and thread state is
    // runnable. When an error is thrown in the thread, the waiting will stop to
    // avoid infinite waiting.
    while (!guiRunner.isInitialized() && t.getState() == State.RUNNABLE) {
      try {
        Thread.sleep(100);
      } catch (final InterruptedException e) {
        e.printStackTrace();
      }
    }
    guiRunner.getGui().update(++counter);
  }

  @Override
  public void doneComputing() {
    guiRunner.getGui().stop();
  }

  static class Gui {
    Display display;
    ProgressBar bar;
    Shell shell;
    final int max;
    Label numberLabel;

    Gui(int m) {
      max = m;
      display = new Display();
      shell = new Shell(display, SWT.TITLE | SWT.BORDER);
      shell.setSize(400, 150);
      shell.setText("RinSim - Experiment");
      Display.setAppName("RinSim - Experiment");

      shell.addListener(SWT.Close, new Listener() {
        @Override
        public void handleEvent(@Nullable Event event) {
          final int style = SWT.APPLICATION_MODAL | SWT.YES | SWT.NO;
          final MessageBox messageBox = new MessageBox(shell, style);
          messageBox.setText("Information");
          messageBox
              .setMessage("Close the shell?\nThis will NOT terminate the current experiment.");
          checkNotNull(event);
          event.doit = messageBox.open() == SWT.YES;
        }
      });

      final Monitor primary = display.getPrimaryMonitor();
      final Rectangle bounds = primary.getBounds();
      final Rectangle rect = shell.getBounds();
      final int x = bounds.x + (bounds.width - rect.width) / 2;
      final int y = bounds.y + (bounds.height - rect.height) / 2;
      shell.setLocation(x, y);

      bar = new ProgressBar(shell, SWT.SMOOTH);
      bar.setMaximum(max);

      final GridLayout layout = new GridLayout(2, false);
      shell.setLayout(layout);

      final GridData gd = new GridData(SWT.CENTER, SWT.CENTER, true, false);
      gd.horizontalSpan = 2;
      gd.grabExcessHorizontalSpace = true;
      gd.minimumWidth = 380;
      bar.setLayoutData(gd);

      final Label lab = new Label(shell, SWT.NONE);
      lab.setText("Received results:");

      numberLabel = new Label(shell, SWT.NONE);
    }

    public void start() {
      shell.layout();
      shell.open();
      while (!shell.isDisposed()) {
        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }
      display.dispose();
    }

    public void update(final int progress) {
      if (display.isDisposed()) {
        return;
      }
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          if (bar.isDisposed()) {
            return;
          }
          bar.setSelection(progress);
          shell.setText("RinSim - Experiment "
              + DoubleMath.roundToInt(
                  100d * (progress / (double) bar.getMaximum()),
                  RoundingMode.HALF_UP) + "%");
          numberLabel.setText(progress + "/" + bar.getMaximum());
          shell.layout();
        }
      });
    }

    public void stop() {
      if (display.isDisposed()) {
        return;
      }
      display.asyncExec(new Runnable() {
        @Override
        public void run() {
          if (shell.isDisposed()) {
            return;
          }
          shell.dispose();
        }
      });
    }

  }

  static class GuiRunner implements Runnable {
    int max;
    Optional<Gui> gui;

    public GuiRunner(int m) {
      max = m;
      gui = Optional.absent();
    }

    @Override
    public void run() {
      gui = Optional.of(new Gui(max));
      gui.get().start();
    }

    public boolean isInitialized() {
      return gui.isPresent();
    }

    public Gui getGui() {
      return gui.get();
    }
  }

}
