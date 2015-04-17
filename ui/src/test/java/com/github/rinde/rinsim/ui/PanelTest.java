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
package com.github.rinde.rinsim.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.core.Simulator;
import com.github.rinde.rinsim.core.model.road.RoadModelBuilders;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.testutil.GuiTests;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.github.rinde.rinsim.ui.renderers.PlaneRoadModelRenderer;
import com.github.rinde.rinsim.ui.renderers.RoadUserRenderer;

/**
 * @author Rinde van Lon
 *
 */
@Category(GuiTests.class)
public final class PanelTest {

  private PanelTest() {}

  public static void main(String[] args) {

    final Simulator sim = Simulator.builder()
      .addModel(RoadModelBuilders.plane()
        .setMinPoint(new Point(0, 0))
        .setMaxPoint(new Point(10, 10))
        .setMaxSpeed(10d)
      ).build();

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
