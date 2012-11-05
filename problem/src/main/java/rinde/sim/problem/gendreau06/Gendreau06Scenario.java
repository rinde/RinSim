/**
 * 
 */
package rinde.sim.problem.gendreau06;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import rinde.sim.core.graph.Point;
import rinde.sim.problem.common.DynamicPDPTWScenario;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

/**
 * 
 * The length of the scenario is a soft constraint. There is a pre defined
 * length of the day (either 4 hours or 7.5 hours), vehicles are allowed to
 * continue driving after the end of the day.
 * 
 * Once a vehicle is moving towards a Parcel it is obliged to service it. This
 * means that diversion is not allowed.
 * 
 * Distance is expressed in km, time is expressed in ms (the original format is
 * in seconds, however it allows fractions as such it was translated to ms),
 * speed is expressed as km/h.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Gendreau06Scenario extends DynamicPDPTWScenario {
	private static final long serialVersionUID = 1386559671732721432L;

	protected Gendreau06Scenario(Collection<? extends TimedEvent> pEvents, Set<Enum<?>> pSupportedTypes) {
		super(pEvents, pSupportedTypes);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
	}

	@Override
	public Point getMin() {
		return new Point(0, 0);
	}

	@Override
	public Point getMax() {
		return new Point(5, 5);
	}

	@Override
	public TimeWindow getTimeWindow() {
		return TimeWindow.ALWAYS;
	}

	@Override
	public long getTickSize() {
		return 1000;
	}

	@Override
	public double getMaxSpeed() {
		return 30.0;
	}
}
