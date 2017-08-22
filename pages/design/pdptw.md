---
title: PDPTW
keywords: [pdptw]
sidebar: design_sidebar
toc: false
permalink: /design/pdptw/

---

The PDPTW module contains implementations that support the simulation of the pickup-and-delivery problem with time windows (PDPTW).

### RouteFollowingVehicle

The [RouteFollowingVehicle](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/RouteFollowingVehicle.java) is a subclass of [Vehicle](/design/core/#modelpdp) with a very simple API. The behavior of this vehicle can be controlled via the ``setRoute(Iterable<Parcel>)`` method. The route consists of one or more parcels, each parcel can occur at maximum twice. The first occurence indicates a pickup, the second occurence indicates a delivery. In case this method is called at a time when the vehicle already has a parcel in cargo, the number of occurences for that parcel is limited to one and will be interpreted as a delivery. The behavior of the vehicle is implemented as a finite state machine, which is visualized below:
{% include image.html file="design/routefollowingvehicle-fsm.png" alt="RouteFollowingVehicle statemachine" caption="RouteFollowingVehicle statemachine. The diagram is obtained by running [dot](http://graphviz.org/) on the dot syntax that was exported from the RouteFollowingVehicle class." %}

As can be seen, there are four states: ``Wait``, ``Goto``, ``WaitAtService``, and ``Service``. There are six triggers (state events) which are documented [here](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/RouteFollowingVehicle.java#L553). The behavior of the vehicle can be overridden by changing the state machine.

### PDPRoadModel

The ``PDPRoadModel`` is a convenience decorator of ``RoadModel``s. It simplifies the API for moving vehicles in the PDPTW case as it allows to always just call ``moveTo(vehicle,parcel)``, depending on the state of the parcel this will let the vehicle move to either the pickup site or the delivery site.

| RoadModel decorator                                                                                                                                                           | Underlying RoadModel           |
| ---                                                                                                                                                                 | ---                         |
| [PDPRoadModel](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/PDPRoadModel.java)                              | ``RoadModel``               |                      
| [PDPGraphRoadModel](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/PDPGraphRoadModel.java)                    | ``GraphRoadModel``          |
| [PDPDynamicGraphRoadModel](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/PDPDynamicGraphRoadModel.java)      | ``DynamicGraphRoadModel``   |
| [PDPCollisionGraphRoadModel](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/PDPCollisionGraphRoadModel.java)  | ``CollisionGraphRoadModel`` |

### Events

This module provides several default implementations of ``TimedEvent`` (see [scenario module](/design/scenario/)) which are useful for PDPTWs:
 - [AddDepotEvent](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/AddDepotEvent.java)
 - [AddParcelEvent](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/AddParcelEvent.java)
 - [AddVehicleEvent](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/AddVehicleEvent.java)
 - [ChangeConnectionSpeedEvent](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/ChangeConnectionSpeedEvent.java)
 
### Statistics

With [StatsTracker](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/StatsTracker.java) it is possible to track a number of statistics relevant for PDPTWs.

| __StatsTracker__ |
|Associated type: | none          |
|Provides:        | `StatisticsProvider`         |
|Dependencies:      | `ScenarioController`, `Clock`, `RoadModel`, `PDPModel` |

Via the `StatisticsProvider` instances of `StatisticsDTO` can be obtained which can be useful to analyse the result of a simulation. Additionally, these statistics can be used for custom stop conditions (see [StatsStopConditions](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/StatsStopConditions.java) ) and for computing an objective value via implementations of [ObjectiveFunction](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/ObjectiveFunction.java).

### User interface 

The module provides several UI extensions that visualize functionality from this module:

| Renderer / Panel       | What is rendered                          |
| ---                                       | ---     |
| [TimeLinePanel](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/TimeLinePanel.java)  | A timeline indicating for each parcel, the pickup timewindow and the delivery timewindow.| |
| [RoutePanel](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/RoutePanel.java)        | Shows a table with a row per RouteFollowingVehicle detailing its route. | |
| [StatsPanel](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/StatsPanel.java)        | Presents a live view on the stats in a side panel. | |
| [RouteRenderer](https://github.com/rinde/RinSim/blob/master/pdptw/src/main/java/com/github/rinde/rinsim/pdptw/common/RouteRenderer.java)  | Renders the current route for each RouteFollowingVehicle. | |


