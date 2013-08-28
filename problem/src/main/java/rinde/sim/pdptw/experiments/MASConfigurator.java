/**
 * 
 */
package rinde.sim.pdptw.experiments;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public interface MASConfigurator {

  MASConfiguration configure(long seed);

  // AgentSystemConfigurator -> AgentSystemConfiguration

  // Solution

  // MASConfigurator -> MASConfiguration

  // MASConfigurator.build(Creator<AddVehicleEvent>)

  public static class Builder {

    public void addModel() {

    }

  }

}
