package rinde.sim.util.io;

import java.nio.file.Paths;

import org.junit.Test;

public class FileProviderCliTest {

  @Test
  public void test() {

    FileProviderCli.execute(
        FileProvider.builder()
            .add(Paths.get("src/main/"))
            .add(Paths.get("src/test/"))
            .filter("glob:**.java"),
        new String[] { "--help" });

  }
}
