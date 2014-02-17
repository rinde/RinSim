package rinde.sim.ui;

import static com.google.common.base.Preconditions.checkState;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.MenuItem;

public class UITestTools {

  /**
   * @return The {@link Display} object if it exists, <code>null</code>
   *         otherwise.
   */
  @Nullable
  public static Display findDisplay() {
    for (final Thread t : Thread.getAllStackTraces().keySet()) {
      final Display disp = Display.findDisplay(t);
      if (disp != null) {
        return disp;
      }
    }
    return null;
  }

  /**
   * Select the play/pause menu item as soon as it is created. This method
   * spawns a new thread which monitors the creation of a display object.
   */
  public static void selectPlayPauseMenuItem() {
    Executors.newSingleThreadExecutor().submit(new Runnable() {
      @Override
      public void run() {
        Display disp = findDisplay();
        while (disp == null) {
          try {
            Thread.sleep(100);
          } catch (final InterruptedException e) {
            throw new IllegalStateException();
          }
          disp = findDisplay();
        }

        final Display d = disp;
        d.asyncExec(new Runnable() {
          @Override
          public void run() {
            while (d.getActiveShell() == null) {
              try {
                Thread.sleep(100);
              } catch (final InterruptedException e) {
                throw new IllegalStateException();
              }
            }
            for (final MenuItem menu : d.getActiveShell().getMenuBar()
                .getItems()) {
              if (menu.getText().contains("Control")) {
                for (final MenuItem m : menu.getMenu().getItems()) {
                  if (m.getText().contains("Play")) {
                    m.notifyListeners(SWT.Selection, new Event());
                  }
                }
              }
            }
          }
        });
      }
    });
  }

  public static void closeActiveShell(long delay) {
    final ScheduledExecutorService scheduler = Executors
        .newScheduledThreadPool(1);
    scheduler.schedule(new Runnable() {
      @Override
      public void run() {
        UITestTools.closeActiveShell();
      }
    }, delay, TimeUnit.MILLISECONDS);
  }

  public static void closeActiveShell() {
    final Display disp = findDisplay();
    checkState(disp != null);
    disp.syncExec(
        new Runnable() {
          @Override
          public void run() {
            disp.getActiveShell().close();
          }
        });
  }
}
