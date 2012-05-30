package rinde.sim.core.graph;

/**
 * Simple data representing an length of the edge
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * 
 */
public class LengthEdgeData implements EdgeData {

	private final double length;

	public LengthEdgeData(double pLength) {
		length = pLength;
	}

	@Override
	public double getLength() {
		return length;
	}

	@Override
	public int hashCode() {
		return Double.valueOf(length).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LengthEdgeData) {
			return Double.compare(length, ((LengthEdgeData) obj).length) == 0;
		}
		return false;
	}

	@Override
	public String toString() {
		return length + "";
	}

	/**
	 * represents an empty value for purpose of {@link TableGraph}
	 */
	public static final LengthEdgeData EMPTY = new LengthEdgeData(Double.NaN);
}
