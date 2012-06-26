package rinde.sim.core.graph;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Class representing a connection in a graph.
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @param <E> Type of {@link EdgeData} that is used. This data object can be
 *            used to add additional information to the connection.
 * @since 2.0
 */
public class Connection<E extends EdgeData> {
	public final Point from;
	public final Point to;
	private E edgeData;

	private final int hashCode;

	public Connection(Point pFrom, Point pTo, E pEdgeData) {
		if (pFrom == null || pTo == null) {
			throw new IllegalArgumentException("points cannot be null");
		}
		this.from = pFrom;
		this.to = pTo;
		this.edgeData = pEdgeData;

		HashCodeBuilder builder = new HashCodeBuilder(13, 17).append(pFrom).append(pTo);
		if (pEdgeData != null) {
			builder.append(pEdgeData);
		}
		hashCode = builder.toHashCode();
	}

	public void setEdgeData(E pEdgeData) {
		this.edgeData = pEdgeData;
	}

	public E getEdgeData() {
		return edgeData;
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Connection)) {
			return false;
		}
		@SuppressWarnings("rawtypes")
		Connection other = (Connection) obj;
		if (!from.equals(other.from)) {
			return false;
		}
		if (!to.equals(other.to)) {
			return false;
		}
		if (edgeData == null) {
			return other.edgeData == null;
		}
		return edgeData.equals(other.getEdgeData());
	}

	@Override
	public String toString() {
		return new StringBuilder(7).append('[').append(from).append("->").append(to).append('[').append(edgeData)
				.append("]]").toString();
	}

}
