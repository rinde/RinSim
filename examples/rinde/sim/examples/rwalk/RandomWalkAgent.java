package rinde.sim.examples.rwalk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.RoadModel;
import rinde.sim.core.RoadUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class RandomWalkAgent implements TickListener, RoadUser {

	protected final RoadModel rs;
	protected RoadUser currentPackage;
	protected Queue<Point> path;
	protected final RandomGenerator rnd;

	public RandomWalkAgent(RoadModel rs, RandomGenerator rnd) {
		this.rs = rs;
		this.rnd = rnd;
		currentPackage = new Package("dummy package " + toString());
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		if (path == null || path.isEmpty()) {
			if (rs.containsObject(currentPackage)) {
				rs.removeObject(currentPackage);
			}
			Point destination = findRandomNode();
			rs.addObjectAt(currentPackage, destination);
			path = new LinkedList<Point>(Graphs.shortestPathDistance(rs.getGraph(), rs.getPosition(this), destination));
		} else {
			// follow current path
			rs.followPath(this, path, 5);
		}

	}

	private Point findRandomNode() {
		List<Point> nodes = new ArrayList<Point>(rs.getNodes());
		return nodes.get(rnd.nextInt(nodes.size()));
	}

	private class Package implements RoadUser {
		public final String name;

		public Package(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

}
