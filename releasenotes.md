# Release notes

## v2.3.2
* Core module
	* StateMachine now optionally allows explicit recursive transitions.

* Problem module
	* Added PostProcessor option to Experiment class, this allows for customs statistics gathering of a simulation.
	* Gendreau06Parser now sorts files by filename

* Dependencies
	* Guava 16.0.1

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