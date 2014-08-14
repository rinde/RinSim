package com.github.rinde.rinsim.io;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Set;

import javax.annotation.Nullable;

import org.junit.Test;

import com.github.rinde.rinsim.cli.CliException;
import com.github.rinde.rinsim.io.FileProvider;
import com.github.rinde.rinsim.io.FileProvider.Builder;
import com.google.common.base.Predicate;

/**
 * Tests for {@link FileProvider}.
 * @author Rinde van Lon 
 */
public class FileProviderTest {

  /**
   * Tests the detection of no paths.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testNoPaths() {
    FileProvider.builder().build();
  }

  /**
   * Tests for filter method.
   */
  @Test
  public void testFilter() {
    final Builder builder = FileProvider.builder()
        .add(Paths.get("src/"));

    builder.filter("glob:src/test/**");
    final Set<Path> paths = builder.build().get();
    for (final Path p : paths) {
      assertTrue(p.startsWith("src/test/"));
    }
    assertEquals(paths, builder
        .cli(System.out, "-f", "glob:src/test/**").build().get());

    builder.filter(new Predicate<Path>() {
      @Override
      public boolean apply(Path input) {
        return input.toString().endsWith("Model.java");
      }
    });
    final Set<Path> paths2 = builder.build().get();
    for (final Path p : paths2) {
      assertTrue(p.toString().endsWith("Model.java"));
    }
    assertEquals(paths2, builder
        .cli(System.out, "-f", "glob:**Model.java").build().get());

    builder.filter(new PathMatcher() {
      @Override
      public boolean matches(@Nullable Path path) {
        checkNotNull(path);
        return path.endsWith("package-info.java");
      }
    });
    final Set<Path> paths3 = builder.build().get();
    for (final Path p : paths3) {
      assertTrue(p.endsWith("package-info.java"));
    }
    assertEquals(paths3, builder
        .cli(System.out, "-f", "glob:**package-info.java").build().get());
  }

  /**
   * Tests that adding paths works both via the builder and via the cli.
   */
  @Test
  public void testAdd() {
    final Set<Path> paths = FileProvider.builder()
        .cli(System.out, "--add", "src/test,src/main")
        .build().get();

    final Set<Path> paths2 = FileProvider.builder()
        .add(asList(Paths.get("src/main/"), Paths.get("src/test/")))
        .build().get();
    assertEquals(paths, paths2);
  }

  /**
   * Tests the correct detection of adding non-existing paths.
   */
  @Test(expected = IllegalArgumentException.class)
  public void testAddFail() {
    FileProvider.builder().add(asList(Paths.get("bull"), Paths.get("shit")));
  }

  /**
   * Test for catching IO exceptions when walking the file tree in the file
   * provider.
   */
  @Test
  public void testCatchIO() {
    final Path p = Paths.get("tmp");
    try {
      Files.createDirectory(p);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }

    final FileProvider<Path> fileProvider = FileProvider.builder().add(p)
        .build();
    try {
      Files.delete(p);
    } catch (final IOException e) {
      throw new IllegalStateException(e);
    }

    boolean error = false;
    try {
      fileProvider.get();
    } catch (final IllegalStateException e) {
      error = true;
    }
    assertTrue(error);
  }

  /**
   * Tests the CLI help method.
   */
  @Test
  public void testHelp() {
    FileProvider.builder()
        .add(Paths.get("src/main/"))
        .add(Paths.get("src/test/"))
        .filter("glob:**.java")
        .cli(System.out, "--help");
  }

  /**
   * Test whether including paths yields the correct result.
   */
  @Test
  public void testInclude() {
    final Path p0 = Paths.get("src/main/");
    final Path p1 = Paths.get("src/test/");
    assertEquals(FileProvider.builder()
        .add(asList(p0, p1))
        .cli(System.out, "-i", "p1")
        .paths, asList(p1));
  }

  /**
   * Test the detection of including too many paths.
   */
  @Test
  public void testIncludeTooManyKeys() {
    final Path p0 = Paths.get("src/main/");
    final Path p1 = Paths.get("src/test/");
    boolean error = false;
    try {
      FileProvider.builder()
          .add(asList(p0, p1))
          .cli(System.out, "-i", "p1,p0,p3");
    } catch (final CliException e) {
      error = true;
      assertEquals("include", e.getMenuOption().get().getLongName().get());
    }
    assertTrue(error);
  }

  /**
   * Test the detection of including an non-existing path.
   */
  @Test
  public void testIncludeInvalidKey() {
    boolean error = false;
    try {
      FileProvider.builder().add(asList(Paths.get("src/main")))
          .cli(System.out, "-i", "p1");
    } catch (final CliException e) {
      error = true;
      assertEquals("include", e.getMenuOption().get().getLongName().get());
    }
    assertTrue(error);
  }

  /**
   * Test whether excluding paths yields the correct result.
   */
  @Test
  public void testExclude() {
    final Path p0 = Paths.get("src/main/");
    final Path p1 = Paths.get("src/test/");
    assertEquals(FileProvider.builder()
        .add(asList(p0, p1))
        .cli(System.out, "-e", "p1")
        .paths, asList(p0));
  }

  /**
   * Test the detection of excluding too many paths.
   */
  @Test
  public void testExcludeTooManyKeys() {
    final Path p0 = Paths.get("src/main/");
    final Path p1 = Paths.get("src/test/");
    boolean error = false;
    try {
      FileProvider.builder()
          .add(asList(p0, p1))
          .cli(System.out, "-e", "p1,p0,p3");
    } catch (final CliException e) {
      error = true;
      assertEquals("exclude", e.getMenuOption().get().getLongName().get());
    }
    assertTrue(error);
  }

  /**
   * Test the detection of excluding an non-existing path.
   */
  @Test
  public void testExcludeInvalidKey() {
    boolean error = false;
    try {
      FileProvider.builder().add(asList(Paths.get("src/main")))
          .cli(System.out, "-e", "p1");
    } catch (final CliException e) {
      error = true;
      assertEquals("exclude", e.getMenuOption().get().getLongName().get());
    }
    assertTrue(error);
  }
}
