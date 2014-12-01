package com.github.rinde.rinsim.pdptw.gendreau06;

import static com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent.ADD_DEPOT;
import static com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent.ADD_PARCEL;
import static com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent.ADD_VEHICLE;
import static com.github.rinde.rinsim.core.model.pdp.PDPScenarioEvent.TIME_OUT;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.github.rinde.rinsim.core.pdptw.ParcelDTO;
import com.github.rinde.rinsim.core.pdptw.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.scenario.AddDepotEvent;
import com.github.rinde.rinsim.scenario.AddParcelEvent;
import com.github.rinde.rinsim.scenario.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.TimedEvent.TimeComparator;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.math.DoubleMath;

/**
 * Parser for files from the Gendreau et al. (2006) data set. The parser allows
 * to customize some of the properties of the scenarios.
 * <p>
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
 * All times are expressed in seconds.
 * <p>
 * <b>References</b>
 * <ul>
 * <li>[1]. Gendreau, M., Guertin, F., Potvin, J.-Y., and Séguin, R.
 * Neighborhood search heuristics for a dynamic vehicle dispatching problem with
 * pick-ups and deliveries. Transportation Research Part C: Emerging
 * Technologies 14, 3 (2006), 157–174.</li>
 * </ul>
 * @author Rinde van Lon 
 */
public final class Gendreau06Parser {
  private static final String REGEX = ".*req_rapide_(\\d+)_(450|240)_(24|33)";
  private static final double TIME_MULTIPLIER = 1000d;
  private static final int TIME_MULTIPLIER_INTEGER = 1000;
  private static final int PARCEL_MAGNITUDE = 0;
  private static final long DEFAULT_TICK_SIZE = 1000L;

  private int numVehicles;
  private boolean allowDiversion;
  private boolean online;
  private long tickSize;
  private final ImmutableMap.Builder<String, ParcelsSupplier> parcelsSuppliers;
  private Optional<ImmutableList<ProblemClass>> problemClasses;

  private Gendreau06Parser() {
    allowDiversion = false;
    online = true;
    numVehicles = -1;
    tickSize = DEFAULT_TICK_SIZE;
    parcelsSuppliers = ImmutableMap.builder();
    problemClasses = Optional.absent();
  }

  /**
   * @return A {@link Gendreau06Parser}.
   */
  public static Gendreau06Parser parser() {
    return new Gendreau06Parser();
  }

  /**
   * Convenience method for parsing a single file.
   * @param file The file to parse.
   * @return The scenario as described by the file.
   */
  public static Gendreau06Scenario parse(File file) {
    return parser().addFile(file).parse().get(0);
  }

  /**
   * Add the specified file to the parser.
   * @param file The file to add.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser addFile(File file) {
    checkValidFileName(file.getName());
    try {
      parcelsSuppliers.put(file.getName(), new InputStreamToParcels(
          new FileInputStream(
              file)));
    } catch (final FileNotFoundException e) {
      throw new IllegalArgumentException(e);
    }
    return this;
  }

  /**
   * Add the specified file to the parser.
   * @param file The file to add.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser addFile(String file) {
    return addFile(new File(file));
  }

  /**
   * Add the specified stream to the parser. A file name needs to be specified
   * for identification of this particular scenario.
   * @param stream The stream to use for the parsing of the scenario.
   * @param fileName The file name that identifies the scenario's class and
   *          instance.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser addFile(InputStream stream, String fileName) {
    checkValidFileName(fileName);
    parcelsSuppliers.put(fileName, new InputStreamToParcels(stream));
    return this;
  }

  /**
   * Add a scenario that is composed using the specified {@link AddParcelEvent}
   * s.
   * @param events The events to include.
   * @param fileName The filename of the scenario.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser addFile(ImmutableList<AddParcelEvent> events,
      String fileName) {
    checkValidFileName(fileName);
    parcelsSuppliers.put(fileName, new Parcels(events));
    return this;
  }

  /**
   * Adds all Gendreau scenario files in the specified directory.
   * @param dir The directory to search.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser addDirectory(String dir) {
    return addDirectory(new File(dir));
  }

  /**
   * Adds all Gendreau scenario files in the specified directory.
   * @param dir The directory to search.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser addDirectory(File dir) {
    final File[] files = dir.listFiles(
        new FileFilter() {
          @SuppressWarnings("null")
          @Override
          public boolean accept(File file) {
            checkNotNull(file);
            return isValidFileName(file.getName());
          }
        });
    Arrays.sort(files);
    for (final File f : files) {
      addFile(f);
    }

    return this;
  }

  /**
   * When this method is called all scenarios generated by this parser will
   * allow vehicle diversion. For more information about the vehicle diversion
   * concept please consult the class documentation of
   * {@link com.github.rinde.rinsim.pdptw.common.PDPRoadModel}.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser allowDiversion() {
    allowDiversion = true;
    return this;
  }

  /**
   * When this method is called, all scenarios generated by this parser will be
   * offline scenarios. This means that all parcel events will arrive at time
   * <code>-1</code>, which means that everything is known beforehand. By
   * default the parser uses the original event arrival times as defined by the
   * scenario file.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser offline() {
    online = false;
    return this;
  }

  /**
   * This method allows to override the number of vehicles in the scenarios. For
   * the default values see {@link GendreauProblemClass}.
   * @param num The number of vehicles that should be used in the parsed
   *          scenarios. Must be positive.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser setNumVehicles(int num) {
    checkArgument(num > 0, "The number of vehicles must be positive.");
    numVehicles = num;
    return this;
  }

  /**
   * Change the default tick size of <code>1000 ms</code> into something else.
   * @param tick Must be positive.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser setTickSize(long tick) {
    checkArgument(tick > 0L, "Tick size must be positive.");
    tickSize = tick;
    return this;
  }

  /**
   * Filters out any files that are added to this parser which are <i>not</i> of
   * one of the specified problem classes.
   * @param classes The problem classes which should be parsed.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser filter(GendreauProblemClass... classes) {
    problemClasses = Optional.of(ImmutableList.<ProblemClass> copyOf(classes));
    return this;
  }

  /**
   * Parses the files which are added to this parser. In case
   * {@link #filter(GendreauProblemClass...)} has been called, only files in one
   * of these problem classes will be parsed.
   * @return A list of scenarios in order of adding them to the parser.
   */
  public ImmutableList<Gendreau06Scenario> parse() {
    final ImmutableList.Builder<Gendreau06Scenario> scenarios = ImmutableList
        .builder();
    for (final Entry<String, ParcelsSupplier> entry : parcelsSuppliers.build()
        .entrySet()) {
      boolean include = false;
      if (!problemClasses.isPresent()) {
        include = true;
      } else {
        for (final ProblemClass pc : problemClasses.get()) {
          if (entry.getKey().endsWith(pc.getId())) {
            include = true;
            break;
          }
        }
      }
      if (include) {
        scenarios.add(parse(entry.getValue(), entry.getKey(), numVehicles,
            tickSize, allowDiversion, online));
      }
    }
    // TODO sort list first by ProblemClass then by Id
    return scenarios.build();
  }

  static Matcher matcher(String fileName) {
    return Pattern.compile(REGEX).matcher(fileName);
  }

  static boolean isValidFileName(String name) {
    return Pattern.compile(REGEX).matcher(name).matches();
  }

  static void checkValidFileName(Matcher m, String name) {
    checkArgument(m.matches(),
        "The filename must conform to the following regex: %s input was: %s",
        REGEX, name);
  }

  static void checkValidFileName(String name) {
    checkValidFileName(matcher(name), name);
  }

  private static Gendreau06Scenario parse(
      ParcelsSupplier parcels,
      String fileName, int numVehicles, final long tickSize,
      final boolean allowDiversion, boolean online) {

    final Set<Enum<?>> evTypes = ImmutableSet.<Enum<?>> of(ADD_PARCEL,
        ADD_DEPOT, ADD_VEHICLE, TIME_OUT);

    final Matcher m = matcher(fileName);
    checkValidFileName(m, fileName);

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

    final List<TimedEvent> events = newArrayList();
    events.add(new AddDepotEvent(-1, depotPosition));
    for (int i = 0; i < vehicles; i++) {
      events.add(new AddVehicleEvent(-1,
          VehicleDTO.builder()
              .startPosition(depotPosition)
              .speed(truckSpeed)
              .capacity(0)
              .availabilityTimeWindow(new TimeWindow(0, totalTime))
              .build()));
    }
    events.addAll(parcels.get(online));
    events.add(new TimedEvent(TIME_OUT, totalTime));
    Collections.sort(events, TimeComparator.INSTANCE);

    return new Gendreau06Scenario(events, evTypes, tickSize,
        problemClass, instanceNumber, allowDiversion);
  }

  static ImmutableList<AddParcelEvent> parseParcels(InputStream inputStream,
      boolean online) {
    final ImmutableList.Builder<AddParcelEvent> listBuilder = ImmutableList
        .builder();
    final BufferedReader reader = new BufferedReader(new InputStreamReader(
        inputStream, Charsets.UTF_8));
    String line;
    try {
      while ((line = reader.readLine()) != null) {
        final Iterator<String> parts = Iterators.forArray(line.split(" "));
        final long requestArrivalTime = DoubleMath.roundToLong(
            Double.parseDouble(parts.next()) * TIME_MULTIPLIER,
            RoundingMode.HALF_EVEN);
        // FIXME currently filtering out first and last lines of file. Is
        // this ok?
        if (requestArrivalTime >= 0) {
          final long pickupServiceTime = Long.parseLong(parts.next())
              * TIME_MULTIPLIER_INTEGER;
          final double pickupX = Double.parseDouble(parts.next());
          final double pickupY = Double.parseDouble(parts.next());
          final long pickupTimeWindowBegin = DoubleMath.roundToLong(
              Double.parseDouble(parts.next()) * TIME_MULTIPLIER,
              RoundingMode.HALF_EVEN);
          final long pickupTimeWindowEnd = DoubleMath.roundToLong(
              Double.parseDouble(parts.next()) * TIME_MULTIPLIER,
              RoundingMode.HALF_EVEN);
          final long deliveryServiceTime = Long.parseLong(parts.next())
              * TIME_MULTIPLIER_INTEGER;
          final double deliveryX = Double.parseDouble(parts.next());
          final double deliveryY = Double.parseDouble(parts.next());
          final long deliveryTimeWindowBegin = DoubleMath.roundToLong(
              Double.parseDouble(parts.next()) * TIME_MULTIPLIER,
              RoundingMode.HALF_EVEN);
          final long deliveryTimeWindowEnd = DoubleMath.roundToLong(
              Double.parseDouble(parts.next()) * TIME_MULTIPLIER,
              RoundingMode.HALF_EVEN);

          // when an offline scenario is desired, all times are set to -1
          final long arrTime = online ? requestArrivalTime : -1;

          final ParcelDTO dto = ParcelDTO.builder(new Point(pickupX, pickupY),
              new Point(deliveryX, deliveryY)).
              pickupTimeWindow(new TimeWindow(
                  pickupTimeWindowBegin, pickupTimeWindowEnd))
              .deliveryTimeWindow(new TimeWindow(
                  deliveryTimeWindowBegin, deliveryTimeWindowEnd))
              .neededCapacity(PARCEL_MAGNITUDE)
              .orderAnnounceTime(arrTime)
              .pickupDuration(pickupServiceTime)
              .deliveryDuration(deliveryServiceTime)
              .build();
          listBuilder.add(new AddParcelEvent(dto));
        }
      }
      reader.close();
    } catch (final IOException e) {
      throw new IllegalArgumentException(e);
    }
    return listBuilder.build();
  }

  public static Function<Path, Gendreau06Scenario> reader() {
    return new Gendreau06Reader();
  }

  static class Gendreau06Reader implements Function<Path, Gendreau06Scenario> {
    @Override
    public Gendreau06Scenario apply(Path input) {
      return Gendreau06Parser.parse(input.toFile());
    }
  }

  interface ParcelsSupplier {
    /**
     * Should return an event list.
     * @param online If false, all events except time-out should have time -1.
     * @return The list of events.
     */
    ImmutableList<AddParcelEvent> get(boolean online);
  }

  static class InputStreamToParcels implements ParcelsSupplier {
    final InputStream inputStream;

    InputStreamToParcels(InputStream is) {
      inputStream = is;
    }

    @Override
    public ImmutableList<AddParcelEvent> get(boolean online) {
      return parseParcels(inputStream, online);
    }
  }

  static class Parcels implements ParcelsSupplier {
    final ImmutableList<AddParcelEvent> parcels;

    Parcels(ImmutableList<AddParcelEvent> ps) {
      parcels = ps;
    }

    @Override
    public ImmutableList<AddParcelEvent> get(boolean online) {
      return parcels;
    }
  }
}
