---
title: Experiment example
keywords: [experiment, example]
sidebar: learn_sidebar
toc: false
permalink: /learn/examples/experiment/
---

When you run the experiment example, this is what you see:
{% include image.html file="examples/experiment-example.gif" alt="Experiment example" caption="This is an animation of the experiment example." %}

The code that is showcased here is from the [experiment module](/design/experiment/), the visualization is from the [PDPTW module](/design/pdptw/).

In the top of the window the `TimeLinePanel` is shown. This panel shows a live view of the current time and the time windows of the parcels. For every parcel there is a small black vertical line that indicates the announce time, the blue box (top) indicates the pickup time window and the red box (bottom) indicates the delivery time window.

In the left of the window there are two panels, but only one (`RoutePanel`) is visible. The `StatsPanel` can be made visible by clicking on the 'Statistics' tab. The `RoutePanel` contains a table with three columns, the first column shows the name of the vehicle, the second column shows the length of the route, and the third columns shows a string representation of the route. In this example, the names are made human readable (see the code for details).

There is justs one vehicle in this example, it is a subclass of the [RouteFollowingVehicle](/design/pdptw/#routefollowingvehicle). The behavior is very simple: when a new parcel arrives the vehicle just adds this parcel at the end of its route. Because the `RouteRenderer` is also used, there is a black line drawn from the vehicle to its destinations (pickup and delivery locations of each parcel).

[View the code](https://github.com/rinde/RinSim/blob/master/example/src/main/java/com/github/rinde/rinsim/examples/experiment/ExperimentExample.java)