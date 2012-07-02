/**
 * 
 */
package rinde.sim.core.graph;

import java.io.Serializable;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Point represents a position in euclidean space, it is defined as a simple
 * tuple (double,double).
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
public class Point implements Serializable {

    private static final long serialVersionUID = -7501053764573661924L;
    /**
     * The x coordinate.
     */
    public final double x;

    /**
     * The y coordinate.
     */
    public final double y;

    private final int hashCode;

    /**
     * Create a new point.
     * @param pX The x coordinate.
     * @param pY The y coordinate.
     */
    public Point(double pX, double pY) {
        x = pX;
        y = pY;
        hashCode = new HashCodeBuilder(17, 37).append(x).append(y).toHashCode();
    }

    /**
     * Computes the distance between two points.
     * @param p1 A point.
     * @param p2 Another point.
     * @return The distance between the two points.
     */
    public static double distance(Point p1, Point p2) {
        final double dx = p1.x - p2.x;
        final double dy = p1.y - p2.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Computes the difference between two points: <code>p1 - p2</code>.
     * @param p1 A point.
     * @param p2 Another point.
     * @return The difference as a point object.
     */
    public static Point diff(Point p1, Point p2) {
        return new Point(p1.x - p2.x, p1.y - p2.y);
    }

    /**
     * Parses the specified string to a point. Example inputs:
     * <ul>
     * <li> <code>(10.0,2)</code></li>
     * <li> <code>5,6</code></li>
     * <li> <code>(7.3242349832,0</code></li>
     * </ul>
     * @param pointString The string to parse.
     * @return A point.
     */
    public static Point parsePoint(String pointString) {
        final String[] parts = pointString.replaceAll("\\(|\\)", "").split(",");
        return new Point(Double.parseDouble(parts[0]),
                Double.parseDouble(parts[1]));
    }

    /**
     * Duplicates the specified point.
     * @param p The point to duplicate.
     * @return A duplicate of p.
     */
    public static Point duplicate(Point p) {
        return new Point(p.x, p.y);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Standard equality method.
     * @param p The point to compare with.
     * @return <code>true</code> when equal, <code>false</code> otherwise.
     */
    public boolean equals(Point p) {
        if (p == null) {
            return false;
        }
        return x == p.x && y == p.y;
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
