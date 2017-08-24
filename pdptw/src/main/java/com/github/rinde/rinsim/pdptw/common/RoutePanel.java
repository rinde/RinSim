/*
 * Copyright (C) 2011-2017 Rinde van Lon, imec-DistriNet, KU Leuven
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
package com.github.rinde.rinsim.pdptw.common;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.CheckReturnValue;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModel;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.core.model.road.RoadModel;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.google.auto.value.AutoValue;

/**
 * A panel that contains a table that lists every {@link RouteFollowingVehicle}
 * in the simulation and shows the route length and route contents.
 * @author Rinde van Lon
 */
public class RoutePanel extends AbstractModel<RouteFollowingVehicle>
    implements PanelRenderer {
  private static final int SIZE_PX = 200;
  private static final int COLUMN_WIDTH_PX = 100;

  Table table;
  RoadModel roadModel;

  List<RouteFollowingVehicle> list;
  private final int preferredPosition;

  RoutePanel(RoadModel rm, int preferredPos) {
    roadModel = rm;
    list = new ArrayList<>();
    preferredPosition = preferredPos;
  }

  @Override
  public void initializePanel(Composite parent) {
    parent.setLayout(new FillLayout());
    table = new Table(parent, SWT.BORDER | SWT.SINGLE);

    final TableColumn tc1 = new TableColumn(table, 0);
    tc1.setText("Vehicle");
    tc1.setWidth(COLUMN_WIDTH_PX);
    final TableColumn tc2 = new TableColumn(table, 0);
    tc2.setText("Route length");
    tc2.setWidth(COLUMN_WIDTH_PX);
    final TableColumn tc3 = new TableColumn(table, 0);
    tc3.setText("Route");
    tc3.setWidth(COLUMN_WIDTH_PX);
    table.setHeaderVisible(true);

    for (final RouteFollowingVehicle v : list) {
      createItem(v);
    }

    table.layout();
  }

  @Override
  public int preferredSize() {
    return SIZE_PX;
  }

  @Override
  public int getPreferredPosition() {
    return preferredPosition;
  }

  @Override
  public String getName() {
    return "Routes";
  }

  @Override
  public void render() {

    if (table.getItemCount() < list.size()) {
      for (int i = table.getItemCount(); i < list.size(); i++) {
        createItem(list.get(i));
      }
    }

    for (int i = 0; i < table.getItemCount(); i++) {
      final RouteFollowingVehicle v = list.get(i);
      table.getItem(i).setText(1, Integer.toString(v.getRoute().size()));
      table.getItem(i).setText(2, v.getRoute().toString());
    }
    table.getParent().redraw();
    table.getParent().layout();
  }

  void createItem(RouteFollowingVehicle v) {
    final TableItem item = new TableItem(table, SWT.NONE);
    item.setText(
      new String[] {v.toString(), Integer.toString(v.getRoute().size()),
        v.getRoute().toString()});
  }

  @Override
  public boolean register(RouteFollowingVehicle v) {
    list.add(v);
    return true;
  }

  @Override
  public boolean unregister(RouteFollowingVehicle element) {
    throw new UnsupportedOperationException();
  }

  /**
   * @return A new {@link Builder} instance.
   */
  public static Builder builder() {
    return Builder.create(SWT.RIGHT);
  }

  /**
   * A builder for {@link RoutePanel}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractModelBuilder<RoutePanel, RouteFollowingVehicle> {

    private static final long serialVersionUID = -5277450398730005609L;

    Builder() {
      setDependencies(RoadModel.class);
    }

    abstract int preferredPosition();

    /**
     * Will set the preferred position to left instead of the default (right).
     * @return A new builder instance.
     */
    @CheckReturnValue
    public Builder withPositionLeft() {
      return create(SWT.LEFT);
    }

    @Override
    public RoutePanel build(DependencyProvider dependencyProvider) {
      return new RoutePanel(dependencyProvider.get(RoadModel.class),
        preferredPosition());
    }

    static Builder create(int position) {
      return new AutoValue_RoutePanel_Builder(position);
    }
  }
}
