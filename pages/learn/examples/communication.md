---
title: Communication example
keywords: [communication]
sidebar: learn_sidebar
toc: false
permalink: /learn/examples/communication/
---

{% include image.html file="examples/communication-example.gif" alt="Communication example" caption="This is an animation of the communication example." %}

Here you can see several agents randomly driving around. The color of each agent indicates the reliability of its communication device, where green is reliable and red is unreliable. The circle around each agent indicates its communication range. Consider agent A and B. If agent A is contained inside the circle of agent B, it can receive messages from agent B. Only if agent B is also contained inside the circle of agent A, can agent A reply.

[View the code](https://github.com/rinde/RinSim/blob/master/example/src/main/java/com/github/rinde/rinsim/examples/comm/CommExample.java)