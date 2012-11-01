/**
 * 
 */
package rinde.sim.problem.fabrirecht;

import java.util.Collection;
import java.util.Set;

import rinde.sim.core.graph.Point;
import rinde.sim.problem.common.DynamicPDPTWScenario;
import rinde.sim.problem.common.VehicleDTO;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class FabriRechtScenario extends DynamicPDPTWScenario {
	private static final long serialVersionUID = 8654500529284785728L;
	public final Point min;
	public final Point max;
	public final TimeWindow timeWindow;
	public final VehicleDTO defaultVehicle;

	// empty scenario
	public FabriRechtScenario(Point pMin, Point pMax, TimeWindow pTimeWindow, VehicleDTO pDefaultVehicle) {
		super();
		min = pMin;
		max = pMax;
		timeWindow = pTimeWindow;
		defaultVehicle = pDefaultVehicle;
	}

	/**
	 * @param pEvents
	 * @param pSupportedTypes
	 */
	public FabriRechtScenario(Collection<? extends TimedEvent> pEvents, Set<Enum<?>> pSupportedTypes, Point pMin,
			Point pMax, TimeWindow pTimeWindow, VehicleDTO pDefaultVehicle) {
		super(pEvents, pSupportedTypes);
		min = pMin;
		max = pMax;
		timeWindow = pTimeWindow;
		defaultVehicle = pDefaultVehicle;
	}

	@Override
	public Point getMin() {
		return min;
	}

	@Override
	public Point getMax() {
		return max;
	}

	@Override
	public TimeWindow getTimeWindow() {
		return timeWindow;
	}

	@Override
	public long getTickSize() {
		return 1L;
	}

}
