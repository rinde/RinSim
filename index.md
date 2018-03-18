---
title: RinSim
keywords: home
tags: 
permalink: /
sidebar:
toc: false
---
<!-- https://deepnn.github.io/index.html -->
<script async defer src="https://buttons.github.io/buttons.js"></script>
<a class="github-button" href="https://github.com/rinde/rinsim" data-size="large" data-show-count="true" aria-label="Star rinde/rinsim on GitHub">Star</a>
<a class="github-button" href="https://github.com/rinde/rinsim/subscription" data-size="large" data-show-count="true" aria-label="Watch rinde/rinsim on GitHub">Watch</a>
<a class="github-button" href="https://github.com/rinde/rinsim/fork" data-size="large" data-show-count="true" aria-label="Fork rinde/rinsim on GitHub">Fork</a>

RinSim is an extensible logistics simulator with support for (de)centralized algorithms for pickup and delivery problems and AGV routing. The simulator focuses on __simplicity__ and __consistency__ making it ideal for performing scientific simulations. Further, software quality is a priority resulting in an ever improving test suite and documentation.

[![Javadocs](https://javadoc.io/badge/com.github.rinde/rinsim-core.svg?color=red)](https://javadoc.io/doc/com.github.rinde/rinsim-core){:.no_icon} 
[![Build Status](https://travis-ci.org/rinde/RinSim.svg?branch=master)](https://travis-ci.org/rinde/RinSim){:.no_icon} 
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.rinde/rinsim-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.rinde/rinsim-core){:.no_icon} 
[![DOI](https://zenodo.org/badge/DOI/10.5281/zenodo.571471.svg)](https://doi.org/10.5281/zenodo.571471){:.no_icon} 
[![Stackoverflow](https://img.shields.io/badge/stackoverflow-rinsim-orange.svg)](http://stackoverflow.com/questions/tagged/rinsim){:.no_icon} 



![Taxi Demo](images/taxi-demo.gif)

## Features

 - <span class="glyphicon glyphicon-time"></span> Discrete time simulator, with support for real-time and simulated time simulations.
 - <i class="fa fa-exchange"></i> Explicit separation of problem and solution.
 - <i class="fa fa-object-group"></i> Flexible graphical user interface.
 - <i class="fa fa-sitemap"></i> Experiment framework for easy setup of factorial experiments, supports distributed computing on many CPUs via [JPPF](https://jppf.org).
 - <span class="glyphicon glyphicon-stats"></span> Support for statistics gathering.
 - <i class="fa fa-history"></i> Supports saving and loading problem scenarios, a scenario constitutes a sequence of (dynamic) events. There is a flexible API for creating scenarios based on probability distributions. 
 - <i class="fa fa-cubes"></i> Support for centralized and decentralized algorithms.
 - <i class="fa fa-coffee"></i> Supports Java 7 and higher, available on Maven Central.


### Problem domains 
 - <i class="fa fa-truck"></i> Dynamic/static pickup-and-delivery problems
 - <i class="fa fa-car"></i> Dynamic/static vehicle routing problems
 - <i class="fa fa-map-marker"></i> Dynamic/static multi-agent route planning

### Consistency and quality
 - <i class="fa fa-check"></i> Thoroughly tested
 - <i class="fa fa-book"></i> Well documented
 - <i class="fa fa-flask"></i> Emphasis on scientific correctness, focus on internal consistency 
 - <i class="fa fa-puzzle-piece"></i> Modular 
 - <span class="fa fa-sliders"></span> Configurable
 - <i class="fa fa-github"></i> [Open source](https://github.com/rinde/RinSim/)

