/**
 * 
 */
package rinde.sim.pdptw.gendreau06;

import static com.google.common.base.Preconditions.checkArgument;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.ADD_DEPOT;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.ADD_PARCEL;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.ADD_VEHICLE;
import static rinde.sim.core.model.pdp.PDPScenarioEvent.TIME_OUT;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.math.RoundingMode;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rinde.sim.core.graph.Point;
import rinde.sim.pdptw.common.AddDepotEvent;
import rinde.sim.pdptw.common.AddParcelEvent;
import rinde.sim.pdptw.common.AddVehicleEvent;
import rinde.sim.pdptw.common.ParcelDTO;
import rinde.sim.pdptw.common.VehicleDTO;
import rinde.sim.scenario.ScenarioBuilder;
import rinde.sim.scenario.ScenarioBuilder.ScenarioCreator;
import rinde.sim.scenario.TimedEvent;
import rinde.sim.util.TimeWindow;

import com.google.common.math.DoubleMath;

/**
 * Expects files from the Gendreau06 dataset [1]. <br/>
 * 
 * <ul>
 * <li>[1]. Gendreau, M., Guertin, F., Potvin, J.-Y., and Séguin, R.
 * Neighborhood search heuristics for a dynamic vehicle dispatching problem with
 * pick-ups and deliveries. Transportation Research Part C: Emerging
 * Technologies 14, 3 (2006), 157–174.</li>
 * </ul>
 * <b>Format specification: (columns)</b>
 * <ul>
 * <li>1: request arrival time</li>
 * <li>2: pick-up service time</li>
 * <li>3 and 4: x and y position for the pick-up</li>
 * <li>5 and 6: service window time of the pick-up</li>
 * <li>7: delivery service time</li>
 * <li>8 and 9:x and y position for the delivery</li>
 * <li>10 and 11: service window time of the delivery</li>
 * </ul>
 * 
 * All times are expressed in seconds.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public final class Gendreau06Parser {

  private static final String REGEX = ".*req_rapide_(1|2|3|4|5)_(450|240)_(24|33)";
  private static final double TIME_MULTIPLIER = 1000d;
  private static final int TIME_MULTIPLIER_INTEGER = 1000;
  private static final int PARCEL_MAGNITUDE = 0;

  private Gendreau06Parser() {}

  public static Gendreau06Scenario parse(String file) {
    return parse(file, -1);
  }

  public static Gendreau06Scenario parse(String file, int numVehicles) {
    FileReader reader;
    try {
      reader = new FileReader(file);
    } catch (final FileNotFoundException e) {
      throw new IllegalArgumentException("File not found: " + e.getMessage());
    }
    return parse(new BufferedReader(reader), new File(file).getName(),
        numVehicles);
  }

  public static Gendreau06Scenario parse(BufferedReader reader,
      String fileName, int numVehicles) {
    return parse(reader, fileName, numVehicles, 1000L);
  }

  /**
   * 
   * @param reader
   * @param fileName
   * @param numVehicles
   * @param tickSize > 0
   * @throws IllegalArgumentException in the following cases:
   *           <ul>
   *           <li><code>numVehicles == 0 || numVehicles <= -2 </code></li>
   *           <li><code>tickSize <= 0</code></li>
   *           <li>The file could not be loaded.</li>
   *           </ul>
   * @return A newly created scenario.
   */
  public static Gendreau06Scenario parse(BufferedReader reader,
      String fileName, int numVehicles, final long tickSize) {
    checkArgument(
        numVehicles > 0 || numVehicles == -1,
        "at least one vehicle is necessary in the scenario, or choose -1 to pick the default number of vehicles for this scenario.");
    checkArgument(tickSize > 0);
    final ScenarioBuilder sb = new ScenarioBuilder(ADD_PARCEL, ADD_DEPOT,
        ADD_VEHICLE, TIME_OUT);

    final Matcher m = Pattern.compile(REGEX).matcher(fileName);
    checkArgument(m.matches(),
        "The filename must conform to the following regex: %s input was: %s",
        REGEX, fileName);

    final int instanceNumber = Integer.parseInt(m.group(1));
    final long minutes = Long.parseLong(m.group(2));
    final long totalTime = minutes * 60000L;
    final long requestsPerHour = Long.parseLong(m.group(3));

    final GendreauProblemClass problemClass = GendreauProblemClass.with(
        minutes, requestsPerHour);

    final int vehicles = numVehicles == -1 ? problemClass.vehicles
        : numVehicles;

    final Point depotPosition = new Point(2.0, 2.5);
    final double truckSpeed = 30;
    sb.addEvent(new AddDepotEvent(-1, depotPosition));
    for (int i = 0; i < vehicles; i++) {
      sb.addEvent(new AddVehicleEvent(-1, new VehicleDTO(depotPosition,
          truckSpeed, 0, new TimeWindow(0, totalTime))));
    }
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        final String[] parts = line.split(" ");
        final long requestArrivalTime = DoubleMath.roundToLong(
            Double.parseDouble(parts[0]) * TIME_MULTIPLIER,
            RoundingMode.HALF_EVEN);
        // FIXME currently filtering out first and last lines of file. Is
        // this ok?
        if (requestArrivalTime >= 0) {
          final long pickupServiceTime = Long.parseLong(parts[1])
              * TIME_MULTIPLIER_INTEGER;
          final double pickupX = Double.parseDouble(parts[2]);
          final double pickupY = Double.parseDouble(parts[3]);
          final long pickupTimeWindowBegin = DoubleMath.roundToLong(
              Double.parseDouble(parts[4]) * TIME_MULTIPLIER,
              RoundingMode.HALF_EVEN);
          final long pickupTimeWindowEnd = DoubleMath.roundToLong(
              Double.parseDouble(parts[5]) * TIME_MULTIPLIER,
              RoundingMode.HALF_EVEN);
          final long deliveryServiceTime = Long.parseLong(parts[6])
              * TIME_MULTIPLIER_INTEGER;
          final double deliveryX = Double.parseDouble(parts[7]);
          final double deliveryY = Double.parseDouble(parts[8]);
          final long deliveryTimeWindowBegin = DoubleMath.roundToLong(
              Double.parseDouble(parts[9]) * TIME_MULTIPLIER,
              RoundingMode.HALF_EVEN);
          final long deliveryTimeWindowEnd = DoubleMath.roundToLong(
              Double.parseDouble(parts[10]) * TIME_MULTIPLIER,
              RoundingMode.HALF_EVEN);

          final ParcelDTO dto = new ParcelDTO(new Point(pickupX, pickupY),
              new Point(deliveryX, deliveryY), new TimeWindow(
                  pickupTimeWindowBegin, pickupTimeWindowEnd), new TimeWindow(
                  deliveryTimeWindowBegin, deliveryTimeWindowEnd),
              PARCEL_MAGNITUDE, requestArrivalTime, pickupServiceTime,
              deliveryServiceTime);
          sb.addEvent(new AddParcelEvent(dto));
        }
      }
      sb.addEvent(new TimedEvent(TIME_OUT, totalTime));
      reader.close();
    } catch (final IOException e) {
      throw new IllegalArgumentException(e.getMessage());
    }

    return sb.build(new ScenarioCreator<Gendreau06Scenario>() {
      @Override
      public Gendreau06Scenario create(List<TimedEvent> eventList,
          Set<Enum<?>> eventTypes) {
        return new Gendreau06Scenario(eventList, eventTypes, tickSize,
            problemClass, instanceNumber);
      }
    });
  }
}
