# RinSim

RinSim is an extensible MAS (Multi-Agent System) simulator.
It supports pluggable models which allow to extend the scope of the simulator.
Out of the box, RinSim currently focusses on MAS for PDP (Pickup and Delivery Problems). 
You can easily extend RinSim by introducing your own custom models.

## Getting Started

The best way to get the simulator and handle future updates is to use git.

Alternatively you can use a zip that contains the current version of RinSim.

<!--
### Using the Zip file

__Only use this method to tryout the simulator. Updating RinSim using this method will be annoying.__

* Download the zipped project [here](http://TODO).
* Unzip the project to your desired location.
* Open eclipse and select _File -> Import... -> General -> Existing Projects into Workspace_
* Browse to the directory where you unzipped the project.
* Click _finish_
-->

### Prerequisites

* [eclipse](http://eclipse.org)
* [m2e](http://www.eclipse.org/m2e/) Maven plugin for eclipse.
	* Update site: 
````
http://download.eclipse.org/technology/m2e/releases
````
* [eGit](http://www.eclipse.org/egit/) Git plugin for eclipse (not required if you use another git client)
	* Update site: 
````
http://download.eclipse.org/egit/updates
````

To install an eclipse plugin:

* Go to _Help -> Install New Software..._
* Click _Add..._
* Enter the update site in location and enter any local name for the update site.
* Select the desired packages and install.

### Getting RinSim

RinSim is hosted on gitHub. You can get it using eGit (the eclipse plugin) or git (standard available on OSX and linux).

#### Using eGit

* Go to _File -> Import..._
* Select _Git -> Projects from Git_ and click _next_.
* Select _URI_ and click _next_.
* Enter
````
git@github.com:rinde/RinSim.git
````
in the URI field (do not alter any other input fields) and click _next_.
* Select __TODO__ branch and click _next_.
* Choose a local directory for your project and click _next_.
* Wait for eGit to download the project.
* Make sure _Import existing projects_ is selected and click _next_.
* Click _finish_.

You will now have one project in eclipse. See _Importing the Maven projects in eclipse_ on how to actually use it.

To update the simulator later on, right-click on the top-level project, go to _Team_ and select _Synchronize Workspace_.


#### Using Git

* Open a terminal.
* Navigate to the directory where you want to store the RinSim project.
* Execute the following git command

	````
	git clone git@github.com:rinde/RinSim.git TODO
	````
	
	This will download all the source files of the RinSim project to you local directory.

To update the simulator later on, you can use the _pull_ command:

````
git pull origin TODO
````

Note that git might require you to first commit your own changes.

### Importing the Maven projects in eclipse

RinSim relies on Maven to load all required dependencies.
To make use of Maven in eclipse you have to execute the following steps:

* In eclipse go to _File -> Import... -> Maven -> Existing Maven Projects
* Browse to your local RinSim directory.
* You will now see a list of _.pom_ files.
* Select the _.pom_ files for _core_, _examples_, and _ui_.
* Click _Finish_

After finishing the import, you should see the following three projects in your workspace:

* _core_: the heart of the simulator and the models.
* _example_: some simple examples of how to use the simulator.
* _ui_: everything related to visualizing stuff for the simulator. 

### Running the example

Execute one of the random walk examples in the _example_ project.
	
* Right-click on _RandomWalkExample.java_ and select _Run As -> Java Application_
* You should now see a map of Leuven. Agents and other objects on the map are represented by dots.
* Use the menu or keyboard shortcuts to start, stop, speedup or slowdown the simulation.

## Simulator Architecture

This section gives a brief overview of the most important elements of the simulator. For a deeper understanding you should have a look at the examples, the source code, and the tests.
A simplified class diagram of the key elements can be found [here](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/docs/classDiagram.png).

### Simulator

The _Simulator_ class is the heart of RinSim.
Its main concern is to simulate time.
This is done in a discrete manner. Time is divided in ticks of a certain length, which is chosen upon initializing the simulator (see examples and code).

Of course time on its own is not so useful, so we can register objects in the simulator. That is, objects implementing the _TickListener_ interface.
These objects will listen to the internal clock of the simulator.

Once started, the simulator will start to tick, and with each tick it will call all registered tickListeners, in turn, to perform some actions within the length of the time step (as illustrated [here](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/docs/tickListeners.png)).

As you can see there is also an _afterTick_, but we'll ignore this for now.

Apart from simulating time, the simulator has little functionality on its own.
All additional functionality (such as movement, communication, etc.) that is required by your simulation, should be delegated to models.
These models can be easily plugged (or registered) in the simulator.

### Models

Out of the box, RinSim comes with two basic models: _RoadModel_ and _CommunicationModel_. Further on, you will see how you can implement your own models.

* __RoadModel__. _RoadModel_ is a model to simulate a physical road on top of a _Graph_ object.
The _Graph_ object represents the structure of the roads. The _RoadModel_ allows to place and move objects (_RoadUsers_) on the roads.
The _RoadModel_ can, for example, be used to simulate physical trucks and packages.

* __CommunicationModel__. _CommunicationModel_ is a model to simulate simple message-based communication.
It supports both direct messaging and broadcasting.
It can also take distance, communication radius, and communication reliability into account.
Messages between agents are send asynchronously (as illustrated [here](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/docs/communication.png)).

### GUI

The GUI is realized by the _SimulationViewer_, which relies on a set of _Renderers_.

* __SimulationViewer__.
This class is responsible for rendering the simulator.
By default is renders the road of the loaded graph.
Additional rendering is done by application specific renderers that are passed on creation of the GUI (see examples and code).

* __Renderer__.
A _Renderer_ is responsible for rendering one or more model (or more).
Examples are the _ObjectRenderer_ to do basic rendering of objects in the _RoadModel_, or _MessagingLayerRenderer_ to visualize messages between agents.
When introducing new models you can create new custom renderers for these models.

### Simulation Entities

Simulation entities are entities that are the actual objects in our simulation, such as agents, trucks, and packages.
They typically implement the _TickListener_ interface and some interfaces to use additional models.

## A simple example

```java
public class SimpleAgent implements TickListener, MovingRoadUser, SimulatorUser {
	protected RoadModel rm;
	protected Queue<Point> currentPath;
	protected RandomGenerator rnd;
	private SimulatorAPI simulator;
	private Point startingPosition;
	private double speed;

	public SimpleAgent(Point startingPosition, double speed) {
		this.speed = speed;
		this.startingPosition = startingPosition;
	}

	@Override
	public void setSimulator(SimulatorAPI api) {
		this.simulator = api;
		this.rnd  = api.getRandomGenerator();
	}
	
	@Override
	public void initRoadUser(RoadModel model) {
		rm = model;
		rm.addObjectAt(this, startingPosition);
	}
	
	@Override
	public void tick(long currentTime, long timeStep) {
		if (currentPath == null || currentPath.isEmpty()) {
			Point destination = rm.getGraph().getRandomNode(rnd);
			currentPath = new LinkedList<Point>(rm.getShortestPathTo(this, destination));
		} else{
			rm.followPath(this, currentPath, timeStep);
		}
	}

	@Override
	public double getSpeed() {
		return speed;
	}
}
```

(as illustrated [here](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/docs/example.png))

* When is what invoked, ...

## How to create a model

_available soon_

## Additional guidelines

### Using gitHub's issues to report changes

_available soon_

### Making pull requests for simulator

_available soon_

### Look at test code for deeper understanding

_available soon_



