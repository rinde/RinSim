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

import com.google.common.base.Optional;

/**
 * Tests the CLI system.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class CliTest {

  @SuppressWarnings("null")
  List<Object> list;
  @SuppressWarnings("null")
  CliMenu menu;

  /**
   * Sets up the subject (a list in this case) and creates the menu.
   */
  @Before
  public void setUp() {
    list = newArrayList();
    menu = CliMenu
        .builder()
        .add(
            CliOption.builder("a", ArgumentParser.LONG).build(),
            list,
            new ArgHandler<List<Object>, Long>() {
              @Override
              public void execute(List<Object> ref, Optional<Long> value) {
                ref.add(value.get());
              }
            }
        )
        .add(
            CliOption
                .builder("aa", ArgumentParser.LONG_LIST)
                .longName("add-all")
                .description(
                    "Add all longs, and then some. Please note that using this option may alter the universe beyond recognition. Use at your own risk.")
                .argOptional()
                .build(),
            list,
            new ArgHandler<List<Object>, List<Long>>() {
              @Override
              public void execute(List<Object> ref, Optional<List<Long>> value) {
                if (value.isPresent()) {
                  ref.addAll(value.get());
                }
              }
            })
        .add(
            CliOption.builder("asl", ArgumentParser.STRING_LIST).build(),
            list,
            new ArgHandler<List<Object>, List<String>>() {
              @Override
              public void execute(List<Object> ref, Optional<List<String>> value) {
                ref.addAll(value.get());
              }
            }
        )
        .openGroup()
        .add(CliOption.builder("x").build(), list, dummyHandler())
        .add(CliOption.builder("y").build(), list, dummyHandler())
        .add(CliOption.builder("z").build(), list, dummyHandler())
        .closeGroup()
        .add(
            CliOption.builder("as", ArgumentParser.STRING).build(),
            list,
            new ArgHandler<List<Object>, String>() {
              @Override
              public void execute(List<Object> ref, Optional<String> value) {
                ref.addAll(value.asSet());
              }
            }
        )
        .addHelpOption("h", "help", "Print this message")
        .footer("This is the bottom")
        .header("This is the header")
        .build();
  }

  /**
   * Test for checking whether duplicate options are detected.
   */
  @Test(expected = IllegalArgumentException.class)
  public void duplicateOptions() {
    final Object subject = new Object();
    CliMenu.builder()
        .add(
            CliOption.builder("a").build(),
            subject,
            dummyHandler())
        .add(
            CliOption.builder("aa", ArgumentParser.STRING)
                .longName("a")
                .build(),
            subject,
            CliTest.<Object, String> dummyArgHandler());
  }

  /**
   * Test help.
   */
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
    // -aa has an optional argument, so this is valid
    assertFalse(menu.execute("-aa").isPresent());
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
    testFail("a", CauseType.INVALID_NUMBER_FORMAT, "-a", "sfd");
  }

  @Test
  public void testNotLongArgType2() {
    testFail("a", CauseType.INVALID_NUMBER_FORMAT, "-a", "6.4");
    testFail("aa", CauseType.INVALID_NUMBER_FORMAT, "-aa", "6.4");
  }

  @Test
  public void testStringArgType() {
    assertTrue(list.isEmpty());
    assertFalse(menu.execute("-asl", "hello world", "bye").isPresent());
    assertEquals(asList("hello world", "bye"), list);
    testFail("asl", CauseType.MISSING_ARG, "-asl");
    assertEquals(asList("hello world", "bye"), list);
    assertFalse(menu.execute("-as", "hello again").isPresent());
    assertEquals(asList("hello world", "bye", "hello again"), list);
    testFail("as", CauseType.MISSING_ARG, "-as");
  }

  /**
   * Empty short name is not allowed.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testAddSubMenuInvalidShortName() {
    CliMenu.builder().addSubMenu("", "long", menu);
  }

  /**
   * Empty long name is not allowed.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testAddSubMenuInvalidLongName() {
    CliMenu.builder().addSubMenu("short", "", menu);
  }

  /**
   * Check for group building.
   */
  @Test(expected = IllegalStateException.class)
  public void testAddSubMenuInvalidGroup() {
    CliMenu.builder().openGroup().addSubMenu("short", "long", menu);
  }

  /**
   * Test for adding an entire sub menu.
   */
  @Test
  public void testAddSubMenu() {
    final CliMenu m = CliMenu.builder().addSubMenu("l", "list.", menu).build();
    // help options are not copied
    assertFalse(m.containsOption("h"));
    assertFalse(m.containsOption("lh"));
    assertEquals(m.getOptionNames().size(), menu.getOptionNames().size() - 2);
  }

  @Test
  public void testGroup() {
    testFail("y", CauseType.ALREADY_SELECTED, "-x", "-y");
    testFail("z", CauseType.ALREADY_SELECTED, "-z", "-z");
    testFail("x", CauseType.ALREADY_SELECTED, "-z", "-x");

    final CliMenu m = CliMenu.builder().addSubMenu("l", "list.", menu).build();

  }

  void testFail(String failingOptionName, CauseType causeType,
      String... args) {
    testFail(menu, failingOptionName, causeType, args);
  }

  static void testFail(CliMenu m, String failingOptionName,
      CauseType causeType, String... args) {
    try {
      m.execute(args);
    } catch (final CliException e) {
      assertEquals(failingOptionName, e.getMenuOption().get().getShortName());
      assertEquals(causeType, e.getCauseType());
      return;
    }
    fail();
  }

  static <S> NoArgHandler<S> dummyHandler() {
    return new NoArgHandler<S>() {
      @Override
      public void execute(S subject) {}
    };
  }

  static <S, V> ArgHandler<S, V> dummyArgHandler() {
    return new ArgHandler<S, V>() {
      @Override
      public void execute(S subject, Optional<V> value) {}
    };
  }
}
