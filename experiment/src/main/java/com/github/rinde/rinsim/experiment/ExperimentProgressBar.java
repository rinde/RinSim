/*
 * Copyright (C) 2011-2015 Rinde van Lon, iMinds-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.experiment;

import static com.google.common.base.Preconditions.checkNotNull;

import java.lang.Thread.UncaughtExceptionHandler;
import java.math.RoundingMode;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
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

import com.github.rinde.rinsim.experiment.Experiment.SimulationResult;
import com.google.common.base.Optional;
import com.google.common.math.DoubleMath;

/**
 * Displays a progress bar for a running {@link Experiment}.
 * @author Rinde van Lon
 */
public class ExperimentProgressBar implements ResultListener,
    UncaughtExceptionHandler {

  static final long THREAD_SLEEP_MS = 100;
  static final String APP_NAME = "RinSim - Experiment";
  static final Point SHELL_SIZE = new Point(400, 150);
  static final int BAR_WIDTH = 380;

  GuiRunner guiRunner;
  Thread t;
  int counter;
  Optional<Throwable> error;

  /**
   * Create a new instance.
   */
  @SuppressWarnings("null")
  public ExperimentProgressBar() {
    counter = 0;
    error = Optional.absent();
  }

  @Override
  public void startComputing(int numberOfSimulations) {
    counter = 0;
    guiRunner = new GuiRunner(numberOfSimulations);
    t = new Thread(guiRunner);
    t.setUncaughtExceptionHandler(this);
    t.start();
  }

  @Override
  public void receive(SimulationResult result) {
    // should wait while runner is not yet initialized and thread state is
    // runnable. When an error is thrown in the thread, the waiting will stop to
    // avoid infinite waiting.
    while (!guiRunner.isInitialized() && !error.isPresent()) {
      try {
        Thread.sleep(THREAD_SLEEP_MS);
      } catch (final InterruptedException e) {
        throw new IllegalStateException(e);
      }
    }
    if (error.isPresent()) {
      throw new IllegalStateException(error.get());
    }
    guiRunner.getGui().update(++counter);
  }

  @Override
  public void doneComputing() {
    guiRunner.getGui().stop();
  }

  @Override
  public void uncaughtException(@Nullable Thread thread, @Nullable Throwable e) {
    error = Optional.fromNullable(e);
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
      shell.setSize(SHELL_SIZE);
      shell.setText(APP_NAME);
      Display.setAppName(APP_NAME);

      shell.addListener(SWT.Close, new Listener() {
        @Override
        public void handleEvent(@SuppressWarnings("null") Event event) {
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
      gd.minimumWidth = BAR_WIDTH;
      bar.setLayoutData(gd);

      final Label lab = new Label(shell, SWT.NONE);
      lab.setText("Received results:");

      numberLabel = new Label(shell, SWT.NONE);
    }

    void start() {
      shell.layout();
      shell.open();
      while (!shell.isDisposed()) {
        if (!display.readAndDispatch()) {
          display.sleep();
        }
      }
      display.dispose();
    }

    void update(final int progress) {
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
          final double maxPerc = 100d;
          shell.setText("RinSim - Experiment "
              + DoubleMath.roundToInt(
                  maxPerc * (progress / (double) bar.getMaximum()),
                  RoundingMode.HALF_UP) + "%");
          numberLabel.setText(progress + "/" + bar.getMaximum());
          shell.layout();
        }
      });
    }

    void stop() {
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

    GuiRunner(int m) {
      max = m;
      gui = Optional.absent();
    }

    @Override
    public void run() {
      gui = Optional.of(new Gui(max));
      gui.get().start();
    }

    boolean isInitialized() {
      return gui.isPresent();
    }

    Gui getGui() {
      return gui.get();
    }
  }
}
