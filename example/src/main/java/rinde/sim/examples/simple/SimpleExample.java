/**
 * 
 */
package rinde.sim.examples.simple;

import javax.measure.Measure;
import javax.measure.unit.SI;

import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.RandomGenerator;

import rinde.sim.core.Simulator;
import rinde.sim.core.TickListener;
import rinde.sim.core.TimeLapse;
import rinde.sim.core.graph.Point;
import rinde.sim.core.model.road.MovingRoadUser;
import rinde.sim.core.model.road.PlaneRoadModel;
import rinde.sim.core.model.road.RoadModel;
import rinde.sim.ui.View;
import rinde.sim.ui.renderers.PlaneRoadModelRenderer;
import rinde.sim.ui.renderers.RoadUserRenderer;

/**
 * This is a very simple example of the RinSim simulator that shows how a
 * simulation is set up. It is heavily documented to provide a sort of
 * 'walk-through' experience for new users of the simulator.<br/>
 * 
 * If this class is run on MacOS it might be neccessary to use
 * -XstartOnFirstThread as a VM argument.
 * 
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class SimpleExample {

  public static void main(String[] args) {
    // initialize a random generator which we use throughout this
    // 'experiment'
    final RandomGenerator rnd = new MersenneTwister(123);

    // initialize a new Simulator instance
    final Simulator sim = new Simulator(rnd, Measure.valueOf(1000L, SI.SECOND));

    // register a PlaneRoadModel, a model which facilitates the moving of
    // RoadUsers on a plane. The plane is bounded by two corner points:
    // (0,0) and (10,10)
    sim.register(new PlaneRoadModel(new Point(0, 0), new Point(10, 10), 10d));
    // configure the simulator, once configured we can no longer change the
    // configuration (i.e. add new models) but we can start adding objects
    sim.configure();

    // add a number of drivers on the road
    final int numDrivers = 200;
    for (int i = 0; i < numDrivers; i++) {
      // when an object is registered in the simulator it gets
      // automatically 'hooked up' with models that it's interested in. An
      // object declares to be interested in an model by implementing an
      // interface.
      sim.register(new Driver(rnd));
    }
    // initialize the gui. We use separate renderers for the road model and
    // for the drivers. By default the road model is rendererd as a square
    // (indicating its boundaries), and the drivers are rendererd as red
    // dots.
    View.create(sim).with(new PlaneRoadModelRenderer(), new RoadUserRenderer())
        .show();
    // in case a GUI is not desired, the simulation can simply be run by
    // calling: sim.start();
  }

  static class Driver implements MovingRoadUser, TickListener {
    // the MovingRoadUser interface indicates that this class can move on a
    // RoadModel. The TickListener interface indicates that this class wants
    // to keep track of time.

    protected RoadModel roadModel;
    protected final RandomGenerator rnd;

    public Driver(RandomGenerator r) {
      // we store the reference to the random generator
      rnd = r;
    }

    @Override
    public void initRoadUser(RoadModel model) {
      // this is where we receive an instance to the model. we store the
      // reference and add ourselves to the model on a random position.
      roadModel = model;
      roadModel.addObjectAt(this, roadModel.getRandomPosition(rnd));
    }

    @Override
    public void tick(TimeLapse timeLapse) {
      // every time step (tick) this gets called. Each time we chose a
      // different destination and move in that direction using the time
      // that was made available to us.
      roadModel.moveTo(this, roadModel.getRandomPosition(rnd), timeLapse);
    }

    @Override
    public void afterTick(TimeLapse timeLapse) {
      // we don't need this in this example. This method is called after
      // all TickListener#tick() calls, hence the name.
    }

    @Override
    public double getSpeed() {
      // the drivers speed
      return .03;
    }

  }
}
