/**
 * 
 */
package com.google.common.collect;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.common.base.Supplier;

/**
 * @author Rinde van Lon (rinde.vanlon@cs.kuleuven.be)
 * 
 *         Currently (February 2012) Guava does not have this class itself. This
 *         class must reside in the com.google.common.collect package since
 *         the {@link StandardTable} class has package visibility.
 */
public class LinkedHashBasedTable<R, C, V> extends StandardTable<R, C, V> {

	private static final long serialVersionUID = 0L;

	private static class Factory<C, V> implements Supplier<Map<C, V>>, Serializable {

		public Factory() {
		}

		@Override
		public Map<C, V> get() {
			return Maps.newLinkedHashMap();
		}

		private static final long serialVersionUID = 0;
	}

	public static <R, C, V> LinkedHashBasedTable<R, C, V> create() {
		return new LinkedHashBasedTable<R, C, V>(new LinkedHashMap<R, Map<C, V>>(), new Factory<C, V>());
	}

	LinkedHashBasedTable(Map<R, Map<C, V>> backingMap, Factory<C, V> factory) {
		super(backingMap, factory);
	}

}
