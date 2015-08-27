package com.github.rinde.rinsim.core.model.time;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class GCLogMonitorTest {
  static Path tempDir;
  Path gclog;
  PrintWriter log;

  @BeforeClass
  public static void setUpClass() {
    try {
      tempDir = Files.createTempDirectory("gclogtest");
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @AfterClass
  public static void tearDownClass() {
    try {
      Files.delete(tempDir);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Before
  public void setUp() {
    try {
      gclog =
          Files.createFile(Paths.get(tempDir.toString(), "gclog.txt"));
      log = new PrintWriter(
          new BufferedWriter(new FileWriter(gclog.toFile(), true)));
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @After
  public void tearDown() {
    log.close();
    try {
      Files.delete(gclog);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  public void test() {
    final RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
    final List<String> arguments = runtimeMxBean.getInputArguments();

    System.out.println(arguments);

    // GCLogMonitor.getInstance()

  }
}
