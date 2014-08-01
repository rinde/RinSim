package rinde.sim.util.cli;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import rinde.sim.util.cli.CliException.CauseType;
import rinde.sim.util.cli.CliOption.OptionArgType;

/**
 * Tests the CLI system.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class CliTest {

  @SuppressWarnings("null")
  List<Long> list;
  @SuppressWarnings("null")
  CliMenu<List<Long>> menu;

  /**
   * Sets up the subject (a list in this case) and creates the menu.
   */
  @Before
  public void setUp() {
    list = newArrayList();
    menu = CliMenu.builder(list)
        .add(
            CliOption.builder("a", OptionArgType.LONG)
                .build(new OptionHandler<List<Long>, Long>() {
                  @Override
                  public boolean execute(List<Long> ref, Value<Long> value) {
                    ref.add(value.asValue());
                    return true;
                  }
                })
        )
        .add(
            CliOption.builder("aa", OptionArgType.LONG_LIST)
                .longName("add-all")
                .build(new OptionHandler<List<Long>, List<Long>>() {
                  @Override
                  public boolean execute(List<Long> ref, Value<List<Long>> value) {
                    ref.addAll(value.asValue());
                    return true;
                  }
                })
        )
        .add(CliOption.builder("h")
            .longName("help")
            .<List<Long>> buildHelpOption())
        .build();
  }

  @Test
  public void testHelp() {
    assertTrue(menu.execute("--help").isPresent());
    assertTrue(menu.execute("-help").isPresent());
    assertTrue(menu.execute("--h").isPresent());
    assertTrue(menu.execute("-h").isPresent());
  }

  /**
   * Test the correct detection of a missing argument.
   */
  @Test
  public void testMissingArg() {
    testFail("a", CauseType.MISSING_ARG, "-a");
  }

  @Test
  public void testLongArgType() {
    assertTrue(list.isEmpty());
    assertFalse(menu.execute("-a", "234").isPresent());
    assertEquals(asList(234L), list);
    assertFalse(menu.execute("-a", "-1").isPresent());
    assertEquals(asList(234L, -1L), list);

    assertFalse(menu.execute("--add-all", "10,100", "-a", "-10").isPresent());
    assertEquals(asList(234L, -1L, 10L, 100L, -10L), list);

    list.clear();
    assertFalse(menu.execute("-a", "-10", "--add-all", "10,100").isPresent());
    assertEquals(asList(-10L, 10L, 100L), list);
  }

  @Test
  public void testNotLongArgType() {
    testFail("a", CauseType.NOT_A_LONG, "-a", "sfd");
  }

  @Test
  public void testNotLongArgType2() {
    testFail("a", CauseType.NOT_A_LONG, "-a", "6.4");
  }

  void testFail(String failingOptionName, CauseType causeType,
      String... args) {
    try {
      menu.execute(args);
    } catch (final CliException e) {
      assertEquals(failingOptionName, e.getMenuOption().getShortName());
      assertEquals(causeType, e.getCauseType());
      return;
    }
    fail();
  }
}
