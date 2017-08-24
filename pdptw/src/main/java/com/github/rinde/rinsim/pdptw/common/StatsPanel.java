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

import static com.google.common.base.Verify.verify;

import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.github.rinde.rinsim.core.model.DependencyProvider;
import com.github.rinde.rinsim.core.model.Model.AbstractModelVoid;
import com.github.rinde.rinsim.core.model.ModelBuilder.AbstractModelBuilder;
import com.github.rinde.rinsim.event.Event;
import com.github.rinde.rinsim.event.Listener;
import com.github.rinde.rinsim.ui.renderers.PanelRenderer;
import com.google.auto.value.AutoValue;
import com.google.common.base.Optional;

/**
 * A UI panel that gives a live view of the current statistics of a simulation.
 * @author Rinde van Lon
 */
public final class StatsPanel extends AbstractModelVoid
    implements PanelRenderer {

  private static final int PREFERRED_SIZE = 300;

  Optional<Table> statsTable;
  private final StatsProvider statsTracker;

  /**
   * Create a new instance using the specified {@link StatsTracker} which
   * supplies the statistics.
   * @param stats The tracker to use.
   */
  StatsPanel(StatsProvider stats) {
    statsTracker = stats;
    statsTable = Optional.absent();
  }

  @Override
  public void initializePanel(Composite parent) {
    final FillLayout layout = new FillLayout();
    layout.marginWidth = 2;
    layout.marginHeight = 2;
    layout.type = SWT.VERTICAL;
    parent.setLayout(layout);

    final Table table = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION
      | SWT.V_SCROLL | SWT.H_SCROLL);
    statsTable = Optional.of(table);
    table.setHeaderVisible(true);
    table.setLinesVisible(true);
    final String[] statsTitles = new String[] {"Variable", "Value"};
    for (int i = 0; i < statsTitles.length; i++) {
      final TableColumn column = new TableColumn(table, SWT.NONE);
      column.setText(statsTitles[i]);
    }
    final StatisticsDTO stats = statsTracker.getStatistics();
    final Field[] fields = stats.getClass().getFields();
    for (final Field f : fields) {
      final TableItem ti = new TableItem(table, 0);
      ti.setText(0, f.getName());
      try {
        ti.setText(1, f.get(stats).toString());
      } catch (final IllegalArgumentException | IllegalAccessException e) {
        ti.setText(1, e.getMessage());
      }
    }
    for (int i = 0; i < statsTitles.length; i++) {
      table.getColumn(i).pack();
    }

    final Table eventList = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION
      | SWT.V_SCROLL | SWT.H_SCROLL);
    eventList.setHeaderVisible(true);
    eventList.setLinesVisible(true);
    final String[] titles = new String[] {"Time", "Tardiness"};
    for (int i = 0; i < titles.length; i++) {
      final TableColumn column = new TableColumn(eventList, SWT.NONE);
      column.setText(titles[i]);
    }
    for (int i = 0; i < titles.length; i++) {
      eventList.getColumn(i).pack();
    }

    statsTracker.getEventAPI().addListener(new Listener() {
      @Override
      public void handleEvent(Event e) {
        verify(e instanceof StatsEvent);
        final StatsEvent se = (StatsEvent) e;
        if (eventList.getDisplay().isDisposed()) {
          return;
        }
        eventList.getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            final TableItem ti = new TableItem(eventList, 0);
            ti.setText(0, Long.toString(se.getTime()));
            ti.setText(1, Long.toString(se.getTardiness()));
          }
        });
      }
    }, StatsProvider.EventTypes.PICKUP_TARDINESS,
      StatsProvider.EventTypes.DELIVERY_TARDINESS);
  }

  @Override
  public int preferredSize() {
    return PREFERRED_SIZE;
  }

  @Override
  public int getPreferredPosition() {
    return SWT.LEFT;
  }

  @Override
  public String getName() {
    return "Statistics";
  }

  @Override
  public void render() {
    final StatisticsDTO stats = statsTracker.getStatistics();

    final Field[] fields = stats.getClass().getFields();
    if (statsTable.get().isDisposed()
      || statsTable.get().getDisplay().isDisposed()) {
      return;
    }
    statsTable.get().getDisplay().syncExec(new Runnable() {
      @Override
      public void run() {
        if (statsTable.get().isDisposed()) {
          return;
        }
        for (int i = 0; i < fields.length; i++) {
          try {
            statsTable.get().getItem(i)
              .setText(1, fields[i].get(stats).toString());
          } catch (final IllegalArgumentException | IllegalAccessException e) {
            statsTable.get().getItem(i).setText(1, e.getMessage());
          }
        }
      }
    });

  }

  /**
   * @return A new {@link Builder}.
   */
  public static Builder builder() {
    return new AutoValue_StatsPanel_Builder();
  }

  /**
   * Builder for the {@link StatsPanel}.
   * @author Rinde van Lon
   */
  @AutoValue
  public abstract static class Builder
      extends AbstractModelBuilder<StatsPanel, Void> {

    private static final long serialVersionUID = 756808574424058913L;

    Builder() {
      setDependencies(StatsProvider.class);
    }

    @Override
    public StatsPanel build(DependencyProvider dependencyProvider) {
      return new StatsPanel(dependencyProvider.get(StatsProvider.class));
    }
  }
}
