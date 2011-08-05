package rinde.graduation.util;

/**
 * @author Rinde van Lon
 *         Created: Apr 28, 2010
 */

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;

import org.apache.commons.math.random.RandomGenerator;

/**
 *
 */
public class Util {

	/**
	 * Compares two int arrays for equality, treats them as sets, meaning that
	 * the order is irrelevant.
	 * @param set1
	 * @param set2
	 * @return true if the two sets contain the same values (regardless of
	 *         order), false otherwise.
	 */
	public static boolean setEquals(final int[] set1, final int[] set2) {
		return new HashSet<Integer>(toIntegerList(set1)).equals(new HashSet<Integer>(toIntegerList(set2)));
	}

	public static boolean deepSetEquals(final int[][] set1, final int[][] set2) {
		for (int i = 0; i < set1.length; i++) {
			if (!setEquals(set1[i], set2[i])) {
				return false;
			}
		}
		return true;
	}

	public static <T> List<T> select(List<T> list, final int... indices) {
		List<T> newList = new ArrayList<T>();
		for (int i : indices) {
			newList.add(list.get(i));
		}
		return newList;
	}

	public static <T> T[] select(final T[] array, final int... indices) {
		@SuppressWarnings("unchecked")
		final T[] subset = (T[]) Array.newInstance(array.getClass().getComponentType(), indices.length);
		for (int i = 0; i < indices.length; i++) {
			subset[i] = array[indices[i]];
		}
		return subset;
	}

	public static int[] select(final int[] array, final int... indices) {
		final int[] subset = new int[indices.length];
		for (int i = 0; i < indices.length; i++) {
			subset[i] = array[indices[i]];
		}
		return subset;
	}

	public static int[] reverse(int[] ints) {
		List<Integer> collection = toIntegerList(ints);
		Collections.reverse(collection);
		return toIntArray(collection);
	}

	public static boolean setEquals(final double[] set1, final double[] set2) {
		return new HashSet<Double>(toDoubleList(set1)).equals(new HashSet<Double>(toDoubleList(set2)));
	}

	public static boolean deepSetEquals(final double[][] set1, final double[][] set2) {
		for (int i = 0; i < set1.length; i++) {
			if (!setEquals(set1[i], set2[i])) {
				return false;
			}
		}
		return true;
	}

	public static double[] quartiles(final double... doubles) {
		final double[] quartiles = new double[5];
		final double[] sortedVals = Arrays.copyOf(doubles, doubles.length);
		Arrays.sort(sortedVals);

		quartiles[0] = sortedVals[0];// min
		quartiles[4] = sortedVals[sortedVals.length - 1];// max

		final int median = (int) Math.floor(sortedVals.length / 2);
		quartiles[2] = sortedVals[median];// median
		quartiles[1] = sortedVals[(int) Math.floor((median - 1) / 2)];// 1st
																		// quartile
		quartiles[3] = sortedVals[median + (int) Math.floor((sortedVals.length - median) / 2)];// 3rd
		// quartile

		return quartiles;
	}

	public static double[] round(final double[] doubles, final int precision) {
		final double[] rounded = new double[doubles.length];
		for (int i = 0; i < doubles.length; i++) {
			rounded[i] = round(doubles[i], precision);
		}
		return rounded;
	}

	public static double round(final double d, final int precision) {
		final int p = (int) Math.pow(10, precision);
		return Math.round(d * p) / (double) p;
	}

	public static double var(final double... doubles) {
		final double mean = mean(doubles);
		double sum = 0;
		for (final double d : doubles) {
			sum += Math.pow(d - mean, 2);
		}
		return sum / doubles.length;
	}

	public static double std(final double... doubles) {
		return Math.sqrt(var(doubles));
	}

	public static double mean(final double... doubles) {
		double sum = 0;
		for (final double d : doubles) {
			sum += d;
		}
		return sum / doubles.length;
	}

	public static double mean(Collection<Long> vals) {
		long sum = 0;
		for (Long l : vals) {
			sum += l;
		}
		return sum / (double) vals.size();
	}

	public static int[] toIntArray(final List<Integer> intList) {
		final int[] intArray = new int[intList.size()];
		for (int i = 0; i < intList.size(); i++) {
			intArray[i] = intList.get(i).intValue();
		}
		return intArray;
	}

	public static int[] toIntArray(final char[] arr) {
		final int[] intArray = new int[arr.length];
		for (int i = 0; i < arr.length; i++) {
			intArray[i] = arr[i];
		}
		return intArray;
	}

	public static List<List<Integer>> asList(final int[][] matrix) {
		final List<List<Integer>> outMatrix = new ArrayList<List<Integer>>();
		for (final int[] i : matrix) {
			outMatrix.add(Util.toIntegerList(i));
		}
		return outMatrix;
	}

	public static List<List<Double>> asList(final double[][] matrix) {
		final List<List<Double>> outMatrix = new ArrayList<List<Double>>();
		for (final double[] i : matrix) {
			outMatrix.add(Util.toDoubleList(i));
		}
		return outMatrix;
	}

	public static <T> List<List<T>> permutations(Collection<T> items) {
		if (items instanceof List) {
			return permutations((List<T>) items);
		}
		return permutations(new ArrayList<T>(items));
	}

	public static <T> List<List<T>> permutations(List<T> items) {
		List<List<T>> perms = new ArrayList<List<T>>();

		//		Find the largest index k such that a[k] < a[k + 1]. If no such index exists, the permutation is the last permutation.
		//		Find the largest index l such that a[k] < a[l]. Since k + 1 is such an index, l is well defined and satisfies k < l.
		//		Swap a[k] with a[l].
		//		Reverse the sequence from a[k + 1] up to and including the final element a[n].

		int[] indices = ints(0, items.size());
		while (true) {
			perms.add(select(items, indices));

			int k = -1;
			for (int j = indices.length - 2; j >= 0; j--) {
				if (indices[j] < indices[j + 1]) {
					k = j;
					break;
				}
			}
			if (k == -1) {
				break;
			}
			int l;
			for (l = indices.length - 1; l >= 0; l--) {
				if (indices[k] < indices[l]) {
					break;
				}
			}
			swap(indices, k, l);
			int[] subset = reverse(select(indices, ints(k + 1, indices.length)));
			indices = concat(Arrays.copyOf(indices, k + 1), subset);
		}
		return perms;
	}

	/**
	 * Repeating indices are allowed.
	 * 
	 * @param <T>
	 * @param from
	 * @param indices
	 * @return An array which will contain the values of <code>from</code> at
	 *         the specified <code>indices</code>
	 */
	@SuppressWarnings("unchecked")
	public static <T> T[] subset(final T[] from, final int[] indices) {
		final Class<?> type = from.getClass();
		final T[] subset = type == Object[].class ? (T[]) new Object[indices.length] : (T[]) Array.newInstance(type.getComponentType(), indices.length);
		for (int i = 0; i < indices.length; i++) {
			subset[i] = from[indices[i]];
		}
		return subset;
	}

	public static double[] toDoubleArray(final List<? extends Number> doubleList) {
		final double[] doubleArray = new double[doubleList.size()];
		for (int i = 0; i < doubleList.size(); i++) {
			doubleArray[i] = doubleList.get(i).doubleValue();
		}
		return doubleArray;
	}

	public static boolean containsDuplicates(final int[] array) {
		return new HashSet<Integer>(toIntegerList(array)).size() != array.length;
	}

	public static double computeSimilarity(final double[] list1, final double[] list2) {
		assert list1.length <= list2.length : "list1 must be smaller or equal to list2";
		assert list1.length > 0 : "lists must contain at least one value";
		int score = 0;
		for (int i = 0; i < list1.length; i++) {
			score += list1[i] == list2[i] ? 1 : 0;
		}
		return (double) score / list1.length;
	}

	public static double computeDistance(final double[] list1, final double[] list2) {
		assert list1.length == list2.length : "lists must be of equal size";
		assert list1.length > 0 : "lists must contain at least one value";
		double score = 0;
		for (int i = 0; i < list1.length; i++) {
			final double dist = Math.abs(list1[i] - list2[i]);
			// TODO think of a good score metric
			score += Double.isNaN(dist) || Double.isInfinite(dist) ? 1 : dist;
			// dist > 0.05 ? 1 : dist;
		}
		return score / list1.length;
	}

	// @SuppressWarnings("unchecked")
	// public static <T> T[][] copyOf(final T[][] original, final int newLength)
	// {
	// return (T[][]) copyOf(original, newLength, original.getClass());
	// }
	//
	// @SuppressWarnings("unchecked")
	// public static <T, U> T[][] copyOf(final T[][] original, final int
	// newLength, final Class<? extends T[][]> newType) {
	// final T[][] copy = ((Object) newType == (Object) Object[][].class) ?
	// (T[][]) new Object[newLength] : (T[][]) Array.newInstance(newType
	// .getComponentType(), newLength);
	// for (int i = 0; i < original.length; i++) {
	// copy[i] = Arrays.copyOf(original[i], original[i].length);
	// }
	// // final T[][] copy = System.arraycopy(original, 0, copy, 0,
	// // Math.min(original.length, newLength));
	// return copy;
	// }

	public static double[][] deepCopyOf(final double[][] original, final int newLength) {
		final double[][] copy = new double[newLength][];
		for (int i = 0; i < Math.min(newLength, original.length); i++) {
			copy[i] = Arrays.copyOf(original[i], original[i].length);
		}
		return copy;
	}

	public static int[][] deepCopyOf(final int[][] original, final int newLength) {
		final int[][] copy = new int[newLength][];
		for (int i = 0; i < Math.min(newLength, original.length); i++) {
			copy[i] = Arrays.copyOf(original[i], original[i].length);
		}
		return copy;
	}

	public static int[] drawRandomSet(final RandomGenerator rnd, final int from, final int to, final int num) {
		return drawRandomSet(rnd, from, to, num, new int[] {});
	}

	/**
	 * Chooses 'num' int's from [from .. (to-1)]
	 * 
	 * @param rnd
	 * @param from
	 * @param to
	 * @param num
	 * @param blockSet
	 * @return A list of random drawn integers
	 */
	public static int[] drawRandomSet(final RandomGenerator rnd, final int from, final int to, final int num, final Set<Integer> blockSet) {

		final ArrayList<Integer> set = new ArrayList<Integer>();
		for (int i = from; i < to; i++) {
			if (!blockSet.contains(new Integer(i))) {
				set.add(new Integer(i));
			}
		}
		Util.shuffle(set, rnd);
		return Util.toIntArray(set.subList(0, num));
	}

	public static int[] drawRandomSet(final RandomGenerator rnd, final int from, final int to, final int num, final int[] blockList) {
		return drawRandomSet(rnd, from, to, num, new HashSet<Integer>(Util.toIntegerList(blockList)));
	}

	public static double[] nRandomDoubles(final RandomGenerator rnd, final int n) {
		final double[] doubles = new double[n];
		for (int i = 0; i < n; i++) {
			doubles[i] = rnd.nextDouble();
		}
		return doubles;
	}

	public static int randomWithoutDuplicates(final RandomGenerator rnd, final int from, final int to, final int[] blockList) {
		assert to - from > blockList.length : from + " to " + to + " > " + blockList.length;
		final ArrayList<Integer> set = new ArrayList<Integer>();
		for (int i = from; i < to; i++) {
			set.add(new Integer(i));
		}
		set.removeAll(toIntegerList(blockList));
		return set.get(rnd.nextInt(set.size())).intValue();
	}

	public static int[] removeAt(final int[] arr, final int index) {
		final int[] ret = new int[arr.length - 1];
		System.arraycopy(arr, 0, ret, 0, arr.length - 1);
		System.arraycopy(arr, index + 1, ret, index, arr.length - index - 1);
		return ret;
	}

	// /**
	// * Removes object in the arr array.
	// *
	// * @param <T>
	// * @param arr
	// * @param index
	// * @return
	// */
	// public static <T> T[] removeAt(final T[] arr, final int index) {
	// final List<T> list = Arrays.asList(arr);
	// list.remove(index);
	// return list.toArray(arr);
	// }

	// int[] concat(final int[] A, final int[] B) {
	// final int[] C = new int[A.length + B.length];
	// System.arraycopy(A, 0, C, 0, A.length);
	// System.arraycopy(B, 0, C, A.length, B.length);
	//
	// return C;
	// }

	public static boolean[] randomFlags(final RandomGenerator rnd, final int n, final double prob) {
		return randomFlags(rnd, 0, n, prob);
	}

	/**
	 * 
	 * @param rnd
	 * @param minTrue
	 * @param n
	 * @param prob
	 * @return A boolean array of size n containing at least minTrue
	 *         <code>true</code> values.
	 */
	public static boolean[] randomFlags(final RandomGenerator rnd, final int minTrue, final int n, final double prob) {
		final boolean[] flags = new boolean[n];
		for (int i = 0; i < n; i++) {
			flags[i] = rnd.nextDouble() < prob;
		}
		// overwrite these indices
		final int[] trueIndices = drawRandomSet(rnd, 0, n, minTrue);
		for (int i = 0; i < minTrue; i++) {
			flags[trueIndices[i]] = true;
		}
		return flags;
	}

	public static int randomCount(final RandomGenerator rnd, final int min, final int num, final double prob) {
		int count = min;
		for (int i = min; i < num; i++) {
			if (rnd.nextDouble() < prob) {
				count++;
			}
		}
		return count;
	}

	public static boolean contains(final double[] array, final double item) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == item) {
				return true;
			}
		}
		return false;
	}

	public static boolean contains(final int[] array, final double item) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == item) {
				return true;
			}
		}
		return false;
	}

	/**
	 * <code>
	 * [1,2]
	 * [1,2]
	 * [1]
	 * []
	 * </code>
	 * 
	 * becomes:<code>
	 * [1,2]
	 * [1]
	 * </code>
	 * 
	 * 
	 * @param list
	 * @return the folded list
	 */
	public static List<int[]> fold(final List<int[]> list) {
		final List<int[]> result = new ArrayList<int[]>();
		for (final int[] i : list) {
			if (i.length > 0 && (result.isEmpty() || !Arrays.equals(result.get(result.size() - 1), i))) {
				result.add(i);
			}
		}
		return result;
	}

	public static List<int[]> generatePaths(final List<double[]> doubles, final int numMax, final int maxEditDistance) {
		final List<int[]> result = Util.multiMaxIndices(numMax, doubles);

		int[] bestPath = new int[result.size()];
		for (int i = 0; i < result.size(); i++) {
			bestPath[i] = result.get(i)[0];
		}

		final ArrayList<int[]> paths = new ArrayList<int[]>();
		paths.addAll(removeDuplicatesInOrder(generateOneOffs(result, bestPath, 0, maxEditDistance - 1)));

		final ArrayList<int[]> finalPaths = new ArrayList<int[]>();
		bestPath = fold(bestPath);
		if (bestPath.length > 0) {
			finalPaths.add(bestPath);
		}
		finalPaths.addAll(paths);
		return finalPaths;
	}

	public static List<int[]> generateOneOffs(final List<int[]> possibilities, final int[] rootPath, final int startIndex, final int depth) {
		final List<int[]> paths = new ArrayList<int[]>();
		final List<int[]> furtherPaths = new ArrayList<int[]>();
		for (int i = startIndex; i < possibilities.size(); i++) {
			if (possibilities.get(i).length > 1) {
				for (int j = 1; j < possibilities.get(i).length; j++) {
					final int[] path = Arrays.copyOf(rootPath, rootPath.length);
					path[i] = possibilities.get(i)[j];
					if (!Arrays.equals(path, rootPath)) {
						paths.add(fold(path));
						if (depth > 0) {
							furtherPaths.addAll(generateOneOffs(possibilities, path, startIndex + 1, depth - 1));
						}
					}
				}
			}
		}
		paths.addAll(furtherPaths);
		return paths;
	}

	public static List<int[]> removeDuplicates(final List<int[]> list) {
		final HashMap<String, int[]> map = new HashMap<String, int[]>();
		for (final int[] i : list) {
			map.put(Arrays.toString(i), i);
		}
		return new ArrayList<int[]>(map.values());
	}

	public static List<int[]> removeDuplicatesInOrder(final List<int[]> list) {
		final HashMap<String, int[]> set = new HashMap<String, int[]>();
		final List<int[]> newList = new ArrayList<int[]>();
		for (final Iterator<int[]> iter = list.iterator(); iter.hasNext();) {
			final int[] element = iter.next();
			if (set.put(Arrays.toString(element), element) == null) {
				newList.add(element);
			}
		}
		return newList;
	}

	public static int[] fold(final int[] trail) {
		final int[] result = new int[trail.length];
		int cur = 0;
		for (int i = 0; i < trail.length; i++) {
			if (cur == 0 || cur > 0 && result[cur - 1] != trail[i]) {
				result[cur] = trail[i];
				cur++;
			}
		}
		return Arrays.copyOf(result, cur);
	}

	public static String fold(final String word) {
		String result = "";
		for (int i = 0; i < word.length(); i++) {
			if (result.length() == 0 || word.charAt(i) != result.charAt(result.length() - 1)) {
				result += word.charAt(i);
			}
		}
		return result;
	}

	public static String[] replaceAll(final String[] list, final String orig, final String dest) {
		final String[] newList = new String[list.length];
		for (int i = 0; i < list.length; i++) {
			if (list[i].equals(orig)) {
				newList[i] = dest;
			} else {
				newList[i] = list[i];
			}
		}
		return newList;
	}

	public static String toString(final String[] strings, final String separator) {
		final StringBuilder sb = new StringBuilder();
		for (final String s : strings) {
			sb.append(s + separator);
		}
		return sb.toString();
	}

	public static String toString(final Object[] objects, final String separator) {
		final StringBuilder sb = new StringBuilder();
		for (final Object s : objects) {
			sb.append(s + separator);
		}
		return sb.toString();
	}

	public static String toString(final double[] doubles, final String separator) {
		final StringBuilder sb = new StringBuilder();
		for (final double d : doubles) {
			sb.append(d + separator);
		}
		return sb.toString();
	}

	public static String[] fold(final String... strings) {
		final List<String> list = new ArrayList<String>();
		for (final String s : strings) {
			if (list.isEmpty() || !list.get(list.size() - 1).equals(s)) {
				list.add(s);
			}
		}
		return list.toArray(new String[list.size()]);
	}

	/**
	 * 
	 * @param matrix must be uniform
	 * @return the matrix folded on its columns
	 */
	public static List<int[]> multiColumnFold(final List<int[]> matrix) {
		final List<int[]> fold = new ArrayList<int[]>();
		final int width = matrix.get(0).length;
		for (int i = 0; i < width; i++) {
			final int[] cur = new int[matrix.size()];
			for (int j = 0; j < matrix.size(); j++) {
				cur[j] = matrix.get(j)[i];
			}
			fold.add(fold(cur));
		}
		return fold;
	}

	public static boolean areValidFiles(final String[] strings) {
		final File[] files = new File[strings.length];
		for (int i = 0; i < strings.length; i++) {
			files[i] = new File(strings[i]);
		}
		return areValidFiles(files);
	}

	public static File[] toFiles(final String[] strings) {
		return toFiles(strings, "");
	}

	public static File[] toFiles(final String[] strings, final String prefix) {
		final File[] files = new File[strings.length];
		for (int i = 0; i < strings.length; i++) {
			files[i] = new File(prefix + strings[i]);
		}
		return files;
	}

	public static boolean areValidFiles(final File[] files) {
		for (final File f : files) {
			if (!isValidFile(f)) {
				return false;
			}
		}
		return true;
	}

	public static boolean isValidFile(final File f) {
		return f != null && f.exists();
	}

	public static boolean contains(final int[] array, final int item) {
		for (int i = 0; i < array.length; i++) {
			if (array[i] == item) {
				return true;
			}
		}
		return false;
	}

	public static double[] toDoubles(final String[] strings) {
		final double[] doubles = new double[strings.length];
		for (int j = 0; j < strings.length; j++) {
			doubles[j] = Double.parseDouble(strings[j]);
		}
		return doubles;
	}

	public static boolean[] toBooleans(final String[] strings) {
		final boolean[] booleans = new boolean[strings.length];
		for (int j = 0; j < strings.length; j++) {
			booleans[j] = Boolean.parseBoolean(strings[j]);
		}
		return booleans;
	}

	public static int[] toInts(final String[] strings) {
		final int[] vals = new int[strings.length];
		for (int j = 0; j < strings.length; j++) {
			vals[j] = Integer.parseInt(strings[j]);
		}
		return vals;
	}

	// rounds the doubles, and returns an int array
	public static int[] toInts(final double[] doubles) {
		final int[] vals = new int[doubles.length];
		for (int j = 0; j < doubles.length; j++) {
			vals[j] = (int) Math.round(doubles[j]);
		}
		return vals;
	}

	public static List<Boolean> toBooleanList(final boolean[] bools) {
		final List<Boolean> boolList = new ArrayList<Boolean>(bools.length);
		for (final boolean bool : bools) {
			boolList.add(new Boolean(bool));
		}
		return boolList;
	}

	public static List<Integer> toIntegerList(final int[] ints) {
		final List<Integer> integerList = new ArrayList<Integer>(ints.length);
		for (final int i : ints) {
			integerList.add(new Integer(i));
		}
		return integerList;
	}

	public static List<Integer> toIntegerList(final double[] doubles) {
		final List<Integer> integerList = new ArrayList<Integer>(doubles.length);
		for (final double d : doubles) {
			integerList.add(new Integer((int) d));
		}
		return integerList;
	}

	public static List<Integer> toIntegerList(final String[] ints) {
		final List<Integer> integerList = new ArrayList<Integer>(ints.length);
		for (final String s : ints) {
			integerList.add(new Integer(Integer.parseInt(s)));
		}
		return integerList;
	}

	public static List<Double> toDoubleList(final double[] doubles) {
		final List<Double> integerList = new ArrayList<Double>(doubles.length);
		for (final double d : doubles) {
			integerList.add(new Double(d));
		}
		return integerList;
	}

	/**
	 * This method is identical to
	 * {@link Collections#shuffle(List, java.util.Random)} except that this
	 * method uses {@link MersenneTwisterFast} as random number generator.
	 * 
	 * @param list
	 * @param rnd
	 * @see Collections#shuffle(List, java.util.Random)
	 */
	@SuppressWarnings("unchecked")
	public static void shuffle(final List<?> list, final RandomGenerator rnd) {
		final int size = list.size();
		if (size < 5 || list instanceof RandomAccess) {
			for (int i = size; i > 1; i--) {
				Collections.swap(list, i - 1, rnd.nextInt(i));
			}
		} else {
			final Object arr[] = list.toArray();

			// Shuffle array
			for (int i = size; i > 1; i--) {
				swap(arr, i - 1, rnd.nextInt(i));
			}

			// Dump array back into list
			@SuppressWarnings("rawtypes")
			final ListIterator it = list.listIterator();
			for (int i = 0; i < arr.length; i++) {
				it.next();
				it.set(arr[i]);
			}
		}
	}

	/**
	 * Swaps the two specified elements in the specified array.
	 */
	private static void swap(final Object[] arr, final int i, final int j) {
		final Object tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}

	private static void swap(final int[] arr, final int i, final int j) {
		final int tmp = arr[i];
		arr[i] = arr[j];
		arr[j] = tmp;
	}

	public static <T> void removeDuplicates(final ArrayList<T> list) {
		final HashSet<T> set = new HashSet<T>(list);
		list.clear();
		list.addAll(set);
	}

	public static String spaceCharacters(final String s) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			sb.append(s.charAt(i) + " ");
		}
		return sb.toString().trim();
	}

	public static String toString(final int[] numbers) {
		return toString(numbers, " ");
	}

	public static String toString(final int[] numbers, final String separator) {
		return toString(numbers, "", separator, "");
	}

	public static String toString(final int[] numbers, final String prefix, final String separator, final String postfix) {
		final StringBuilder sb = new StringBuilder(prefix);
		for (int i = 0; i < numbers.length; i++) {
			if (i < numbers.length - 1) {
				sb.append(numbers[i] + separator);
			} else {
				sb.append(numbers[i] + postfix);
			}
		}
		return sb.toString();
	}

	public static double sum(final double... doubles) {
		double sum = 0;
		for (final double d : doubles) {
			sum += d;
		}
		return sum;
	}

	/**
	 * @param doubles
	 * @return the min value
	 */
	public static double min(final double... doubles) {
		if (doubles.length > 0) {
			double min = doubles[0];
			for (int i = 1; i < doubles.length; i++) {
				min = Math.min(min, doubles[i]);
			}
			return min;
		}
		return 0;
	}

	/**
	 * @param doubles
	 * @return the max value or 0 if there are no doubles specified
	 */
	public static double max(final double... doubles) {
		if (doubles.length > 0) {
			double max = doubles[0];
			for (int i = 1; i < doubles.length; i++) {
				max = Math.max(max, doubles[i]);
			}
			return max;
		}
		return 0;
	}

	/**
	 * num = 2, doubles = [4,2,8] returns [2,0]
	 * num = 2, doubles = [4,4,8] returns [2]
	 * @param num
	 * @param doubles
	 * @return the indices of the n highest values in the doubles array
	 */
	public static int[] maxIndices(final int num, final double... doubles) {
		final double[] copy = Arrays.copyOf(doubles, doubles.length);
		int returnLength = Math.min(num, doubles.length);

		final List<Entry> list = new ArrayList<Entry>();
		for (int i = 0; i < copy.length; i++) {
			list.add(new Entry(i, copy[i]));
		}

		Collections.sort(list);

		final List<Entry> sub = list.subList(list.size() - returnLength, list.size());
		if (returnLength < list.size()) {
			int cutOff = 0;
			for (int i = 0; i < sub.size(); i++) {
				if (list.get(list.size() - returnLength - 1).val == sub.get(i).val) {
					cutOff++;
				} else {
					break;
				}
			}
			returnLength -= cutOff;
		}

		final int[] indices = new int[returnLength];
		for (int i = 0; i < returnLength; i++) {
			indices[i] = list.get(list.size() - 1 - i).index;
		}
		return indices;
	}

	public static int maxIndex(final double... doubles) {
		if (doubles.length > 0) {
			double max = doubles[0];
			int index = 0;
			for (int i = 1; i < doubles.length; i++) {
				if (doubles[i] > max) {
					max = doubles[i];
					index = i;
				}
			}
			return index;
		}
		return -1;
	}

	public static int minIndex(final double... doubles) {
		if (doubles.length > 0) {
			double min = doubles[0];
			int index = 0;
			for (int i = 1; i < doubles.length; i++) {
				if (doubles[i] < min) {
					min = doubles[i];
					index = i;
				}
			}
			return index;
		}
		return -1;
	}

	public static int indexOf(final int[] ints, final int num) {
		for (int i = 0; i < ints.length; i++) {
			if (ints[i] == num) {
				return i;
			}
		}
		return -1;
	}

	public static List<int[]> multiMaxIndices(final int num, final List<double[]> list) {
		final List<int[]> multiIndices = new ArrayList<int[]>(list.size());
		for (int i = 0; i < list.size(); i++) {
			multiIndices.add(maxIndices(num, list.get(i)));
		}
		return fold(multiIndices);// multiColumnFold(multiIndices);
	}

	public static void print(final List<int[]> list) {
		System.out.println("*** begin ***");
		for (final int[] i : list) {
			System.out.println(Arrays.toString(i));
		}
		System.out.println("*** end ***");
	}

	public static int[] concat(final int[]... ints) {
		int length = 0;
		for (final int[] i : ints) {
			length += i.length;
		}
		final int[] arr = new int[length];
		int c = 0;
		for (final int[] i : ints) {
			for (final int j : i) {
				arr[c] = j;
				c++;
			}
		}
		return arr;
	}

	public static int[] ints(final int from, final int to) {
		final int[] arr = new int[Math.abs(to - from)];
		int c = 0;
		for (int i = from; i < to; i++) {
			arr[c] = i;
			c++;
		}
		return arr;
	}

	public static String addLeadingZeros(final int num, final int desiredLength) {
		final StringBuilder sb = new StringBuilder();
		final int l = desiredLength - ("" + num).length();
		for (int i = 0; i < l; i++) {
			sb.append("0");
		}
		sb.append(num);
		return sb.toString();
	}

	public static List<File> listFiles(final File dir, final FilenameFilter filter) {
		final List<File> result = new ArrayList<File>();

		final File[] files = dir.listFiles(filter);
		result.addAll(Arrays.asList(files));

		final File[] folders = dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File pPathname) {
				return pPathname.isDirectory();
			}
		});

		for (final File folder : folders) {
			result.addAll(listFiles(folder, filter));
		}

		return result;
	}

	public static <R extends Collection<V>, V> R deepCollectionCopy(Collection<V> original, Class<R> returnType) {
		try {
			R copy = returnType.newInstance();
			for (V obj : original) {
				copy.add(obj);
			}
			return copy;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Collection<V>, V> T deepCollectionCopy(T original) {
		return (T) deepCollectionCopy(original, original.getClass());
	}

	public static <R extends Map<K, V>, T extends Map<K, V>, K, V> R deepMapCopy(T original, Class<R> returnType) {
		try {
			R copy = returnType.newInstance();
			for (java.util.Map.Entry<K, V> entry : original.entrySet()) {
				copy.put(entry.getKey(), entry.getValue());
			}
			return copy;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends Map<K, V>, K, V> T deepMapCopy(T original) {
		return (T) deepMapCopy(original, original.getClass());
	}
}

final class Entry implements Comparable<Entry> {
	double val;
	int index;

	public Entry(final int pIndex, final double pVal) {
		val = pVal;
		index = pIndex;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final Entry other) {
		return Double.compare(val, other.val);
	}

	@Override
	public String toString() {
		return index + " = " + val;
	}
}
