---
title: Experiment
keywords: [experiment]
sidebar: design_sidebar
toc: false
permalink: /design/experiment/

---

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


PostProcessor
Computer: multi-threading + Jppf support
CLI


link to experiment example

update experiment example (improve GUI)
