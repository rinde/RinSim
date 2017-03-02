---
title: Installation troubleshooting
keywords: [installation, eclipse]
sidebar: installation_sidebar
toc: false
permalink: /installation/troubleshooting/
folder: installation
---

- You might see this message in the console:
  ```
  SLF4J: Failed to load class "org.slf4j.impl.StaticLoggerBinder".
  SLF4J: Defaulting to no-operation (NOP) logger implementation
  SLF4J: See http://www.slf4j.org/codes.html#StaticLoggerBinder for further details.
  ```
  This is not a critical error, what it means is that [SLF4J](https://www.slf4j.org/), the logging framework used by RinSim, can't find an implementation at runtime. This means that logging is disabled unless you include an implementation of the SLF4J framework.

- When Maven is complaining and you are sure you followed all instructions, force the Maven plugin to update. 
	- Eclipse: Right click on your project -> ``Maven`` -> ``Update Project..``.
	- IntelliJ: Right click on your project -> ``Maven`` -> ``Reimport``.

- When Maven says it cannot find one of your dependencies and you are sure that you have configured your pom file correctly you can inspect your local Maven repository. The local maven repository is stored in your user folder: ``~/.m2/``. You can choose to delete the entire repository or only the dependencies that cause trouble. As soon as Maven detects that some dependencies are missing it will attempt to redownload them.

- When you have copied some RinSim code to your own project and you see a compile error such as 'the method/constructor is undefined' make sure that you didn't accidently use the 'organize imports' feature of your favorite IDE and selected the wrong class. E.g. choose ``rinde.sim.core.graph.Point`` over ``java.awt.Point``.

- When the compiler complains about ``@Override`` annotations in code that you have copied from RinSim, make sure that Maven targets Java version 1.7 or later (see step 7 of the instructions for Eclipse or IntelliJ for the XML code to do this).

- If you can not run the UI on Mac OS X make sure that you add ``-XstartOnFirstThread`` as a VM argument.

- On OS X Mavericks there may be a problem where you can not see the menu bar of RinSim (it usually shows the menu of Eclipse), if this happens it can be resolved by using a shortcut (e.g. CMD-P for play) or by CMD tabbing away and back to the RinSim app.

- If you have a problem with the SWT UI of RinSim on a 64-bit version of Windows while using IntelliJ IDEA, [see this StackOverflow thread](https://stackoverflow.com/questions/29793596/how-do-i-get-intellij-idea-to-import-rinsim-from-maven-correctly/) for a solution.