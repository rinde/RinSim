---
title: User Interface
keywords: [design]
sidebar: design_sidebar
toc: false
permalink: /design/ui/

---
__Maven artifactId:__ _rinsim-ui_ 

The user interface module provides a set of standard UI components which can be used for configuring a graphical user interface. By adding the `View` to the simulator as follows, the `Simulator` will, when `start()` is called, launch the UI instead of starting the simulation.
```java
Simulator.builder()
         .addModel(
           View.builder()
               .with(..) // add renderers 
         )
```

The `View` class provides a basic view that takes over the control of the simulator but it doesn't render anything itself. The UI is modularized using the notion of a `Renderer`. A `Renderer` renders (i.e. draws) on the screen. The `View` defines a canvas on which can be drawn, and UI components can be placed around the canvas. To facilitate this distinction, there are two types of renderers:
 - [CanvasRenderer](https://github.com/rinde/RinSim/blob/master/ui/src/main/java/com/github/rinde/rinsim/ui/renderers/CanvasRenderer.java), allows to draw on the canvas.
 - [PanelRenderer](https://github.com/rinde/RinSim/blob/master/ui/src/main/java/com/github/rinde/rinsim/ui/renderers/PanelRenderer.java), allows to define a panel that is placed alongside the canvas, can be used to show additional information, buttons, etc.

Since `Renderer`s are also `Model`s, dependencies can be requested similarly to regular models.

### Available renderers

| Renderer        | What is rendered                          | Example usage |
| ---                                       | ---     |
| [AGVRenderer](https://github.com/rinde/RinSim/blob/master/ui/src/main/java/com/github/rinde/rinsim/ui/renderers/AGVRenderer.java) | Renders vehicles as AGVs. | [AGV example](/learn/examples/agv/) |
| [CommRenderer](https://github.com/rinde/RinSim/blob/master/ui/src/main/java/com/github/rinde/rinsim/ui/renderers/CommRenderer.java) | Draws `CommDevice`s as dots, with a circle to indicate max range. | [Communication example](/learn/examples/communication/)  
| [GraphRoadModelRenderer](https://github.com/rinde/RinSim/blob/master/ui/src/main/java/com/github/rinde/rinsim/ui/renderers/GraphRoadModelRenderer.java) | Draws the graph as black lines. |[Taxi example](/learn/examples/taxi/) |
| [PDPModelRenderer](https://github.com/rinde/RinSim/blob/master/ui/src/main/java/com/github/rinde/rinsim/ui/renderers/PDPModelRenderer.java) | Renders the different PDP objects. |[Gradient field example](/learn/examples/gradientfield/) |
| [PlaneRoadModelRenderer](https://github.com/rinde/RinSim/blob/master/ui/src/main/java/com/github/rinde/rinsim/ui/renderers/PlaneRoadModelRenderer.java) | Draws the plane as a white square. | [Simple example](/learn/examples/simple/)
| [RoadUserRenderer](https://github.com/rinde/RinSim/blob/master/ui/src/main/java/com/github/rinde/rinsim/ui/renderers/RoadUserRenderer.java) | Allows to customize visualization of each `RoadUser`. | [Simple example](/learn/examples/simple/)
| [WarehouseRenderer](https://github.com/rinde/RinSim/blob/master/ui/src/main/java/com/github/rinde/rinsim/ui/renderers/WarehouseRenderer.java) | Advanced graph visualization that draws a corridor for each connection. | [AGV example](/learn/examples/agv/) |

Additional renderers and panels are defined in the [PDPTW module](/design/pdptw/#user-interface).

Each of these renderers can be added using their respective builder. Many of them also have several options for changing the visualization, the best way to explore all options is to use the auto-complete feature of your favorite IDE. For example, Eclipse shows this:
{% include image.html file="design/ui-builder-auto-complete.png" alt="RinSim ticks" caption="Auto-complete options as shown by Eclipse for WarehouseRenderer." %}

{% include tuto.html content="[How to implement a renderer?](/learn/tutorials/renderer/)" %}


### Shortcuts
The menu defines some shortcuts, for example for play/pause of the simulator (CTRL-P/CMD-P). If a French keyboard layout is detected, different short cuts are used. However, if the detection does not work, or if you want to customize the shortcuts, you can use the following code:
```java
View.builder()
  .withAccelerators(MenuItems.AZERTY_ACCELERATORS)
```