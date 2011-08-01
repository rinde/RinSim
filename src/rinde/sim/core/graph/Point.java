/**
 * 
 */
package rinde.sim.core.graph;

import java.io.Serializable;
import java.util.List;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class Point implements Serializable {

	private static final long serialVersionUID = -7501053764573661924L;
	public final double x;
	public final double y;

	public Point(double x, double y) {
		this.x = x;
		this.y = y;
	}

	public static double distance(Point p1, Point p2) {
		double dx = p1.x - p2.x;
		double dy = p1.y - p2.y;
		return Math.sqrt(dx * dx + dy * dy);
	}

	public static double length(List<Point> path) {
		double length = 0;
		for (int i = 1; i < path.size(); i++) {
			length += Point.distance(path.get(i - 1), path.get(i));
		}
		return length;
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
		return new Double(x).hashCode() ^ new Double(y).hashCode();
	}

	@Override
	public boolean equals(Object p) {
		if (p instanceof Point) {
			return ((Point) p).x == x && ((Point) p).y == y;
		}
		return false;
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + ")";
	}

}
