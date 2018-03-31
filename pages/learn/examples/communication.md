---
title: Communication example
keywords: [communication]
sidebar: learn_sidebar
toc: false
permalink: /learn/examples/communication/
---
Example of using communication facilities, using the [communication model](/design/core/#modelcomm).

{% include image.html file="examples/communication-example.gif" alt="Communication example" caption="This is an animation of the communication example." %}

Here you can see several agents randomly driving around. The color of each agent indicates the reliability of its communication device, where green is reliable and red is unreliable. The circle around each agent indicates its communication range. Consider agent A and B. If agent A is contained inside the circle of agent B, it can receive messages from agent B. Only if agent B is also contained inside the circle of agent A, can agent A reply.

## Relevant classes

|-------|--------|
| [CommExample](https://github.com/rinde/RinSim/blob/master/example/src/main/java/com/github/rinde/rinsim/examples/comm/CommExample.java) | Contains the main of the example. It configures the simulator models and visualization and adds several agents. |
| [ExampleCommunicatingAgent](https://github.com/rinde/RinSim/blob/master/example/src/main/java/com/github/rinde/rinsim/examples/comm/ExampleCommunicatingAgent.java) | An example implementation of a `CommUser`. It shows how broadcasting and direct messaging works. Take a look at the `tick(..)` method for details. |
