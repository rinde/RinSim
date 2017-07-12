---
title: Core
keywords: [design]
sidebar: design_sidebar
toc: true
permalink: /design/core/

---

The core module contains the core simulation components. The core module is the entry point for getting to know the basics of RinSim. It also contains the most used models.

There are generally two classes that can be used to configure RinSim:

 - [rinsim.core.Simulator](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/Simulator.java), a simple and direct way to interact with the simulator. This is the recommended way to learn how RinSim works. Explained in this section.
 - [rinsim.experiment.Experiment](https://github.com/rinde/RinSim/blob/master/experiment/src/main/java/com/github/rinde/rinsim/experiment/Experiment.java), a more advanced interface that encapsulates Simulator, it is specifically designed for (scientific) experiments. Explained in the [Experiment section](/design/experiment/).

## _rinsim.core_

The simulator is in essence a collection of models. When configuring the simulator, using ``Simulator.builder()``, the desired models can be added. For ease of use, the simulator contains a ``TimeModel`` and a ``RandomModel`` by default. The configuration phase is concluded by calling ``build()`` of the ``Simulator.Builder`` class which returns a ``Simulator`` instance.

```java
Simulator sim = Simulator.builder()
                         .addModel(...)
                         // add more models
                         .build()
```                            

The ``Simulator`` object provides several methods for controlling the simulation. It also has a ``register()`` method that allows to register simulation entities into the simulator. Typically, a simulation entity interacts with one or more models that are configured in the simulator. A common way for a simulation entity to interact with a model is to implement an [_associated type_](#rinsimcoremodel) of a model.

{% include tip.html content="The [simple example](/examples/simple/) shows how a simple simulation with two models can be configured." %}


## _rinsim.core.model_

A _model_ in RinSim is a software entity that models something, usually a real-world concept (e.g. traveling over a road in the ``RoadModel``), but a model can also be a simulation utility (e.g. generating random numbers in the ``RandomModel``). A [Model](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/Model.java) has a generic type ``T`` that is called the _associated type_ (or _supported type_) of a ``Model``. A model that is part of a simulator, automatically receives all instances of its associated type that are added to the simulator. Using this mechanism it is also possible to implement [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) of a model into its associated type. An example:

```java
interface ExampleType {
  void injectExampleModel(ExampleModel m)
}

class ExampleModel extends AbstractModel<ExampleType> {

  public boolean register(ExampleType element){
    // dependency injection:
    element.injectExampleModel(this);
  }

  .. // other required methods
}
```

When this ``ExampleModel`` is part of the ``Simulator`` any object that implements ``ExampleType`` that is added to the simulator via ``Simulator.register(..)`` will receive the reference to ``ExampleModel`` via the ``injectExampleModel(..)`` method. Using this mechanism, RinSim achieves a modular and configurable design that enables reuse of models.

Besides the associated type, there are two other relationships that a ``Model`` can have: it can _provide_ types and it can have _dependencies_ on types. A model that has declared dependencies can only be used in a simulator with instances of those types available. For resolving dependencies, the simulator uses the provided types of a model.

The header comment of each ``Model`` implementation should contain the following section:
```java
/**
 * <p> 
 * <b>Model properties</b>
 * <ul>
 * <li><i>Associated type:</i> {@link ExampleType}.</li>
 * <li><i>Provides:</i> nothing.</li>
 * <li><i>Dependencies:</i> none.</li>
 * </ul>
 */
```

{% include tuto.html content="[How to implement your own model?](/learn/tutorials/model/)" %}

## _rinsim.core.model.time_

RinSim is a discrete time simulator, this means that time is sliced into fixed-length intervals called 'ticks'. [TimeModel](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/time/TimeModel.java) is the model that is responsible for the advancing of time. Its associated type is [TickListener](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/time/TickListener.java). On every tick, all registered `TickListener`s will be called in the order in which they are registered in the model. `TickListener`s can be registered [by calling the register(..) method of the Simulator](#rinsimcore) The figure below shows the order of execution of the `tick(..)` and `afterTick(..)` methods of the registered `TickListener`s.

{% include image.html file="design/ticks.png" alt="RinSim ticks" caption="This is a graphical depiction of what happens in a RinSim tick." %}

Implementing a `TickListener` allows you to receive ticks and receive a [TimeLapse](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/time/TimeLapse.java) reference, e.g.:
```java
class MyTickListener implements TickListener {
  
  public void tick(TimeLapse timeLapse){

  }

  public void afterTick(TimeLapse timeLapse){

  }
}
```

A `TimeLapse` is a _consumable_ interval of time: `[start, end)`. The difference between `start` and `end` is the tick length, which is constant within a simulation (it can be configured). The `TimeLapse` can be consumed, which means that it can be used as credit to spend on actions that require time. For example, moving over the [RoadModel](#rinsimcoremodelroad) requires time and can therefore only be done from within a `tick(..)` method, using the available time inside the `TimeLapse`. Since the travel distance is proportional to the amount of time available (among others), this mechanism allows RinSim to enforce time consistency for time dependent actions. Note that in the `afterTick(..)` method, the `TimeLapse` is always empty (can not be consumed), regardless of whether it was consumed in the `tick(..)` method.

{% include important.html content="Never store a reference to `TimeLapse` as it is mutable. Internally, there is just a single instance which is used for all `TickListener`s during the entire simulation." %}

Note that adding and removing a `TickListener` from within a `tick(..)` or `afterTick(..)`  only has effect _after all_ `tick(..)` or `afterTick(..)` invocations have been executed, respectively. For example, if you remove `TickListener` __A__ from within a `tick(..)` (could be from within the `tick(..)` of __A__ itself), the last call that __A__ receives is `tick(..)`, it will not receive its `afterTick(..)`.


{% include note.html content="The provided types of `TimeModel` are [Clock](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/time/Clock.java) and [ClockController](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/time/ClockController.java)" %}



### configuration

Since the `TimeModel` is such a central component to the simulator, it is added by default as a model in `Simulator`. This means that it can be configured via two different ways. Using the default `TimeModel`, via the `Simulator`:

```java
Simulator.builder()
         .setTimeUnit(..)    // allows to change the time unit e.g.: 
                             // SI.MILLI(SI.SECOND) or NonSI.MINUTE.
         .setTickLength(..)  // allows to change the length of the tick

```
Or by creating and adding the `TimeModel` manually:
```java
Simulator.builder()
         .addModel(
            TimeModel.builder()
                     .setTimeUnit(..).setTickLength(..) // same as above
                     .withRealTime() // only when needed, see real-time section below 
         )

```

### real-time
_Parts of the following text have been adapted from [van Lon, R.R.S. & Holvoet, T. (2017)](http://dx.doi.org/10.1007/s10458-017-9371-y) (section 3.1)._

The standard Java virtual machine (JVM) has no built-in support for real-time execution. However, with a careful software design, the standard JVM can be used to obtain soft real-time behavior. Soft real-time, as opposed to hard real-time, allows occasional deviations from the desired execution timing. 

When simulating without real-time constraints, the `TimeModel` will compute all ticks as fast as possible. In a real-time simulator the interval between the start of two ticks should be the tick length (e.g. 250 ms). Since the JVM doesn’t allow precise control over the timings of threads it is generally impossible to guarantee hard real-time constraints. In real-time mode, RinSim uses a dedicated thread for executing the ticks. If computations need to be done that are expected to last longer than a tick, they must be done in a different thread. RinSim provides a separate model for running solvers in a separate thread called `RtSolverModel` (see [this section](/design/central/#rtsolvermodel) for more information). This minimizes interference of `RtSolverModel` computations with the advancing of time in the simulated world as executed by the `TimeModel`. Additionally, the processor affinity of the threads are set at the operating system level. Setting the processor affinity to a Java thread instructs the operating system to use one processor exclusively for executing that thread. In practice, the actual scheduling of threads on processors depends on the number of available processors and the operating system. Informal tests on a multi core processor running Linux have shown that different threads are indeed run on different processor cores, exactly as specified. By setting the processor affinity of the `TimeModel` thread, deviations from the desired execution timing are minimized.

{% include note.html content="When simulating in real-time the `TimeModel` has an additional provided type: [RealtimeClockController](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/time/RealtimeClockController.java)." %}


{% include tuto.html content="[How to simulate in real-time?](/learn/tutorials/real-time/)" %}

## _rinsim.core.model.comm_

## _rinsim.core.model.pdp_

## _rinsim.core.model.rand_

## _rinsim.core.model.road_

Instances can be obtained via [RoadModelBuilders](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/road/RoadModelBuilders.java)

* [RoadModel](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/road/RoadModel.java)
  * PlaneRoadModel
    * CollisionPlaneRoadModel
  * GraphRoadModel
    * DynamicGraphRoadModel
      * CollisionGraphRoadModel

Associated types:
* RoadUser
  * MovingRoadUser

## _rinsim.util_
