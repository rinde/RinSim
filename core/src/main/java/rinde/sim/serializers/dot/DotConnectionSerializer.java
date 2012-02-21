package rinde.sim.serializers.dot;

import rinde.sim.core.graph.Connection;
import rinde.sim.core.graph.EdgeData;

/**
 * Used to serialize graphs
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 *
 * @param <E>
 * @since 2.0
 */
public abstract class DotConnectionSerializer<E extends EdgeData> {
	public abstract String serializeConnection(String idFrom, String idTo, Connection<E> conn);
	public abstract E deserialize(String connection);
}
