package rinde.sim.pdptw.generator;

import java.io.File;
import java.io.IOException;
import java.util.List;

import rinde.sim.core.graph.Point;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

public class Analysis {

  private Analysis() {}

  /**
   * Writes the specified list of {@link Point}s to the specified file. Each
   * point is printed on a separate line using the following format:
   * 
   * <pre>
   * {@code x y }
   * </pre>
   * 
   * i.e. the coordinates are space-separated.
   * @param locations The locations to write to file.
   * @param f The file to write to, non-existing parent directories will be
   *          created.
   */
  public static void writeLocationList(List<Point> locations, File f) {
    final StringBuilder sb = new StringBuilder();
    for (final Point p : locations) {
      sb.append(p.x).append(" ").append(p.y).append("\n");
    }
    try {
      Files.createParentDirs(f);
      Files.write(sb.toString(), f, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeLoads(List<? extends Number> loads, File f) {
    final StringBuilder sb = new StringBuilder();
    int i = 0;
    for (; i < loads.size(); i++) {
      sb.append(i).append(" ").append(loads.get(i)).append("\n");
    }
    sb.append(i).append(" ").append(0).append("\n");
    try {
      Files.createParentDirs(f);
      Files.write(sb.toString(), f, Charsets.UTF_8);
    } catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

}
