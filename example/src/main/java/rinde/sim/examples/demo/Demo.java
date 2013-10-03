/**
 * 
 */
package rinde.sim.examples.demo;

import static com.google.common.collect.Lists.newArrayList;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import rinde.sim.examples.factory.FactoryExample;
import rinde.sim.examples.pdp.PDPExample;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Demo {

  public static void main(String[] args) throws FileNotFoundException,
      IOException, InterruptedException {

    final long time = 6 * 60 * 60 * 1000L;
    Display.setAppName("RinSim");
    final Display d = new Display();

    final List<DemoRunner> demoRunners = newArrayList();

    final Shell shell = new Shell(d, SWT.TITLE | SWT.CLOSE);
    shell.setText("AgentWise Demo Control");
    shell.setLayout(new RowLayout(SWT.VERTICAL));
    final Button runButton = new Button(shell, SWT.CHECK);
    runButton.setText("Run demo");

    final Button fullScreenButton = new Button(shell, SWT.CHECK);
    fullScreenButton.setText("Full screen");
    fullScreenButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e) {
        for (final DemoRunner dr : demoRunners) {
          dr.setFullScreen(((Button) e.widget).getSelection());
        }
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {}
    });

    final Text timeText = new Text(shell, SWT.NONE);
    timeText.setText("6");
    timeText.setLayoutData(new RowData(40, SWT.DEFAULT));

    timeText.addListener(SWT.Verify, new Listener() {
      @Override
      public void handleEvent(Event e) {
        final String string = e.text;
        final char[] chars = new char[string.length()];
        string.getChars(0, chars.length, chars, 0);
        for (int i = 0; i < chars.length; i++) {
          if (!('0' <= chars[i] && chars[i] <= '9')) {
            e.doit = false;
            return;
          }
        }
        for (final DemoRunner dr : demoRunners) {
          dr.setTime(Long.parseLong(e.text) * 60 * 60 * 1000L);
        }
      }
    });

    final List<Button> monitorCheckBoxes = newArrayList();
    for (int i = 0; i < d.getMonitors().length; i++) {
      final Monitor m = d.getMonitors()[i];
      final Button b = new Button(shell, SWT.CHECK);
      b.setData(m);
      final boolean isPrimary = (m.equals(d.getPrimaryMonitor()));
      b.setText(i + " " + m.getBounds().width + "x" + m.getBounds().height
          + (isPrimary ? " PRIMARY" : ""));
      b.setSelection(!isPrimary);
      monitorCheckBoxes.add(b);
    }

    runButton.addSelectionListener(new SelectionListener() {
      @Override
      public void widgetSelected(SelectionEvent e) {
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

        if (((Button) e.widget).getSelection()) {
          for (final Button b : monitorCheckBoxes) {
            if (b.getSelection()) {
              final DemoRunner dr = new DemoRunner(d);
              dr.setMonitor((Monitor) b.getData());
              demoRunners.add(dr);
              // d.asyncExec(dr);
            }
          }

          // d.asyncExec(new MasterRunner(demoRunners, d));
          new MasterRunner(demoRunners, d).start();
        }
      }

      @Override
      public void widgetDefaultSelected(SelectionEvent e) {}
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

    shell.pack();
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

  static class DemoRunner implements Runnable, rinde.sim.event.Listener {

    boolean running;
    long time;
    boolean fullScreen;
    Monitor monitor;
    Display display;

    DemoRunner(Display d) {
      display = d;
      time = 6 * 60 * 60 * 1000L;
      alternate = true;
    }

    void setTime(long t) {
      time = t;
    }

    void setRunning(boolean r) {
      running = r;
    }

    void setFullScreen(boolean fs) {
      fullScreen = fs;
    }

    void setMonitor(Monitor m) {
      monitor = m;
    }

    boolean alternate;

    @Override
    public void run() {
      running = true;
      next();
    }

    public void next() {
      if (running) {
        final rinde.sim.event.Listener l = this;
        if (alternate) {
          display.syncExec(new Runnable() {
            @Override
            public void run() {
              FactoryExample.run(time, display, monitor, l);
            }
          });
        } else {
          display.syncExec(new Runnable() {
            @Override
            public void run() {
              // "../core/dot-files/leuven-simple.dot",
              PDPExample.run(time, "leuven-simple.dot", display, monitor, l);
            }
          });
        }
        alternate = !alternate;
      }
    }

    @Override
    public void handleEvent(rinde.sim.event.Event e) {
      next();
    }
  }

}
