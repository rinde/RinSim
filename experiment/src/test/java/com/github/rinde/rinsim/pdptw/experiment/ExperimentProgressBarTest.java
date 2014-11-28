package com.github.rinde.rinsim.pdptw.experiment;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.github.rinde.rinsim.testutil.GuiTests;

@Category(GuiTests.class)
public class ExperimentProgressBarTest {

  @SuppressWarnings("null")
  @Test
  public void test() {
    final ExperimentProgressBar pb = new ExperimentProgressBar();
    pb.startComputing(30);
    for (int i = 0; i < 30; i++) {
      try {
        Thread.sleep(50);
      } catch (final InterruptedException e) {
        throw new IllegalStateException();
      }
      pb.receive(null);
    }
    pb.doneComputing();
  }

}
