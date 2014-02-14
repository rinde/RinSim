/**
 * 
 */
package rinde.sim.examples.core.rwalk5;

/**
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @since 2.0
 */
public class RandomWalkExample {

  public static void main(String[] args) throws Exception {
    // TODO clean this mess

    // create a new simulator, load map of Leuven

    final int simStep = 100;

    // create simple scenario

    // final TimeConverter conv = new TimeConverter(simStep);

    // final ScenarioBuilder builder = new ScenarioBuilder(ADD_TRUCK);
    //
    // builder.addEventGenerator(new
    // ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, 10,
    // new ScenarioBuilder.EventTypeFunction(ADD_TRUCK)));
    // builder.addEventGenerator(new
    // ScenarioBuilder.MultipleEventGenerator<TimedEvent>(conv.hour(3).min(30).toTime(),
    // 10, new ScenarioBuilder.EventTypeFunction(ADD_TRUCK)));
    // builder.addEventGenerator(new
    // ScenarioBuilder.TimeSeries<TimedEvent>(conv.day(1).toTime(),
    // conv.day(4).toTime(), conv.hour(12).min(17).toTime(), new
    // ScenarioBuilder.EventTypeFunction(ADD_TRUCK)));
    //
    // builder.addEventGenerator(new
    // ScenarioBuilder.TimeSeries<TimedEvent>(conv.day(3).toTime(),
    // conv.day(15)
    // .toTime(), conv.hour(1).min(1).toTime(), new
    // ScenarioBuilder.EventTypeFunction(ADD_TRUCK)));
    //
    // builder.addEventGenerator(new
    // ScenarioBuilder.TimeSeries<TimedEvent>(conv.tick(2000).toTime(),
    // conv.tick(20000)
    // .toTime(), conv.tick(1000).toTime(), new
    // ScenarioBuilder.EventTypeFunction(ADD_TRUCK)));
    //
    // final ScenarioBuilder builder2 = new ScenarioBuilder(ADD_TRUCK);
    // builder2.addEventGenerator(new
    // ScenarioBuilder.MultipleEventGenerator<TimedEvent>(0, 100,
    // new ScenarioBuilder.EventTypeFunction(ADD_TRUCK)));
    // builder2.addEventGenerator(new
    // ScenarioBuilder.MultipleEventGenerator<TimedEvent>(conv.day(1).toTime(),
    // 200,
    // new ScenarioBuilder.EventTypeFunction(ADD_TRUCK)));
    // builder2.addEventGenerator(new
    // ScenarioBuilder.MultipleEventGenerator<TimedEvent>(conv.day(3).toTime(),
    // 300,
    // new ScenarioBuilder.EventTypeFunction(ADD_TRUCK)));
    //
    // final Scenario s = builder2.build();
    //
    // // run scenario with visualization attached
    // final String MAP_DIR = "../core/files/maps/";
    //
    // new SimpleController(s, MAP_DIR + "leuven-simple.dot");
  }
}
