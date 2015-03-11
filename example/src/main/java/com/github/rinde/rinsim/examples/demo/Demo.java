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
package com.github.rinde.rinsim.examples.demo;

import static com.google.common.base.Verify.verifyNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.math.RoundingMode;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.examples.core.taxi.TaxiExample;
import com.github.rinde.rinsim.examples.demo.factory.FactoryExample;
import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * @author Rinde van Lon
 *
 */
public class Demo {

  private Demo() {}

  /**
   * Runs the demo.
   * @param args Ignored.
   * @throws IOException Should not happen.
   * @throws InterruptedException Should not happen.
   */
  public static void main(String[] args) throws IOException,
      InterruptedException {

    Display.setAppName("RinSim");
    final Display d = new Display();

    final List<DemoRunner> demoRunners = newArrayList();

    final Shell shell = new Shell(d, SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
    shell.setText("AgentWise Demo Control Center™");
    shell.setLayout(new RowLayout(SWT.VERTICAL));

    final Monitor primary = d.getPrimaryMonitor();
    shell.setLocation(primary.getClientArea().x, primary.getClientArea().y);
    shell
        .setSize(primary.getClientArea().width, primary.getClientArea().height);

    final Composite controlsComposite = new Composite(shell, SWT.NONE);
    controlsComposite.setLayout(new RowLayout(SWT.HORIZONTAL));

    final Label timeLabel = new Label(controlsComposite, SWT.NONE);
    timeLabel.setText("End simulation time (in hours): ");

    final Text timeText = new Text(controlsComposite, SWT.NONE);
    timeText.setText("6.0");
    timeText.setLayoutData(new RowData(40, SWT.DEFAULT));

    timeText.addListener(SWT.Verify, new Listener() {
      @Override
      public void handleEvent(@Nullable Event e) {
        assert e != null;
        final String string = e.text;
        final char[] chars = new char[string.length()];
        string.getChars(0, chars.length, chars, 0);
        for (int i = 0; i < chars.length; i++) {
          if (!('0' <= chars[i] && chars[i] <= '9') && chars[i] != '.') {
            e.doit = false;
            return;
          }
        }
        for (final DemoRunner dr : demoRunners) {
          dr.setTime(Double.parseDouble(e.text));
        }
      }
    });
    final List<Button> monitorCheckBoxes = newArrayList();
    for (int i = 0; i < d.getMonitors().length; i++) {
      final Monitor m = d.getMonitors()[i];
      final Button b = new Button(controlsComposite, SWT.CHECK);
      b.setData(m);
      final boolean isPrimary = m.equals(d.getPrimaryMonitor());
      b.setText(i + " " + m.getBounds().width + "x" + m.getBounds().height
          + (isPrimary ? " PRIMARY" : ""));
      b.setSelection(!isPrimary);
      monitorCheckBoxes.add(b);
    }

    final List<Button> demoCheckBoxes = newArrayList();
    for (final DemoType dt : DemoType.values()) {
      final Button b = new Button(controlsComposite, SWT.CHECK);
      demoCheckBoxes.add(b);
      b.setText(dt.name());
      b.setData(dt);
      b.setSelection(true);
    }

    final Button runButton = new Button(shell, SWT.TOGGLE);
    runButton.setSize(300, 100);
    runButton.setText("Start demo");

    final Font f = runButton.getFont();
    final FontData[] fontData = f.getFontData();
    for (int i = 0; i < fontData.length; i++) {
      fontData[i].setHeight(60);
    }
    final Font newFont = new Font(d, fontData);
    runButton.setFont(newFont);

    final Composite panelComposite = new Composite(shell, SWT.NONE);
    // panelComposite.setLayout(new RowLayout(SWT.HORIZONTAL));

    final List<DemoRunnerControlPanel> panels = newArrayList();
    runButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(@Nullable SelectionEvent e) {
        assert e != null;
        // clear old runners
        for (final DemoRunner dr : demoRunners) {
          dr.setRunning(false);
        }
        demoRunners.clear();
        // close old shells
        for (final Shell s : d.getShells()) {
          if (s != shell) {
            s.close();
          }
        }

        for (final DemoRunnerControlPanel panel : panels) {
          panel.remove();
        }
        panels.clear();

        controlsComposite.setEnabled(!((Button) e.widget).getSelection());
        for (final Control c : controlsComposite.getChildren()) {

          c.setEnabled(!((Button) e.widget).getSelection());
        }

        if (((Button) e.widget).getSelection()) {
          runButton.setText("Stop demo");
        } else {
          runButton.setText("Start demo");
        }

        if (((Button) e.widget).getSelection()) {
          int index = 0;
          final List<DemoType> types = newArrayList();
          for (final Button b : demoCheckBoxes) {
            if (b.getSelection()) {
              types.add((DemoType) b.getData());
            }
          }
          final ImmutableList<DemoType> demoTypes = ImmutableList.copyOf(types);
          for (final Button b : monitorCheckBoxes) {
            if (b.getSelection()) {
              final Monitor m = (Monitor) b.getData();
              final DemoRunner dr = new DemoRunner(d, demoTypes, index);
              index++;

              dr.setTime(Double.parseDouble(timeText.getText()));

              panels.add(new DemoRunnerControlPanel(panelComposite, m, dr));

              dr.setMonitor(m);
              demoRunners.add(dr);
            }
          }
          panelComposite.layout();
          shell.layout();
          new MasterRunner(demoRunners, d).start();
        }
      }

      @Override
      public void widgetDefaultSelected(@Nullable SelectionEvent e) {}
    });

    shell.addListener(SWT.Close, new org.eclipse.swt.widgets.Listener() {
      @Override
      public void handleEvent(@Nullable org.eclipse.swt.widgets.Event event) {
        for (final DemoRunner dr : demoRunners) {
          dr.setRunning(false);
        }
        d.dispose();
      }
    });

    shell.open();
    while (!shell.isDisposed()) {
      if (!d.readAndDispatch()) {
        d.sleep();
      }
    }
  }

  static class MasterRunner extends Thread {

    final List<DemoRunner> runners;
    final Display display;

    MasterRunner(List<DemoRunner> r, Display d) {
      runners = ImmutableList.copyOf(r);
      display = d;
    }

    @Override
    public void run() {
      for (final DemoRunner r : runners) {
        new Thread(r).start();
      }
    }
  }

  static class DemoRunnerControlPanel {

    final Group group;
    final Label label;
    final DemoRunner runner;
    final Monitor monitor;

    public DemoRunnerControlPanel(Composite parent, Monitor m, DemoRunner dr) {
      group = new Group(parent, SWT.NONE);
      runner = dr;
      monitor = m;

      dr.addListener(this);

      final Rectangle displayBounds = parent.getDisplay().getBounds();

      final double maxDimension = Math.max(displayBounds.width,
          displayBounds.height);

      final double wRatio = m.getBounds().width / maxDimension;
      final double hRatio = m.getBounds().height / maxDimension;

      final double xRatio = (m.getBounds().x - displayBounds.x) / maxDimension;
      final double yRatio = (m.getBounds().y - displayBounds.y) / maxDimension;

      final double displayWidth = parent.getShell().getBounds().width - 100;

      group.setSize((int) (wRatio * displayWidth),
          (int) (hRatio * displayWidth));

      final int xLoc = 50 + (int) (xRatio * displayWidth);
      final int yLoc = 50 + (int) (yRatio * displayWidth);

      group.setLocation(xLoc, yLoc);
      group.setLayout(new FillLayout(SWT.VERTICAL));
      label = new Label(group, SWT.NONE);

      final Font f = label.getFont();
      final FontData[] fontData = f.getFontData();
      for (int i = 0; i < fontData.length; i++) {
        fontData[i].setHeight(40);
      }
      final Font newFont = new Font(parent.getDisplay(), fontData);
      label.setFont(newFont);

      final Button next = new Button(group, SWT.PUSH);
      next.setText("next");
      next.addSelectionListener(new SelectionListener() {
        @Override
        public void widgetSelected(@Nullable SelectionEvent e) {
          runner.next();
        }

        @Override
        public void widgetDefaultSelected(@Nullable SelectionEvent e) {}
      });
      next.setFont(newFont);
      newFont.dispose();
      group.layout();
      update();
    }

    /**
     *
     */
    public void remove() {
      group.dispose();
    }

    public void update() {
      label.getDisplay().asyncExec(new Runnable() {
        @Override
        public void run() {
          label.setText(monitor.getBounds().width + "x"
              + monitor.getBounds().height + "\n" + runner.getState());
        }
      });
    }
  }

  enum DemoType {
    FACTORY, TAXI;
  }

  static class DemoRunner implements Runnable,
      com.github.rinde.rinsim.event.Listener {
    static final long DEFAULT_DURATION = 6 * 60 * 60 * 1000L;

    boolean running;
    long time;
    @Nullable
    Monitor monitor;
    Display display;

    ImmutableList<DemoType> demoTypes;
    int demoIndex;
    @Nullable
    DemoRunnerControlPanel listener;
    final List<Simulator> sims;

    DemoRunner(Display d, ImmutableList<DemoType> dt, int startIndex) {
      display = d;
      time = DEFAULT_DURATION;
      demoTypes = dt;
      demoIndex = startIndex;
      sims = newArrayList();
    }

    void addListener(DemoRunnerControlPanel demoRunnerControlPanel) {
      listener = demoRunnerControlPanel;
    }

    void setTime(double t) {
      time = DoubleMath.roundToLong(t * 60d * 60d * 1000d,
          RoundingMode.HALF_DOWN);
    }

    void setRunning(boolean r) {
      running = r;
    }

    void setMonitor(Monitor m) {
      monitor = m;
    }

    @Override
    public void run() {
      running = true;
      next();
    }

    public DemoType getState() {
      return demoTypes.get(demoIndex);
    }

    public void next() {
      if (!sims.isEmpty()) {
        for (final Simulator s : sims) {
          s.stop();
        }
        sims.clear();
        return;
      }
      demoIndex++;
      demoIndex = demoIndex % demoTypes.size();

      if (running) {
        final com.github.rinde.rinsim.event.Listener l = this;
        if (getState() == DemoType.FACTORY) {
          verifyNotNull(listener).update();
          display.asyncExec(new Runnable() {
            @Override
            public void run() {
              sims.add(FactoryExample.run(time, display, monitor, l));
            }
          });
        } else {
          verifyNotNull(listener).update();
          display.asyncExec(new Runnable() {
            @Override
            public void run() {
              sims.add(TaxiExample.run(false, time,
                  "/data/maps/leuven-simple.dot",
                  display, monitor, l));
            }
          });
        }
      }
    }

    @Override
    public void handleEvent(com.github.rinde.rinsim.event.Event e) {
      sims.clear();
      next();
    }
  }
}
