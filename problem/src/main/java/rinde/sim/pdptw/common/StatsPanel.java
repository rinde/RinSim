/**
 * 
 */
package rinde.sim.pdptw.common;

import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.pdptw.common.StatsTracker.StatisticsEvent;
import rinde.sim.pdptw.common.StatsTracker.StatisticsEventType;
import rinde.sim.ui.renderers.PanelRenderer;

import com.google.common.base.Optional;

/**
 * A UI panel that gives a live view of the current statistics of a simulation.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
final class StatsPanel implements PanelRenderer, TickListener {

  private static final int PREFERRED_SIZE = 300;

  Optional<Table> statsTable;
  private final StatsTracker statsTracker;

  /**
   * Create a new instance using the specified {@link StatsTracker} which
   * supplies the statistics.
   * @param stats The tracker to use.
   */
  StatsPanel(StatsTracker stats) {
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
    final String[] statsTitles = new String[] { "Variable", "Value" };
    for (int i = 0; i < statsTitles.length; i++) {
      final TableColumn column = new TableColumn(table, SWT.NONE);
      column.setText(statsTitles[i]);
    }
    final StatisticsDTO stats = statsTracker.getStatsDTO();
    final Field[] fields = stats.getClass().getFields();
    for (final Field f : fields) {
      final TableItem ti = new TableItem(table, 0);
      ti.setText(0, f.getName());
      try {
        ti.setText(1, f.get(stats).toString());
      } catch (final Exception e) {
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
    final String[] titles = new String[] { "Time", "Tardiness" };
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
        final StatisticsEvent se = (StatisticsEvent) e;
        if (eventList.getDisplay().isDisposed()) {
          return;
        }
        eventList.getDisplay().asyncExec(new Runnable() {
          @Override
          public void run() {
            final TableItem ti = new TableItem(eventList, 0);
            ti.setText(0, Long.toString(se.time));
            ti.setText(1, Long.toString(se.tardiness));
          }
        });
      }
    }, StatisticsEventType.PICKUP_TARDINESS,
        StatisticsEventType.DELIVERY_TARDINESS);
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
  public void tick(TimeLapse timeLapse) {}

  @Override
  public void afterTick(TimeLapse timeLapse) {

  }

  @Override
  public void render() {
    final StatisticsDTO stats = statsTracker.getStatsDTO();

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
          } catch (final Exception e) {
            statsTable.get().getItem(i).setText(1, e.getMessage());
          }
        }
      }
    });

  }
}
