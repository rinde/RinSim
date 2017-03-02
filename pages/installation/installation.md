---
title: Installation
keywords: installation
sidebar: installation_sidebar
toc: false
permalink: /installation/
folder: installation
---

RinSim uses [Maven](http://maven.apache.org/) for managing its dependencies. The core module of RinSim can be added to your Maven project by including the following in your pom file, where x, y, and z represents the preferred version number. More detailed instructions are available for [Eclipse](/installation/eclipse/) and [IntelliJ](/installation/intellij/).

```xml
<dependency>
	<groupId>com.github.rinde</groupId>
	<artifactId>rinsim-core</artifactId>
	<version>x.y.z</version>
</dependency>
```	

The latest RinSim version is [![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.rinde/rinsim-core/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.rinde/rinsim-core){: .no_icon}