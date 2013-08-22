/**
 * 
 */
package rinde.sim.pdptw.common;

import java.io.Serializable;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class VehicleDTO implements Serializable {

  private static final long serialVersionUID = -631074532620454156L;
  public final Point startPosition;
  public final double speed;
  public final int capacity;
  public final TimeWindow availabilityTimeWindow;

  public VehicleDTO(Point pStartPosition, double pSpeed, int pCapacity,
      TimeWindow pAvailabilityTimeWindow) {
    startPosition = pStartPosition;
    speed = pSpeed;
    capacity = pCapacity;
    availabilityTimeWindow = pAvailabilityTimeWindow;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("VehicleDTO{startPosition:");
    sb.append(startPosition);
    sb.append(",speed:");
    sb.append(speed);
    sb.append(",capacity:");
    sb.append(capacity);
    sb.append(",availabilityTimeWindow:");
    sb.append(availabilityTimeWindow);
    sb.append("}");
    return sb.toString();
  }

}
