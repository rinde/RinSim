package rinde.sim.core.graph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ConnectionData} implementation which allows to associate multiple
 * attributes to a connection (through a {@link HashMap}). There are two
 * "default" supported properties defined: connection length, and maximum speed
 * on a connection.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * @since 2.0
 */
public class MultiAttributeData implements ConnectionData {

	/**
	 * Key for length of a connection.
	 */
	public static final String KEY_LENGTH = "data.length";

	/**
	 * Key for maximum speed of a connection.
	 */
	public static final String KEY_MAX_SPEED = "data.max.speed";

	private final HashMap<String, Object> attributes;

	/**
	 * New instance only using a single attribute: length.
	 * @param length The length to set.
	 */
	public MultiAttributeData(double length) {
		attributes = new HashMap<String, Object>();
		attributes.put(KEY_LENGTH, length);
	}

	/**
	 * New instance using both length and maximum speed attributes.
	 * @param length The length of the connection.
	 * @param maxSpeed The maximum speed for the connection.
	 */
	public MultiAttributeData(double length, double maxSpeed) {
		attributes = new HashMap<String, Object>();
		attributes.put(KEY_LENGTH, length);
		attributes.put(KEY_MAX_SPEED, maxSpeed);
	}

	/**
	 * Returns edge's length. If the length is not specified the
	 * {@link Double#NaN} value is returned
	 * @see rinde.sim.core.graph.ConnectionData#getLength()
	 */
	@Override
	public double getLength() {
		Object l = attributes.get(KEY_LENGTH);
		if (l instanceof Double) {
			return (Double) l;
		}
		return Double.NaN;
	}

	/**
	 * Returns max speed defined for an edge. If the max speed is not specified
	 * the {@link Double#NaN} value is returned
	 * @return The max speed.
	 * @see rinde.sim.core.graph.ConnectionData#getLength()
	 */
	public double getMaxSpeed() {
		Object l = attributes.get(KEY_MAX_SPEED);
		if (l instanceof Double) {
			return (Double) l;
		}
		return Double.NaN;
	}

	/**
	 * Set max speed.
	 * @param maxSpeed The new speed.
	 * @return old max speed or {@link Double#NaN}.
	 */
	public double setMaxSpeed(double maxSpeed) {
		Object l = attributes.put(KEY_MAX_SPEED, maxSpeed);
		if (l instanceof Double) {
			return (Double) l;
		}
		return Double.NaN;
	}

	/**
	 * Add an attribute. Note: this can override existing attributes.
	 * @param key A string used as key.
	 * @param value The value associated with <code>key</code>.
	 */
	public <E> void put(String key, E value) {
		attributes.put(key, value);
	}

	/**
	 * Retrieve an attribute.
	 * @param key The key to use.
	 * @param type The type of object that needs to be retrieved.
	 * @return An object associated to the key or <code>null</code> if it does
	 *         not exist.
	 */
	@SuppressWarnings("unchecked")
	public <E> E get(String key, Class<E> type) {
		Object r = attributes.get(key);
		if (r != null && type.isAssignableFrom(r.getClass())) {
			return (E) r;
		}
		return null;
	}

	/**
	 * @return Unmodifiable view on the attributes.
	 */
	public Map<String, Object> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

	@Override
	public int hashCode() {
		return attributes.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof MultiAttributeData)) {
			return false;
		}

		MultiAttributeData other = (MultiAttributeData) obj;
		return attributes.equals(other.getAttributes());
	}

	/**
	 * represents an empty value for purpose of {@link TableGraph}
	 */
	public static final MultiAttributeData EMPTY = new MultiAttributeData(0);
	static {
		EMPTY.attributes.clear();
	}

}
