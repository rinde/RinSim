# Release notes

## v4.4.4
 * Vehicle.getSpeed() is now non-final, allowing subclasses to specifiy variable speed.
 * RinSim now requires JDK 8 or higher to be build, source code is still Java 7.

## v4.4.3
 * GraphRoadRenderer and WarehouseRenderer now update upon graph changes.
 * CommDevice.broadcast(MessageContents,double) added by [Tom Houben](https://github.com/tomhouben95).
 * Added code of conduct and contributing docs.
 * Updated examples and documentation.

## v4.4.2
 * Added support for GeomHeuristic in RouteFollowingVehicle (thanks to [Vincent Van Gestel](https://github.com/VincentVanGestel) for the implementation and [Christof Luyten](https://github.com/christofluyten) for finding the bug). 

## v4.4.1
 * Some small improvements in traffic generation system (thanks to [Vincent Van Gestel](https://github.com/VincentVanGestel)).

## v4.4.0
 * Added support for using solvers in combination with graphs (thanks to [Vincent Van Gestel](https://github.com/VincentVanGestel)).
 * Added support for dynamic speeds in graphs, including support in the scenario-util module (thanks to [Vincent Van Gestel](https://github.com/VincentVanGestel)).
 * Many bugfixes and small additions (thanks to [Christof Luyten](https://github.com/christofluyten) for several bugfixes).

## v4.3.0
 * Refactored MeasurableSolver system, introduced Measurable interface.
 * Refactored PDPRoadModel system for compatibility with GraphRoadModel.
 * GraphRoadMoel builder that links to an external graph can now be serialized in a scenario.

## v4.2.0
 * Created adapters to allow RealtimeSolver instances to run in simulated time.
 * Created MeasureableSolver and MeasureableRealtimeSolver interfaces that allows for measuring solver computation times.
 * Experiment now has an option to create composite tasks for use in distributed setting, this can improve throughput (by reducing number of messages sent) when runtime of a single simulation is relatively short while the total number of simulations is long.
 * Added convenience methods and bugfixes.

## v4.1.1
 * Improved JPPF integration
 * Added point methods by [Jens Claes](https://github.com/caske33), see [this commit](https://github.com/rinde/RinSim/commit/74b623ded305b3f82e63a77dfe4e5a1a4acaa210). 

## v4.1.0
 * Further refactored Experiment
 * Real-time clock system is stable now, including RtCentral and RtSolverModel.
 * Fixed bug related to Experiment and UI. Now using SWT 4.5.2.

## v4.0.0
 * Many classes have been refactored, including the builder system in core.
 * Experiment is more flexible using PostProcessors.
 * Several data value object have been refactored to use getter instead of fields and a factory method instead of a constructor.
 * ScenarioIO has been greatly improved, it no longer relies on any object serialization.
 * Support for real-time clock (not stable yet)

## v3.2.4
 * CommModel now supports unregister
 * CollisionGraphRoadModel new method: 
 ```
 public boolean isOccupiedBy(Point node, MovingRoadUser user)
 ```
 * DynamicGraphRoadModel new method: 
 ```
 public ImmutableSet<RoadUser> getRoadUsersOn(Point from, Point to)
 ```

## v3.2.3
 * CollisionGraphRoadModel bug fix: when a road user is removed from the model the position it was occupying is now released which was previously not the case.

## v3.2.2
 * CollisionGraphRoadModel is changed such that only implementors of MovingRoadUser (instead of RoadUser) are blocking. This means that depots and parcels are no longer blocking the way of AGVs which means that the PDPModel can now actually be used together with the CollisionGraphRoadModel.

## v3.2.1
 * CommUser.getPosition() now returns an optional instead of a position, CommModel and CommDevice have been adapted accordingly
 * Bug fixes

## v3.2.0
 * CommModel replaces the old CommunicationModel. Has a new API and is better tested.
 * Serializers have moved to graph.io package. Reworked API and fixed some bugs.

## v3.1.1
 * CollisionGraphRoadModel: occupancy of a node is now a smaller area.
 * Updated SWT version to 4.4
 * Several small bug fixes.

## v3.1.0
 * Core module: Simulator class now has a deprecated constructor and a new Builder class to construct instances.
 * Core module: __New:__ DynamicGraphRoadModel, which allows adding and removing connections to a graph while it is being used (with some restrictions).
 * Core module: __New:__ CollisionGraphRoadModel supports (the avoidance of) collisions between _RoadUser_ objects.
 * UI module: __New:__ WarehouseRenderer and AGVRenderer for visualizing the CollisionGraphRoadModel and its objects.
 * Examples module: __New:__ WarehouseExample
 * Geom module: Connection and ConnectionData are now immutable.

## v3.0.0
RinSim is restructured in a total of 14 modules:
 * Central
 * CLI
 * Core 
 * Event
 * Examples
 * Experiment
 * FSM
 * Geometry
 * IO
 * PDPTW
 * Scenario
 * Scenario Utils
 * Test Utils
 * UI

Starting with the current version RinSim targets Java 7 and uses [semantic versioning](http://semver.org), all minor releases will be continuously checked to make sure they are backwards compatible to the major release. Also, RinSim is now officially released under the Apache License, Version 2.0, a license header is added to every source file.

### New features
 - _Scenario Utils_: Added an advanced scenario generator framework that allows easy generation of entire datasets, it uses sensible defaults while allowing overriding and customizing all settings.
 - _CLI_: Created a command-line interface framework that allows to easily define a CLI that can be hooked to an existing API.
 - _Experiment_: The experiment API can now be used from the command-line.
 - _Experiment_: Added distributed computation support using JPPF library.

## v2.3.3
* Example module
	* Fixed problem of non-existing file in demo.

## v2.3.2
* Core module
	* StateMachine now optionally allows explicit recursive transitions.

* Problem module
	* Added PostProcessor option to Experiment class, this allows for customs statistics gathering of a simulation.
	* Gendreau06Parser now sorts files by filename

* Dependencies
	* updated Guava to 16.0.1

## v2.3.1

* Problem 
	* Gendreau06Parser is rewritten using a fluent API. Adds support for allowing diversion and offline scenarios

* Overall
	* Reduced runtime of tests

* Web
	* Added tutorial and troubleshooting tips
	* Added animated gif to homepage

## v2.3.0

* Core module
	* drop parcel method in PDPModel
	* RoadModel and PDPModel can now be decorated. Includes two Forwarding*Models

* UI module
	* The GUI can now be constructed via a fluent API (builder).
	* Accelerators are now user configurable
	* Improved tests
	* Fixed GUI bugs.
	* Improved documentation

* Problem module
	* new _rinde.sim.pdptw.experiment_ package that supports configuring and running experiments.
	* new _rinde.sim.pdptw.central_ package This package allows a centralized algorithm to target problems defined in the simulator.
	* new _rinde.sim.pdptw.generator_ package [beta] Will provide extensive support for generating scenarios using specific properties.

* Example module
	* improved and restructured examples

* Dependencies
	* Guava 15.0
	* SWT 4.3

## v2.2.0

* Core module
	* ScenarioController now sends setup events on scenario start.
	* PDPModel now dispatches an PARCEL_AVAILABLE event when a parcel becomes available.
	* Improved documentation and tests of state machine classes.
	
* UI module
	* No major changes.

* Problem module
	* Improved TimeLinePanel for visualizing PDPTW problems on a timeline.
	* Added PDPRoadModel, a special RoadModel that integrates nicely with PDPModel. It only allows vehicles to move to parcels or the depot. It can optionally disable vehicle diversion.
	* Scenario's now also define the units that they use (via new dependency on JScience) and they define the required models.
	
* Overall
	* Improved performance.

## v2.1.0 

* Core module
	* Added simple reusable state machine.
	* Added reusable implementation of specification pattern.
* UI module
	* Refactored Renderer system, now supports adding panels.
	* Fixed memory leaks of Color objects.
* Problem module
	* Added general problem class used for initializing entire problem configurations including custom agents, statistics, and fancy visualizations.
	* Refactored FabriRecht problem to use problem class.
	* Added Gendreau problem.