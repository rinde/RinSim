package rinde.sim.core.model.communication;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.commons.math.random.RandomGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

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
	
	protected Multimap<CommunicationUser, Message> sendQueue;

	protected RandomGenerator generator;
	
	
	public CommunicationModel(RandomGenerator generator) {
		if(generator == null) throw new IllegalArgumentException("generator cannot be null");
		users = new LinkedHashSet<CommunicationUser>();
		sendQueue = LinkedHashMultimap.create();
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
		sendQueue.removeAll(element);
		Collection<Message> values = sendQueue.values();
		Collection<Message> toRemove = new LinkedList<Message>();
		for (Message m : values) {
			if(m.sender.equals(element)) toRemove.add(m);
		}
		values.removeAll(toRemove);
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
		Multimap<CommunicationUser, Message> cache = sendQueue;
		sendQueue = LinkedHashMultimap.create();
		for (CommunicationUser u : cache.keySet()) {
			Collection<Message> mesgs = cache.get(u);
			for (Message m : mesgs) {
				try {
					u.receive(m);		
					//TODO [bm] add msg delivered event
				} catch(Exception e) {
					LOGGER.warn("unexpected exception while passing message", e);
				}
			}
		}
	}


	@Override
	public void send(CommunicationUser recipient, Message message) {
		if(!users.contains(recipient)) {
			//TODO [bm] implement dropped message EVENT
			return;
		}
		
		if(new CanCommunicate(message.sender).apply(recipient)) {
			sendQueue.put(recipient, message);
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
		HashSet<CommunicationUser> uSet = new HashSet<CommunicationUser>(Collections2.filter(users, predicate));
		for (CommunicationUser u : uSet) {
			try {
				sendQueue.put(u, message.clone());
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

		public CanCommunicate(CommunicationUser sender, Class<? extends CommunicationUser> clazz) {
			this.sender = sender;
			this.clazz = clazz;
		}
		
		public CanCommunicate(CommunicationUser sender) {
			this(sender, null);
		}
		
		@Override
		public boolean apply(CommunicationUser input) {
			if(input == null) return false;
			if(clazz != null && !clazz.equals(input.getClass())) {
				return false;
			}
			if(input.equals(sender)) return false;
			
			double prob = input.getReliability() * sender.getReliability();
			double minRadius = Math.min(input.getRadius(), sender.getRadius());
			double rand = generator.nextDouble();
			return Point.distance(sender.getPosition(), input.getPosition()) <= minRadius && prob > rand;
		}
		
	}
	
}
