/**
 * 
 */
package rinde.sim.core.guice;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class APItest {

    public static void main(String[] args) {

        
        // should create sim with default time model
        final Simulator sim = Simulator.builder().add(RandomModel.builder(123)).add(GraphRoadModel.builder("path/to/graph")).build()
        
                for(int i=0; i < 10; i++){
                    sim.add(MyAgent.builder(new Point(123,45)));
                }
        
        
        // scenario should define all standard models -> CompositeBuilder ?
        // should be able to restrict creation of certain kind of objects -> registration in certain models can be prohibited
        // should be able to customize implementations of some types
        final Simulator sim = Simulator.build(RandomModel.builder(123),ScenarioController.builder(scenario))
                
                sim.start()
        
        
    }

    class MyOwnModel implements RoadAPI, Model {

    }
}
