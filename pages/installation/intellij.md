---
title: Running RinSim examples using IntelliJ
keywords: [installation, intellij]
sidebar: installation_sidebar
toc: false
permalink: /installation/intellij/
folder: installation
---

{% include note.html content="Are you having trouble with running RinSim? See the [troubleshooting tips](/installation/troubleshooting/)." %}

## Prerequisites: 

- __installed__ [IntelliJ IDEA](https://www.jetbrains.com/idea/)

## Instructions:

1. Open IntelliJ IDEA, choose ``Create New Project``<br/>
![New]({{"images/installation/intellij/1.png" | relative_url }})

2. Choose ``Maven`` on the left and click ``Next >``<br/>
<img src="{{ "images/installation/intellij/2.png" | relative_url }}" width="800">

3. For ``Group Id:`` choose a unique _personal_ identifier, often a reversed Internet domain name is used for this. For ``Artifact Id:`` choose an identifier for your project.<br/>
Click ``Next``<br/>
<img src="{{ "images/installation/intellij/3.png" | relative_url }}" width="800">

4. Choose a name and a location for your project.<br/>
Click ``Finish``<br/>
<img src="{{ "images/installation/intellij/4.png" | relative_url }}" width="800">

5. In your ``Package Explorer`` you should see the following:<br/>
<img src="{{ "images/installation/intellij/5.png" | relative_url }}" width="300"><br/>
Note that by default Maven uses (the ancient) Java 1.5. Since RinSim requires at least Java 1.7 we will change this in the next steps.

6. Open the ``pom.xml`` file.

7. You will see an XML view of the file. Add (paste) the following XML code between the ``project`` tags. Make sure to not overwrite the existing XML tags.
    ```xml
    <dependencies>
        <dependency>
            <groupId>com.github.rinde</groupId>
            <artifactId>rinsim-example</artifactId>
            <version>x.y.z</version>
        </dependency>
    </dependencies>

    <build>
		<plugins>
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>
				<version>2.1</version>
				<configuration>
					<source>1.7</source>
					<target>1.7</target>
				</configuration>
			</plugin>
		</plugins>
	</build>
    ```

8. Replace ``x.y.z`` with the current latest version (the latest version is shown [here](https://github.com/rinde/RinSim/)). The pom file should now look similar to this:<br/>
<img src="{{ "images/installation/intellij/6.png" | relative_url }}" width="800"><br/>
 Check that the ``JRE System Library`` as shown by Eclipse is version 1.7 (or higher), if this isn't the case it often helps to force Maven to update the project: right click on your project -> ``Maven`` -> ``Update Project..``. If that doesn't work it may be that Eclipse can't find a correct Java version in which case you need to update your Eclipse settings.

9. Maven will now start downloading the dependencies. When it is done, make sure your can find the ``rinsim-example-x.y.z.jar`` in your ``Project Explorer``:<br/>
<img src="{{ "images/installation/intellij/7.png" | relative_url }}" width="300">

10. Open ``rinsim-example-x.y.z.jar`` -> Find ``SimpleExample`` -> Right click -> ``Run 'SimpleExample.main()'``<br/>
<img src="{{ "images/installation/intellij/8.png" | relative_url }}" width="500">

11. You will see the following window. Select your project in ``Use classpath of module``. (If you are a Mac user, add ``-XstartOnFirstThread`` to ``VM options``, otherwise, just leave ``VM options`` empty) Click ``Apply`` and then ``Run``<br/>
<img src="{{ "images/installation/intellij/9.png" | relative_url }}" width="500">

12. You should now see the following window:<br/>
![New]({{ "images/installation/eclipse/5e.png" | relative_url }})<br/>
Congratulations, Your setup is complete, you can start working with RinSim!
Click ``Control`` -> ``Play`` to start the simulation. For more information about the other available examples, click [here](../example/README.md).

{% include tip.html content="you can download the sources of RinSim and all other dependencies by right clicking your project -> ``Maven`` -> ``Download Sources and Documentation``" %}
