---
title: Installation
keywords: installation
sidebar: installation_sidebar
toc: false
permalink: installation_index.html
folder: installation
---

RinSim uses [Maven](http://maven.apache.org/) for managing its dependencies. RinSim can be added to your Maven project by including the following in your pom file, where x and y represents the preferred version number. More __[detailed instructions](docs/howtorun.md)__ are available.

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.rinde/rinsim-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.rinde/rinsim-core)
```xml
<dependency>
	<groupId>com.github.rinde</groupId>
	<artifactId>rinsim-core</artifactId>
	<version>x.y.z</version>
</dependency>
```	

For more detailed instructions on how create a Maven project and add RinSim as a dependency see the [instructions for Eclipse](eclipse.md) or [instructions for IntelliJ]().