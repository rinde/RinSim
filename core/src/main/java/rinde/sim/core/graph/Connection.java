package rinde.sim.core.graph;

import org.apache.commons.lang.builder.HashCodeBuilder;

import rinde.sim.core.graph.Point;

/**
 * Class representing a connection in a graph. 
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class Connection<E extends EdgeData> {
	public final Point from;
	public final Point to;
	public E edgeData;
	
	public void setEdgeData(E edgeData) {
		this.edgeData = edgeData;
	}

	private int hashCode;
	
	public Connection(Point from, Point to, E edgeData) {
		if(from == null || to == null) throw new IllegalArgumentException("points cannot be null");
		this.from = from;
		this.to = to;
		this.edgeData = edgeData;
		
		HashCodeBuilder builder = new HashCodeBuilder(13, 17).append(from).append(to);
		if(edgeData != null) {
			builder.append(edgeData);
		}
		hashCode = builder.toHashCode();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if(!(obj instanceof Connection)) return false;
		@SuppressWarnings("rawtypes")
		Connection other = (Connection) obj;
		if(!from.equals(other.from)) return false;
		if(!to.equals(other.to)) return false;
		if(edgeData == null) return other.edgeData == null;
		
		return edgeData.equals(other.edgeData);
	}
	
}
