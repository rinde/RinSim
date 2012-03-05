package rinde.sim.core.model.communication;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.math.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import rinde.sim.core.TickListener;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.Model;
import rinde.sim.core.model.RoadUser;

/**
 * The communication model. The message is send at the end of a current tick.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class CommunicationModel implements Model<CommunicationUser>, TickListener, CommunicationAPI {

	protected static final Logger LOGGER = LoggerFactory.getLogger(CommunicationModel.class);
	
	protected final Set<CommunicationUser> users;
	
	protected List<Entry<CommunicationUser, Message>> sendQueue;

	protected RandomGenerator generator;
	
	public CommunicationModel(RandomGenerator generator) {
		if(generator == null) throw new IllegalArgumentException("generator cannot be null");
		users = new LinkedHashSet<CommunicationUser>();
		sendQueue = new LinkedList<Entry<CommunicationUser,Message>>();
		this.generator = generator;
	}
	
	/** 
	 * Register communication user {@link CommunicationUser}. Communication user is registered only 
	 * when it is also {@link RoadUser}. This is required as communication model depends on elements positions.
	 */
	@Override
	public boolean register(CommunicationUser element) {
		boolean result = users.add(element);
		if(! result) return false;
		// callback
		try {
			element.setCommunicationAPI(this);			
		} catch (Exception e) {
			// if you miss-behave you don't deserve to use our infrastructure :D
			LOGGER.warn("callback for the communication user failed. Unregistering", e);
			users.remove(element);
			return false;
		}
		return true;
	}
	
	@Override
	public boolean unregister(CommunicationUser element) {
		if(element == null) return false;
		List<Entry<CommunicationUser, Message>> toRemove = new LinkedList<Entry<CommunicationUser,Message>>();
		for (Entry<CommunicationUser, Message> e : sendQueue) {
			if(element.equals(e.getKey()) || element.equals(e.getValue().getSender())) {
				toRemove.add(e);
			}
		}
		sendQueue.removeAll(toRemove);
		
		return users.remove(element);
	}

	@Override
	public Class<CommunicationUser> getSupportedType() {
		return CommunicationUser.class;
	}

	@Override
	public void tick(long currentTime, long timeStep) {
		// empty implementation
	}

	@Override
	public void afterTick(long currentTime, long timeStep) {
		long timeMillis = System.currentTimeMillis();
		List<Entry<CommunicationUser, Message>> cache = sendQueue;
		sendQueue = new LinkedList<Entry<CommunicationUser,Message>>();
		for (Entry<CommunicationUser, Message> e : cache) {
			try {
				e.getKey().receive(e.getValue());		
				//TODO [bm] add msg delivered event
			} catch(Exception e1) {
				LOGGER.warn("unexpected exception while passing message", e1);
			}
		}
		if(LOGGER.isDebugEnabled()) {
			timeMillis = (System.currentTimeMillis() - timeMillis);
			LOGGER.debug("broadcast lasted for:" +  timeMillis);			
		}
	}


	@Override
	public void send(CommunicationUser recipient, Message message) {
		if(!users.contains(recipient)) {
			//TODO [bm] implement dropped message EVENT
			return;
		}
		
		if(new CanCommunicate(message.sender).apply(recipient)) {
			sendQueue.add(SimpleEntry.entry(recipient, message));
		} else {
			//TODO [bm] implement dropped message EVENT
			return;
		}
		
	}

	@Override
	public void broadcast(Message message) {
		broadcast(message, new CanCommunicate(message.sender));
	}

	@Override
	public void broadcast(Message message,
			Class<? extends CommunicationUser> type) {
		broadcast(message, new CanCommunicate(message.sender, type));
		
	}

	private void broadcast(Message message, Predicate<CommunicationUser> predicate) {
		if(! users.contains(message.sender)) return;
		HashSet<CommunicationUser> uSet = new HashSet<CommunicationUser>(users.size() / 2);
		
		for (CommunicationUser u : users) {
			if(predicate.apply(u))
				uSet.add(u);			
		}
		
		for (CommunicationUser u : uSet) {
			try {
				sendQueue.add(SimpleEntry.entry(u, message.clone()));
			} catch (CloneNotSupportedException e) {
				LOGGER.error("clonning exception for message", e);
			}
		}
	}

	/**
	 * Check if an message from a given sender can be deliver to recipient 
	 * @see CanCommunicate#apply(CommunicationUser)
	 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
	 * @since 2.0
	 */
	class CanCommunicate implements Predicate<CommunicationUser> {

		private Class<? extends CommunicationUser> clazz;
		private final CommunicationUser sender;
		private Rectangle rec;

		public CanCommunicate(CommunicationUser sender, Class<? extends CommunicationUser> clazz) {
			this.sender = sender;
			this.clazz = clazz;
			if(sender.getPosition() != null)
				rec = new Rectangle(sender.getPosition(), sender.getRadius());
		}
		
		public CanCommunicate(CommunicationUser sender) {
			this(sender, null);
		}
		
		@Override
		public boolean apply(CommunicationUser input) {
			if(input == null || rec == null) return false;
			if(clazz != null && !clazz.equals(input.getClass())) {
				return false;
			}
			if(input.equals(sender)) return false;
			final Point iPos = input.getPosition();
			if(!rec.contains(iPos)) {
				return false;
			}
			double prob = input.getReliability() * sender.getReliability();
			double minRadius = Math.min(input.getRadius(), sender.getRadius());
			double rand = generator.nextDouble();
			Point sPos = sender.getPosition();
			return Point.distance(sPos, iPos) <= minRadius && prob > rand;
		}
	}
	
	private static class Rectangle {
		private double y1;
		private double x1;
		private double y0;
		private double x0;

		public Rectangle(Point p, double radius) {
			this.x0 = p.x - radius;
			this.y0 = p.y - radius;
			this.x1 = p.x + radius;
			this.y1 = p.y + radius;
		}
		
		public boolean contains(Point p) {
			if(p == null) return false;
			if(p.x < x0 || p.x > x1) return false;
			if(p.y < y0 || p.y > y1) return false;
			return true;
		}
	}
	
	protected static class SimpleEntry<K,V> implements Entry<K,V> {

		private final V value;
		private final K key;

		public SimpleEntry(K key, V value) {
			this.key = key;
			this.value = value;
		}
		
		@Override
		public K getKey() {
			return key;
		}

		@Override
		public V getValue() {
			return value;
		}

		@Override
		public V setValue(V value) {
			return null;
		}
		
		static <V,K> Entry<V,K> entry(V v, K k) {
			return new SimpleEntry<V,K>(v,k);
		}
		
	}
	
}
