package rinde.sim.examples.rwalk;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import rinde.sim.core.PathFinder;
import rinde.sim.core.Point;
import rinde.sim.core.RoadStructure;
import rinde.sim.core.TickListener;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class RandomWalkAgent implements TickListener {

	protected final RoadStructure rs;
	protected String currentPackage;
	protected Queue<Point> path;
	protected final Random rnd;

	public RandomWalkAgent(RoadStructure rs, Random rnd) {
		this.rs = rs;
		this.rnd = rnd;
		currentPackage = "dummy package " + toString();
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		if (path == null || path.isEmpty()) {
			if (rs.containsObject(currentPackage)) {
				rs.removeObject(currentPackage);
			}
			Point destination = findRandomNode();
			rs.addObjectAt(currentPackage, destination);
			path = new LinkedList<Point>(PathFinder.shortestDistance(rs.getGraph(), rs.getPosition(this), destination));
		} else {
			// follow current path
			rs.followPath(this, path, 5);
		}
	}

	private Point findRandomNode() {
		List<Point> nodes = rs.getNodes();
		return nodes.get(rnd.nextInt(nodes.size()));
	}

}
