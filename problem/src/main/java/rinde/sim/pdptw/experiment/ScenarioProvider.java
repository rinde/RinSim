/**
 * 
 */
package rinde.sim.pdptw.experiment;

import rinde.sim.pdptw.common.DynamicPDPTWScenario;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface ScenarioProvider {

  ImmutableList<DynamicPDPTWScenario> provide();

}