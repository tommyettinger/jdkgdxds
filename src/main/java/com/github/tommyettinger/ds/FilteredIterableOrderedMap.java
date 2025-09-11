/*
 * Copyright (c) 2022-2025 See AUTHORS file.
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

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.sort.FilteredComparators;
import com.github.tommyettinger.ds.support.util.PartialParser;
import com.github.tommyettinger.function.ObjPredicate;
import com.github.tommyettinger.function.ObjToSameFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
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
 * <br>
 * This is very similar to {@link FilteredIterableMap},
 * except that this class maintains insertion order and can be sorted with {@link #sort()}, {@link #sort(Comparator)}, etc.
 * Note that because each Iterable is stored in here in its original form (not modified to make it use the filter and editor),
 * the sorted order might be different than you expect.
 * You can use {@link FilteredComparators#makeComparator(Comparator, ObjPredicate, ObjToSameFunction)} to create a Comparator
 * for {@code I} Iterable items that uses the same rules this class does.
 */
public class FilteredIterableOrderedMap<K, I extends Iterable<K>, V> extends ObjectObjectOrderedMap<I, V> {
	protected ObjPredicate<K> filter = c -> true;
	protected ObjToSameFunction<K> editor = c -> c;

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This considers all sub-keys in an Iterable key and does not edit any sub-keys.
	 *
	 * @param type either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *             use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(OrderType type) {
		super(type);
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity keys before growing the backing table.
	 * This considers all sub-keys in an Iterable key and does not edit any sub-keys.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param type            either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                        use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(int initialCapacity, OrderType type) {
		super(initialCapacity, type);
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This set will hold initialCapacity keys before
	 * growing the backing table.
	 * This considers all sub-keys in an Iterable key and does not edit any sub-keys.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 * @param type            either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                        use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(int initialCapacity, float loadFactor, OrderType type) {
		super(initialCapacity, loadFactor, type);
	}

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param type   either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *               use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, OrderType type) {
		super(type);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity keys before growing the backing table.
	 * This uses the specified filter and editor.
	 *
	 * @param filter          a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor          a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param type            either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                        use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, int initialCapacity, OrderType type) {
		super(initialCapacity, type);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This set will hold initialCapacity keys before
	 * growing the backing table.
	 * This uses the specified filter and editor.
	 *
	 * @param filter          a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor          a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 * @param type            either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *                        use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, int initialCapacity, float loadFactor, OrderType type) {
		super(initialCapacity, loadFactor, type);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map  another FilteredIterableMap to copy
	 * @param type either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *             use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(FilteredIterableOrderedMap<K, ? extends I, ? extends V> map, OrderType type) {
		super(map.size(), map.loadFactor, type);
		filter = map.filter;
		editor = map.editor;
		this.hashMultiplier = map.hashMultiplier;
		putAll(map);
	}

	/**
	 * Creates a new map with the given filter and editor, and attempts to insert every entry from the given {@code map}
	 * into the new data structure. Not all keys from {@code map} might be entered if the filter and editor consider
	 * some as equal.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param map    a Map to copy
	 * @param type   either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *               use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, Map<? extends I, ? extends V> map, OrderType type) {
		this(filter, editor, map.size(), type);
		putAll(map);
	}

	/**
	 * Given two side-by-side collections, one of keys, one of values, this constructs a map and inserts each pair of key and value into it.
	 * If keys and values have different lengths, this only uses the length of the smaller collection.
	 * This uses the specified filter and editor, including while it enters the keys and values.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 * @param type   either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *               use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, Collection<? extends I> keys, Collection<? extends V> values, OrderType type) {
		this(filter, editor, keys.size(), type);
		putAll(keys, values);
	}

	/**
	 * Creates a new map using all the keys from the given {@code keys} and {@code values}.
	 * This uses the specified filter and editor, including while it enters the keys and values.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param keys   an array to draw keys from
	 * @param values an array to draw values from
	 * @param type   either {@link OrderType#BAG} to use unreliable ordering with faster deletion, or anything else to
	 *               use a list type that takes longer to delete but maintains insertion order reliably
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, I[] keys, V[] values, OrderType type) {
		this(filter, editor, Math.min(keys.length, values.length), type);
		putAll(keys, values);
	}

	// default order type

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This considers all sub-keys in an Iterable key and does not edit any sub-keys.
	 */
	public FilteredIterableOrderedMap() {
		super();
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity keys before growing the backing table.
	 * This considers all sub-keys in an Iterable key and does not edit any sub-keys.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredIterableOrderedMap(int initialCapacity) {
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
	public FilteredIterableOrderedMap(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new map with an initial capacity of {@link Utilities#getDefaultTableCapacity()} and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor) {
		super();
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity keys before growing the backing table.
	 * This uses the specified filter and editor.
	 *
	 * @param filter          a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor          a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, int initialCapacity) {
		super(initialCapacity);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map with the specified initial capacity and load factor. This set will hold initialCapacity keys before
	 * growing the backing table.
	 * This uses the specified filter and editor.
	 *
	 * @param filter          a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor          a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new map identical to the specified map.
	 *
	 * @param map another FilteredIterableMap to copy
	 */
	public FilteredIterableOrderedMap(FilteredIterableOrderedMap<K, ? extends I, ? extends V> map) {
		super(map.size(), map.loadFactor, map.getOrderType());
		filter = map.filter;
		editor = map.editor;
		this.hashMultiplier = map.hashMultiplier;
		putAll(map);
	}

	/**
	 * Creates a new map with the given filter and editor, and attempts to insert every entry from the given {@code map}
	 * into the new data structure. Not all keys from {@code map} might be entered if the filter and editor consider
	 * some as equal.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param map    a Map to copy
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, Map<? extends I, ? extends V> map) {
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
	 * @param keys   a Collection of keys
	 * @param values a Collection of values
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, Collection<? extends I> keys, Collection<? extends V> values) {
		this(filter, editor, keys.size());
		putAll(keys, values);
	}

	/**
	 * Creates a new map using all the keys from the given {@code keys} and {@code values}.
	 * This uses the specified filter and editor, including while it enters the keys and values.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param keys   an array to draw keys from
	 * @param values an array to draw values from
	 */
	public FilteredIterableOrderedMap(ObjPredicate<K> filter, ObjToSameFunction<K> editor, I[] keys, V[] values) {
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
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @return this, for chaining
	 */
	public FilteredIterableOrderedMap<K, I, V> setFilter(ObjPredicate<K> filter) {
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
	 *
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @return this, for chaining
	 */
	public FilteredIterableOrderedMap<K, I, V> setEditor(ObjToSameFunction<K> editor) {
		clear();
		this.editor = editor;
		return this;
	}

	/**
	 * Equivalent to calling {@code myMap.setFilter(filter).setEditor(editor)}, but only clears the data structure once.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @return this, for chaining
	 * @see #setFilter(ObjPredicate)
	 * @see #setEditor(ObjToSameFunction)
	 */
	public FilteredIterableOrderedMap<K, I, V> setModifiers(ObjPredicate<K> filter, ObjToSameFunction<K> editor) {
		clear();
		this.filter = filter;
		this.editor = editor;
		return this;
	}

	protected int hashHelper(I s) {
		int hash = hashMultiplier;
		for (K c : s) {
			if (filter.test(c)) {
				hash = BitConversion.imul(hash ^ editor.apply(c).hashCode(), hashMultiplier);
			}
		}
		return hash ^ (hash << 23 | hash >>> 9) ^ (hash << 11 | hash >>> 21);
	}

	@Override
	protected int place(@NotNull Object item) {
		if (item instanceof Iterable) {
			return hashHelper((I) item) & mask;
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
	public boolean equate(Object left, @Nullable Object right) {
		if (left == right)
			return true;
		if (right == null) return false;
		if ((left instanceof Iterable) && (right instanceof Iterable)) {
			Iterable l = (Iterable) left, r = (Iterable) right;
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
					if (found) countL++;
				}
				if (!j.hasNext()) {
					cr = null;
				} else {
					boolean found = false;
					while (j.hasNext() && !(found = filter.test(cr = j.next()))) {
						cr = null;
					}
					if (found) countR++;
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
	public int hashCode() {
		int h = size;
		Object[] keyTable = this.keyTable;
		V[] valueTable = this.valueTable;
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Object key = keyTable[i];
			if (key != null) {
				h ^= hashHelper((I) key);
				V value = valueTable[i];
				if (value != null) {
					h ^= value.hashCode();
				}
			}
		}
		return h;
	}

	/**
	 * The same as {@link #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)}, except this takes no
	 * keys or values, and doesn't allocate an array from using varargs.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param <K>    the type of sub-keys inside each Iterable key
	 * @param <I>    the type of keys, which must extend Iterable; inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 * @return a new FilteredIterableOrderedMap containing only the given key and value
	 * @see #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)
	 */
	public static <K, I extends Iterable<K>, V> FilteredIterableOrderedMap<K, I, V> with(ObjPredicate<K> filter, ObjToSameFunction<K> editor) {
		return new FilteredIterableOrderedMap<>(filter, editor, 0);
	}

	/**
	 * The same as {@link #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)}, except this only takes one
	 * key-value pair, and doesn't allocate an array from using varargs.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param key    the only key that will be present in the returned map
	 * @param value  the only value that will be present in the returned map
	 * @param <K>    the type of sub-keys inside each Iterable key
	 * @param <I>    the type of keys, which must extend Iterable; inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 * @return a new FilteredIterableOrderedMap containing only the given key and value
	 * @see #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)
	 */
	public static <K, I extends Iterable<K>, V> FilteredIterableOrderedMap<K, I, V> with(ObjPredicate<K> filter, ObjToSameFunction<K> editor, I key, V value) {
		FilteredIterableOrderedMap<K, I, V> map = new FilteredIterableOrderedMap<>(filter, editor, 1);
		map.put(key, value);
		return map;
	}

	/**
	 * The same as {@link #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)}, except this only takes
	 * the given key-value pairs, and doesn't allocate an array from using varargs.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param key0   a key that will be present in the returned map
	 * @param value0 a value that will be present in the returned map
	 * @param key1   a key that will be present in the returned map
	 * @param value1 a value that will be present in the returned map
	 * @param <K>    the type of sub-keys inside each Iterable key
	 * @param <I>    the type of keys, which must extend Iterable; inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 * @return a new FilteredIterableOrderedMap containing only the given keys and values
	 * @see #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)
	 */
	public static <K, I extends Iterable<K>, V> FilteredIterableOrderedMap<K, I, V> with(ObjPredicate<K> filter, ObjToSameFunction<K> editor, I key0, V value0, I key1, V value1) {
		FilteredIterableOrderedMap<K, I, V> map = new FilteredIterableOrderedMap<>(filter, editor, 2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * The same as {@link #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)}, except this only takes
	 * the given key-value pairs, and doesn't allocate an array from using varargs.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param key0   a key that will be present in the returned map
	 * @param value0 a value that will be present in the returned map
	 * @param key1   a key that will be present in the returned map
	 * @param value1 a value that will be present in the returned map
	 * @param key2   a key that will be present in the returned map
	 * @param value2 a value that will be present in the returned map
	 * @param <K>    the type of sub-keys inside each Iterable key
	 * @param <I>    the type of keys, which must extend Iterable; inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 * @return a new FilteredIterableOrderedMap containing only the given keys and values
	 * @see #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)
	 */
	public static <K, I extends Iterable<K>, V> FilteredIterableOrderedMap<K, I, V> with(ObjPredicate<K> filter, ObjToSameFunction<K> editor, I key0, V value0, I key1, V value1, I key2, V value2) {
		FilteredIterableOrderedMap<K, I, V> map = new FilteredIterableOrderedMap<>(filter, editor, 3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * The same as {@link #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)}, except this only takes
	 * the given key-value pairs, and doesn't allocate an array from using varargs.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param key0   a key that will be present in the returned map
	 * @param value0 a value that will be present in the returned map
	 * @param key1   a key that will be present in the returned map
	 * @param value1 a value that will be present in the returned map
	 * @param key2   a key that will be present in the returned map
	 * @param value2 a value that will be present in the returned map
	 * @param key3   a key that will be present in the returned map
	 * @param value3 a value that will be present in the returned map
	 * @param <K>    the type of sub-keys inside each Iterable key
	 * @param <I>    the type of keys, which must extend Iterable; inferred from key0
	 * @param <V>    the type of values, inferred from value0
	 * @return a new FilteredIterableOrderedMap containing only the given keys and values
	 * @see #with(ObjPredicate, ObjToSameFunction, Iterable, Object, Object...)
	 */
	public static <K, I extends Iterable<K>, V> FilteredIterableOrderedMap<K, I, V> with(ObjPredicate<K> filter, ObjToSameFunction<K> editor, I key0, V value0, I key1, V value1, I key2, V value2, I key3, V value3) {
		FilteredIterableOrderedMap<K, I, V> map = new FilteredIterableOrderedMap<>(filter, editor, 4);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Constructs a map given a filter, an editor, and then alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #FilteredIterableOrderedMap(ObjPredicate, ObjToSameFunction, Iterable[], Object[])},
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
	public static <K, I extends Iterable<K>, V> FilteredIterableOrderedMap<K, I, V> with(ObjPredicate<K> filter, ObjToSameFunction<K> editor, I key0, V value0, Object... rest) {
		FilteredIterableOrderedMap<K, I, V> map = new FilteredIterableOrderedMap<>(filter, editor, 1 + (rest.length >>> 1));
		map.put(key0, value0);
		map.putPairs(rest);
		return map;
	}

	/**
	 * Creates a new map by parsing all of {@code str} with the given PartialParser for keys and
	 * for values, with entries separated by {@code entrySeparator}, such as {@code ", "} and
	 * the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns a {@code I} Iterable from a section of {@code str}
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 */
	public static <K, I extends Iterable<K>, V> FilteredIterableOrderedMap<K, I, V> parse(ObjPredicate<K> filter,
																				   ObjToSameFunction<K> editor,
																				   String str,
																				   String entrySeparator,
																				   String keyValueSeparator,
																				   PartialParser<I> keyParser,
																				   PartialParser<V> valueParser) {
		return parse(filter, editor, str, entrySeparator, keyValueSeparator, keyParser, valueParser, false);
	}
	/**
	 * Creates a new map by parsing all of {@code str} (or if {@code brackets} is true, all but the first and last
	 * chars) with the given PartialParser for keys and for values, with entries separated by {@code entrySeparator},
	 * such as {@code ", "} and the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns a {@code I} Iterable from a section of {@code str}
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param brackets          if true, the first and last chars in {@code str} will be ignored
	 */
	public static <K, I extends Iterable<K>, V> FilteredIterableOrderedMap<K, I, V> parse(ObjPredicate<K> filter,
																				   ObjToSameFunction<K> editor,
																				   String str,
																				   String entrySeparator,
																				   String keyValueSeparator,
																				   PartialParser<I> keyParser,
																				   PartialParser<V> valueParser,
																				   boolean brackets) {
		FilteredIterableOrderedMap<K, I, V> m = new FilteredIterableOrderedMap<>(filter, editor);
		if(brackets)
			m.putLegible(str, entrySeparator, keyValueSeparator, keyParser, valueParser, 1, str.length() - 1);
		else
			m.putLegible(str, entrySeparator, keyValueSeparator, keyParser, valueParser, 0, -1);
		return m;
	}

	/**
	 * Creates a new map by parsing the given subrange of {@code str} with the given PartialParser for keys and for
	 * values, with entries separated by {@code entrySeparator}, such as {@code ", "} and the keys separated from values
	 * by {@code keyValueSeparator}, such as {@code "="}.
	 * <br>
	 * Various {@link PartialParser} instances are defined as constants, such as
	 * {@link PartialParser#DEFAULT_STRING}, and others can be created by static methods in PartialParser, such as
	 * {@link PartialParser#objectListParser(PartialParser, String, boolean)}.
	 *
	 * @param filter a ObjPredicate<K> that should return true iff a sub-key should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<K> that will be given a sub-key and may return a potentially different {@code K} sub-key
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param keyParser         a PartialParser that returns a {@code I} Iterable from a section of {@code str}
	 * @param valueParser       a PartialParser that returns a {@code V} value from a section of {@code str}
	 * @param offset            the first position to read parseable text from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public static <K, I extends Iterable<K>, V> FilteredIterableOrderedMap<K, I, V> parse(ObjPredicate<K> filter,
																				   ObjToSameFunction<K> editor,
																				   String str,
																				   String entrySeparator,
																				   String keyValueSeparator,
																				   PartialParser<I> keyParser,
																				   PartialParser<V> valueParser,
																				   int offset,
																				   int length) {
		FilteredIterableOrderedMap<K, I, V> m = new FilteredIterableOrderedMap<>(filter, editor);
		m.putLegible(str, entrySeparator, keyValueSeparator, keyParser, valueParser, offset, length);
		return m;
	}
}
