---
title: Taxi example
keywords: [taxi]
sidebar: learn_sidebar
toc: false
permalink: /learn/examples/taxi/
---

Showcase of a dynamic pickup and delivery problem (PDP).
{% include image.html file="examples/taxi-example.gif" alt="Taxi example" caption="This is an animation that shows the visualization of the taxi example in the city of Leuven. The persons are customers waiting for pickup, the number above a taxi indicates the number of customers that are onboard. The building indicates the depot." %}

New customers are continuosly placed on the map. The strategy each [taxi](https://github.com/rinde/RinSim/blob/master/example/src/main/java/com/github/rinde/rinsim/examples/core/taxi/Taxi.java) follows is: 
  1. goto closest customer, 
  2. pickup customer, 
  3. drive to destination, 
  4. deliver customer, go back to 1. 

In case multiple vehicles move to the same customer, the first one to arrive will service the customer, the others will have to change their destination.

[View the code](https://github.com/rinde/RinSim/blob/master/example/src/main/java/com/github/rinde/rinsim/examples/core/taxi/TaxiExample.java)
