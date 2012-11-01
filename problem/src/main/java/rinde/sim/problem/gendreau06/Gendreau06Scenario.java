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
		return 1;
	}
}
