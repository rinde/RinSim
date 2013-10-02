/**
 * 
 */
package rinde.sim.examples.demo;

import java.io.FileNotFoundException;
import java.io.IOException;

import rinde.sim.examples.factory.FactoryExample;
import rinde.sim.examples.pdp.PDPExample;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Demo {

  public static void main(String[] args) throws FileNotFoundException,
      IOException {

    final long time = 6 * 60 * 60 * 1000L;

    while (true) {
      FactoryExample.main(new String[] { time + "" });
      PDPExample.main(new String[] { time + "" });
    }
  }

}
