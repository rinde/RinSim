# RinSim

RinSim is an extensible MAS (Multi-Agent System) simulator.
It supports pluggable models which allow to extend the scope of the simulator.
Out of the box, RinSim currently focusses on MAS for PDP (Pickup and Delivery Problems). 
You can easily extend RinSim by introducing your own custom models.

## Getting Started

The best way to get the simulator and handle future updates is to use git.

Alternatively you can use a zip that contains the current version of RinSim.

### Using the Zip file

__Only use this method to tryout the simulator. Updating RinSim using this method will be annoying.__

* Download the zipped project [here](http://TODO).
* Unzip the project to your desired location.
* Open eclipse and select _File -> Import... -> General -> Existing Projects into Workspace_
* Browse to the directory where you unzipped the project.
* Click _finish_

### Using Git

#### Prerequisites

* git
* eclipse
* m2e Maven plugin for eclipse.
	* Get it [here](http://www.eclipse.org/m2e/), or use the following update site:
````
http://download.eclipse.org/technology/m2e/releases
````

#### Cloning the RinSim project

_If you using a windows or graphical client for git, please see their documentation on how to clone a project._

* Open a terminal.
* Navigate to the directory where you want to store the RinSim project.
* Execute the following git command

	````
	git clone git@github.com:rinde/RinSim.git
	````
	
	This will download all the source files of the RinSim project to you local directory.

#### Importing the RinSim project into eclipse

* In eclipse go to _File -> Import... -> Maven -> Existing Maven Projects
* Browse to your local RinSim directory.
* You will now see a list of .pom files (they should all be selected).
* Click _Finish_

#### Updating the simulator

The simulator will likely change in the future (updates, bug fixes, etc.)

* To update the simulator you can use the normal git pull command:

	````
	git pull origin master
	````

Note that git will require you to first commit your own changes.

### The RinSim project structure

After finishing the import (with any of the above methods), you should see five projects in eclipse:

* _core_: the heart of the simulator and the models.
* _example_: some simple examples of how to use the simulator.
* _main_: main Maven project. 
* _playground_: TODO
* _ui_: everything related to visualizing stuff for the simulator. 

If desired, you can group the projects into one working set.

### Running the example

Execute one of the random walk examples in the _example_ project.
	
* Right-click on _RandomWalkExample.java_ and select _Run As -> Java Application_
* You should now see a map of Leuven. Agents and other objects on the map are represented by dots.
* Use the menu or keyboard shortcuts to start, stop, speedup or slowdown the simulation.

## Simulator Architecture

The simulator consists of four important parts: the _Simulator_, _Models_, the GUI, and application objects.
A simplified class diagram can be found [here](docs/classDiagram.png).

### Simulator

The _Simulator_ class is the heart of RinSim.
It has little functionality on its own, apart from maintaining the time.
Application-specific simulator functionality is realized by models that can be registered in the simulator.
The simulator uses _ModelManager_ to maintain all its models.

TODO

* TickListener
* Pluggable models

### Models

By using models the simulator can easily be extended.

Out of the box, RinSim comes with two basic models.

#### RoadModel

TODO

#### CommunicationModel

TODO

### GUI

The GUI is realized by the _SimulationViewer_, which relies on a set of _Renderers_.

#### SimulationViewer

This class is responsible for rendering the simulator.
By default is renders the road of the loaded graph.
Additional rendering is done by application specific renderers.

#### Renderer

A _Renderer_ is responsible for rendering one or more model (or more).
Examples are the _ObjectRenderer_ to do basic rendering of objects in the _RoadModel_, or _MessagingLayerRenderer_ to visualize messages between agents.
When introducing new models you can create new custom renderers for these models.

### Application Objects

## How to create an agent

### Simple sequence diagram

* When is what invoked, ...

## How to create a model

* (Available soon)

## Additional guidelines

### Using gitHub's issues to report changes

### Making pull requests for simulator

### Look at test code for deeper understanding



