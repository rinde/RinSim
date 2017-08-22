---
title: Scenario
keywords: [scenario]
sidebar: design_sidebar
toc: false
permalink: /design/scenario/

---


The scenario module provides classes that allow to specify the problem side of a simulation as a ``Scenario``. A ``Scenario`` is mainly a list of events that describe what will happen when and a set of models that define parts of the problem. To use a scenario in a simulation, you can wrap it in a ``ScenarioController`` as follows:

```java
Simulator.builder()
  .addModel(
    ScenarioController.builder(scenario)
      // configure controller here
  )
  .build()

```
Scenario is a value object and a ``ScenarioController`` is an active component that dispatches the events and registers the models.

| __ScenarioController__ |
|Associated type: | none          |
|Provides:        | `ScenarioController`         |
|Dependencies:    | `SimulatorAPI`, `ClockController` |

### TimedEvent

The events in a scenario have to be of type ``TimedEvent``. Each ``TimedEvent`` occurs on a specific time (specified as a long value). Since the scenario is meant to only encode problem specific parts of the simulation, details about algorithms should not be part of a scenario. However, an algorithm may want to react to an event (e.g. a change in the problem). For that reason, it is possible to add ``TimedEventHandler``s to the ``ScenarioController`` like so:

```java
ScenarioController.builder(scenario)
  .withEventHandler(MyEvent.class, new MyEventHandler())
```
In this example, the ``MyEventHandler`` instance will handle all events of type ``MyEvent`` that are defined in the scenario. The handler could for example inform the algorithm of the change in the problem, or it could create an agent to represent the information that is contained in the ``MyEvent``.



{% include tip.html content="There are default implementations of ``TimedEvent``s (and their handlers) for the [PDPTW](/design/pdptw/#events)" %}

{% include tip.html content="If you are interested in constructing an entire dataset of scenarios, you might find the [scenario utilities](/design/scenario-util/) useful." %}

### Configuring a scenario

``Scenario`` has a builder that can be used to configure scenarios as follows:

```java
Scenario.builder()
  .setStopCondition(..) // allows to set a stop condition: when should the simulation terminate?
  .addEvent(..) // allows to add TimedEvent
  .addModel(..) // allows to add models
  .instanceId(..) // a string identifying this particular scenario instance
  .problemClass(..) // an instance of ProblemClass that identifies the type of problem, should be a value object.

```

{% include tip.html content="There are default implementations of ``StopCondition`` in [StopConditions](https://github.com/rinde/RinSim/blob/master/scenario/src/main/java/com/github/rinde/rinsim/scenario/StopConditions.java)" %}

### ScenarioIO

Using [ScenarioIO]() it is possible to read and write scenario instances to disk using the JSON format. For example:

```java  
Path file = // some path
ScenarioIO.write(scenarioIn, file);
Scenario scenarioOut = ScenarioIO.reader().apply(file);

scenarioIn.equals(scenarioOut); // returns true
```