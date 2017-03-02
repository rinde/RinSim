---
title: Running RinSim examples using Eclipse
keywords: [installation, eclipse]
sidebar: installation_sidebar
toc: false
permalink: /installation/eclipse/
folder: installation
---
{% include note.html content="Are you having trouble with running RinSim? See the [troubleshooting tips](/installation/troubleshooting/)." %}

## Prerequisites: 

- __installed__ [Eclipse 4.2 or later](http://www.eclipse.org/)
- __installed__ [Maven plugin for Eclipse](http://www.eclipse.org/m2e/)

## Instructions:

1. Open Eclipse, choose ``File`` -> ``New`` -> ``Project...`` <br/> 
![New]( {{ "images/installation/eclipse/1a.png" | relative_url }}) ![Project]({{ "images/installation/eclipse/1b.png" | relative_url }})
<br/><br/> 

2. Choose ``Maven Project`` and click ``Next >``<br/> 
![New]({{ "images/installation/eclipse/2a.png" | relative_url }})
<br/><br/>

3. Check ``Create a simple project (skip archetype selection)`` and click ``Next >``  ![New]({{ "images/installation/eclipse/2b.png" | relative_url }})
<br/><br/> 

4. For ``Group Id:`` choose a unique _personal_ identifier, often a reversed Internet domain name is used for this. For ``Artifact Id:`` choose an identifier for your project.<br/>
Click ``Finish``<br/>
![New]({{ "images/installation/eclipse/2c.png" | relative_url }})
<br/><br/> 

5. In your ``Package Explorer`` you should see the following: <br/>
![New]({{ "images/installation/eclipse/3.png" | relative_url }})
<br/>
Note that by default Maven uses (the ancient) Java 1.5. Since RinSim requires at least Java 1.7 we will change this in the next steps.

6. Open the ``pom.xml`` file with the ``Maven POM Editor``. Choose the ``pom.xml`` tab in the bottom. <br/>
![New]({{ "images/installation/eclipse/4a.png" | relative_url }})
<br/>

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

8. Replace ``x.y.z`` with the current latest version (the latest version is shown [here](https://github.com/rinde/RinSim/)). The pom file should now look similar to this: <br/>
![New]({{ "images/installation/eclipse/4b.png" | relative_url }})
<br/><br/>
Check that the ``JRE System Library`` as shown by Eclipse is version 1.7 (or higher), if this isn't the case it often helps to force Maven to update the project: right click on your project -> ``Maven`` -> ``Update Project..``. If that doesn't work it may be that Eclipse can't find a correct Java version in which case you need to update your Eclipse settings.

8. Maven will now start downloading the dependencies. When it is done, make sure your can find the ``rinsim-example-x.y.z.jar`` in your ``Package Explorer``:<br/>
![New]({{ "images/installation/eclipse/5a.png" | relative_url }})
<br/><br/>

9. Right click ``rinsim-example-x.y.z.jar`` -> ``Run As`` -> ``Java Application``<br/>
![New]({{ "images/installation/eclipse/5b.png" | relative_url }})![New]({{ "images/installation/eclipse/5c.png" | relative_url }})
<br/><br/>

10. You will see the following window, select ``SimpleExample`` and click ``Ok``<br/>
![New]({{ "images/installation/eclipse/5d.png" | relative_url }})
<br/><br/>

11. You should now see the following window:<br/>
![New]({{ "images/installation/eclipse/5e.png" | relative_url }})<br/>
Congratualations, Your setup is complete, you can start working with RinSim!
Click ``Control`` -> ``Play`` to start the simulation. For more information about the other available examples, click [here](../example/README.md).


{% include tip.html content="you can download the sources of RinSim and all other dependencies by right clicking your project -> ``Maven`` -> ``Download Sources``" %}


