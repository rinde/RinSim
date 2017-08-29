---
title: Simple example
keywords: [simple]
sidebar: learn_sidebar
toc: false
permalink: /learn/examples/simple/
---

When you run the simple example (instructions for [Eclipse](/installation/eclipse/), [IntelliJ](/installation/intellij/)), this is what you see:
{% include image.html file="examples/simple-example.gif" alt="Simple example" caption="This is an animation of the simple example." %}


The effect that is visible (the agents all converging on the center but never quite reaching it) is an emergent property of the individual behavior of the vehicles.

The code that is responsible for this particular effect is this method in the ``Driver`` class:
```java
@Override
public void tick(TimeLapse timeLapse) {
  if (!roadModel.containsObject(this)) {
    roadModel.addObjectAt(this, roadModel.getRandomPosition(rnd));
  }
  roadModel.moveTo(this, roadModel.getRandomPosition(rnd), timeLapse);
}
```
{% include tip.html content="A good exercise for better understanding how discrete time simulation (like in RinSim) works is to try to change the behavior of these drivers such that they start driving in straight lines towards their destinations." %}


[View the code](https://github.com/rinde/RinSim/blob/master/example/src/main/java/com/github/rinde/rinsim/examples/core/SimpleExample.java)

