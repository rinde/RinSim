package rinde.sim.ui;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.MenuItem;

public class UITestTools {

  public static Display findDisplay() {
    for (final Thread t : Thread.getAllStackTraces().keySet()) {
      final Display disp = Display.findDisplay(t);
      if (disp != null) {
        return disp;
      }
    }
    throw new IllegalStateException("There is no display");
  }

  public static void delayedSelectPlayPauseMenuItem(long msToWait) {
    final ScheduledExecutorService scheduler = Executors
        .newScheduledThreadPool(1);
    scheduler.schedule(new Runnable() {
      @Override
      public void run() {
        UITestTools.selectPlayPauseMenuItem();
      }
    }, msToWait, TimeUnit.MILLISECONDS);
  }

  public static void selectPlayPauseMenuItem() {
    final Display d = findDisplay();
    d.asyncExec(new Runnable() {
      @Override
      public void run() {
        for (final MenuItem menu : d.getActiveShell().getMenuBar().getItems()) {
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
    disp.syncExec(
        new Runnable() {
          @Override
          public void run() {
            disp.getActiveShell().close();
          }
        });
  }
}
