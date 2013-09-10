package rinde.sim.core.graph;

import javax.annotation.Nullable;

import com.google.common.base.Objects;

/**
 * Class representing a directed connection (link/edge) in a graph.
 * @param <E> Type of {@link ConnectionData} that is used. This data object can
 *          be used to add additional information to the connection.
 * @since 2.0
 * @author Bartosz Michalik <bartosz.michalik@cs.kuleuven.be>
 * @author Rinde van Lon <rinde.vanlon@cs.kuleuven.be>
 */
public class Connection<E extends ConnectionData> {
  /**
   * The starting point of the connection.
   */
  public final Point from;

  /**
   * The end point of the connection.
   */
  public final Point to;

  @Nullable
  private E data;
  private final int hashCode;

  /**
   * Instantiates a new connection.
   * @param pFrom The starting point of the connection.
   * @param pTo The end point of the connection.
   * @param pData The data that is associated to this connection.
   */
  public Connection(Point pFrom, Point pTo, @Nullable E pData) {
    this.from = pFrom;
    this.to = pTo;
    this.data = pData;

    if (pData != null) {
      hashCode = Objects.hashCode(from, to, data);
    } else {
      hashCode = Objects.hashCode(from, to);
    }
  }

  /**
   * Sets the data associated to this connection to the specified value.
   * @param pData The new data to be associated to this connection.
   */
  public void setData(E pData) {
    this.data = pData;
  }

  /**
   * @return The data that is associated to this connection.
   */
  @Nullable
  public E getData() {
    return data;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof Connection)) {
      return false;
    }
    @SuppressWarnings("rawtypes")
    final Connection other = (Connection) obj;
    if (!from.equals(other.from)) {
      return false;
    }
    if (!to.equals(other.to)) {
      return false;
    }
    final E d = data;
    if (d == null) {
      return other.data == null;
    }
    return d.equals(other.getData());
  }

  @Override
  public String toString() {
    return new StringBuilder(7).append('[').append(from).append("->")
        .append(to).append('[').append(data).append("]]").toString();
  }

}
