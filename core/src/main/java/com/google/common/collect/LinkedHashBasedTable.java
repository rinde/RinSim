/**
 * 
 */
package com.google.common.collect;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Supplier;

/**
 * Similar to {@link HashBasedTable} but with a predictable iteration order,
 * this is similar to the relation between {@link java.util.HashMap} and
 * {@link java.util.LinkedHashMap}.<br/>
 * <br/>
 * 
 * Currently (February 2012) Guava does not have this class itself. This class
 * must reside in the <code>com.google.common.collect</code> package since the
 * {@link StandardTable} class has package visibility.
 * @param <R> row
 * @param <C> column
 * @param <V> value
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 */
@SuppressWarnings("javadoc")
public class LinkedHashBasedTable<R, C, V> extends StandardTable<R, C, V> {

    private static final long serialVersionUID = 0L;

    LinkedHashBasedTable(Map<R, Map<C, V>> pBackingMap, Factory<C, V> pFactory) {
        super(pBackingMap, pFactory);
    }

    /**
     * Instantiates a {@link LinkedHashBasedTable}.
     * @return The newly created instance.
     */
    public static <R, C, V> LinkedHashBasedTable<R, C, V> create() {
        return new LinkedHashBasedTable<R, C, V>(
                new LinkedHashMap<R, Map<C, V>>(), new Factory<C, V>());
    }

    private static class Factory<C, V> implements Supplier<Map<C, V>>,
            Serializable {

        private static final long serialVersionUID = 0;

        public Factory() {}

        @Override
        public Map<C, V> get() {
            return Maps.newLinkedHashMap();
        }
    }
}
