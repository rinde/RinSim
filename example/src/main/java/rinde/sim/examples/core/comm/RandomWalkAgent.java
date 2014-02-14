package rinde.sim.examples.core.comm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.SimulatorAPI;
import rinde.sim.core.SimulatorUser;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.communication.CommunicationAPI;
import rinde.sim.core.model.communication.CommunicationUser;
import rinde.sim.core.model.communication.Mailbox;
import rinde.sim.core.model.communication.Message;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.core.model.road.RoadUser;
import rinde.sim.examples.common.Package;

/**
 * Example of the simple random agent with the use of simulation facilities.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 */
class RandomWalkAgent implements TickListener, MovingRoadUser, SimulatorUser,
    CommunicationUser {

  public static final String C_BLACK = "color.black";
  public static final String C_YELLOW = "color.yellow";
  public static final String C_GREEN = "color.green";

  private static final int MAX_MSGs = 20;
  private static final int COMMUNICATION_PERIOD = 10000; // 10s
  protected RoadModel rs;
  protected RoadUser currentPackage;
  protected Queue<Point> path;
  protected RandomGenerator rnd;
  private SimulatorAPI simulator;
  private final double speed;
  private CommunicationAPI cm;
  private final int radius;

  HashMap<RandomWalkAgent, Long> lastCommunicationTime;

  private Set<RandomWalkAgent> communicatedWith;
  private final Mailbox mailbox;
  private final int maxSize;

  private final ReentrantLock lock;

  private int communications;

  private long lastCommunication;
  private final double reliability;

  /**
   * Create simple agent.
   * @param speed default speed of object in graph units per millisecond
   * @param radius in which it can communicate
   */
  public RandomWalkAgent(double speed, int radius, double reliability) {
    this.speed = speed;
    this.radius = radius;
    communicatedWith = new HashSet<RandomWalkAgent>();
    lastCommunicationTime = new HashMap<RandomWalkAgent, Long>();
    mailbox = new Mailbox();
    maxSize = 1;
    lock = new ReentrantLock();
    communications = 0;
    this.reliability = reliability;
  }

  @Override
  public void tick(TimeLapse timeLapse) {
    checkMsgs(timeLapse.getTime());
    refreshList(timeLapse.getTime());

    if (path == null || path.isEmpty()) {
      if (currentPackage != null && rs.containsObject(currentPackage)) {
        simulator.unregister(currentPackage);
      }
      if (communications > MAX_MSGs) {
        simulator.unregister(this);
        return;
      }
      Point destination = rs.getRandomPosition(rnd);
      currentPackage = new Package("dummy package", destination);
      simulator.register(currentPackage);
      path = new LinkedList<Point>(rs.getShortestPathTo(this, destination));
    } else {
      rs.followPath(this, path, timeLapse);
    }

    sendMsgs(timeLapse.getStartTime());
  }

  private void refreshList(long currentTime) {
    if (lastCommunication + COMMUNICATION_PERIOD < currentTime) {
      lock.lock();
      communicatedWith = new HashSet<RandomWalkAgent>();
      for (Entry<RandomWalkAgent, Long> e : lastCommunicationTime.entrySet()) {
        if (e.getValue() + COMMUNICATION_PERIOD * 100 >= currentTime) {
          communicatedWith.add(e.getKey());
        }
      }
      lock.unlock();
    }
  }

  private void sendMsgs(long currentTime) {
    if (lastCommunication + COMMUNICATION_PERIOD < currentTime) {
      lastCommunication = currentTime;
      if (cm != null) {
        cm.broadcast(new Message(this) {});
      }
    }
  }

  private void checkMsgs(long currentTime) {
    Queue<Message> messages = mailbox.getMessages();

    for (Message m : messages) {
      lastCommunicationTime.put((RandomWalkAgent) m.getSender(), currentTime);
      communications++;
    }
  }

  public Set<RandomWalkAgent> getCommunicatedWith() {
    lock.lock();
    HashSet<RandomWalkAgent> result = new HashSet<RandomWalkAgent>(
        communicatedWith);
    lock.unlock();
    return result;
  }

  @Override
  public void initRoadUser(RoadModel model) {
    rs = model;
    Point pos = rs.getRandomPosition(rnd);
    rs.addObjectAt(this, pos);
  }

  @Override
  public void setSimulator(SimulatorAPI api) {
    simulator = api;
    rnd = api.getRandomGenerator();
  }

  @Override
  public double getSpeed() {
    return speed;
  }

  @Override
  public void afterTick(TimeLapse timeLapse) {
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
    return reliability;
  }

  @Override
  public void receive(Message message) {
    mailbox.receive(message);
  }

  public int getNoReceived() {
    return communications;
  }

}
