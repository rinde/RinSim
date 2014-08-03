package rinde.sim.pdptw.experiment;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static rinde.sim.util.cli.CliTest.testFail;

import java.io.File;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.pdptw.central.Central;
import rinde.sim.pdptw.central.RandomSolver;
import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;
import rinde.sim.util.cli.CliException.CauseType;
import rinde.sim.util.cli.CliMenu;
import rinde.sim.util.io.FileProvider;

/**
 * Test for commandline interface of experiment.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ExperimentCliTest {

  @SuppressWarnings("null")
  MASConfiguration configA;
  @SuppressWarnings("null")
  MASConfiguration configB;
  @SuppressWarnings("null")
  MASConfiguration configC;
  @SuppressWarnings("null")
  Builder builder;
  @SuppressWarnings("null")
  CliMenu menu;

  /**
   * Set up the experiment builder and create the CLI menu.
   */
  @Before
  public void setUp() {
    configA = Central.solverConfiguration(RandomSolver.supplier(), "A");
    configB = Central.solverConfiguration(RandomSolver.supplier(), "B");
    configC = Central.solverConfiguration(RandomSolver.supplier(), "C");
    builder = Experiment
        .build(Gendreau06ObjectiveFunction.instance())
        .addScenario(
            Gendreau06Parser.parse(new File(
                "files/test/gendreau06/req_rapide_1_240_24")))
        .addConfiguration(configA)
        .addConfiguration(configB)
        .addConfiguration(configC)
        .addScenarios(
            FileProvider.builder().add(
                Paths.get("files/test/gendreau06/"))
        )
        .setScenarioReader(Gendreau06Parser.reader());

    menu = ExperimentCli.createMenu(builder);
  }

  Builder testSuccess(String args) {
    ExperimentCli.execute(builder, args.split(" "));
    return builder;
  }

  /**
   * Test for various inputs of seed.
   */
  @Test
  public void testSeed() {
    testFail(menu, "s", CauseType.MISSING_ARG, "-s");
    testFail(menu, "s", CauseType.INVALID_ARG_FORMAT, "-b", "10", "-s", "x");
    assertEquals(10, testSuccess("-s 10").masterSeed);
  }

  /**
   * Test for various inputs of batches.
   */
  @Test
  public void testBatches() {
    testFail(menu, "b", CauseType.MISSING_ARG, "--batches");
    testFail(menu, "b", CauseType.INVALID_ARG_FORMAT, "-b", "x", "-s", "1");
    testFail(menu, "b", CauseType.INVALID, IllegalArgumentException.class,
        "-b", "0", "-s", "1");
    assertEquals(1, testSuccess("-b 1").numBatches);
    assertEquals(10, testSuccess("-b 10").numBatches);
  }

  /**
   * Test for various inputs of threads.
   */
  @Test
  public void testThreads() {
    testFail(menu, "t", CauseType.MISSING_ARG, "--threads");
    testFail(menu, "t", CauseType.INVALID_ARG_FORMAT, "-t", "x", "-s", "1");
    testFail(menu, "t", CauseType.INVALID, IllegalArgumentException.class,
        "-t", "0", "-s", "1");
    assertEquals(1, testSuccess("-t 1").numThreads);
    assertEquals(10, testSuccess("-t 10").numThreads);
  }

  /**
   * Test for various inputs of repetitions.
   */
  @Test
  public void testRepetitions() {
    testFail(menu, "r", CauseType.MISSING_ARG, "--repetitions");
    testFail(menu, "r", CauseType.INVALID_ARG_FORMAT, "-r", "x", "-s", "1");
    testFail(menu, "r", CauseType.INVALID, IllegalArgumentException.class,
        "-r", "0", "-s", "1");
    assertEquals(1, testSuccess("-r 1").repetitions);
    assertEquals(10, testSuccess("-r 10").repetitions);
  }

  /**
   * Tests whether the include option is effective.
   */
  @Test
  public void testInclude() {
    testFail(menu, "i", CauseType.MISSING_ARG, "--include");
    testFail(menu, "i", CauseType.INVALID, IllegalArgumentException.class,
        "--include", "x2");
    testFail(menu, "i", CauseType.INVALID, IllegalArgumentException.class,
        "--include", "c1,,c2");
    testFail(menu, "i", CauseType.INVALID, IllegalArgumentException.class,
        "--i", "c1,c1,c1,c1");
    setUp();

    assertEquals(newHashSet(configB, configC),
        testSuccess("-i c1,c2").configurationsSet);
    setUp();

    assertEquals(newHashSet(configA),
        testSuccess("-i c0").configurationsSet);
    setUp();

    assertEquals(newHashSet(configA, configB, configC),
        testSuccess("-i c0,c2,c1").configurationsSet);
    setUp();

    assertEquals(newHashSet(configC),
        testSuccess("-i c2,c2,c2").configurationsSet);
  }

  /**
   * Tests whether the exclude option is effective.
   */
  @Test
  public void testExclude() {
    testFail(menu, "e", CauseType.MISSING_ARG, "--exclude");
    testFail(menu, "e", CauseType.INVALID, IllegalArgumentException.class,
        "--exclude", "x2");
    testFail(menu, "e", CauseType.INVALID, IllegalArgumentException.class,
        "--exclude", "c1,,c2");
    testFail(menu, "e", CauseType.INVALID, IllegalArgumentException.class,
        "--e", "c1,c1,c1,c1");
    testFail(menu, "e", CauseType.INVALID, IllegalArgumentException.class,
        "--e", "c0,c1,c");
    setUp();

    assertEquals(newHashSet(configA),
        testSuccess("-e c1,c2").configurationsSet);
    setUp();

    assertEquals(newHashSet(configB, configC),
        testSuccess("-e c0").configurationsSet);
    setUp();

    assertEquals(newHashSet(configA),
        testSuccess("-e c2,c1").configurationsSet);
    setUp();

    assertEquals(newHashSet(configA, configB),
        testSuccess("-e c2,c2").configurationsSet);
  }

  /**
   * Batches and threads can not be used together.
   */
  @Test
  public void batchesThreadsFail() {
    testFail(menu, "t", CauseType.ALREADY_SELECTED, "--batches", "2", "-t", "4");
    testFail(menu, "b", CauseType.ALREADY_SELECTED, "--threads", "4", "--b",
        "2");
  }

  /**
   * Include and exclude can not be used together.
   */
  @Test
  public void excludesIncludesFail() {
    testFail(menu, "i", CauseType.ALREADY_SELECTED, "-e", "c1", "-i", "c1");
    testFail(menu, "e", CauseType.ALREADY_SELECTED, "-i", "c1", "-e", "c1");
  }

  /**
   * Jppf and local can not be used together.
   */
  @Test
  public void localJppfFail() {
    testFail(menu, "j", CauseType.ALREADY_SELECTED, "--local", "--jppf");
    testFail(menu, "l", CauseType.ALREADY_SELECTED, "--jppf", "--local");
  }

  /**
   * Tests help.
   */
  @Test
  public void testHelp() {
    testSuccess("-h");
  }

  /**
   * Tests a dry run of an experiment.
   */
  @Test
  public void dryRun() {
    builder.perform(new String[] { "-dr" }, System.out);
  }
}
