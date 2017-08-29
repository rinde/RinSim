---
title: AGV example
keywords: [agv]
sidebar: learn_sidebar
toc: false
permalink: /learn/examples/agv/
---

Example showing the functionality of [CollisionGraphRoadModel](https://github.com/rinde/RinSim/blob/master/core/src/main/java/com/github/rinde/rinsim/core/model/road/CollisionGraphRoadModel.java)

{% include image.html file="examples/agv-example.gif" alt="AGV example" caption="This is an animation of the autonomous guided vehicles example." %}

In this example the AGVs are moving randomly between locations. The model ensures that vehicles do not drive into each other, and the warehouse layout with only one-way roads ensures that deadlock situations are not possible. The code in this example can be modified to include a more complicated (and realistic) graph where deadlock situations are possible, look for this line:
```java
 RoadModelBuilders.dynamicGraph(GraphCreator.createSimpleGraph())
```
Replace it with:
```java
 RoadModelBuilders.dynamicGraph(GraphCreator.createGraph())
```

[View source](https://github.com/rinde/RinSim/blob/master/example/src/main/java/com/github/rinde/rinsim/examples/agv/AgvExample.java)