---
title: UAV example
keywords: [UAV]
sidebar: learn_sidebar
toc: false
permalink: /learn/examples/uav/
---
Example showing the functionality of the [CollisionPlaneRoadModel](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/road/CollisionPlaneRoadModel.java).

{% include image.html file="examples/uav-example.gif" alt="UAV example" caption="This is an animation of the unmanned aerial vehicles example." %}

In this example, the UAVs are randomly moving over the plane. When two UAVs collide, the model will make sure that their areas do not overlap. If a UAV detects that it is stuck for more than 10 ticks it will move in another (random) direction.

[View code](https://github.com/rinde/RinSim/blob/master/example/src/main/java/com/github/rinde/rinsim/examples/uav/UavExample.java)