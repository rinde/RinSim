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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.math3.random.RandomAdaptor;
import org.apache.commons.math3.random.RandomGenerator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.rand.RandomProvider;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.examples.demo.SwarmDemo.Vehicle;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.google.auto.value.AutoValue;

class DemoPanel extends AbstractModel<Void>implements PanelRenderer, Listener {
  final RoadModel roadModel;
  final RandomGenerator rng;
  final Set<Vehicle> vehicles;
  final String startString;

  DemoPanel(String s, RoadModel rm, RandomGenerator r) {
    startString = s;
    roadModel = rm;
    rng = r;
    vehicles = new LinkedHashSet<>();
  }

  @Override
  public void initializePanel(Composite parent) {
    final FillLayout rl = new FillLayout();
    parent.setLayout(rl);
    final Text t = new Text(parent, SWT.SINGLE | SWT.ICON_CANCEL | SWT.CANCEL);
    t.setText(startString);

    final int chars = 30;
    final GC gc = new GC(t);
    final FontMetrics fm = gc.getFontMetrics();
    final int width = chars * fm.getAverageCharWidth();
    final int height = fm.getHeight();
    gc.dispose();
    t.setSize(t.computeSize(width, height));
    t.addListener(SWT.DefaultSelection, this);
    t.addListener(SWT.Modify, this);

    vehicles.addAll(roadModel.getObjectsOfType(Vehicle.class));
  }

  @Override
  public int preferredSize() {
    return 30;
  }

  @Override
  public int getPreferredPosition() {
    return SWT.TOP;
  }

  @Override
  public String getName() {
    return "Demo";
  }

  @Override
  public void handleEvent(@Nullable Event event) {
    assert event != null;
    final Iterator<Point> points = SwarmDemo.measureString(
        ((Text) event.widget).getText(), SwarmDemo.FONT_SIZE, 30d, 0)
        .iterator();
    final List<Vehicle> vs = newArrayList(vehicles);
    if (event.type == SWT.DefaultSelection) {
      Collections.shuffle(vs, new RandomAdaptor(rng));
    }
    for (final Vehicle v : vs) {
      if (points.hasNext()) {
        v.setDestination(points.next());
      } else {
        v.setInactive();
      }
    }
  }

  @Override
  public void render() {}

  @Override
  public boolean register(Void element) {
    return false;
  }

  @Override
  public boolean unregister(Void element) {
    return false;
  }

  static Builder builder(String s) {
    return new AutoValue_DemoPanel_Builder(s);
  }

  @AutoValue
  abstract static class Builder extends AbstractModelBuilder<DemoPanel, Void> {

    Builder() {
      setDependencies(RoadModel.class, RandomProvider.class);
    }

    abstract String string();

    @Override
    public DemoPanel build(DependencyProvider dependencyProvider) {
      final RoadModel rm = dependencyProvider.get(RoadModel.class);
      final RandomGenerator r = dependencyProvider.get(RandomProvider.class)
          .newInstance();
      return new DemoPanel(string(), rm, r);
    }
  }
}
