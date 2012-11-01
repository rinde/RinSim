/**
 * 
 */
package rinde.sim.problem.gendreau06;

import java.io.IOException;

import org.junit.Test;

/**
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 * 
 */
public class Gendreau06Test {
	@Test
	public void test() throws IOException {
		System.out.println(Gendreau06Parser.parse("data/test/gendreau06/req_rapide_1_240_24", 5));
	}
}
