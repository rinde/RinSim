---
title: Design
keywords: [design]
sidebar: design_sidebar
toc: false
permalink: /design/

---

RinSim has a modular design which means its configuration is flexible and is very extensible. An important concept in RinSim is that of a [Model](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/Model.java). A model encapsulates a part of a simulation problem and/or a part of functionality. Ideally, a model represents a single concept such that it has high cohesion and can easily be reused in many contexts. An example is [RoadModel](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/road/RoadModel.java), one of the most used models in RinSim, it focuses solely on the simulation of transportation. By configuring RinSim with multiple models, it is possible to create a custom simulation environment tailored to the task at hand. The following figure shows a typical configuration:
{% include image.html file="design/rinsim-component.png" alt="Jekyll" caption="This is a [UML component diagram](https://en.wikipedia.org/wiki/Component_diagram) showing a typical, but simplified, RinSim configuration." %}
In the above example, RinSim is configured with a number of models that interact with each other. The ```MAS```, ```Solver```, and ```GUI``` components can also interact with these models (shown in the image as a single RinSim interface). In this example the ```MAS``` and ```Solver``` components represent algorithms that are using various RinSim APIs.


The code is organized in a number of Maven modules:

__Main modules__:
- Core
- User Interface
- PDPTW
- Scenario Utilities
- Scenario
- Experiment
- Central

__Auxiliary modules__:
- CLI
- Event
- FSM
- Geom
- IO

## Main modules



### UI

### PDPTW

### Scenario

### Scenario Util

### Experiment

### Central

## Auxiliary modules

### CLI

### Event

### FSM

### Geom

### IO


<!-- 
Topics todo:
 - Model details: how it works, how to use, how to create a model
 - TimeModel
 	- TickListener, TimeLapse, show tick image
 	- real-time
 - RoadModels:
 	- PlaneRoadModel
 	- GraphRoadModel
 	- DynamicGraphRoadModel / CollisionGraphRoadModel
 - PDPModel
 - CommModel
 - RandomModel

 - Maven Modules overview 
 	- central
 	- cli
 	- core
 	- event
 	- example
 	- experiment
 	- fsm
 	- geom
 	- io
 	- pdptw
 	- scenario
 	- scenario-util
 	- test-util
 	- ui
 - Follows Guava's conventions (i.e. RoadModels)
 - Talk about AutoValue, what is it how is it used. How to install?

-->