/**
 * 
 */
package rinde.sim.pdptw.common;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ScenarioParser<T extends DynamicPDPTWScenario> {

  T parse(String file);

}
