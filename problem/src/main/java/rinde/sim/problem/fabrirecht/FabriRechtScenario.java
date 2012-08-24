/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import java.util.Collection;
import java.util.Set;

import rinde.sim.core.graph.Point;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class FabriRechtScenario extends Scenario {
	private static final long serialVersionUID = -855531034069220296L;
	public final Point min;
	public final Point max;
	public final TimeWindow timeWindow;

	// empty scenario
	public FabriRechtScenario(Point pMin, Point pMax, TimeWindow pTimeWindow) {
		super();
		min = pMin;
		max = pMax;
		timeWindow = pTimeWindow;
	}

	/**
	 * @param pEvents
	 * @param pSupportedTypes
	 */
	public FabriRechtScenario(Collection<? extends TimedEvent> pEvents, Set<Enum<?>> pSupportedTypes, Point pMin,
			Point pMax, TimeWindow pTimeWindow) {
		super(pEvents, pSupportedTypes);
		min = pMin;
		max = pMax;
		timeWindow = pTimeWindow;
	}

}
