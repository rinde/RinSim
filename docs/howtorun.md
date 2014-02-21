# How to run RinSim examples

##Prerequisites: 

- __installed__ [Eclipse 4.2 or later](http://www.eclipse.org/)
- __installed__ [Maven plugin for Eclipse](http://www.eclipse.org/m2e/)

##Instructions:

1. Open Eclipse, choose ``File`` -> ``New`` -> ``Project...`` <br/> 
![New](tutorial/1a.png) ![Project](tutorial/1b.png)
<br/><br/> 

2. Choose ``Maven Project`` and click ``Next >``<br/> 
![New](tutorial/2a.png)
<br/><br/>

3. Check ``Create a simple project (skip archetype selection)`` and click ``Next >``  ![New](tutorial/2b.png) 
<br/><br/> 

4. For ``Group Id:`` choose a unique _personal_ identifier, often a reversed Internet domain name is used for this. For ``Artifact Id:`` choose an identifier for your project.<br/>
Click ``Finish``<br/>
![New](tutorial/2c.png)
<br/><br/> 

5. In your ``Package Explorer`` you should see the following: <br/>
![New](tutorial/3.png)
<br/><br/>

6. Open the ``pom.xml`` file with the ``Maven POM Editor``. Choose the ``pom.xml`` tab in the bottom. <br/>
![New](tutorial/4a.png)
<br/><br/>

7. You will see an XML view of the file. Paste the following XML between the ``project`` tags.

    ```xml
    <dependencies>
        <dependency>
            <groupId>com.github.rinde</groupId>
            <artifactId>rinsim-example</artifactId>
            <version>x.y.z</version>
        </dependency>
    </dependencies> 
    ``` 

8. Replace ``x.y.z`` with the current latest version (the latest version is shown [here](https://github.com/rinde/RinSim/)). The pom file should now look similar to this: <br/>
![New](tutorial/4b.png)
<br/><br/>

8. Maven will now start downloading the dependencies. When it is done, make sure your can find the ``rinsim-example-x.y.z.jar`` in your ``Package Explorer``:<br/>
![New](tutorial/5a.png)
<br/><br/>

9. Right click ``rinsim-example-x.y.z.jar`` -> ``Run As`` -> ``Java Application``<br/>
![New](tutorial/5b.png)![New](tutorial/5c.png)
<br/><br/>

10. You will see the following window, select ``SimpleExample`` and click ``Ok``<br/>
![New](tutorial/5d.png)
<br/><br/>

11. You should now see the following window:<br/>
![New](tutorial/5e.png)<br/>
Congratualations, Your setup is complete, you can start working with RinSim!
Click ``Control`` -> ``Play`` to start the simulation. For more information about the other available examples, click [here](../example/README.md)

## Troubleshooting