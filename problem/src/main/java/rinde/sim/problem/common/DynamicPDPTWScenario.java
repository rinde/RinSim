/**
 * 
 */
package rinde.sim.problem.common;

import java.util.Collection;
import java.util.Set;

import rinde.sim.core.graph.Point;
import rinde.sim.problem.common.DynamicPDPTWProblem.StopCondition;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public abstract class DynamicPDPTWScenario extends Scenario {
	private static final long serialVersionUID = 7258024865764689371L;

	public DynamicPDPTWScenario() {
		super();
	}

	public DynamicPDPTWScenario(Collection<? extends TimedEvent> events, Set<Enum<?>> supportedTypes) {
		super(events, supportedTypes);
	}

	public abstract Point getMin();

	public abstract Point getMax();

	public abstract TimeWindow getTimeWindow();

	public abstract long getTickSize();

	public abstract double getMaxSpeed();

	public abstract StopCondition getStopCondition();

	public abstract boolean useSpeedConversion();

}
