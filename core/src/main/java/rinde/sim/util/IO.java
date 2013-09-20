/**
 * 
 */
package rinde.sim.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 */
@Deprecated
public class IO {

  public static void serialize(final Serializable o, final String file) {
    try {
      final ObjectOutputStream s = new ObjectOutputStream(new GZIPOutputStream(
          new BufferedOutputStream(new FileOutputStream(file))));
      s.writeObject(o);
      s.close();
    } catch (final IOException e) {
      throw new RuntimeException("Could not serialize object. "
          + e.getMessage(), e);
    }
  }

  public static <T extends Serializable> T deserialize(final String file,
      Class<T> type) {
    return type.cast(deserialize(file));
  }

  /**
   * @param file
   * @return The object
   * 
   **/
  public static Object deserialize(final String file) {
    try {
      final ObjectInputStream s = new ObjectInputStream(new GZIPInputStream(
          new BufferedInputStream(new FileInputStream(file))));
      final Object o = s.readObject();
      s.close();
      return o;
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static boolean saveToFile(final String file, final CharSequence text) {
    return toFile(file, text, false);
  }

  public static boolean appendToFile(final String file, final CharSequence text) {
    return toFile(file, text, true);
  }

  protected static boolean toFile(final String file, final CharSequence text,
      final boolean append) {
    try {
      final BufferedWriter writer = new BufferedWriter(new FileWriter(file,
          append));
      writer.append(text);
      writer.close();
      return true;
    } catch (final IOException ioe) {
      ioe.printStackTrace();
      return false;
    }
  }

}
