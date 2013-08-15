# Release notes

## v2.3.0

* Core module

* UI module

* Problem module
	* rinde.sim.pdptw.generator package
	* rinde.sim.pdptw.central package


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