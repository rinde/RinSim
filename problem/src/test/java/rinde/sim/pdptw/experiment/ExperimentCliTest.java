package rinde.sim.pdptw.experiment;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Paths;

import org.apache.commons.cli.AlreadySelectedException;
import org.apache.commons.cli.MissingArgumentException;
import org.junit.Before;
import org.junit.Test;

import rinde.sim.pdptw.central.Central;
import rinde.sim.pdptw.central.RandomSolver;
import rinde.sim.pdptw.experiment.Experiment.Builder;
import rinde.sim.pdptw.experiment.ExperimentCli.Exclude;
import rinde.sim.pdptw.experiment.ExperimentCli.Include;
import rinde.sim.pdptw.experiment.ExperimentCli.MenuOptions;
import rinde.sim.pdptw.gendreau06.Gendreau06ObjectiveFunction;
import rinde.sim.pdptw.gendreau06.Gendreau06Parser;
import rinde.sim.util.io.CliException;
import rinde.sim.util.io.FileProvider;
import rinde.sim.util.io.MenuOption;

/**
 * Test for commandline interface of experiment.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class ExperimentCliTest {

  MASConfiguration configA;
  MASConfiguration configB;
  MASConfiguration configC;
  Builder builder;

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
  }

  void testFail(MenuOption failingOption, Class<?> causeType, String args) {
    try {
      ExperimentCli.execute(builder, args.split(" "));
    } catch (final CliException e) {
      assertEquals(failingOption, e.getMenuOption());
      assertEquals(causeType, e.getCause().getClass());
      return;
    }
    fail();
  }

  void testFail(Class<?> classOfFailingOption, Class<?> causeType, String args) {
    try {
      ExperimentCli.execute(builder, args.split(" "));
    } catch (final CliException e) {
      assertEquals(classOfFailingOption, e.getMenuOption().getClass());
      assertEquals(causeType, e.getCause().getClass());
      return;
    }
    fail();
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
    testFail(MenuOptions.SEED, MissingArgumentException.class, "-s");
    testFail(MenuOptions.SEED, IllegalArgumentException.class, "-b 10 -s x");
    assertEquals(10, testSuccess("-s 10").masterSeed);
  }

  /**
   * Test for various inputs of batches.
   */
  @Test
  public void testBatches() {
    testFail(MenuOptions.BATCHES, MissingArgumentException.class, "--batches");
    testFail(MenuOptions.BATCHES, IllegalArgumentException.class, "-b x -s 1");
    testFail(MenuOptions.BATCHES, IllegalArgumentException.class, "-b 0 -s 1");
    assertEquals(1, testSuccess("-b 1").numBatches);
    assertEquals(10, testSuccess("-b 10").numBatches);
  }

  /**
   * Test for various inputs of threads.
   */
  @Test
  public void testThreads() {
    testFail(MenuOptions.THREADS, MissingArgumentException.class, "--threads");
    testFail(MenuOptions.THREADS, IllegalArgumentException.class, "-t x -s 1");
    testFail(MenuOptions.THREADS, IllegalArgumentException.class, "-t 0 -s 1");
    assertEquals(1, testSuccess("-t 1").numThreads);
    assertEquals(10, testSuccess("-t 10").numThreads);
  }

  /**
   * Test for various inputs of repetitions.
   */
  @Test
  public void testRepetitions() {
    testFail(MenuOptions.REPETITIONS, MissingArgumentException.class,
        "--repetitions");
    testFail(MenuOptions.REPETITIONS, IllegalArgumentException.class,
        "-r x -s 1");
    testFail(MenuOptions.REPETITIONS, IllegalArgumentException.class,
        "-r 0 -s 1");
    assertEquals(1, testSuccess("-r 1").repetitions);
    assertEquals(10, testSuccess("-r 10").repetitions);
  }

  /**
   * Tests whether the include option is effective.
   */
  @Test
  public void testInclude() {
    testFail(Include.class, MissingArgumentException.class, "--include");
    testFail(Include.class, IllegalArgumentException.class, "--include x2");
    testFail(Include.class, IllegalArgumentException.class, "--include c1,,c2");
    testFail(Include.class, IllegalArgumentException.class, "--i c1,c1,c1,c1");
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
    testFail(Exclude.class, MissingArgumentException.class, "--exclude");
    testFail(Exclude.class, IllegalArgumentException.class, "--exclude x2");
    testFail(Exclude.class, IllegalArgumentException.class, "--exclude c1,,c2");
    testFail(Exclude.class, IllegalArgumentException.class, "--e c1,c1,c1,c1");
    testFail(Exclude.class, IllegalArgumentException.class, "--e c0,c1,c");
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
    testFail(MenuOptions.THREADS, AlreadySelectedException.class,
        "--batches 2 -t 4");
    testFail(MenuOptions.BATCHES, AlreadySelectedException.class,
        "--threads 4 --b 2 ");
  }

  /**
   * Include and exclude can not be used together.
   */
  @Test
  public void excludesIncludesFail() {
    testFail(Include.class, AlreadySelectedException.class,
        "-e c1 -i c1");
    testFail(Exclude.class, AlreadySelectedException.class,
        "-i c1 -e c1 ");
  }

  /**
   * Jppf and local can not be used together.
   */
  @Test
  public void localJppfFail() {
    testFail(MenuOptions.JPPF, AlreadySelectedException.class,
        "--local --jppf");
    testFail(MenuOptions.LOCAL, AlreadySelectedException.class,
        "--jppf --local ");
  }

  /**
   * Tests help.
   */
  @Test
  public void testHelp() {
    testSuccess("-h");
  }

  @Test
  public void dryRun() {
    builder.perform(new String[] { "-dr" });
  }
}
