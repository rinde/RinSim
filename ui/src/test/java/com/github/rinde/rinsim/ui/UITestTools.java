/*
 * Copyright (C) 2011-2014 Rinde van Lon, iMinds DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.ui;

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
   * Select the play/pause menu item as soon as it is created. Then wait for the
   * specified number of milliseconds and close the active shell. This method
   * spawns a new thread which monitors the creation of a display object.
   * @param delay The time (ms) to wait before the shell is closed. When delay
   *          is negative, the shell is not closed.
   */
  public static void startAndClose(final long delay) {
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
        d.syncExec(new Runnable() {
          @Override
          public void run() {

            while (d.getShells().length == 0) {
              try {
                Thread.sleep(100);
              } catch (final InterruptedException e) {
                throw new IllegalStateException();
              }
            }
            for (final MenuItem menu : d.getShells()[0].getMenuBar()
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

        if (delay > 0) {
          try {
            Thread.sleep(delay);
          } catch (final InterruptedException e) {
            throw new IllegalStateException();
          }

          d.syncExec(
              new Runnable() {
                @Override
                public void run() {
                  d.getShells()[0].close();
                }
              });
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
    checkState(disp != null);
    disp.syncExec(
        new Runnable() {
          @Override
          public void run() {
            disp.getShells()[0].close();
          }
        });
  }
}
