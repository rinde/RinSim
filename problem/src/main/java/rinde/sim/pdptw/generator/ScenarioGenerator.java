/**
 * 
 */
package rinde.sim.pdptw.generator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static rinde.sim.pdptw.generator.Metrics.travelTime;

import java.math.RoundingMode;
import java.util.List;

import org.apache.commons.math3.distribution.ExponentialDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.graph.Point;
import rinde.sim.core.model.pdp.PDPScenarioEvent;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.scenario.Scenario;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.util.TimeWindow;

import com.google.common.collect.ImmutableList;
import com.google.common.math.DoubleMath;

/**
 * ScenarioGenerator generates {@link Scenario}s of a specific problem class.
 * Instances can be obtained via {@link #builder()}.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ScenarioGenerator {

  private final ArrivalTimesGenerator arrivalTimesGenerator;
  private final LocationsGenerator locationsGenerator;
  private final TimeWindowGenerator timeWindowGenerator;
  private final VehicleGenerator vehicleGenerator;

  private final long length;
  private final Point depotLocation;

  // input:
  // - dynamism (distribution?)
  // - scale (factor and ratios, density?)
  // - load
  private ScenarioGenerator(ArrivalTimesGenerator atg, LocationsGenerator lg,
      TimeWindowGenerator twg, VehicleGenerator vg, Point depotLoc,
      long scenarioLength) {
    arrivalTimesGenerator = atg;
    locationsGenerator = lg;
    timeWindowGenerator = twg;
    vehicleGenerator = vg;
    depotLocation = depotLoc;
    length = scenarioLength;
  }

  public Scenario generate(RandomGenerator rng) {
    final ImmutableList<Long> times = arrivalTimesGenerator.generate(rng);
    final ImmutableList<Point> locations = locationsGenerator.generate(times
        .size(), rng);
    int index = 0;

    final ScenarioBuilder sb = new ScenarioBuilder(PDPScenarioEvent.ADD_DEPOT,
        PDPScenarioEvent.ADD_PARCEL, PDPScenarioEvent.ADD_VEHICLE);
    sb.addEvent(new AddDepotEvent(-1, depotLocation));
    sb.addEvents(vehicleGenerator.generate(rng));

    for (final long time : times) {
      final Point pickup = locations.get(index++);
      final Point delivery = locations.get(index++);
      final ImmutableList<TimeWindow> tws = timeWindowGenerator
          .generate(time, pickup, delivery, rng);
      sb.addEvent(new AddParcelEvent(new ParcelDTO(pickup, delivery,
          tws.get(0), tws.get(1), 0, time, 5, 5)));
    }
    return sb.build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private final double vehicleSpeed = 40;
    private final int vehicleCapacity = 1;// this is actually irrelevant
                                          // since parcels are weightless
    private final long minResponseTime = 30;
    private final long serviceTime = 5;

    private int vehicles = -1;
    private double size = -1;
    private double announcementIntensity = -1;
    private double ordersPerAnnouncement = -1;
    private long scenarioLength = -1;

    private Builder() {}

    public Builder setAnnouncementIntensityPerKm2(double intensity) {
      checkArgument(intensity > 0d, "Intensity must be a positive number.");
      announcementIntensity = intensity;
      return this;
    }

    public Builder setScenarioLength(long minutes) {
      checkArgument(minutes > minResponseTime, "Scenario length must be greater than minResponseTime %s.", minResponseTime);
      scenarioLength = minutes;
      return this;
    }

    // avg number of orders per announcement, must be >= 1
    public Builder setOrdersPerAnnouncement(double orders) {
      checkArgument(orders >= 1d, "Orders per announcement must be >= 1.");
      ordersPerAnnouncement = orders;
      return this;
    }

    // public void setMinimumResponseTime(long minutes) {
    //
    // }

    // note: these are averages of the entire area, this says nothing about
    // the actual spatial distribution!
    // num of vehicles per km2
    // num of parcels per km2 -> results in 2*parcels service points
    // area: size x size km
    // note that even if the vehicle density and size is set low such that
    // there will be only < .5 vehicles, the number of vehicles is set to 1.
    // TODO comment about num vehicles rounding etc
    public Builder setScale(double numVehiclesKM2, double size) {
      checkArgument(numVehiclesKM2 > 0d, "Number of vehicles per km2 must be a positive number.");
      checkArgument(size > 0d, "Size must be a positive number.");
      this.size = size;
      final double area = size * size;
      vehicles = Math.max(1, DoubleMath
          .roundToInt(numVehiclesKM2 * area, RoundingMode.HALF_DOWN));
      return this;
    }

    // public void setLoad(double min, double avg, double max) {
    //
    // }

    public ScenarioGenerator build() {
      checkArgument(vehicles > 0 && vehicles > 0, "Cannot build generator, scale needs to be set via setScale(double,double).");
      checkArgument(ordersPerAnnouncement > 0, "Cannot build generator, orders need to be set via setOrdersPerAnnouncement(double).");
      checkArgument(scenarioLength > 0, "Cannot build generator, scenario length needs to be set via setScenarioLength(long).");
      checkArgument(announcementIntensity > 0, "Cannot build generator, announcement intensity needs to be set via setAnnouncementIntensityPerKm2(double).");

      final double area = size * size;
      final double globalAnnouncementIntensity = area * announcementIntensity;
      final Point depotLoc = new Point(size / 2, size / 2);
      final Point extreme1 = new Point(0, 0);
      final Point extreme2 = new Point(size, size);

      // this computes the traveltime it would take to travel from one of
      // the corners of the environment to another corner of the
      // environment and then back to the depot.
      final long time1 = travelTime(extreme1, extreme2, vehicleSpeed);
      final long time2 = travelTime(extreme2, depotLoc, vehicleSpeed);
      final long travelTime = time1 + time2;

      // this is the maximum *theoretical* time that is required to
      // service an order. In this context, theoretical means: given
      // enough resources (vehicles).
      final long maxRequiredTime = minResponseTime + travelTime
          + (2 * serviceTime);
      final long latestOrderAnnounceTime = scenarioLength - maxRequiredTime;

      // TODO this can be improved by allowing orders which are closer to
      // the depot for a longer time. This could be implemented by simply
      // rejecting any orders which are not feasible. This could be a
      // reasonable company policy in case minimizing overTime is more
      // important than customer satisfaction.
      checkArgument(scenarioLength > maxRequiredTime, "The scenario length must be long enough such that there is enough time for a vehicle to service a pickup at one end of the environment and to service a delivery at an opposite end of the environment and be back in time at the depot.");

      final VehicleDTO vehicleDto = new VehicleDTO(depotLoc, vehicleSpeed,
          vehicleCapacity, new TimeWindow(0, scenarioLength));

      return new ScenarioGenerator(new PoissonProcessArrivalTimes(
          latestOrderAnnounceTime, globalAnnouncementIntensity,
          ordersPerAnnouncement),
          new NormalLocationsGenerator(size, .15, .05),
          new ProportionateUniformTWGenerator(depotLoc, scenarioLength,
              serviceTime, minResponseTime, vehicleSpeed),//
          new HomogenousVehicleGenerator(vehicles, vehicleDto), depotLoc,
          scenarioLength);
    }
  }

  public static void main(String[] args) {

    final int lambda = 33;
    final double mean = 1d / lambda;
    System.out.println("lambda " + lambda + " mean " + mean);
    final ExponentialDistribution ed = new ExponentialDistribution(mean);
    ed.reseedRandomGenerator(0);

    final double totalTime = 1;
    double sum = 0;
    final List<Double> samples = newArrayList();
    while (sum < totalTime) {
      final double sample = ed.sample();
      sum += sample;
      samples.add(sample);
    }
    System.out.println(samples.size());
    System.out.println(samples);
  }

}
