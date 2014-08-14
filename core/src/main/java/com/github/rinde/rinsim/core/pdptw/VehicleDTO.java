/**
 * 
 */
package com.github.rinde.rinsim.core.pdptw;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Objects;

import javax.annotation.Nullable;

import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.util.TimeWindow;

/**
 * An immutable value object representing a vehicle in a pickup-and-delivery
 * problem. Instances can be created via {@link #builder()}.
 * @author Rinde van Lon
 */
public final class VehicleDTO {
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

  @Override
  public int hashCode() {
    return Objects.hash(startPosition, speed, capacity, availabilityTimeWindow);
  }

  @Override
  public boolean equals(@Nullable Object o) {
    if (null == o) {
      return false;
    }
    if (this == o) {
      return true;
    }
    if (o.getClass() != getClass()) {
      return false;
    }
    final VehicleDTO v = (VehicleDTO) o;
    return Objects.equals(v.startPosition, startPosition)
        && Objects.equals(v.speed, speed)
        && Objects.equals(v.capacity, capacity)
        && Objects.equals(v.availabilityTimeWindow, availabilityTimeWindow);
  }

  /**
   * @return A new builder for constructing {@link VehicleDTO}s.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for constructing {@link VehicleDTO}s.
   * @author Rinde van Lon
   */
  public static class Builder {
    private static final double DEFAULT_SPEED = 50d;

    Point startPosition;
    double speed;
    int capacity;
    TimeWindow availabilityTimeWindow;

    Builder() {
      startPosition = new Point(0, 0);
      speed = DEFAULT_SPEED;
      capacity = 1;
      availabilityTimeWindow = TimeWindow.ALWAYS;
    }

    /**
     * Copy the value of the specified vehicle into this builder.
     * @param dto The dto to copy values from.
     * @return This, as per the builder pattern.
     */
    public Builder use(VehicleDTO dto) {
      return startPosition(dto.startPosition)
          .availabilityTimeWindow(dto.availabilityTimeWindow)
          .speed(dto.speed)
          .capacity(dto.capacity);
    }

    /**
     * Sets the start position of the vehicle. Default value: (0,0).
     * @param point The position.
     * @return This, as per the builder pattern.
     */
    public Builder startPosition(Point point) {
      startPosition = point;
      return this;
    }

    /**
     * Sets the speed of the vehicle. Default value: 50 (using the speed unit of
     * the scenario/simulator where it is used).
     * @param s The speed, must be <code> &gt; 0</code>.
     * @return This, as per the builder pattern.
     */
    public Builder speed(double s) {
      checkArgument(s > 0, "Speed must be positive, found %s.", s);
      speed = s;
      return this;
    }

    /**
     * Sets the capacity of the vehicle. Default value: 1.
     * @param c The capacity, must be <code> &gt;= 0</code>.
     * @return This, as per the builder pattern.
     */
    public Builder capacity(int c) {
      checkArgument(c >= 0, "Capacity may not be negative, found %s.", c);
      capacity = c;
      return this;
    }

    /**
     * Sets the availability {@link TimeWindow} of the vehicle. Default value:
     * {@link TimeWindow#ALWAYS}.
     * @param tw The time window.
     * @return This, as per the builder pattern.
     */
    public Builder availabilityTimeWindow(TimeWindow tw) {
      availabilityTimeWindow = tw;
      return this;
    }

    /**
     * @return A new {@link VehicleDTO} instance.
     */
    public VehicleDTO build() {
      return new VehicleDTO(this);
    }
  }
}
