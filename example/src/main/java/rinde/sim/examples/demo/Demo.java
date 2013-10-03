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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import rinde.sim.core.Simulator;
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

    final Shell shell = new Shell(d, SWT.TITLE | SWT.CLOSE | SWT.RESIZE);
    shell.setText("AgentWise Demo Control");
    shell.setLayout(new RowLayout(SWT.VERTICAL));

    final Monitor primary = d.getPrimaryMonitor();

    shell.setLocation(primary.getClientArea().x, primary.getClientArea().y);
    shell
        .setSize(primary.getClientArea().width, primary.getClientArea().height);

    final Button runButton = new Button(shell, SWT.TOGGLE);
    runButton.setSize(300, 100);
    runButton.setText("Run demo");

    final Font f = runButton.getFont();
    final FontData[] fontData = f.getFontData();
    for (int i = 0; i < fontData.length; i++) {
      fontData[i].setHeight(60);
    }
    final Font newFont = new Font(d, fontData);
    runButton.setFont(newFont);

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

    final List<DemoRunnerControlPanel> panels = newArrayList();
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

        for (final DemoRunnerControlPanel panel : panels) {
          panel.remove();
        }
        panels.clear();
        if (((Button) e.widget).getSelection()) {
          runButton.setText("Stop demo");
        } else {
          runButton.setText("Run demo");
        }
        boolean alternate = false;
        if (((Button) e.widget).getSelection()) {
          for (final Button b : monitorCheckBoxes) {
            if (b.getSelection()) {
              final Monitor m = (Monitor) b.getData();
              final DemoRunner dr = new DemoRunner(d, alternate);
              alternate = !alternate;

              panels.add(new DemoRunnerControlPanel(shell, m, dr));

              dr.setMonitor(m);
              demoRunners.add(dr);
              // d.asyncExec(dr);
            }
          }
          shell.layout();

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

    public DemoRunnerControlPanel(Shell s, Monitor m, DemoRunner dr) {
      group = new Group(s, SWT.NONE);
      runner = dr;
      monitor = m;

      dr.addListener(this);

      group.setLayout(new FillLayout());
      label = new Label(group, SWT.NONE);

      final Font f = label.getFont();
      final FontData[] fontData = f.getFontData();
      for (int i = 0; i < fontData.length; i++) {
        fontData[i].setHeight(40);
      }
      final Font newFont = new Font(s.getDisplay(), fontData);
      label.setFont(newFont);

      final Button next = new Button(group, SWT.PUSH);
      next.setText("next");
      next.addSelectionListener(new SelectionListener() {
        @Override
        public void widgetSelected(SelectionEvent e) {
          runner.next();
        }

        @Override
        public void widgetDefaultSelected(SelectionEvent e) {}
      });
      next.setFont(newFont);
      newFont.dispose();
      // g.setText(m.getBounds().toString());
      group.setLayoutData(new RowData(m.getBounds().width / 3,
          m.getBounds().height / 3));

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
              + monitor.getBounds().height + "\n" + runner.state);
        }
      });
    }
  }

  static class DemoRunner implements Runnable, rinde.sim.event.Listener {

    static final String FACTORY = "factory";
    static final String TAXI = "taxi";

    boolean running;
    long time;
    Monitor monitor;
    Display display;

    String state;

    DemoRunnerControlPanel listener;

    DemoRunner(Display d, boolean alt) {
      display = d;
      time = 6 * 60 * 60 * 1000L;
      alternate = alt;
      state = alt ? FACTORY : TAXI;
      sims = newArrayList();
    }

    /**
     * @param demoRunnerControlPanel
     */
    public void addListener(DemoRunnerControlPanel demoRunnerControlPanel) {
      listener = demoRunnerControlPanel;

    }

    void setTime(long t) {
      time = t;
    }

    void setRunning(boolean r) {
      running = r;
    }

    void setMonitor(Monitor m) {
      monitor = m;
    }

    boolean alternate;
    final List<Simulator> sims;

    @Override
    public void run() {
      running = true;
      next();
    }

    public void next() {
      if (!sims.isEmpty()) {
        for (final Simulator s : sims) {
          s.stop();
        }
        sims.clear();
        return;
      }

      alternate = !alternate;
      state = alternate ? FACTORY : TAXI;

      if (running) {
        final rinde.sim.event.Listener l = this;
        if (alternate) {
          listener.update();
          display.syncExec(new Runnable() {
            @Override
            public void run() {
              sims.add(FactoryExample.run(time, display, monitor, l));
            }
          });
        } else {
          listener.update();
          display.syncExec(new Runnable() {
            @Override
            public void run() {
              // "../core/dot-files/leuven-simple.dot",
              sims.add(PDPExample.run(time, "leuven-simple.dot", display,
                  monitor, l));
            }
          });
        }
      }
    }

    @Override
    public void handleEvent(rinde.sim.event.Event e) {
      sims.clear();
      next();
    }
  }
}
