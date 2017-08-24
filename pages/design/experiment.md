---
title: Experiment
keywords: [experiment]
sidebar: design_sidebar
toc: false
permalink: /design/experiment/

---
__Maven artifactId:__ _rinsim-experiment_ 

The experiment module provides support for conducting scientific simulation experiments. The [Experiment](https://github.com/rinde/RinSim/blob/master/experiment/src/main/java/com/github/rinde/rinsim/experiment/Experiment.java) class is the main access point for all functionality in this module.

An experiment is comprised of [Scenario](/design/scenario/)s and `MASConfiguration`s. A `MASConfiguration` configures a multi-agent system (algorithm) that has to solve the problem as specified by the `Scenario`. Using the experiment builder it is very easy to construct a factorial experiment setup:
```java
Experiment.builder()
  .addConfiguration(config1)
  .addConfiguration(config2)
  .addScenario(scen1)
  .addScenarios(asList(scen2,scen3))
  .repeat(2)
  .perform();
```
In the example above there are two configurations and three scenarios. A total of `2 x 3 x 2 = 12` simulations will be run.

{% include tip.html content="The [experiment example](/learn/examples/experiment/) shows how the experiment API can be used to setup a simple experiment." %}

### MASConfiguration

The `MASConfiguration` class consists of a number of models and event handlers. The handlers allow the configuration to determine what should happen when a certain event occurs. For example, the configuration can respond to an event indicating that a new vehicle is added by creating a new agent that is responsible for controlling that vehicle. A `MASConfiguration` can specify a centralized algorithm, a decentralized algorithm, or any kind of hybrid algorithm. The models that are added in the configuration should not be needed to simulate the problem (as those should be part of a scenario), they should only be needed for the configuration itself.


### Other experiment features
 - `PostProcessor` allows to collect results from a Simulator instance when a simulation has ended.
 - Multi-threading support: see `withThreads(int)`.
 - Cloud computing support (using JPPF): see `computeDistributed()`.
 - Can be controlled via the commandline via `perform(PrintStream,String...)`.
