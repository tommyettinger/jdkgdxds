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

import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.function.ObjPredicate;
import com.github.tommyettinger.function.ObjToSameFunction;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * A customizable variant on ObjectSet that uses Iterable items made of T sub-items, and only considers a sub-item (for
 * equality and hashing purposes) if that sub-item satisfies a predicate. This can also edit the sub-items that pass
 * the filter, such as by normalizing their data during comparisons (and hashing). You will usually want to call
 * {@link #setFilter(ObjPredicate)} and/or {@link #setEditor(ObjToSameFunction)} to change the behavior of hashing and
 * equality before you enter any items, unless you have specified the filter and/or editor you want in the constructor.
 * Calling {@link #setModifiers(ObjPredicate, ObjToSameFunction)} is recommended if you need to set both the filter and
 * the editor; you could also set them in the constructor.
 * <br>
 * This class is related to {@link FilteredStringSet}, which can be seen as using a String as an item and the characters
 * of that String as its sub-items. That means this is also similar to {@link CaseInsensitiveSet}, which is essentially
 * a specialized version of FilteredIterableSet (which can be useful for serialization).
 */
public class FilteredIterableSet<T, I extends Iterable<T>> extends ObjectSet<I> {
	protected ObjPredicate<T>      filter = c -> true;
	protected ObjToSameFunction<T> editor = c -> c;

	/**
	 * Used by {@link #place(Object)} to mix hashCode() results. Changes on every call to {@link #resize(int)} by default.
	 * This only needs to be serialized if the full key and value tables are serialized, or if the iteration order should be
	 * the same before and after serialization. Iteration order is better handled by using {@link ObjectOrderedSet}.
	 */
	protected int hashMultiplier = 0xEFAA28F1;

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This considers all sub-items in an Iterable item and does not edit any sub-items.
	 */
	public FilteredIterableSet () {
		super();
	}

	/**
	 * Creates a new set with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity items before growing the backing table.
	 * This considers all sub-items in an Iterable item and does not edit any sub-items.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredIterableSet (int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 * This considers all sub-items in an Iterable item and does not edit any sub-items.
	 *
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredIterableSet (int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates a new set with an initial capacity of 51 and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a ObjPredicate<T> that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<T> that will be given a sub-item and may return a potentially different {@code T} sub-item
	 */
	public FilteredIterableSet (ObjPredicate<T> filter, ObjToSameFunction<T> editor) {
		super();
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new set with the specified initial capacity and a load factor of {@link Utilities#getDefaultLoadFactor()}.
	 * This set will hold initialCapacity items before growing the backing table.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a ObjPredicate<T> that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<T> that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 */
	public FilteredIterableSet (ObjPredicate<T> filter, ObjToSameFunction<T> editor, int initialCapacity) {
		super(initialCapacity);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new set with the specified initial capacity and load factor. This set will hold initialCapacity items before
	 * growing the backing table.
	 * This uses the specified filter and editor.
	 *
	 * @param filter a ObjPredicate<T> that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<T> that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param initialCapacity If not a power of two, it is increased to the next nearest power of two.
	 * @param loadFactor      what fraction of the capacity can be filled before this has to resize; 0 &lt; loadFactor &lt;= 1
	 */
	public FilteredIterableSet (ObjPredicate<T> filter, ObjToSameFunction<T> editor, int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
		this.filter = filter;
		this.editor = editor;
	}

	/**
	 * Creates a new set identical to the specified set.
	 *
	 * @param set another FilteredIterableSet to copy
	 */
	public FilteredIterableSet (FilteredIterableSet<T, ? extends I> set) {
		super(set.size());
		filter = set.filter;
		editor = set.editor;
		this.hashMultiplier = set.hashMultiplier;
		addAll(set);
	}

	/**
	 * Creates a new set that contains all distinct elements in {@code coll}.
	 * This uses the specified filter and editor, including while it enters the items in coll.
	 *
	 * @param filter a ObjPredicate<T> that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<T> that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param coll a Collection implementation to copy, such as an ObjectList or a Set that isn't a FilteredIterableSet
	 */
	public FilteredIterableSet (ObjPredicate<T> filter, ObjToSameFunction<T> editor, Collection<? extends I> coll) {
		this(filter, editor, coll.size());
		addAll(coll);
	}

	/**
	 * Creates a new set using {@code length} items from the given {@code array}, starting at {@code} offset (inclusive).
	 * This uses the specified filter and editor, including while it enters the items in array.
	 *
	 * @param filter a ObjPredicate<T> that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<T> that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param array  an array to draw items from
	 * @param offset the first index in array to draw an item from
	 * @param length how many items to take from array; bounds-checking is the responsibility of the using code
	 */
	public FilteredIterableSet (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I[] array, int offset, int length) {
		this(filter, editor, length);
		addAll(array, offset, length);
	}

	/**
	 * Creates a new set containing all the items in the given array.
	 * This uses the specified filter and editor, including while it enters the items in array.
	 *
	 * @param filter a ObjPredicate<T> that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<T> that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param array an array that will be used in full, except for duplicate items
	 */
	public FilteredIterableSet (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I[] array) {
		this(filter, editor, array, 0, array.length);
	}

	public ObjPredicate<T> getFilter() {
		return filter;
	}

	/**
	 * Sets the filter that determines which sub-items in an Iterable are considered for equality and hashing, then
	 * returns this object, for chaining. ObjPredicate<T> filters could be lambdas or method references that take a
	 * sub-item and return true if that sub-item will be used for hashing/equality, or return false to ignore it.
	 * The default filter always returns true. If the filter changes, that invalidates anything previously entered into
	 * this, so before changing the filter <em>this clears the entire data structure</em>, removing all existing items.
	 * @param filter a ObjPredicate<T> that should return true iff a sub-item should be considered for equality/hashing
	 * @return this, for chaining
	 */
	public FilteredIterableSet<T, I> setFilter(ObjPredicate<T> filter) {
		clear();
		this.filter = filter;
		return this;
	}

	public ObjToSameFunction<T> getEditor() {
		return editor;
	}

	/**
	 * Sets the editor that can alter the sub-items in an Iterable item when they are being used for equality and
	 * hashing. This does not apply any changes to the items in this data structure; it only affects how they are
	 * hashed or compared. An editor could be a lambda or method reference; the only real requirement is that it
	 * takes a {@code T} sub-item and returns a {@code T} sub-item.
	 * The default filter returns the sub-item it is passed without changes. If the editor changes, that invalidates
	 * anything previously entered into this, so before changing the editor <em>this clears the entire data
	 * structure</em>, removing all existing items.
	 * @param editor a ObjToSameFunction<T> that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @return this, for chaining
	 */
	public FilteredIterableSet<T, I> setEditor(ObjToSameFunction<T> editor) {
		clear();
		this.editor = editor;
		return this;
	}

	/**
	 * Equivalent to calling {@code mySet.setFilter(filter).setEditor(editor)}, but only clears the data structure once.
	 * @see #setFilter(ObjPredicate)
	 * @see #setEditor(ObjToSameFunction)
	 * @param filter a ObjPredicate<T> that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a ObjToSameFunction<T> that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @return this, for chaining
	 */
	public FilteredIterableSet<T, I> setModifiers(ObjPredicate<T> filter, ObjToSameFunction<T> editor) {
		clear();
		this.filter = filter;
		this.editor = editor;
		return this;
	}

	protected int hashHelper(I s) {
		int hash = hashMultiplier;
		for (T c : s) {
			if(filter.test(c)){
				hash = BitConversion.imul(hash ^ editor.apply(c).hashCode(), hashMultiplier);
			}
		}
		return hash ^ (hash << 23 | hash >>> 9) ^ (hash << 11 | hash >>> 21);
	}

	@Override
	protected int place (@NonNull Object item) {
		if (item instanceof Iterable) {
			return hashHelper((I) item) & mask;
		}
		return super.place(item);
	}

	/**
	 * This actually does something here because the hash multiplier can change.
	 *
	 * @return this class' current hash multiplier
	 */
	@Override
	public int getHashMultiplier() {
		return hashMultiplier;
	}

	/**
	 * This actually does something here because the hash multiplier can change.
	 * The {@code mul} will be made negative and odd if it wasn't both already.
	 *
	 * @param mul any int; will be made negative and odd before using
	 */
	@Override
	public void setHashMultiplier(int mul) {
		hashMultiplier = mul | 0x80000001;
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
			Iterator<? extends T> i = l.iterator(), j = r.iterator();
			T cl = null, cr = null;
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
		for (int i = 0, n = keyTable.length; i < n; i++) {
			Object key = keyTable[i];
			if (key != null) {h += hashHelper((I)key);}
		}
		return h;
	}

	protected void resize (int newSize) {
		int oldCapacity = keyTable.length;
		threshold = (int)(newSize * loadFactor);
		mask = newSize - 1;
		shift = BitConversion.countLeadingZeros(mask) + 32;

		hashMultiplier = Utilities.GOOD_MULTIPLIERS[BitConversion.imul(hashMultiplier, shift) >>> 5 & 511];
		@Nullable I[] oldKeyTable = keyTable;

		keyTable = (I[])new Object[newSize];

		if (size > 0) {
			for (int i = 0; i < oldCapacity; i++) {
				I key = oldKeyTable[i];
				if (key != null) {addResize(key);}
			}
		}
	}

	/**
	 * Constructs a new FilteredIterableSet with the given filter and editor, without contents, and returns the set.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @return a new FilteredIterableSet containing nothing
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor) {
		FilteredIterableSet<T, I> set = new FilteredIterableSet<>(filter, editor);
		return set;
	}

	/**
	 * Constructs a new FilteredIterableSet with the given filter and editor, inserts {@code item} into it, and returns the set.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param item   the one item to initially include in the set
	 * @return a new FilteredIterableSet containing {@code item}
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I item) {
		FilteredIterableSet<T, I> set = new FilteredIterableSet<>(filter, editor, 1);
		set.add(item);
		return set;
	}

	/**
	 * Constructs a new FilteredIterableSet with the given filter and editor, inserts items into it, and returns the set.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param item0  an Iterable of T to initially include in the set
	 * @param item1  an Iterable of T to initially include in the set
	 * @return a new FilteredIterableSet containing the given items
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I item0, I item1) {
		FilteredIterableSet<T, I> set = new FilteredIterableSet<>(filter, editor, 2);
		set.add(item0, item1);
		return set;
	}

	/**
	 * Constructs a new FilteredIterableSet with the given filter and editor, inserts items into it, and returns the set.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param item0  an Iterable of T to initially include in the set
	 * @param item1  an Iterable of T to initially include in the set
	 * @param item2  an Iterable of T to initially include in the set
	 * @return a new FilteredIterableSet containing the given items
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I item0, I item1, I item2) {
		FilteredIterableSet<T, I> set = new FilteredIterableSet<>(filter, editor, 3);
		set.add(item0, item1, item2);
		return set;
	}

	/**
	 * Constructs a new FilteredIterableSet with the given filter and editor, inserts items into it, and returns the set.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param item0  an Iterable of T to initially include in the set
	 * @param item1  an Iterable of T to initially include in the set
	 * @param item2  an Iterable of T to initially include in the set
	 * @param item3  an Iterable of T to initially include in the set
	 * @return a new FilteredIterableSet containing the given items
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I item0, I item1, I item2, I item3) {
		FilteredIterableSet<T, I> set = new FilteredIterableSet<>(filter, editor, 4);
		set.add(item0, item1, item2, item3);
		return set;
	}

	/**
	 * Constructs a new FilteredIterableSet with the given filter and editor, inserts items into it, and returns the set.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param item0  an Iterable of T to initially include in the set
	 * @param item1  an Iterable of T to initially include in the set
	 * @param item2  an Iterable of T to initially include in the set
	 * @param item3  an Iterable of T to initially include in the set
	 * @param item4  an Iterable of T to initially include in the set
	 * @return a new FilteredIterableSet containing the given items
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I item0, I item1, I item2, I item3, I item4) {
		FilteredIterableSet<T, I> set = new FilteredIterableSet<>(filter, editor, 5);
		set.add(item0, item1, item2, item3);
		set.add(item4);
		return set;
	}

	/**
	 * Constructs a new FilteredIterableSet with the given filter and editor, inserts items into it, and returns the set.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param item0  an Iterable of T to initially include in the set
	 * @param item1  an Iterable of T to initially include in the set
	 * @param item2  an Iterable of T to initially include in the set
	 * @param item3  an Iterable of T to initially include in the set
	 * @param item4  an Iterable of T to initially include in the set
	 * @param item5  an Iterable of T to initially include in the set
	 * @return a new FilteredIterableSet containing the given items
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I item0, I item1, I item2, I item3, I item4, I item5) {
		FilteredIterableSet<T, I> set = new FilteredIterableSet<>(filter, editor, 6);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5);
		return set;
	}

	/**
	 * Constructs a new FilteredIterableSet with the given filter and editor, inserts items into it, and returns the set.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param item0  an Iterable of T to initially include in the set
	 * @param item1  an Iterable of T to initially include in the set
	 * @param item2  an Iterable of T to initially include in the set
	 * @param item3  an Iterable of T to initially include in the set
	 * @param item4  an Iterable of T to initially include in the set
	 * @param item5  an Iterable of T to initially include in the set
	 * @param item6  an Iterable of T to initially include in the set
	 * @return a new FilteredIterableSet containing the given items
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I item0, I item1, I item2, I item3, I item4, I item5, I item6) {
		FilteredIterableSet<T, I> set = new FilteredIterableSet<>(filter, editor, 7);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6);
		return set;
	}

	/**
	 * Constructs a new FilteredIterableSet with the given filter and editor, inserts items into it, and returns the set.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param item0  an Iterable of T to initially include in the set
	 * @param item1  an Iterable of T to initially include in the set
	 * @param item2  an Iterable of T to initially include in the set
	 * @param item3  an Iterable of T to initially include in the set
	 * @param item4  an Iterable of T to initially include in the set
	 * @param item5  an Iterable of T to initially include in the set
	 * @param item6  an Iterable of T to initially include in the set
	 * @param item7  an Iterable of T to initially include in the set
	 * @return a new FilteredIterableSet containing the given items
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I item0, I item1, I item2, I item3, I item4, I item5, I item6, I item7) {
		FilteredIterableSet<T, I> set = new FilteredIterableSet<>(filter, editor, 8);
		set.add(item0, item1, item2, item3);
		set.add(item4, item5, item6, item7);
		return set;
	}

	/**
	 * This is the same as {@link #FilteredIterableSet(ObjPredicate, ObjToSameFunction, Iterable[])}, but
	 * can take the array argument as either an array or as varargs. It can be useful for code-generation scenarios.
	 *
	 * @param filter a {@code ObjPredicate<T>} that should return true iff a sub-item should be considered for equality/hashing
	 * @param editor a {@code ObjToSameFunction<T>} that will be given a sub-item and may return a potentially different {@code T} sub-item
	 * @param items  an array or varargs of {@code I} that will be used in the new set
	 * @return a new FilteredIterableSet containing the entirety of items, as the filter and editor permit
	 * @param <T> the type of sub-items
	 * @param <I> the type of items, which must be either Iterable or an implementing class, containing {@code T} sub-items
	 */
	@SafeVarargs
	public static <T, I extends Iterable<T>> FilteredIterableSet<T, I> with (ObjPredicate<T> filter, ObjToSameFunction<T> editor, I... items) {
        return new FilteredIterableSet<>(filter, editor, items);
	}
}
