// Copyright © 2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.util;

import java.util.*;

/**
 * A simple facade to represent immutable lists as map from integer indexes to values.
 */
public class ImmutableIntListAsMap<T> implements Map<Integer, T> {

	private final List<T> l;

	private transient volatile Set<Integer> keySet = null;
	private transient volatile Set<Entry<Integer, T>>  entrySet = null;

	public ImmutableIntListAsMap(List<T> l) {
		this.l = l;
	}

	@Override
	public int size() {
		return l.size();
	}

	@Override
	public boolean isEmpty() {
		return l.isEmpty();
	}

	private static int idx(Object key) {
		return key == null ? -1 : (int)key;
	}

	private boolean containsIdx(int i) {
		return i >= 0 && i < size();
	}

	@Override
	public boolean containsKey(Object key) {
		int i = idx(key);
		return containsIdx(i);
	}

	@Override
	public boolean containsValue(Object value) {
		return l.contains(value);
	}

	@Override
	public T get(Object key) {
		int i = idx(key);
		return containsIdx(i) ? l.get(i) : null;
	}

	@Override
	public T put(Integer key, T value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T remove(Object key) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void putAll(Map<? extends Integer, ? extends T> m) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Set<Integer> keySet() {
		Set<Integer> ks = keySet;
		return (ks != null ? ks : (keySet = new KeySet()));
	}

	@Override
	public Collection<T> values() {
		return l;
	}

	@Override
	public Set<Entry<Integer, T>> entrySet() {
		Set<Entry<Integer, T>> es = entrySet;
		return (es != null ? es : (entrySet = new EntrySet()));
	}

	class KeySet extends AbstractSet<Integer> {

		@Override
		public int size() {
			return l.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsKey(o);
		}

		@Override
		public Iterator<Integer> iterator() {
			return new Iterator<Integer>() {
				final int sz = size();
				int next = 0;

				@Override
				public boolean hasNext() {
					return next < sz;
				}

				@Override
				public Integer next() {
					return next++;
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public boolean add(Integer key) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}

	class EntrySet extends AbstractSet<Entry<Integer, T>> {

		@Override
		public int size() {
			return l.size();
		}

		@Override
		public boolean contains(Object o) {
			return containsValue(o);
		}

		@Override
		public Iterator<Entry<Integer, T>> iterator() {
			return new Iterator<Entry<Integer, T>>() {
				final int sz = size();
				int nextIdx = 0;

				@Override
				public boolean hasNext() {
					return nextIdx < sz;
				}

				@Override
				public Entry<Integer, T> next() {
					if (!hasNext()) {
						throw new NoSuchElementException();
					}
					T val = get(nextIdx);
					return new AbstractMap.SimpleImmutableEntry<Integer, T>(nextIdx++, val);
				}

				@Override
				public void remove() {
					throw new UnsupportedOperationException();
				}
			};
		}

		@Override
		public boolean add(Entry<Integer, T> e) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean removeAll(Collection<?> c) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException();
		}
	}
}
