/**
 * 
 */
package com.github.rinde.rinsim.ui;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.PlaneRoadModel;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.View;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public final class PanelTest {

  private PanelTest() {}

  public static void main(String[] args) {

    final Simulator sim = new Simulator(new MersenneTwister(123),
        Measure.valueOf(1000L, SI.MILLI(SI.SECOND)));
    sim.register(new PlaneRoadModel(new Point(0, 0), new Point(10, 10), 10));
    sim.configure();

    View.create(sim)
        .with(new RoadUserRenderer(), new PlaneRoadModelRenderer(),
            new TestPanelRenderer("LEFT", SWT.LEFT, 200),
            new TestPanelRenderer("RIHGT BOEEE YEAH", SWT.RIGHT, 300),
            new TestPanelRenderer("RIHGT BOEEE YEAH", SWT.TOP, 100),
            new TestPanelRenderer("TOP2", SWT.TOP, 100),
            new TestPanelRenderer("LEFT2", SWT.LEFT, 100),
            new TestPanelRenderer("LEFT3", SWT.LEFT, 150)).show();

  }

  static class TestPanelRenderer implements PanelRenderer {

    protected final String name;
    protected final int position;
    protected final int size;

    public TestPanelRenderer(String n, int pos, int s) {
      name = n;
      position = pos;
      size = s;
    }

    @Override
    public void initializePanel(Composite c) {
      c.setLayout(new FillLayout());
      final Button b = new Button(c, SWT.PUSH);

      b.setText("push me " + name);
      // TODO Auto-generated method stub

    }

    @Override
    public int getPreferredPosition() {
      return position;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public int preferredSize() {
      return size;
    }

    @Override
    public void render() {}

  }
}
