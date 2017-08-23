---
title: Scenario Utilities
keywords: [scenario utilities]
sidebar: design_sidebar
toc: true
permalink: /design/scenario-util/

---

This module consists of a number of [scenario](/design/scenario/) related packages.

### _scenario.generator_

Contains many utilities for drawing variables based on probability distributions that are useful for constructing scenarios. The [ScenarioGenerator](https://github.com/rinde/RinSim/blob/master/scenario-util/src/main/java/com/github/rinde/rinsim/scenario/generator/ScenarioGenerator.java) class allows to construct PDPTW datasets (multiple scenarios) based on specific properties. 

Typical usage of the `ScenarioGenerator` may look something like this:
```java
ScenarioGenerator generator = ScenarioGenerator.builder()
  .vehicles(vehicleGenerator)
  .parcels(parcelGenerator)
  .depots(depotGenerator)
  .addModel(..)
  .build();

List<Scenario> scenarios = new ArrayList<>();
for( int i = 0; i < numberOfDesiredScenarios; i++){
  scenarios.add(generator.generate(..,..));
}
```

For the methods in the example above, there are the following helper classes for supplying values:
 - `Vehicles`
 - `Parcels`
 - `Depots`

With `Parcels` it is for example possible to create a `ParcelGenerator` as follows:
```java
Parcels.builder()
  // sets the pickup durations of all parcels to '10'
  .pickupDurations(StochasticSuppliers.constant(10L))
  // the delivery durations for each parcel will be drawn from a normal distribution N(10,3) (rounded to a long value)
  .deliveryDurations(
    StochasticSuppliers.normal()
      .mean(10)
      .std(3)
      .buildLong())
  // the pickup and delivery locations will both be draw uniformly in a plane
  .locations(
    Locations.builder()
      .min(new Point(0, 0))
      .max(new Point(10, 10))
      .buildUniform())
  // the announce times of the parcels will be based on a Poisson process
  .announceTimes(TimeSeries.homogenousPoisson(500, 20))
  // the time windows for each parcel are created using the TimeWindows helper class
  .timeWindows(
    TimeWindows.builder()
      .pickupUrgency(StochasticSuppliers.uniformLong(5, 10))
      .deliveryOpening(StochasticSuppliers.constant(10L))
      .build())
  .build(); // returns a ParcelGenerator instance

```
As can be seen from this example, this module contains several additional helper classes for constructing properties based on probability distributions such as `TimeWindows`, `TimeSeries`, and `Locations`. Each of these helper classes in turn usually accepts instances of type `StochasticSupplier`. Using the [StochasticSuppliers](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/util/StochasticSuppliers.java) utility class, it is possible to construct suppliers for constants, uniform random, and normal random.

{% include note.html content="Based on the functionality of this package, a separate [dataset generator](https://github.com/rinde/pdptw-dataset-generator/) was created. " %}

### _scenario.measure_

This package contains some utility methods for measuring scenarios. The most notable methods are in the [Metrics](https://github.com/rinde/RinSim/blob/master/scenario-util/src/main/java/com/github/rinde/rinsim/scenario/measure/Metrics.java) class:

 - `measureUrgency(Scenario)`
 - `measureDynamism(Scenario)`

The concepts of urgency and dynamism were first introduced in the following paper:
{% include cite.html authors="van Lon R.R.S., Ferrante E., Turgut A., Wenseleers T., Vanden Berghe G., Holvoet T." title="Measures of dynamism and urgency in logistics" venue="European Journal of Operational Research" details="volume 253, issue 3, pages 614-624" year="2016" doi="10.1016/j.ejor.2016.03.021" lirias="https://lirias.kuleuven.be/bitstream/123456789/540032/6/van-Lon-et-al-2016-Measures-of-dynamism-and-urgency-in-logistics.pdf" %}

### _scenario.vanlon15_

This package contains the `ProblemClass` for the scenarios that were constructed for the following paper:

{% include cite.html authors="van Lon R.R.S., Holvoet T." title="Towards systematic evaluation of multi-agent systems in large scale and dynamic logistics" venue="PRIMA 2015: Principles and Practice of Multi-Agent Systems" details="Vol. 9387, (pp. 248-264)" year="2015" doi="10.1007/978-3-319-25524-8_16" lirias="https://lirias.kuleuven.be/bitstream/123456789/512404/1/van-Lon%2C-Holvoet-2015-Towards-systematic-evaluation-of-multi-agent-systems-in-large-scale-and-dynamic-logistics.pdf" %}
The dataset generator that is presented in this paper can be found [here](https://github.com/rinde/pdptw-dataset-generator/).

### _scenario.gendreau06_
Contains code for parsing scenarios as used in this paper:
{% include cite.html authors="Gendreau, M., Guertin, F., Potvin, J.-Y., and Séguin, R." title="Neighborhood search heuristics for a dynamic vehicle dispatching problem with pick-ups and deliveries" venue="Transportation Research Part C: Emerging Technologies" details="14, 3, 157-174" year="2006" doi="10.1016/j.trc.2006.03.002" %}

