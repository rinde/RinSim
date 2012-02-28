package rinde.sim.examples.rwalk4;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Graphs;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.MovingRoadUser;
import rinde.sim.core.model.RoadModel;
import rinde.sim.core.model.RoadUser;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;

import rinde.sim.example.rwalk.common.Package;

/**
 * Example of the simple random agent with the use of simulation facilities. 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 */
class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser, CommunicationUser {

	private static final int MAX_MSGs = 100;
	private static final int COMMUNICATION_PERIOD = 100000; //100s
	protected RoadModel rs;
	protected RoadUser currentPackage;
	protected Queue<Point> path;
	protected RandomGenerator rnd;
	private SimulatorAPI simulator;
	private double speed;
	private CommunicationAPI cm;
	private int radius;
	
	private Queue<RandomWalkAgent> communicatedWith;
	private Mailbox mailbox;
	private int maxSize;
	
	private ReentrantLock lock;
	
	private int communications; 
	
	private long lastCommunication;

	/**
	 * Create simple agent. 
	 * @param speed default speed of object in graph units per millisecond
	 * @param radius in which it can communicate
	 */
	public RandomWalkAgent(double speed, int radius) {
		this.speed = speed;
		this.radius = radius;
		communicatedWith = new LinkedList<RandomWalkAgent>();
		mailbox = new Mailbox();
		maxSize = 1; 
		lock = new ReentrantLock();
		communications = 0;
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		checkMsgs();			
		
		if (path == null || path.isEmpty()) {
			if (rs.containsObject(currentPackage)) {
				simulator.unregister(currentPackage);
			}
			if(communications > MAX_MSGs) {
				simulator.unregister(this);
				return;
			}
			Point destination = rs.getGraph().getRandomNode(rnd);
			currentPackage = new Package("dummy package", destination);
			simulator.register(currentPackage);
			path = new LinkedList<Point>(Graphs.shortestPathEuclidianDistance(rs.getGraph(), rs.getPosition(this), destination));
		} else {
			rs.followPath(this, path, timeStep);
		}
		
		sendMsgs(currentTime);
	}

	private void sendMsgs(long currentTime) {
		double percent = rnd.nextDouble();
		if(percent < 0.1 && lastCommunication + COMMUNICATION_PERIOD < currentTime) {
			lastCommunication = currentTime;
			if(cm != null)
			cm.broadcast(new Message(this) {});			
		}
	}

	private void checkMsgs() {
		Queue<Message> messages = mailbox.getMessages();
		lock.lock();
		for (Message m : messages) {
			if(communicatedWith.size() >= maxSize) {
				communicatedWith.poll();
			}
			communications++;
			communicatedWith.offer((RandomWalkAgent) m.getSender());
		}
		lock.unlock();
	}

	public Set<RandomWalkAgent> getCommunicatedWith() {
		lock.lock();
		HashSet<RandomWalkAgent> result = new HashSet<RandomWalkAgent>(communicatedWith);
		lock.unlock();
		return result;
	}

	@Override
	public void initRoadUser(RoadModel model) {
		rs = model;
		Point pos = rs.getGraph().getRandomNode(rnd);
		rs.addObjectAt(this, pos);
	}


	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
		this.rnd  = api.getRandomGenerator();
	}

	@Override
	public double getSpeed() {
		return speed;
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		// empty by default
	}

	@Override
	public void setCommunicationAPI(CommunicationAPI api) {
		cm = api;
	}

	@Override
	public Point getPosition() {
		return rs.containsObject(this) ? rs.getPosition(this) : null; 
	}

	@Override
	public double getRadius() {
		return radius;
	}

	@Override
	public double getReliability() {
		return 1;
	}

	@Override
	public void receive(Message message) {
		mailbox.receive(message);
	}
}
