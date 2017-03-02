---
title: Examples
keywords: [examples]
sidebar: examples_sidebar
toc: false
permalink: /examples/
---

RinSim comes with a number of examples, each example listed here can be run directly from the jar file. Instructions on how to run them can be found [here](/installation/).

## Core examples
These are examples showcasing functionality from the `RinSim-core` project.

 - [SimpleExample](src/main/java/com/github/rinde/rinsim/examples/core/SimpleExample.java) The RinSim 'hello world' example.
 - [AgentCommunicationExample](src/main/java/com/github/rinde/rinsim/examples/core/comm/AgentCommunicationExample.java) An example showing agent communication.
 - [TaxiExample](src/main/java/com/github/rinde/rinsim/examples/core/taxi/TaxiExample.java) Showcase of a dynamic pickup and delivery problem (PDP). New customers are continuosly placed on the map. The strategy each vehicle follows is: (1) goto closest customer, (2) pickup customer, (3) drive to destination, (4) deliver customer, go back to (1). In case multiple vehicles move to the same customer, the first one to arrive will pick the customer up, the others will have to change their destination.
  - [WarehouseExample](src/main/java/com/github/rinde/rinsim/examples/warehouse/WarehouseExample.java) An example showing AGVs driving around a warehouse.

<!-- 
 - ScenarioExample [TODO] example showing how a scenario can be created.
 - ModelExample [TODO] example showing how a custom model can be created. 
-->


## Problem examples

- [GradientFieldExample](src/main/java/com/github/rinde/rinsim/examples/pdptw/gradientfield/GradientFieldExample.java) example showing how the dataset from Gendreau et al. can be used in an experiment using a gradient field multi-agent approach.

<!--
 - ExperimentsExample [TODO] example showing how an experiment can be setup.
 - AgentsExample [TODO] example showing how a custom agent system can be using in an experiment.
 - SolverExample [TODO] example showing how a solver algorithm can be used to centrally control all agents.

## UI examples

 - VisualizationShowcase [TODO] example showing all available visualizations.
 - VisualizationExample [TODO] example showing how a custom visualization can be setup.
 -->