/*
 * Copyright (C) 2011-2016 Rinde van Lon, iMinds-DistriNet, KU Leuven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.rinde.rinsim.scenario.gendreau06;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.github.rinde.rinsim.core.model.pdp.Parcel;
import com.github.rinde.rinsim.core.model.pdp.ParcelDTO;
import com.github.rinde.rinsim.core.model.pdp.VehicleDTO;
import com.github.rinde.rinsim.geom.Point;
import com.github.rinde.rinsim.pdptw.common.AddDepotEvent;
import com.github.rinde.rinsim.pdptw.common.AddParcelEvent;
import com.github.rinde.rinsim.pdptw.common.AddVehicleEvent;
import com.github.rinde.rinsim.scenario.Scenario.ProblemClass;
import com.github.rinde.rinsim.scenario.TimeOutEvent;
import com.github.rinde.rinsim.scenario.TimedEvent;
import com.github.rinde.rinsim.scenario.TimedEvent.TimeComparator;
import com.github.rinde.rinsim.util.TimeWindow;
import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
  private static final Point DEPOT_POSITION = new Point(2.0, 2.5);

  private int numVehicles;
  private int numParcels;
  private boolean allowDiversion;
  private boolean online;
  private boolean realtime;
  private long tickSize;
  private final ImmutableMap.Builder<String, ParcelsSupplier> parcelsSuppliers;
  private Optional<ImmutableList<ProblemClass>> problemClasses;

  private Gendreau06Parser() {
    allowDiversion = false;
    online = true;
    realtime = false;
    numVehicles = -1;
    numParcels = Integer.MAX_VALUE;
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
   * @return The {@link Gendreau06Parser} as a {@link Function}.
   */
  public static Function<Path, Gendreau06Scenario> reader() {
    return Gendreau06Reader.INSTANCE;
  }

  /**
   * Add the specified file to the parser.
   * @param file The file to add.
   * @return This, as per the builder pattern.
   */
  public Gendreau06Parser addFile(File file) {
    checkValidFileName(file.getName());
    try {
      parcelsSuppliers.put(file.getName(),
        new InputStreamToParcels(new FileInputStream(file)));
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
    checkArgument(dir.isDirectory(), "%s is not a directory.", dir);
    final File[] files = checkNotNull(dir.listFiles(
      new FileFilter() {
        @Override
        public boolean accept(@Nullable File file) {
          assert file != null;
          return isValidFileName(file.getName());
        }
      }));
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

  public Gendreau06Parser realtime() {
    realtime = true;
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

  public Gendreau06Parser setNumParcels(int num) {
    checkArgument(num > 0);
    numParcels = num;
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
    problemClasses = Optional.of(ImmutableList.<ProblemClass>copyOf(classes));
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
        scenarios.add(
          parse(entry.getValue(), entry.getKey(), numVehicles, numParcels,
            tickSize, allowDiversion, online, realtime));
      }
    }
    return scenarios.build();
  }

  public Function<Path, Gendreau06Scenario> asParseFunction() {
    final int numV = numVehicles;
    final int numP = numParcels;
    final long tick = tickSize;
    final boolean div = allowDiversion;
    final boolean onl = online;
    final boolean rt = realtime;

    return new Function<Path, Gendreau06Scenario>() {
      @Nonnull
      @Override
      public Gendreau06Scenario apply(@Nullable Path input) {
        checkArgument(input != null);
        try {
          final File file = input.toFile();
          return parse(
            new InputStreamToParcels(new FileInputStream(file)),
            file.getName(), numV, numP, tick, div, onl, rt);
        } catch (final FileNotFoundException e) {
          throw new IllegalArgumentException();
        }
      }
    };
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
      String fileName, int numVehicles, int numParcels, final long tickSize,
      final boolean allowDiversion, boolean online, boolean realtime) {

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

    final Point depotPosition = DEPOT_POSITION;
    final double truckSpeed = 30;

    final List<TimedEvent> events = newArrayList();
    events.add(AddDepotEvent.create(-1, depotPosition));
    for (int i = 0; i < vehicles; i++) {
      events.add(AddVehicleEvent.create(-1,
        VehicleDTO.builder()
          .startPosition(depotPosition)
          .speed(truckSpeed)
          .capacity(0)
          .availabilityTimeWindow(TimeWindow.create(0, totalTime))
          .build()));
    }

    List<AddParcelEvent> parcelList = parcels.get(online);
    parcelList = parcelList.subList(0, Math.min(parcelList.size(), numParcels));
    events.addAll(parcelList);
    events.add(TimeOutEvent.create(totalTime));
    Collections.sort(events, TimeComparator.INSTANCE);

    return Gendreau06Scenario.create(events, tickSize,
      problemClass, instanceNumber, allowDiversion, realtime);
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
        // currently filtering out first and last lines of file. Is this ok?
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

          final ParcelDTO dto = Parcel.builder(new Point(pickupX, pickupY),
            new Point(deliveryX, deliveryY)).pickupTimeWindow(TimeWindow.create(
              pickupTimeWindowBegin, pickupTimeWindowEnd))
            .deliveryTimeWindow(TimeWindow.create(
              deliveryTimeWindowBegin, deliveryTimeWindowEnd))
            .neededCapacity(PARCEL_MAGNITUDE)
            .orderAnnounceTime(arrTime)
            .pickupDuration(pickupServiceTime)
            .deliveryDuration(deliveryServiceTime)
            .buildDTO();
          listBuilder.add(AddParcelEvent.create(dto));
        }
      }
      reader.close();
    } catch (final IOException e) {
      throw new IllegalArgumentException(e);
    }
    return listBuilder.build();
  }

  enum Gendreau06Reader implements Function<Path, Gendreau06Scenario> {
    INSTANCE {
      @Override
      @Nonnull
      public Gendreau06Scenario apply(@Nullable Path input) {
        assert input != null;
        return Gendreau06Parser.parse(input.toFile());
      }

      @Override
      public String toString() {
        return String.format("%s.reader()",
          Gendreau06Parser.class.getSimpleName());
      }
    };
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
