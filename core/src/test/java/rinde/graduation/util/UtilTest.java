/**
 * 
 */
package rinde.graduation.util;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
public class UtilTest {

	@Test
	public void testPermutations() {
		assertEquals(1, toSet(Util.permutations(Arrays.asList("A"))).size());
		assertEquals(2, toSet(Util.permutations(Arrays.asList("A", "B"))).size());
		assertEquals(6, toSet(Util.permutations(Arrays.asList("A", "B", "C"))).size());
		assertEquals(24, toSet(Util.permutations(Arrays.asList("A", "B", "C", "D"))).size());
		assertEquals(120, toSet(Util.permutations(Arrays.asList("A", "B", "C", "D", "E"))).size());
		assertEquals(720, toSet(Util.permutations(Arrays.asList("A", "B", "C", "D", "E", "F"))).size());

		List<List<String>> perms = Util.permutations(Arrays.asList("A", "B", "C", "D", "E", "F", "G"));
		Set<String> set = toSet(perms);
		assertEquals(set.size(), perms.size());
		assertEquals(5040, set.size());
	}

	private static Set<String> toSet(List<List<String>> perms) {
		Set<String> set = new HashSet<String>();
		for (List<String> perm : perms) {
			String s = "";
			for (String p : perm) {
				s += p;
			}
			set.add(s);
		}
		return set;
	}
}
