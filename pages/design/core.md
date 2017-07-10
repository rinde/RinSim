---
title: Core
keywords: [design]
sidebar: design_sidebar
toc: true
permalink: /design/core/

---

The core module contains the core simulation components. The core module is the entry point for getting to know the basics of RinSim. It also contains the most used models.

There are generally two classes that can be used to configure RinSim:

 - [rinsim.core.Simulator](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/Simulator.java), a simple and direct way to interact with the simulator. This is the recommended way to learn how RinSim works. Explained in this section.
 - [rinsim.experiment.Experiment](https://github.com/rinde/RinSim/blob/master/experiment/src/main/java/com/github/rinde/rinsim/experiment/Experiment.java), a more advanced interface that encapsulates Simulator, it is specifically designed for (scientific) experiments. Explained in the [Experiment section](/design/experiment/).

## _rinsim.core_

The simulator is in essence a collection of models. When configuring the simulator, using ``Simulator.builder()``, the desired models can be added. For ease of use, the simulator contains a ``TimeModel`` and a ``RandomModel`` by default. The configuration phase is concluded by calling ``build()`` of the ``Simulator.Builder`` class which returns a ``Simulator`` instance.

```java
Simulator sim = Simulator.builder()
                         .addModel(...)
                         // add more models
                         .build()
```                            

The ``Simulator`` object provides several methods for controlling the simulation. It also has a ``register()`` method that allows to register simulation entities into the simulator. Typically, a simulation entity interacts with one or more models that are configured in the simulator. A common way for a simulation entity to interact with a model is to implement an [_associated type_](#rinsimcoremodel) of a model.

{% include tip.html content="The [simple example](/examples/simple/) shows how a simple simulation with two models can be configured." %}


## _rinsim.core.model_

A _model_ in RinSim is a software entity that models something, usually a real-world concept (e.g. traveling over a road in the ``RoadModel``), but a model can also be a simulation utility (e.g. generating random numbers in the ``RandomModel``). A [Model](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/Model.java) has a generic type ``T`` that is called the _associated type_ (or _supported type_) of a ``Model``. A model that is part of a simulator, automatically receives all instances of its associated type that are added to the simulator. Using this mechanism it is also possible to implement [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) of a model into its associated type. An example:

```java
interface ExampleType {
  void injectExampleModel(ExampleModel m)
}

class ExampleModel extends AbstractModel<ExampleType> {

  public boolean register(ExampleType element){
    // dependency injection:
    element.injectExampleModel(this);
  }

  .. // other required methods
}
```

When this ``ExampleModel`` is part of the ``Simulator`` any object that implements ``ExampleType`` that is added to the simulator via ``Simulator.register(..)`` will receive the reference to ``ExampleModel`` via the ``injectExampleModel(..)`` method. Using this mechanism, RinSim achieves a modular and configurable design that enables reuse of models.

Besides the associated type, there are two other relationships that a ``Model`` can have: it can _provide_ types and it can have _dependencies_ on types. A model that has declared dependencies can only be used in a simulator with instances of those types available. For resolving dependencies, the simulator uses the provided types of a model.

The header comment of each ``Model`` implementation should contain the following section:
```java
/**
 * <p> 
 * <b>Model properties</b>
 * <ul>
 * <li><i>Associated type:</i> {@link ExampleType}.</li>
 * <li><i>Provides:</i> nothing.</li>
 * <li><i>Dependencies:</i> none.</li>
 * </ul>
 */
```

{% include tuto.html content="[How to implement your own model?](/learn/tutorials/model/)" %}

## _rinsim.core.model.time_

RinSim is a discrete time simulator. Add figures from paper here about ticks, real-time, etc.

{% include image.html file="design/ticks.png" alt="Simple example" caption="This is a screenshot of the simple example." %}

## _rinsim.core.model.comm_

## _rinsim.core.model.pdp_

## _rinsim.core.model.rand_

## _rinsim.core.model.road_

## _rinsim.util_
