/**
 * 
 */
package rinde.sim.pdptw.common;

/**
 * A parser for {@link DynamicPDPTWScenario}s.
 * @param <T> The type of scenario that is parsed.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public interface ScenarioParser<T extends DynamicPDPTWScenario> {
  /**
   * Should parse the file.
   * @param file The file to parse.
   * @return The {@link DynamicPDPTWScenario} instance.
   */
  T parse(String file);
}
