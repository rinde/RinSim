package rinde.sim.core.graph;

import java.util.HashMap;

/**
 * MultiAttributeEdageData allows for storing varying data on the edges. There are two 
 * "extra" supported properties defined: edge's length, and maximum speed on the edge.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class MultiAttributeEdgeData implements EdgeData {

	public static final String KEY_LENGTH = "data.length";
	public static final String KEY_MAX_SPEED = "data.max.speed";
	
	private HashMap<String, Object> properties;
	
	public MultiAttributeEdgeData(double length) {
		properties = new HashMap<String, Object>();
		properties.put(KEY_LENGTH, length);
	}
	
	public MultiAttributeEdgeData(double length, double maxSpeed) {
		properties = new HashMap<String, Object>();
		properties.put(KEY_LENGTH, length);
		properties.put(KEY_MAX_SPEED, maxSpeed);
	}
	
	/**
	 * Returns edge's length. If the length is not specified the {@link Double#NaN} value is returned
	 * @see rinde.sim.core.graph.EdgeData#getLength()
	 */
	@Override
	public double getLength() {
		Object l = properties.get(KEY_LENGTH);
		if(l instanceof Double) {
			return (Double) l;
		}
		return Double.NaN;
	}
	
	/**
	 * Returns max speed defined for an edge. If the max speed is not specified the {@link Double#NaN} value is returned
	 * @see rinde.sim.core.graph.EdgeData#getLength()
	 */
	public double getMaxSpeed() {
		Object l = properties.get(KEY_MAX_SPEED);
		if(l instanceof Double) {
			return (Double) l;
		}
		return Double.NaN;
	}
	
	/**
	 * Set max speed. 
	 * @param maxSpeed
	 * @return old max speed or {@link Double#NaN}.
	 */
	public double setMaxSpeed(double maxSpeed) {
		Object l = properties.put(KEY_MAX_SPEED, maxSpeed);
		if(l instanceof Double) {
			return (Double) l;
		}
		return Double.NaN;
	}
	
	/** 
	 * Add property
	 * @param key
	 * @param value
	 */
	public <E> void put(String key, E value) {
		properties.put(key, value);
	}
	
	@SuppressWarnings("unchecked")
	public <E> E get(String key, Class<E> type) {
		Object r = properties.get(key);
		if(r != null && type.isAssignableFrom(r.getClass())) {
			return (E) r;
		}
		return null;
	}
	
	@Override
	public int hashCode() {
		return properties.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof MultiAttributeEdgeData)) return false;
		
		MultiAttributeEdgeData other = (MultiAttributeEdgeData) obj;
		return properties.equals(other.properties);
	}

	/**
	 * represents a empty value for purpose of {@link TableGraph}
	 */
	public static final MultiAttributeEdgeData EMPTY = new MultiAttributeEdgeData(0);
	static {EMPTY.properties.clear();}

}
