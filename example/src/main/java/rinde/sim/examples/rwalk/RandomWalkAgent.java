package rinde.sim.examples.rwalk;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class RandomWalkAgent implements TickListener, RoadUser {

	protected RoadModel rs;
	protected RoadUser currentPackage;
	protected Queue<Point> path;
	protected final RandomGenerator rnd;

	public RandomWalkAgent(RandomGenerator rnd) {
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
			path = new LinkedList<Point>(Graphs.shortestPathEuclidianDistance(rs.getGraph(), rs.getPosition(this), destination));
		} else {
			// follow current path
//			double
			rs.followPath(this, path, 100);
		}

	}

	private Point findRandomNode() {
		List<Point> nodes = new ArrayList<Point>(rs.getNodes());
		return nodes.get(rnd.nextInt(nodes.size()));
	}

	@Override
	public void initRoadUser(RoadModel model) {
		rs = model;
		List<Point> nodes = new ArrayList<Point>(rs.getNodes());
		Point pos = nodes.get(rnd.nextInt(nodes.size()));
		rs.addObjectAt(this, pos);
	}

	

}
