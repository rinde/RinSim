/**
 * 
 */
package rinde.sim.pdptw.experiment;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newLinkedHashSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.random.RandomGenerator;

import com.google.common.collect.ImmutableList;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class ExperimentUtil {

  public static List<String> getFilesFromDir(String dir, final String suffix) {
    final File directory = new File(dir);
    checkArgument(directory.isDirectory());
    final String[] names = directory.list(new FilenameFilter() {
      @Override
      public boolean accept(File d, String name) {
        return name.endsWith(suffix) && new File(d + "/" + name).isFile();
      }
    });
    // sort on file name such that order of returned list does not depend on
    // filesystem ordering.
    Arrays.sort(names);
    final List<String> paths = newArrayList();
    for (final String scen : names) {
      paths.add(dir + scen);
    }
    return paths;
  }

  public static List<List<String>> createFolds(String dir, int n,
      final String suffix) {
    final List<String> files = getFilesFromDir(dir, suffix);
    final List<List<String>> fs = newArrayList();
    for (int i = 0; i < n; i++) {
      fs.add(new ArrayList<String>());
    }
    for (int i = 0; i < files.size(); i++) {
      fs.get(i % n).add(files.get(i));
    }
    return fs;
  }

  public static List<String> createTrainSet(List<List<String>> fds, int testFold) {
    final List<String> set = newArrayList();
    for (int i = 0; i < fds.size(); i++) {
      if (testFold != i) {
        set.addAll(fds.get(i));
      }
    }
    return set;
  }

  // TODO can be replaced with Files.readLines ?
  public static String textFileToString(String file) throws IOException {
    final StringBuilder sb = new StringBuilder();
    final BufferedReader bf = new BufferedReader(new FileReader(file));
    String line;
    while ((line = bf.readLine()) != null) {
      sb.append(line + "\n");
    }
    return sb.toString();

  }

  public static ImmutableList<Long> generateDistinct(RandomGenerator rng,
      int size) {
    final Set<Long> numbers = newLinkedHashSet();
    while (numbers.size() < size) {
      numbers.add(rng.nextLong());
    }
    return ImmutableList.copyOf(numbers);
  }

}
