package rinde.sim.core.model.communication;

import rinde.sim.core.graph.Point;

/**
 * Defines the interface  of the agent that wants to communicate.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 */
public interface CommunicationUser {
	
	/**
	 * Get position. The position is required to determine the entities you can communicate with  
	 * @return positing on the communication user
	 */
	Point getPosition();
	
	/**
	 * Get the distance in which you want to communicate. 
	 * @return
	 */
	double getRadius();
	
	/**
	 * Get the connection reliability. This is probability (0,1] that the message is send/received.
	 * When two entities communicate the probability of message delivery is a product of their reliability.  
	 * @return
	 */
	double getReliability();
}
