package rinde.sim.pdptw.gendreau06;

/**
 * This is an enum containing the three problem classes used in the Gendreau06
 * benchmark.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public enum GendreauProblemClass {
  /**
   * Length of day: 240 minutes, frequency: 24 requests per hour, vehicles: 10.
   */
  SHORT_LOW_FREQ(240, 24, 10),

  /**
   * Length of day: 240 minutes, frequency: 33 requests per hour, vehicles: 10.
   */
  SHORT_HIGH_FREQ(240, 33, 10),

  /**
   * Length of day: 450 minutes, frequency: 24 requests per hour, vehicles: 20.
   */
  LONG_LOW_FREQ(450, 24, 20);

  /**
   * The (postfix) file identifier of this class. This can be used to filter for
   * this problem class in a set of files.
   */
  public final String fileId;

  /**
   * The length of the day in minutes.
   */
  public final int duration;

  /**
   * The frequency of new incoming requests in requests per hour.
   */
  public final int frequency;

  /**
   * The total number of vehicles available during the entire day.
   */
  public final int vehicles;

  private GendreauProblemClass(int d, int f, int v) {
    duration = d;
    frequency = f;
    vehicles = v;
    fileId = "_" + duration + "_" + frequency;
  }
}
