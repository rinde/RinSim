/**
 * 
 */
package rinde.sim.pdptw.scenario;


/**
 * A parser for {@link PDPScenario}s.
 * @param <T> The type of scenario that is parsed.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface ScenarioParser<T extends PDPScenario> {
  /**
   * Should parse the file.
   * @param file The file to parse.
   * @return The {@link PDPScenario} instance.
   */
  T parse(String file);
}
