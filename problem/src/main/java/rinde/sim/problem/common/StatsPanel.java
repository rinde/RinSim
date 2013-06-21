/**
 * 
 */
package rinde.sim.problem.common;

import java.lang.reflect.Field;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import rinde.sim.core.model.time.TickListener;
import rinde.sim.core.model.time.TimeLapse;
import rinde.sim.event.Event;
import rinde.sim.event.Listener;
import rinde.sim.problem.common.StatsTracker.StatisticsDTO;
import rinde.sim.problem.common.StatsTracker.StatisticsEvent;
import rinde.sim.problem.common.StatsTracker.StatisticsEventType;
import rinde.sim.ui.renderers.PanelRenderer;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class StatsPanel implements PanelRenderer, TickListener {

	protected final StatsTracker statsTracker;

	protected Table statsTable;

	public StatsPanel(StatsTracker stats) {
		statsTracker = stats;
	}

	@Override
	public void initializePanel(Composite parent) {
		final FillLayout layout = new FillLayout();
		layout.marginWidth = 2;
		layout.marginHeight = 2;
		layout.type = SWT.VERTICAL;
		parent.setLayout(layout);

		statsTable = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
		statsTable.setHeaderVisible(true);
		statsTable.setLinesVisible(true);
		final String[] statsTitles = new String[] { "Variable", "Value" };
		for (int i = 0; i < statsTitles.length; i++) {
			final TableColumn column = new TableColumn(statsTable, SWT.NONE);
			column.setText(statsTitles[i]);
		}
		final StatisticsDTO stats = statsTracker.getStatsDTO();
		final Field[] fields = stats.getClass().getFields();
		for (final Field f : fields) {
			final TableItem ti = new TableItem(statsTable, 0);
			ti.setText(0, f.getName());
			try {
				ti.setText(1, f.get(stats).toString());
			} catch (final Exception e) {
				ti.setText(1, e.getMessage());
			}
		}
		for (int i = 0; i < statsTitles.length; i++) {
			statsTable.getColumn(i).pack();
		}

		final Table eventList = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL);
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
				eventList.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						final TableItem ti = new TableItem(eventList, 0);
						ti.setText(0, "" + se.time);
						ti.setText(1, "" + se.tardiness);

					}
				});

			}
		}, StatisticsEventType.PICKUP_TARDINESS, StatisticsEventType.DELIVERY_TARDINESS);
	}

	@Override
	public int preferredSize() {
		return 300;
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
		final StatisticsDTO stats = statsTracker.getStatsDTO();

		final Field[] fields = stats.getClass().getFields();
		if (statsTable.isDisposed()) {
			return;
		}
		statsTable.getDisplay().syncExec(new Runnable() {
			@Override
			public void run() {
				if (statsTable.isDisposed()) {
					return;
				}
				for (int i = 0; i < fields.length; i++) {
					try {
						statsTable.getItem(i).setText(1, fields[i].get(stats).toString());
					} catch (final Exception e) {
						statsTable.getItem(i).setText(1, e.getMessage());
					}
				}
			}
		});

	}
}
