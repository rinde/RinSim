package rinde.sim.examples.rwalk2;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;

/**
 * Example of the simple random agent with the use of simulation facilities. 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 */
public class RandomWalkAgent implements TickListener, RoadUser, SimulatorUser {

	protected RoadModel rs;
	protected RoadUser currentPackage;
	protected Queue<Point> path;
	protected RandomGenerator rnd;
	private SimulatorAPI simulator;
	private double speed;

	/**
	 * Create simple agent. 
	 * @param rnd generator to be removed
	 * @param speed default speed of object in graph units per millisecond
	 */
	public RandomWalkAgent(double speed) {
		this.speed = speed;
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		if (path == null || path.isEmpty()) {
			if (rs.containsObject(currentPackage)) {
				rs.removeObject(currentPackage);
			}
			
			Point destination = findRandomNode();
			currentPackage = new Package("dummy package", destination);
			simulator.register(currentPackage);
			path = new LinkedList<Point>(Graphs.shortestPathEuclidianDistance(rs.getGraph(), rs.getPosition(this), destination));
		} else {
			double distance = speed * timeStep;
			rs.followPath(this, path, distance);
		}

	}

	private Point findRandomNode() {
		List<Point> nodes = new ArrayList<Point>(rs.getNodes());
		return nodes.get(rnd.nextInt(nodes.size()));
	}

	@Override
	public void initRoadUser(RoadModel model) {
		rs = model;
		Point pos = findRandomNode();
		rs.addObjectAt(this, pos);
	}


	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
		this.rnd  = api.getRandomGenerator();
	}
}
