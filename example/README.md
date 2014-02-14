# RinSim Examples

RinSim comes with a number of examples. The examples listed on this pages are ordered in order of increasing complexity. The examples can be run ...

## Core examples
These are examples showcasing functionality from the `RinSim-core` project.

 - [SimpleExample](src/main/java/rinde/sim/examples/core/SimpleExample.java) The RinSim 'hello world' example.
 - [AgentCommunicationExample](src/main/java/rinde/sim/examples/core/comm/AgentCommunicationExample.java) [TODO]
 - [TaxiExample](src/main/java/rinde/sim/examples/core/taxi/TaxiExample.java) Showcase of a dynamic pickup and delivery problem (PDP). New customers are continuosly placed on the map. The strategy each vehicle follows is: (1) goto closest customer, (2) pickup customer, (3) drive to destination, (4) deliver customer, go back to (1). In case multiple vehicles move to the same customer, the first one to arrive will pick the customer up, the others will have to change their destination.
 - ScenarioExample [TODO] example showing how a scenario can be created.
 - ModelExample [TODO] example showing how a custom model can be created.


## Problem examples

 - ExperimentsExample [TODO] example showing how an experiment can be setup.
 - GendreauExample [TODO] example showing how the dataset from Gendreau et al. can be used in an experiment
 - AgentsExample [TODO] example showing how a custom agent system can be using in an experiment.
 - SolverExample [TODO] example showing how a solver algorithm can be used to centrally control all agents.

## UI examples

 - VisualizationShowcase [TODO] example showing all available visualizations.
 - VisualizationExample [TODO] example showing how a custom visualization can be setup.