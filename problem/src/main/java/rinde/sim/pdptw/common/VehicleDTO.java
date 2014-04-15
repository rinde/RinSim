/**
 * 
 */
package rinde.sim.pdptw.common;

import java.io.Serializable;

import rinde.sim.core.graph.Point;
import rinde.sim.util.TimeWindow;

/**
 * An immutable value object representing a vehicle in a pickup-and-delivery
 * problem.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class VehicleDTO implements Serializable {
  private static final long serialVersionUID = -631074532620454156L;

  /**
   * The start position of the vehicle.
   */
  public final Point startPosition;

  /**
   * The maximum speed of the vehicle.
   */
  public final double speed;

  /**
   * The maximum capacity of the vehicle.
   */
  public final int capacity;

  /**
   * The time window in which this vehicle is available.
   */
  public final TimeWindow availabilityTimeWindow;

  /**
   * Create a new vehicle value object.
   * @param pStartPosition {@link #startPosition}.
   * @param pSpeed {@link #speed}.
   * @param pCapacity {@link #capacity}.
   * @param pAvailabilityTimeWindow {@link #availabilityTimeWindow}.
   * @deprecated Use {@link #builder()} instead.
   */
  @Deprecated
  public VehicleDTO(Point pStartPosition, double pSpeed, int pCapacity,
      TimeWindow pAvailabilityTimeWindow) {
    startPosition = pStartPosition;
    speed = pSpeed;
    capacity = pCapacity;
    availabilityTimeWindow = pAvailabilityTimeWindow;
  }

  VehicleDTO(Builder b) {
    startPosition = b.startPosition;
    speed = b.speed;
    capacity = b.capacity;
    availabilityTimeWindow = b.availabilityTimeWindow;
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

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    Point startPosition;
    double speed;
    int capacity;
    TimeWindow availabilityTimeWindow;

    Builder() {
      startPosition = new Point(0, 0);
      speed = 50;
      capacity = 1;
      availabilityTimeWindow = TimeWindow.ALWAYS;
    }

    public Builder startPosition(Point point) {
      startPosition = point;
      return this;
    }

    public Builder speed(double s) {
      speed = s;
      return this;
    }

    public Builder capacity(int c) {
      capacity = c;
      return this;
    }

    public Builder availabilityTimeWindow(TimeWindow tw) {
      availabilityTimeWindow = tw;
      return this;
    }

    public VehicleDTO build() {
      return new VehicleDTO(this);
    }
  }
}
