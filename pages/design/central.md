---
title: Central
keywords: [central]
sidebar: design_sidebar
toc: false
permalink: /design/central/

---
__Maven artifactId:__ _rinsim-central_ 


This module provides functionality for using solvers for the PDPTW. They can be used in a centralized fashion (e.g. a single solver for all vehicles) as well as a decentralized fashion (e.g. a solver per vehicle). There is a package for usage in simulated time as well as a package for usage in real-time.

### _central_

By implementing the [Solver](https://github.com/rinde/RinSim/blob/master/central/src/main/java/com/github/rinde/rinsim/central/Solver.java) interface it is possible to 'solve' an instance of the PDPTW. It consists of one method:
```java
ImmutableList<ImmutableList<Parcel>> solve(GlobalStateObject state)
```

The `GlobalStateObject` is a value object that encodes the problem that needs to be solved by the solver. For every vehicle that occurs in the `GlobalStateObject`, the output of the `solve` method should contain a (possibly empty) list with the route that that vehicle should follow.

Usage:
 - __Centralized__: via `Central`, either using:
   - `Central.builder(..)` in case of using a regular `Simulator` instance.
   - or, `Central.solverConfiguration(..)` in case of using the [experiment API](/design/experiment/) (recommended).
 - __Decentralized__: via `SolverModel`. By adding `SolverModel` to the simulation, objects that implement the `SolverUser` interface can use a solver to solve their PDPTW instances. 


{% include tip.html content="The [RinLog](https://github.com/rinde/RinLog) project contains several `Solver` implementations." %}


### _central.rt_

The structure of this package is very similar to the _central_ package, but the implementation details are more complicated. The reason is that in real-time, the solvers have to be asynchronous. The computation result therefore arrives in a callback and not in the return of the method.

The [RealtimeSolver](https://github.com/rinde/RinSim/blob/master/central/src/main/java/com/github/rinde/rinsim/central/rt/RealtimeSolver.java) is the real-time equivalent of `Solver`.

{% include warning.html content="The remainder of this section is still under construction." %}

#### RtSolverModel


{% include image.html file="design/continuous-updates.png" alt="Continuous updates" caption="Continuous updates" %}

{% include image.html file="design/threads.png" alt="Threads" caption="Threads." %}