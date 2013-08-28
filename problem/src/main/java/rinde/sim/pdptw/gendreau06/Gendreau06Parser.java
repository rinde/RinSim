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
import java.io.FileReader;
import java.io.IOException;
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

  private Gendreau06Parser() {}

  public static Gendreau06Scenario parse(String file, int numVehicles)
      throws IOException {
    return parse(new BufferedReader(new FileReader(file)), new File(file).getName(), numVehicles);
  }

  public static Gendreau06Scenario parse(BufferedReader reader,
      String fileName, int numVehicles) throws IOException {
    return parse(reader, fileName, numVehicles, 1000L);
  }

  public static Gendreau06Scenario parse(BufferedReader reader,
      String fileName, int numVehicles, final long tickSize) throws IOException {
    checkArgument(numVehicles > 0, "at least one vehicle is necessary in the scenario");
    checkArgument(tickSize > 0);
    final ScenarioBuilder sb = new ScenarioBuilder(ADD_PARCEL, ADD_DEPOT,
        ADD_VEHICLE, TIME_OUT);

    final String regex = ".*req_rapide_(1|2|3|4|5)_(450|240)_(24|33)";
    final Matcher m = Pattern.compile(regex).matcher(fileName);
    checkArgument(m.matches(), "The filename must conform to the following regex: %s input was: %s", regex, fileName);

    final int instanceNumber = Integer.parseInt(m.group(1));
    final long minutes = Long.parseLong(m.group(2));
    final long totalTime = minutes * 60000;
    final long requestsPerHour = Long.parseLong(m.group(3));

    final GendreauProblemClass problemClass = GendreauProblemClass
        .with(minutes, requestsPerHour);

    final Point depotPosition = new Point(2.0, 2.5);
    final double truckSpeed = 30;
    sb.addEvent(new AddDepotEvent(-1, depotPosition));
    for (int i = 0; i < numVehicles; i++) {
      sb.addEvent(new AddVehicleEvent(-1, new VehicleDTO(depotPosition,
          truckSpeed, 0, new TimeWindow(0, totalTime))));
    }
    String line;
    while ((line = reader.readLine()) != null) {
      final String[] parts = line.split(" ");
      final long requestArrivalTime = (long) (Double.parseDouble(parts[0]) * 1000.0);
      // FIXME currently filtering out first and last lines of file. Is
      // this ok?
      if (requestArrivalTime >= 0) {
        final long pickupServiceTime = Long.parseLong(parts[1]) * 1000;
        final double pickupX = Double.parseDouble(parts[2]);
        final double pickupY = Double.parseDouble(parts[3]);
        final long pickupTimeWindowBegin = (long) (Double.parseDouble(parts[4]) * 1000.0);
        final long pickupTimeWindowEnd = (long) (Double.parseDouble(parts[5]) * 1000.0);
        final long deliveryServiceTime = Long.parseLong(parts[6]) * 1000;
        final double deliveryX = Double.parseDouble(parts[7]);
        final double deliveryY = Double.parseDouble(parts[8]);
        final long deliveryTimeWindowBegin = (long) (Double
            .parseDouble(parts[9]) * 1000.0);
        final long deliveryTimeWindowEnd = (long) (Double
            .parseDouble(parts[10]) * 1000.0);

        final ParcelDTO dto = new ParcelDTO(new Point(pickupX, pickupY),
            new Point(deliveryX, deliveryY), new TimeWindow(
                pickupTimeWindowBegin, pickupTimeWindowEnd), new TimeWindow(
                deliveryTimeWindowBegin, deliveryTimeWindowEnd), 0,
            requestArrivalTime, pickupServiceTime, deliveryServiceTime);
        sb.addEvent(new AddParcelEvent(dto));
      }
    }

    sb.addEvent(new TimedEvent(TIME_OUT, totalTime));
    reader.close();
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
