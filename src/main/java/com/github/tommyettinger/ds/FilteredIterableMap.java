/*
 * Copyright (c) 2022-2023 See AUTHORS file.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.tommyettinger.ds;

import com.github.tommyettinger.function.ObjPredicate;
import com.github.tommyettinger.function.ObjToSameFunction;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * A customizable variant on ObjectMap that uses Iterable keys made of K sub-keys, and only considers a sub-key (for
 * equality and hashing purposes) if that sub-key satisfies a predicate. This can also edit the sub-keys that pass
 * the filter, such as to normalize their data during comparisons (and hashing). You will usually want to call
 * {@link #setFilter(ObjPredicate)} and/or {@link #setEditor(ObjToSameFunction)} to change the behavior of hashing and
 * equality before you enter any keys, unless you have specified the filter and/or editor you want in the constructor.
 * Calling {@link #setModifiers(ObjPredicate, ObjToSameFunction)} is recommended if you need to set both the filter and
 * the editor; you could also set them in the constructor.
 * <br>
 * This class is related to {@link FilteredStringMap}, which can be seen as using a String as a key and the characters
 * of that String as its sub-keys. That means this is also similar to {@link CaseInsensitiveMap}, which is essentially
 * a specialized version of FilteredIterableMap (which can be useful for serialization).
 */
public class FilteredIterableMap<K, I extends Iterable<K>, V> extends ObjectObjectMap<I, V> {
	protected ObjPredicate<K>      filter = c -> true;
	protected ObjToSameFunction<K> editor = c -> c;
	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This considers all sub-keys in an Iterable key and does not edit any sub-keys.
	 */
	public FilteredIterableMap () {
		super();
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity keys before growing the backing table.
	 * This considers all sub-keys in an Iterable key and does not edit any sub-keys.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredIterableMap (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This set will hold initialCapacity keys before
	 * growing the backing table.
	 * This considers all sub-keys in an Iterable key and does not edit any sub-keys.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredIterableMap (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 */
	public FilteredIterableMap (ObjPredicate<K> filter, ObjToSameFunction<K> editor) {
		super();
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity keys before growing the backing table.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredIterableMap (ObjPredicate<K> filter, ObjToSameFunction<K> editor, int initialCapacity) {
		super(initialCapacity);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This set will hold initialCapacity keys before
	 * growing the backing table.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredIterableMap (ObjPredicate<K> filter, ObjToSameFunction<K> editor, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map another FilteredIterableMap to copy
	 */
	public FilteredIterableMap (FilteredIterableMap<K, ? extends I, ? extends V> map) {
		super(map);
		filter = map.filter;
		editor = map.editor;
	}

	/**
	 * Creates a new map with the given filter and editor, and attempts to insert every entry from the given {@code map}
	 * into the new data structure. Not all keys from {@code map} might be entered if the filter and editor consider
	 * some as equal.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param map a Map to copy
	 */
	public FilteredIterableMap (ObjPredicate<K> filter, ObjToSameFunction<K> editor, Map<? extends I, ? extends V> map) {
		this(filter, editor, map.size());
		putAll(map);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 * This uses the specified filter and editor, including while it enters the keys and values.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param keys a Collection of keys
	 * @param values a Collection of values
	 */
	public FilteredIterableMap (ObjPredicate<K> filter, ObjToSameFunction<K> editor, Collection<? extends I> keys, Collection<? extends V> values) {
		this(filter, editor, keys.size());
		putAll(keys, values);
	}

	/**
	 * Creates a new map using all the keys from the given {@code keys} and {@code values}.
	 * This uses the specified filter and editor, including while it enters the keys and values.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param keys  an array to draw keys from
	 * @param values  an array to draw values from
	 */
	public FilteredIterableMap (ObjPredicate<K> filter, ObjToSameFunction<K> editor, I[] keys, V[] values) {
		this(filter, editor, Math.min(keys.length, values.length));
		putAll(keys, values);
	}

	public ObjPredicate<K> getFilter() {
		return filter;
	}

	/**
	 * Sets the filter that determines which sub-keys in an Iterable are considered for equality and hashing, then
	 * returns this object, for chaining. ObjPredicate<K> filters could be lambdas or method references that take a
	 * sub-key and return true if that sub-key will be used for hashing/equality, or return false to ignore it.
	 * The default filter always returns true. If the filter changes, that invalidates anything previously entered into
	 * this, so before changing the filter <em>this clears the entire data structure</em>, removing all existing entries.
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @return this, for chaining
	 */
	public FilteredIterableMap<K, I, V> setFilter(ObjPredicate<K> filter) {
		clear();
		this.filter = filter;
		return this;
	}

	public ObjToSameFunction<K> getEditor() {
		return editor;
	}

	/**
	 * Sets the editor that can alter the sub-keys in an Iterable key when they are being used for equality and
	 * hashing. This does not apply any changes to the keys in this data structure; it only affects how they are
	 * hashed or compared. An editor could be a lambda or method reference; the only real requirement is that it
	 * takes a {@code K} sub-key and returns a {@code K} sub-key.
	 * The default filter returns the sub-key it is passed without changes. If the editor changes, that invalidates
	 * anything previously entered into this, so before changing the editor <em>this clears the entire data
	 * structure</em>, removing all existing entries.
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @return this, for chaining
	 */
	public FilteredIterableMap<K, I, V> setEditor(ObjToSameFunction<K> editor) {
		clear();
		this.editor = editor;
		return this;
	}

	/**
	 * Equivalent to calling {@code myMap.setFilter(filter).setEditor(editor)}, but only clears the data structure once.
	 * @see #setFilter(ObjPredicate)
	 * @see #setEditor(ObjToSameFunction)
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @return this, for chaining
	 */
	public FilteredIterableMap<K, I, V> setModifiers(ObjPredicate<K> filter, ObjToSameFunction<K> editor) {
		clear();
		this.filter = filter;
		this.editor = editor;
		return this;
	}

	protected long hashHelper(I s) {
		long hash = hashMultiplier;
		for (K c : s) {
			if(filter.test(c)){
				hash = (hash + editor.apply(c).hashCode()) * hashMultiplier;
			}
		}
		return hash;
	}

	@Override
	protected int place (Object item) {
		if (item instanceof Iterable) {
			return (int)(hashHelper((I) item) >>> shift);
		}
		return super.place(item);
	}

	/**
	 * Compares two objects for equality by the rules this filtered data structure uses for keys.
	 * This will return true if the arguments are reference-equivalent or both null. Otherwise, it
	 * requires that both are {@link Iterable}s and compares them using the {@link #getFilter() filter}
	 * and {@link #getEditor() editor} of this object.
	 *
	 * @param left  must be non-null; typically a key being compared, but not necessarily
	 * @param right may be null; typically a key being compared, but can often be null for an empty key slot, or some other type
	 * @return true if left and right are equivalent according to the rules this filtered type uses
	 */
	@Override
	public boolean equate (Object left, @Nullable Object right) {
		if (left == right)
			return true;
		if(right == null) return false;
		if ((left instanceof Iterable) && (right instanceof Iterable)) {
			Iterable l = (Iterable)left, r = (Iterable)right;
			int countL = 0, countR = 0;
			Iterator<? extends K> i = l.iterator(), j = r.iterator();
			K cl = null, cr = null;
			while (i.hasNext() || j.hasNext()) {
				if (!i.hasNext()) {
					cl = null;
				} else {
					boolean found = false;
					while (i.hasNext() && !(found = filter.test(cl = i.next()))) {
						cl = null;
					}
					if(found) countL++;
				}
				if (!j.hasNext()) {
					cr = null;
				} else {
					boolean found = false;
					while (j.hasNext() && !(found = filter.test(cr = j.next()))) {
						cr = null;
					}
					if(found) countR++;
				}
				if (!Objects.equals(cl, cr) && !Objects.equals((editor.apply(cl)), (editor.apply(cr)))) {
					return false;
				}
			}
			return countL == countR;
		}
		return false;
	}

	@Override
	public int hashCode () {
		int h = size;
		Object[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Object key = keyTable[i];
			if (key != null) {
				h ^= hashHelper((I)key);
				V value = valueTable[i];
				if (value != null) {h ^= value.hashCode();}
			}
		}
		return h;
	}

	/**
	 * The same as {@link #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)}, except this only takes one
	 * key-value pair, and doesn't allocate an array from using varargs.
	 *
	 * @see #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param key    the only key that will be present in the returned map
	 * @param value  the only value that will be present in the returned map
	 * @return a new FilteredIterableOrderedMap containing only the given key and value
	 * @param <K>    the type of sub-keys inside each Iterable key
	 * @param <I>    the type of keys, which must extend Iterable; inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 */
	public static <K, I extends Iterable<K>, V> FilteredIterableMap<K, I, V> with (ObjPredicate<K> filter, ObjToSameFunction<K> editor, I key, V value) {
		FilteredIterableMap<K, I, V> map = new FilteredIterableMap<>(filter, editor, 1);
		map.put(key, value);
		return map;
	}

	/**
	 * Constructs a map given a filter, an editor, and then alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #FilteredIterableMap(ObjPredicate, ObjToSameFunction, Iterable[], Object[])},
	 * which takes all keys and then all values.
	 * This needs all keys to have the same type and all values to have the same type, because
	 * it gets those types from the first key parameter and first value parameter. Any keys that don't
	 * have I as their type or values that don't have V as their type have that entry skipped.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param key0   the first key; will be used to determine the type of all keys
	 * @param value0 the first value; will be used to determine the type of all values
	 * @param rest   an array or varargs of alternating I, V, I, V... elements
	 * @param <K>    the type of sub-keys inside each Iterable key
	 * @param <I>    the type of keys, which must extend Iterable; inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 * @return a new map containing the given keys and values
	 */
	@SuppressWarnings("unchecked")
	public static <K, I extends Iterable<K>, V> FilteredIterableMap<K, I, V> with (ObjPredicate<K> filter, ObjToSameFunction<K> editor, I key0, V value0, Object... rest) {
		FilteredIterableMap<K, I, V> map = new FilteredIterableMap<>(filter, editor, 1 + (rest.length >>> 1));
		map.put(key0, value0);
		for (int i = 1; i < rest.length; i += 2) {
			try {
				map.put((I)rest[i - 1], (V)rest[i]);
			} catch (ClassCastException ignored) {
			}
		}
		return map;
	}

}
