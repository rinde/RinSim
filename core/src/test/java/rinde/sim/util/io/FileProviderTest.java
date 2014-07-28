package rinde.sim.util.io;

import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.junit.Test;

import rinde.sim.util.io.FileProvider.Builder;

import com.google.common.base.Predicate;

/**
 * Tests for {@link FileProvider}.
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
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
  }
}
