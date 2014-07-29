package rinde.sim.util.io;

import java.nio.file.Paths;

import org.junit.Test;

public class FileProviderCliTest {

  @Test
  public void testHelp() {

    FileProviderCli.execute(
        FileProvider.builder()
            .add(Paths.get("src/main/"))
            .add(Paths.get("src/test/"))
            .filter("glob:**.java"),
        new String[] { "--help" });

  }

  @Test(expected = IllegalArgumentException.class)
  public void test() {
    FileProvider.builder()
        .cli(new String[] { "-f", "glob:**.java" })
        .build();
  }

}
