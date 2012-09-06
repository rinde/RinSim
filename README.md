# RinSim

RinSim is an extensible MAS (Multi-Agent System) simulator. The simulator focusses on __simplicity__ and __consistency__ which makes it ideal for performing scientific simulations. Further, there is huge focus on software quality which results in an ever improving test suite and JavaDoc comments. RinSim supports pluggable models which allow to extend the scope of the simulator. Out of the box, RinSim currently focusses on MAS for PDP (Pickup and Delivery Problems). You can easily extend RinSim by introducing your own custom models.

[<img src="https://github.com/rinde/RinSim/raw/v2/docs/screenshot.png">](http://vimeo.com/rinde/rinsim-gecco-demo)

Click the image above to view a movie showing the simulator in action.

## Installation
For installing the RinSim simulator there are generally two options:

* Use the latest builds available on [this page](http://people.cs.kuleuven.be/~rinde.vanlon/rinsim/binaries/). The zip file contains all the jars, dependencies and JavaDocs of the simulator. All Jars have to be added manually to your classpath.
* Use Git and Maven, see the section on [Git & Maven](https://github.com/rinde/RinSim#git-and-maven) . Currently this is the preferred option since it allows one to easily follow changes in the code by updating the repository.

 
## Getting Started 
Once the simulator is installed, you are ready to explore the simulator. It is recommended to start by running and studying the [simple example](https://github.com/rinde/RinSim/blob/v2/example/src/main/java/rinde/sim/examples/simple/SimpleExample.java). The JavaDocs are also available online on [this page](http://people.cs.kuleuven.be/~rinde.vanlon/rinsim/javadoc/). The remainder of this page gives a high level overview of the simulator.

<!--

The best way to get the simulator and handle future updates is to use git.

Alternatively you can use a zip that contains the current version of RinSim.

### Using the Zip file

__Only use this method to tryout the simulator. Updating RinSim using this method will be annoying.__

* Download the zipped project [here](http://TODO).
* Unzip the project to your desired location.
* Open eclipse and select _File -> Import... -> General -> Existing Projects into Workspace_
* Browse to the directory where you unzipped the project.
* Click _finish_
-->

<!-- ### Prerequisites

To use RinSim, you need the following:

__Note__: if you install the latest [_Eclipse IDE for Java Developers_](http://www.eclipse.org/downloads/packages/eclipse-ide-java-developers/indigosr2), the m2e and eGit plugins are preinstalled, but __not__ PDE.
If you install _Eclipse IDE for Java EE Developers_ or _Eclipse Classic_, PDE will be preinstalled, but not m2e or eGit.

* [eclipse](http://www.eclipse.org/)
* [m2e](http://www.eclipse.org/m2e/) Maven plugin for eclipse.
	* Update site: 
````
http://download.eclipse.org/technology/m2e/releases
````
* [eGit](http://www.eclipse.org/egit/) Git plugin for eclipse (or another git client)
	* Update site: 
````
http://download.eclipse.org/egit/updates
````
* PDE (Eclipse Plug-In Development Environment)

To install m2e and eGit:

* Go to _Help -> Install New Software..._
* Click _Add..._
* Enter the update site in location and enter any local name for the update site.
* Select the desired packages and install.

To install PDE

* Go to _Help -> Install New Software..._.
* In _Work with_, click the drop down and select _[Indigo - http://download.eclipse.org/releases/indigo]()_ (or the update site for your eclipse release).
* Search for _plug-in_.
* Install the _Eclipse Plug-In Development Environment_.

### Getting RinSim

RinSim is hosted on gitHub. You can get it using eGit (the eclipse plugin) or git.
-->
<!--

(If you are using a pc from the lab and cannot install eclipse plugins, you can find a zipped workspace [here](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/simulator2.zip).
You should open this workspace as a workspace in eclipse _File -> Switch Workspace -> Other..._
Using this zipped workspace is not the recommended method, since you cannot update RinSim.)
-->
<!--


<!-- ### Running the example

Execute one of the examples in the _example_ project.
	
* Right-click on _Example.java_ or _RandomWalkExample.java_ and select _Run As -> Java Application_
* You should now see a map of Leuven. Agents and other objects on the map are represented by dots.
* Use the menu or keyboard shortcuts to start, stop, speedup or slowdown the simulation.
-->

## Simulator Architecture

This section gives a brief overview of the most important elements of the simulator. For a deeper understanding you should have a look at the examples, the source code, and the tests.
<!--A simplified class diagram of the key elements can be found [here](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/docs/classDiagram.png). -->

### Simulator

The _Simulator_ class is the heart of RinSim. Its main concern is to simulate time. This is done in a discrete manner. Time is divided in ticks of a certain length, which is chosen upon initializing the simulator (see examples and code).

Of course time on its own is not so useful, so we can register objects in the simulator, such as objects implementing the _TickListener_ interface. These objects will listen to the internal clock of the simulator. You can also register other objects, as we will see in a moment.

Once started, the simulator will start to tick, and with each tick it will call all registered tickListeners, in turn, to perform some actions within the length of the time step (as illustrated [here](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/docs/tickListeners.png)). Time consistency is enforced by the _TimeLapse_ objects. Each _TickListener_ receives a single _TimeLapse_ object every tick, the time in this object can be 'spent' on actions. This spending can be done only once, as such an agent can not violate the time consistency in the simulator. For example, calling _RoadModel#moveTo(..)_ several times will have no effect.

As you can see there is also an _afterTick_, but we'll ignore this for now.

Apart from simulating time, the simulator has little functionality on its own.
All additional functionality (such as movement, communication, etc.) that is required by your simulation, should be delegated to models.
These models can be easily plugged (or registered) in the simulator.

### Models

Out of the box, RinSim comes with three basic models: _RoadModel_, _CommunicationModel_ and _PDPModel_. When this is not enough, it is easy to define your own custom model.

* __RoadModel__: simulates a physical road structure. The _RoadModel_ allows to place and move objects (_RoadUsers_) on roads. It comes in two flavors:
	* __GraphRoadModel__: A graph based road model, objects can only move on edges of the graph. Several maps are currently available [here](http://people.cs.kuleuven.be/~rinde.vanlon/rinsim/maps/).
	* __PlaneRoadModel__: A plane based road model, objects can move anywhere within the plane.
* __PDPModel__: the pickup-and-delivery model. The model collaborates with the _RoadModel_, the models comes with three different _RoadUser_s: _Vehicle_, _Parcel_ and _Depot_. _Vehicle_s can transport _Parcel_s from and to _Depot_s. The model enforces capacity constraints, time windows and position consistency.
* __CommunicationModel__: simulates simple message-based communication between objects implementing the _CommunicationUser_ interface.
It supports both direct messaging and broadcasting.
It can also take distance, communication radius, and communication reliability into account.
Messages between agents are send asynchronously (as illustrated [here](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/docs/communication.png)).

### GUI

The GUI is realized by the _SimulationViewer_, which relies on a set of _Renderers_.

* __SimulationViewer__: is responsible for rendering the simulator. Specific renderers can be added for each model, for the provided models there exist default renderers.

* __Renderer__: is responsible for rendering one model (or more).
Examples are the _RoadUserRenderer_ to do basic rendering of objects in the _RoadModel_, or _MessagingLayerRenderer_ to visualize messages between agents.
When introducing new models you can create new custom renderers for these models.

### Simulation Entities

Simulation entities are entities that are the actual objects in our simulation, such as agents, trucks, and packages.
They can implement the _TickListener_ interface and/or other interfaces to use additional models.
Once registered in the simulator, the simulator will make sure they receive ticks (if required) and are registered in all required models (see the example below).

<!--
## A simple example

The following code illustrates how a simulator can be created.
A sequence diagram can be found [here](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/docs/example.png).

```java
//create a new random number generator
MersenneTwister rand = new MersenneTwister(123);

//create a new simulator
Simulator simulator = new Simulator(rand, 1000);

//load graph of Leuven
Graph<MultiAttributeEdgeData> graph = DotGraphSerializer.getMultiAttributeGraphSerializer(new SelfCycleFilter()).read(MAP_DIR + "leuven-simple.dot");

//create a new road model for Leuven and register it in the simulator
RoadModel roadModel = new RoadModel(graph);
simulator.register(roadModel);

//create a new communication model and register it in the simulator
CommunicationModel communicationModel = new CommunicationModel(rand);
simulator.register(communicationModel);

//configure the simulator
//after this call, no more models be registered in the simulator
//before this call, no simulation entities can be registered in the simulator
simulator.configure();
		
//create some agent and register it in the simulator
SomeAgent agent = new SomeAgent();
simulator.register(agent);
				
//create a ui schema used by object renderer
UiSchema schema = new UiSchema();
//some agents will be red
schema.add(SomeAgent.class, new RGB(255,0,0));
		
//start the GUI with a simple object renderer
View.startGui(simulator, 5, new ObjectRenderer(roadModel, schema, false));
```
-->
<!--## How to create a model

_available soon_

## Additional guidelines
-->

## Git and Maven
This section assumes that you are using [Eclipse](http://www.eclipse.org) with [m2e](http://eclipse.org/m2e/) and optionally [eGit](http://www.eclipse.org/egit/). Installation instructions for each can be found on their respective websites.

### Using eGit

* Go to _File -> Import..._
* Select _Git -> Projects from Git_ and click _next_.
* Select _URI_ and click _next_.
* Enter
````
git@github.com:rinde/RinSim.git
````
in the URI field, select _https_ as protocol, and click _next_.
* Select the __v2__ branch and click _next_.
* Choose a local directory for your project and click _next_.
* Wait for eGit to download the project.
* Make sure _Import existing projects_ is selected and click _next_.
* Click _finish_.

You will now have one project in eclipse. See _Importing the Maven projects in eclipse_ on how to actually use it.

To update the simulator later on, right-click on the top-level project, go to _Team_ and select and select _Pull_.


### Using Git (commandline)

* Open a terminal.
* Navigate to the directory where you want to store the RinSim project.
* Execute the following git command

	````
	git clone git@github.com:rinde/RinSim.git -b v2
	````
	
	This will download all the source files of the RinSim project to you local directory.

To update the simulator later on, you can use the _pull_ command:

````
git pull origin v2
````

Note that git might require you to first commit your own changes.

### Importing the Maven projects in eclipse

RinSim relies on Maven to load all required dependencies.
To make use of Maven in eclipse you have to execute the following steps:

* In eclipse go to _File -> Import... -> Maven -> Existing Maven Projects_.
* Browse to your local RinSim directory.
* You will now see a list of _.pom_ files.
* Select the _.pom_ files for __core__, __example__, and __ui__.
* Click _Finish_.

After finishing the import, you should see the following three projects in your workspace:

* _core_: the heart of the simulator and the models.
* _ui_: everything related to visualizing stuff for the simulator. 
* _example_: some simple examples of how to use the simulator.



#### Using eGit

1. Go to _File -> Import..._
* Select _Git -> Projects from Git_.
* Select _URI_.
* Enter
````
git@github.com:rinde/RinSim.git
````
in the URI field (do not alter any other input fields) and click _next_.
* __Only__ select the __v2__ branch and click _next_.
* Choose a local directory for your project and click _next_.
* Wait for eGit to download the project.
* Make sure _Import existing projects_ is selected and click _next_.
* Click _finish_.

You will now have one project in eclipse.
Because we use Maven, you cannot use this project directly.
Instead, You now have to import the following three sub-projects individually: __core__, __ui__, and __example__.
Perform steps __1__ to __9__ again for __core__, __ui__, and __example__.

__Important__: In step 6, choose another directory for the specific sub-project.
In step 8, select core/ui/example from the working directory (like [this](http://people.cs.kuleuven.be/~robrecht.haesevoets/mascourse/docs/Subproject.png)).

__Note__: Some versions of eclipse do not show the sub-directories in step 8.
To solve this, first click _back_ then again _next_.

To update the simulator later on, right-click on a specific sub-project, go to _Team_ and select _Pull_.


#### Using Git

* Open a terminal.
* Navigate to the directory where you want to store the RinSim project.
* Execute the following git command

	````
	git clone git@github.com:rinde/RinSim.git -b v2
	````
	
	This will download all the source files of the RinSim project to you local directory.

RinSim relies on Maven to load all required dependencies.
To make use of Maven in eclipse you have to execute the following steps:

* In eclipse go to _File -> Import... -> Maven -> Existing Maven Projects_
* Browse to your local RinSim directory.
* You will now see a list of _.pom_ files.
* Select the _.pom_ files for _core_, _examples_, and _ui_.
* Click _Finish_

After finishing the import, you should see the following three projects in your workspace:

* _core_: the heart of the simulator and the models.
* _ui_: everything related to visualizing stuff for the simulator.
* _example_: some simple examples of how to use the simulator. This is where you will initially write your application code.

To update the simulator later on, you can use the _pull_ command:

````
git pull origin v2
````

Note that git might require you to first commit your own changes.





<!--
### Using gitHub's issues to report changes

You can use gitHub's issue feature to report problems, bugs, or useful features for RinSim.

Remember:

* The issue system should only be used for stuff directly related to RinSim, not for questions about the MAS course or for questions on how to do stuff with RinSim. You can use Toledo/lab sessions/fellow students for this.
* Check if your issue has already been reported.
* Be precise in the description of your issue.
* When reporting a bug, give sufficient information on how to reproduce the bug.
* Think twice before creating a new issue.
-->
<!-- 
_more guidelines available soon_

### Making pull requests for RinSim

_available soon_ -->