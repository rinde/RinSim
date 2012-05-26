/**
 * 
 */
package rinde.sim.core.graph;

import java.io.Serializable;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Point implements Serializable {

	private static final long serialVersionUID = -7501053764573661924L;
	public final double x;
	public final double y;

	private final int hashCode;

	public Point(double pX, double pY) {
		x = pX;
		y = pY;
		hashCode = new HashCodeBuilder(17, 37).append(x + "," + y).toHashCode();
	}

	public static double distance(Point p1, Point p2) {
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public static Point diff(Point p1, Point p2) {
		return new Point(p1.x - p2.x, p1.y - p2.y);
	}

	public static Point parsePoint(String pointString) {
		String[] parts = pointString.replaceAll("\\(|\\)", "").split(",");
		return new Point(Double.parseDouble(parts[0]), Double.parseDouble(parts[1]));
	}

	public static Point duplicate(Point p) {
		return new Point(p.x, p.y);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	public boolean equals(Point p) {
		if (p == null) {
			return false;
		}
		return x == p.x && y == p.y; // hashCode() == p.hashCode();
	}

	@Override
	public boolean equals(Object p) {
		if (p instanceof Point) {
			return equals((Point) p);
		}
		return false;
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}

}
