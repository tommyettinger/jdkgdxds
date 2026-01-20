/*
 * Copyright (c) 2026 See AUTHORS file.
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

package com.github.tommyettinger.ds.hood;

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.ds.EnhancedCollection;
import com.github.tommyettinger.ds.PrimitiveCollection;
import com.github.tommyettinger.ds.PrimitiveSet;
import com.github.tommyettinger.ds.Utilities;
import com.github.tommyettinger.ds.support.util.IntAppender;
import com.github.tommyettinger.ds.support.util.IntIterator;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;

/**
 * An initial iteration of <a href="https://www.corsix.org/content/my-favourite-small-hash-table">this hash table</a>.
 * <br>
 * Constraints at this stage:
 * <ol>
 *     <li>Keys are randomly-distributed 32-bit integers.</li>
 *     <li>Values are also 32-bit integers.</li>
 *     <li>If the key 0 is present, it is stored outside the normal part of the hash table.</li>
 * </ol>
 * Keys are not mixed.
 */
public class Table0 implements Iterable<Table0.Entry> {
	protected long[] slots;
	protected int mask;
	protected int count;
	protected int defaultValue = 0;

	public Table0() {
		this(48);
	}

	public Table0(int capacity){
		mask = Utilities.tableSize(Math.max(12, capacity), 0.75f) - 1;
		slots = new long[mask+2];
		count = 0;
	}

	public Table0(int[] keys, int[] values){
		int capacity = Math.min(keys.length, values.length);
		mask = Utilities.tableSize(Math.max(12, capacity), 0.75f) - 1;
		slots = new long[mask+2];
		count = 0;
		putAll(keys, values, 0, capacity);
	}

	public Table0(int[] keys, int[] values, int offset, int length){
		int capacity = Math.min(Math.min(keys.length, values.length) - offset, length);
		mask = Utilities.tableSize(Math.max(12, capacity), 0.75f) - 1;
		slots = new long[mask+2];
		count = 0;
		putAll(keys, values, offset, length);
	}

	public boolean containsKey(int key) {
		if(key == 0){
			return (int) slots[mask + 1] == -1;
		}
		for (int d = 0; ; ++d) {
			int idx = (key + d) & mask;
			long slot = slots[idx];
			int h = (int) slot;
			if (slot == 0) {
				return false;
			} else if (key == h) {
				return true;
			} else if (((idx - h) & mask) < d) {
				return false;
			}
		}
	}

	public int get(int key) {
		if(key == 0){
			long slot = slots[mask+1];
			if((int)slot == -1)
				return (int)(slot>>>32);
			return defaultValue;
		}
		for (int d = 0; ; ++d) {
			int idx = (key + d) & mask;
			long slot = slots[idx];
			int h = (int) slot;
			if (slot == 0) {
				return defaultValue;
			} else if (key == h) {
				return (int)(slot >>> 32);
			} else if (((idx - h) & mask) < d) {
				return defaultValue;
			}
		}
	}

	public Entry getEntry(int key) {
		if(key == 0){
			long slot = slots[mask+1];
			if((int)slot == -1)
				return new Entry(0, (int)(slot>>>32));
			return null;
		}
		for (int d = 0; ; ++d) {
			int idx = (key + d) & mask;
			long slot = slots[idx];
			int h = (int) slot;
			if (slot == 0) {
				return null;
			} else if (key == h) {
				return new Entry(key, (int)(slot >>> 32));
			} else if (((idx - h) & mask) < d) {
				return null;
			}
		}
	}

	public long getSlot(int key) {
		if(key == 0){
			long slot = slots[mask+1];
			if((int)slot == -1)
				return slot;
			return 0L;
		}
		for (int d = 0; ; ++d) {
			int idx = (key + d) & mask;
			long slot = slots[idx];
			int h = (int) slot;
			if (slot == 0) {
				return 0L;
			} else if (key == h) {
				return slot;
			} else if (((idx - h) & mask) < d) {
				return 0L;
			}
		}
	}

	public int getOrDefault(int key, int defaultValue) {
		if(key == 0){
			long slot = slots[mask+1];
			if((int)slot == -1)
				return (int)(slot>>>32);
			return defaultValue;
		}
		for (int d = 0; ; ++d) {
			int idx = (key + d) & mask;
			long slot = slots[idx];
			int h = (int) slot;
			if (slot == 0) {
				return defaultValue;
			} else if (key == h) {
				return (int)(slot >>> 32);
			} else if (((idx - h) & mask) < d) {
				return defaultValue;
			}
		}
	}

	public int put(int key, int value) {
		if(key == 0) {
			if((int)slots[mask+1] == -1) {
				int old = (int) (slots[mask + 1] >>> 32);
				slots[mask + 1] = (long) value << 32 | 0xFFFFFFFFL;
				return old;
			}
			slots[mask + 1] = (long) value << 32 | 0xFFFFFFFFL;
			++count;
			return defaultValue;
		}
		long kv = (key & 0xFFFFFFFFL) | (long) value << 32;
		for (int d = 0; ; d++) {
			int idx = key + d & mask;
			long slot = slots[idx];
			int low = (int)slot;
			if(low == 0){
				// Insert new value (slot was previously empty)
				slots[idx] = kv;
				break;
			} else if(key == low) {
				// Overwrite existing value
				slots[idx] = kv;
				return (int)(slot >>> 32);
			} else {
				int d2 = idx - low & mask;
				if(d2 < d) {
					// Insert new value and move existing slot
					slots[idx] = kv;
					putResize(slots, slot, d2);
					break;
				}
			}
		}
		if(++count >= mask * 0.75){
			resize(mask+2);
		}
		return defaultValue;
	}

	public boolean putAll(int[] keys, int[] values, int offset, int length) {
		if (keys == null || values == null) return false;
		int oldCount = count;
		int len = Math.min(Math.min(keys.length, values.length) - offset, length);
		if(count + len >= mask * 0.75){
			resize(count + len + 1);
		}
		PER_ITEM:
		for (int i = offset, target = offset + len; i < target; i++) {
			int key = keys[i], value = values[i];
			if (key == 0) {
				if ((int) slots[mask + 1] != -1) ++count;
				slots[mask + 1] = (long) value << 32 | 0xFFFFFFFFL;
				continue;
			}
			long kv = (key & 0xFFFFFFFFL) | (long) value << 32;
			for (int d = 0; ; d++) {
				int idx = key + d & mask;
				long slot = slots[idx];
				int low = (int) slot;
				if (low == 0) {
					// Insert new value (slot was previously empty)
					slots[idx] = kv;
					break;
				} else if (key == low) {
					// Overwrite existing value
					slots[idx] = kv;
					continue PER_ITEM;
				} else {
					int d2 = idx - low & mask;
					if (d2 < d) {
						// Insert new value and move existing slot
						slots[idx] = kv;
						putResize(slots, slot, d2);
						break;
					}
				}
			}
			++count;
		}
		return oldCount != count;
	}

	public void resize(int capacity) {
		capacity = Utilities.tableSize(capacity, 0.75f);
		if(capacity - 1 <= mask) return;
		int oldMask = mask;
		mask = capacity - 1;
		long[] newSlots = new long[capacity+1];
		long[] oldSlots = slots;
		newSlots[capacity] = oldSlots[oldMask+1];
		int idx = 0;
		do {
			long slot = oldSlots[idx];
			if((int)slot != 0){
				putResize(newSlots, slot, 0);
			}
		} while (idx++ != oldMask);
		slots = newSlots;
	}

	private void putResize(long[] slots, long kv, int d) {
		for (; ; ++d) {
			int h = (int) kv;
			int idx = h + d & mask;
			long slot = slots[idx];
			int low = (int) slot;
			if (low == 0) {
				slots[idx] = kv;
				return;
			} else {
				int d2 = idx - low & mask;
				if(d2 < d){
					slots[idx] = kv;
					kv = slot;
					d = d2;
				}
			}
		}
	}

	public int remove(int key) {
		if(key == 0){
			if((int)slots[mask+1] == -1) {
				int old = (int) (slots[mask + 1] >>> 32);
				slots[mask + 1] = 0;
				--count;
				return old;
			}
			return defaultValue;
		}
		for (int d = 0; ; d++) {
			int idx = key + d & mask;
			long slot = slots[idx];
			int low = (int)slot;
			if(low == 0){
				return defaultValue;
			} else if(key == low){
				int next = idx + 1 & mask;
				--count;
				while ((int)slots[next] != 0 && ((slots[next] ^ next) & mask) != 0){
					slots[idx] = slots[next];
					idx = next;
					next = idx + 1 & mask;
				}
				slots[idx] = 0;
				return (int) (slot >>> 32);
			} else if((idx - low & mask) < d){
				return defaultValue;
			}
		}
	}

	public boolean delete(int key) {
		if(key == 0){
			if((int)slots[mask+1] == -1) {
				slots[mask + 1] = 0;
				--count;
				return true;
			}
			return false;
		}
		for (int d = 0; ; d++) {
			int idx = key + d & mask;
			int low = (int)slots[idx];
			if(low == 0){
				return false;
			} else if(key == low){
				int next = idx + 1 & mask;
				--count;
				while ((int)slots[next] != 0 && ((slots[next] ^ next) & mask) != 0){
					slots[idx] = slots[next];
					idx = next;
					next = idx + 1 & mask;
				}
				slots[idx] = 0;
				return true;
			} else if((idx - low & mask) < d){
				return false;
			}
		}
	}

	public boolean removeAll(int[] keys, int offset, int length) {
		if(keys == null) return false;
		int oldCount = count;
		int target = offset + Math.min(keys.length - offset, length);
		PER_ITEM:
		for (int i = offset; i < target; i++) {
			int key = keys[i];
			if (key == 0) {
				if ((int) slots[mask + 1] == -1) {
					slots[mask + 1] = 0;
					--count;
					continue;
				}
				continue;
			}
			for (int d = 0; ; d++) {
				int idx = key + d & mask;
				int low = (int) slots[idx];
				if (low == 0) {
					continue PER_ITEM;
				} else if (key == low) {
					int next = idx + 1 & mask;
					--count;
					while ((int) slots[next] != 0 && ((slots[next] ^ next) & mask) != 0) {
						slots[idx] = slots[next];
						idx = next;
						next = idx + 1 & mask;
					}
					slots[idx] = 0;
					continue PER_ITEM;
				} else if ((idx - low & mask) < d) {
					continue PER_ITEM;
				}
			}
		}
		return oldCount != count;
	}

	public boolean containsValue(int item) {
		int idx = -1;
		for (int n = slots.length; ++idx < n; ) {
			long slot = slots[idx];
			if ((int)slot != 0 && (int)(slot >>> 32) == item) {
				return true;
			}
		}
		return false;
	}

	public void clear() {
		Arrays.fill(slots, 0);
		count = 0;
	}

	public Iterator<Entry> iterator() {
		return new EntryIterator(this);
	}

	public KeySet keySet() {
		return new KeySet(this);
	}

	public Values values() {
		return new Values(this);
	}

	public EntrySet entrySet() {
		return new EntrySet(this);
	}

	public int size() {
		return count;
	}

	public int getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(int defaultValue) {
		this.defaultValue = defaultValue;
	}


	@Override
	public int hashCode() {
		int h = count;
		long[] slots = this.slots;
		for (int i = 0, n = slots.length; i < n; i++) {
			long slot = slots[i];
			int key = (int)slot;
			if (key != 0) {
				h ^= key;
				h ^= (int) (slot>>>32);
			}
		}
		return h;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof Table0)) {
			return false;
		}
		Table0 other = (Table0) obj;
		if (other.count != count) {
			return false;
		}
		long[] slots = this.slots;
		for (int i = 0, n = mask; i <= n; i++) {
			long slot = slots[i];
			int key = (int) slot;
			if (key != 0) {
				long otherValue = other.getSlot(key);
				if (otherValue != slot)
					return false;
			}
		}
		return (slots[mask+1] == other.slots[other.mask+1]);
	}

	@Override
	public String toString() {
		return toString(", ", true);
	}

	/**
	 * Delegates to {@link #toString(String, boolean)} with the given entrySeparator and without braces.
	 * This is different from {@link #toString()}, which includes braces by default.
	 *
	 * @param entrySeparator how to separate entries, such as {@code ", "}
	 * @return a new String representing this map
	 */
	public String toString(String entrySeparator) {
		return toString(entrySeparator, false);
	}

	public String toString(String entrySeparator, boolean braces) {
		return appendTo(new StringBuilder(32), entrySeparator, braces).toString();
	}

	/**
	 * Makes a String from the contents of this Table0, but uses the given {@link IntAppender} and
	 * {@link IntAppender} to convert each key and each value to a customizable representation and append them
	 * to a temporary StringBuilder. These functions are often method references to methods in Base, such as
	 * {@link Base#appendReadable(CharSequence, int)} and {@link Base#appendUnsigned(CharSequence, int)}. To use
	 * the default String representation, you can use {@link IntAppender#DEFAULT}
	 * as an appender. To write values so that they can be read back as Java source code, use
	 * {@link IntAppender#READABLE} for each appender.
	 * <br>
	 * Using {@code READABLE} appenders, if you separate keys
	 * from values with {@code ", "} and also separate entries with {@code ", "}, that allows the output to be
	 * copied into source code that calls {@link #with(Number, Number, Number...)} (if {@code braces} is false).
	 *
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and an int, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and an int, and returns the modified StringBuilder
	 * @return a new String representing this map
	 */
	public String toString(String entrySeparator, String keyValueSeparator, boolean braces,
						   IntAppender keyAppender, IntAppender valueAppender) {
		return appendTo(new StringBuilder(), entrySeparator, keyValueSeparator, braces, keyAppender, valueAppender).toString();
	}

	public StringBuilder appendTo(StringBuilder sb, String entrySeparator, boolean braces) {
		return appendTo(sb, entrySeparator, "=", braces, IntAppender.DEFAULT, IntAppender.DEFAULT);
	}

	/**
	 * Appends to a StringBuilder from the contents of this Table0, but uses the given {@link IntAppender} and
	 * {@link IntAppender} to convert each key and each value to a customizable representation and append them
	 * to a StringBuilder. These functions are often method references to methods in Base, such as
	 * {@link Base#appendReadable(CharSequence, int)} and {@link Base#appendUnsigned(CharSequence, int)}. To use
	 * the default String representation, you can use {@link IntAppender#DEFAULT}
	 * as an appender. To write values so that they can be read back as Java source code, use
	 * {@link IntAppender#READABLE} for each appender.
	 * <br>
	 * Using {@code READABLE} appenders, if you separate keys
	 * from values with {@code ", "} and also separate entries with {@code ", "}, that allows the output to be
	 * copied into source code that calls {@link #with(Number, Number, Number...)} (if {@code braces} is false).
	 *
	 * @param sb                a StringBuilder that this can append to
	 * @param entrySeparator    how to separate entries, such as {@code ", "}
	 * @param keyValueSeparator how to separate each key from its value, such as {@code "="} or {@code ":"}
	 * @param braces            true to wrap the output in curly braces, or false to omit them
	 * @param keyAppender       a function that takes a StringBuilder and an int, and returns the modified StringBuilder
	 * @param valueAppender     a function that takes a StringBuilder and an int, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended keys and values of this map
	 */
	public StringBuilder appendTo(StringBuilder sb, String entrySeparator, String keyValueSeparator, boolean braces,
								  IntAppender keyAppender, IntAppender valueAppender) {
		if (count == 0) {
			return braces ? sb.append("{}") : sb;
		}
		if (braces) {
			sb.append('{');
		}
		long[] slots = this.slots;
		long zeroSlot = slots[mask+1];
		if ((int)(zeroSlot) == -1) {
			keyAppender.apply(sb, 0).append(keyValueSeparator);
			valueAppender.apply(sb, (int) (zeroSlot>>>32));
			if (count > 1) {
				sb.append(entrySeparator);
			}
		}
		int i = mask + 1;
		while (i-- > 0) {
			long slot = slots[i];
			int key = (int) slot;
			if (key == 0) {
				continue;
			}
			keyAppender.apply(sb, key).append(keyValueSeparator);
			valueAppender.apply(sb, (int) (slot>>>32));
			break;
		}
		while (i-- > 0) {
			long slot = slots[i];
			int key = (int) slot;
			if (key == 0) {
				continue;
			}
			sb.append(entrySeparator);
			keyAppender.apply(sb, key).append(keyValueSeparator);
			valueAppender.apply(sb, (int) (slot>>>32));
		}
		if (braces) {
			sb.append('}');
		}
		return sb;
	}


	protected static class MapIterator {
		protected Table0 table;
		protected int nextIndex = -1;
		protected boolean hasNext;

		public MapIterator(Table0 table){
			this.table = table;
			if(table.count > 0)
				findNextIndex();
			else
				hasNext = false;
		}

		public boolean hasNext() {
			return hasNext;
		}

		protected void findNextIndex() {
			long[] slots = table.slots;
			for (int n = slots.length; ++nextIndex < n; ) {
				if ((int)slots[nextIndex] != 0) {
					hasNext = true;
					return;
				}
			}
			hasNext = false;
		}
	}

	public static class KeyIterator extends MapIterator implements IntIterator {
		public KeyIterator(Table0 table) {
			super(table);
		}

		@Override
		public int nextInt() {
			int i = (nextIndex > table.mask) ? 0 : (int) table.slots[nextIndex];
			findNextIndex();
			return i;
		}
	}

	public static class KeySet implements PrimitiveSet.SetOfInt {
		protected KeyIterator it;
		public KeySet(Table0 table) {
			it = new KeyIterator(table);
		}
		@Override
		public boolean add(int item) {
			throw new UnsupportedOperationException("add() is not supported on a KeySet view.");
		}

		@Override
		public boolean remove(int item) {
			return it.table.delete(item);
		}

		@Override
		public boolean contains(int item) {
			return it.table.containsKey(item);
		}

		@Override
		public IntIterator iterator() {
			return it;
		}

		@Override
		public int size() {
			return it.table.count;
		}

		@Override
		public void clear() {
			it.table.clear();
		}

		@Override
		public String toString() {
			return toString(", ", true);
		}
	}

	public static class ValueIterator extends MapIterator implements IntIterator {
		public ValueIterator(Table0 table) {
			super(table);
		}

		@Override
		public int nextInt() {
			int i = (int) (table.slots[nextIndex] >>> 32);
			findNextIndex();
			return i;
		}
	}

	public static class Values implements PrimitiveCollection.OfInt {
		protected ValueIterator it;
		public Values(Table0 table) {
			it = new ValueIterator(table);
		}
		@Override
		public boolean add(int item) {
			throw new UnsupportedOperationException("add() is not supported on a Values view.");
		}

		@Override
		public boolean remove(int item) {
			throw new UnsupportedOperationException("remove() is not supported on a Values view.");
		}

		@Override
		public boolean contains(int item) {
			return it.table.containsValue(item);
		}

		@Override
		public IntIterator iterator() {
			return it;
		}

		@Override
		public int size() {
			return it.table.count;
		}

		@Override
		public void clear() {
			throw new UnsupportedOperationException("clear() is not supported on a Values view.");
		}

		@Override
		public String toString() {
			return toString(", ", true);
		}
	}

	public static class Entry {
		public int key;
		public int value;

		public Entry() {
		}

		public Entry(int key, int value) {
			this.key = key;
			this.value = value;
		}

		public Entry(Entry entry) {
			this.key = entry.key;
			this.value = entry.value;
		}

		@Override
		public String toString() {
			return key + "=" + value;
		}

		public int getKey() {
			return key;
		}

		public int getValue() {
			return value;
		}

		public int setValue(int value) {
			int old = this.value;
			this.value = value;
			return old;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			Entry entry = (Entry) o;

			if (key != entry.key) {
				return false;
			}
			return value == entry.value;
		}

		@Override
		public int hashCode() {
			return key * 31 + value;
		}
	}

	public static class EntryIterator extends MapIterator implements Iterator<Entry> {
		public EntryIterator(Table0 table) {
			super(table);
		}

		@Override
		public Entry next() {
			long slot = table.slots[nextIndex];
			findNextIndex();
			return new Entry((int) slot, (int)(slot >>> 32));
		}
	}

	public static class EntrySet extends AbstractSet<Entry> implements EnhancedCollection<Entry> {
		EntryIterator it;

		public EntrySet(Table0 table) {
			it = new EntryIterator(table);
		}

		@Override
		public Iterator<Entry> iterator() {
			return it;
		}

		@Override
		public int size() {
			return it.table.count;
		}

		@Override
		public boolean isEmpty() {
			return it.table.count == 0;
		}

		@Override
		public boolean contains(Object o) {
			if(!(o instanceof Entry)) return false;
			Entry e = (Entry) o, mine = it.table.getEntry(e.key);
			return e.key == mine.key && e.value == mine.value;
		}

		@Override
		public boolean add(Entry entry) {
			throw new UnsupportedOperationException("add() is not supported on an EntrySet view.");
		}

		@Override
		public boolean remove(Object o) {
			if(!(o instanceof Entry)) return false;
			return it.table.delete(((Entry) o).key);
		}

		@Override
		public void clear() {
			it.table.clear();
		}

		@Override
		public String toString() {
			return toString(", ", true);
		}
	}


	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static Table0 with() {
		return new Table0(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number keys and values to primitive int and int, regardless of which
	 * Number type was used.
	 *
	 * @param key0   the first and only key; will be converted to primitive int
	 * @param value0 the first and only value; will be converted to primitive int
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static Table0 with(Number key0, Number value0) {
		Table0 map = new Table0(1);
		map.put(key0.intValue(), value0.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number keys and values to primitive int and int, regardless of which
	 * Number type was used.
	 *
	 * @param key0   a Number key; will be converted to primitive int
	 * @param value0 a Number for a value; will be converted to primitive int
	 * @param key1   a Number key; will be converted to primitive int
	 * @param value1 a Number for a value; will be converted to primitive int
	 * @return a new map containing the given key-value pairs
	 */
	public static Table0 with(Number key0, Number value0, Number key1, Number value1) {
		Table0 map = new Table0(2);
		map.put(key0.intValue(), value0.intValue());
		map.put(key1.intValue(), value1.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number keys and values to primitive int and int, regardless of which
	 * Number type was used.
	 *
	 * @param key0   a Number key; will be converted to primitive int
	 * @param value0 a Number for a value; will be converted to primitive int
	 * @param key1   a Number key; will be converted to primitive int
	 * @param value1 a Number for a value; will be converted to primitive int
	 * @param key2   a Number key; will be converted to primitive int
	 * @param value2 a Number for a value; will be converted to primitive int
	 * @return a new map containing the given key-value pairs
	 */
	public static Table0 with(Number key0, Number value0, Number key1, Number value1, Number key2, Number value2) {
		Table0 map = new Table0(3);
		map.put(key0.intValue(), value0.intValue());
		map.put(key1.intValue(), value1.intValue());
		map.put(key2.intValue(), value2.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Like the more-argument with(), this will
	 * convert its Number keys and values to primitive int and int, regardless of which
	 * Number type was used.
	 *
	 * @param key0   a Number key; will be converted to primitive int
	 * @param value0 a Number for a value; will be converted to primitive int
	 * @param key1   a Number key; will be converted to primitive int
	 * @param value1 a Number for a value; will be converted to primitive int
	 * @param key2   a Number key; will be converted to primitive int
	 * @param value2 a Number for a value; will be converted to primitive int
	 * @param key3   a Number key; will be converted to primitive int
	 * @param value3 a Number for a value; will be converted to primitive int
	 * @return a new map containing the given key-value pairs
	 */
	public static Table0 with(Number key0, Number value0, Number key1, Number value1, Number key2, Number value2, Number key3, Number value3) {
		Table0 map = new Table0(4);
		map.put(key0.intValue(), value0.intValue());
		map.put(key1.intValue(), value1.intValue());
		map.put(key2.intValue(), value2.intValue());
		map.put(key3.intValue(), value3.intValue());
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #Table0(int[], int[])}, which takes all keys and then all values.
	 * This needs all keys to be some kind of (boxed) Number, and converts them to primitive
	 * {@code int}s. It also needs all values to be a (boxed) Number, and converts them to
	 * primitive {@code int}s. Any keys or values that aren't {@code Number}s have that
	 * entry skipped.
	 *
	 * @param key0   the first key; will be converted to a primitive int
	 * @param value0 the first value; will be converted to a primitive int
	 * @param rest   an array or varargs of Number elements
	 * @return a new map containing the given key-value pairs
	 */
	public static Table0 with(Number key0, Number value0, Number... rest) {
		Table0 map = new Table0(1 + (rest.length >>> 1));
		map.put(key0.intValue(), value0.intValue());
		map.putPairs(rest);
		return map;
	}

	/**
	 * Attempts to put alternating key-value pairs into this map, drawing a key, then a value from {@code pairs}, then
	 * another key, another value, and so on until another pair cannot be drawn.  All keys and values must be some type
	 * of boxed Number, such as {@link Integer} or {@link Double}, and will be converted to primitive {@code int}s.
	 * <br>
	 * If any item in {@code pairs} cannot be cast to the appropriate Number type for its position in the
	 * arguments, that pair is ignored and neither that key nor value is put into the map. If any key is null, that pair
	 * is ignored, as well. If {@code pairs} is a Number array that is null, the entire call to putPairs() is ignored.
	 * If the length of {@code pairs} is odd, the last item (which will be unpaired) is ignored.
	 *
	 * @param pairs an array or varargs of Number elements
	 */
	public void putPairs(Number... pairs) {
		if (pairs != null) {
			for (int i = 1; i < pairs.length; i += 2) {
				try {
					if (pairs[i - 1] != null && pairs[i] != null)
						put(pairs[i - 1].intValue(), pairs[i].intValue());
				} catch (ClassCastException ignored) {
				}
			}
		}
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, boolean)}. Every key-value pair should be separated by
	 * {@code ", "}, and every key should be followed by {@code "="} before the value (which
	 * {@link #toString()} does).
	 * Each item can vary significantly in length, and should use
	 * {@link Base#BASE10} digits, which should be human-readable. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str a String containing BASE10 chars
	 */
	public void putLegible(String str) {
		putLegible(str, ", ", "=", 0, -1);
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, boolean)}. Every key-value pair should be separated by
	 * {@code entrySeparator}, and every key should be followed by "=" before the value (which
	 * {@link #toString(String)} does).
	 * Each item can vary significantly in length, and should use
	 * {@link Base#BASE10} digits, which should be human-readable. Any brackets inside the given range
	 * of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str            a String containing BASE10 chars
	 * @param entrySeparator the String separating every key-value pair
	 */
	public void putLegible(String str, String entrySeparator) {
		putLegible(str, entrySeparator, "=", 0, -1);
	}

	/**
	 * Adds items to this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, String, boolean, IntAppender, IntAppender)}. Each item can vary
	 * significantly in length, and should use {@link Base#BASE10} digits, which should be human-readable. Any brackets
	 * inside the given range of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str               a String containing BASE10 chars
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 */
	public void putLegible(String str, String entrySeparator, String keyValueSeparator) {
		putLegible(str, entrySeparator, keyValueSeparator, 0, -1);
	}

	/**
	 * Puts key-value pairs into this map drawn from the result of {@link #toString(String)} or
	 * {@link #appendTo(StringBuilder, String, String, boolean, IntAppender, IntAppender)}. Each item can vary
	 * significantly in length, and should use {@link Base#BASE10} digits, which should be human-readable. Any brackets
	 * inside the given range of characters will ruin the parsing, so increase offset by 1 and
	 * reduce length by 2 if the original String had brackets added to it.
	 *
	 * @param str               a String containing BASE10 chars
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param offset            the first position to read BASE10 chars from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public void putLegible(String str, String entrySeparator, String keyValueSeparator, int offset, int length) {
		int sl, el, kvl;
		if (str == null || entrySeparator == null || keyValueSeparator == null
			|| (sl = str.length()) < 1 || (el = entrySeparator.length()) < 1 || (kvl = keyValueSeparator.length()) < 1
			|| offset < 0 || offset > sl - 1) return;
		final int lim = length < 0 ? sl : Math.min(offset + length, sl);
		int end = str.indexOf(keyValueSeparator, offset + 1);
		int k = 0;
		boolean incomplete = false;
		while (end != -1 && end + kvl < lim) {
			k = Base.BASE10.readInt(str, offset, end);
			offset = end + kvl;
			end = str.indexOf(entrySeparator, offset + 1);
			if (end != -1 && end + el < lim) {
				put(k, Base.BASE10.readInt(str, offset, end));
				offset = end + el;
				end = str.indexOf(keyValueSeparator, offset + 1);
			} else {
				incomplete = true;
			}
		}
		if (incomplete && offset < lim) {
			put(k, Base.BASE10.readInt(str, offset, lim));
		}
	}

	/**
	 * Attempts to put alternating key-value pairs into this map, drawing a key, then a value from {@code pairs}, then
	 * another key, another value, and so on until another pair cannot be drawn.  All keys and values must be primitive
	 * {@code int}s.
	 * <br>
	 * If {@code pairs} is an int array that is null, the entire call to putPairs() is ignored.
	 * If the length of {@code pairs} is odd, the last item (which will be unpaired) is ignored.
	 *
	 * @param pairs an array or varargs of int elements
	 */
	public void putPairsPrimitive(int... pairs) {
		if (pairs != null) {
			for (int i = 1; i < pairs.length; i += 2) {
				put(pairs[i - 1], pairs[i]);
			}
		}
	}

	/**
	 * Constructs an empty map.
	 * This is usually less useful than just using the constructor, but can be handy
	 * in some code-generation scenarios when you don't know how many arguments you will have.
	 *
	 * @return a new map containing nothing
	 */
	public static Table0 withPrimitive() {
		return new Table0(0);
	}

	/**
	 * Constructs a single-entry map given one key and one value.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Unlike the vararg with(), this doesn't
	 * box its arguments into Number items.
	 *
	 * @param key0   the first and only key
	 * @param value0 the first and only value
	 * @return a new map containing just the entry mapping key0 to value0
	 */
	public static Table0 withPrimitive(int key0, int value0) {
		Table0 map = new Table0(1);
		map.put(key0, value0);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Unlike the vararg with(), this doesn't
	 * box its arguments into Number items.
	 *
	 * @param key0   a int key
	 * @param value0 a int value
	 * @param key1   a int key
	 * @param value1 a int value
	 * @return a new map containing the given key-value pairs
	 */
	public static Table0 withPrimitive(int key0, int value0, int key1, int value1) {
		Table0 map = new Table0(2);
		map.put(key0, value0);
		map.put(key1, value1);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Unlike the vararg with(), this doesn't
	 * box its arguments into Number items.
	 *
	 * @param key0   a int key
	 * @param value0 a int value
	 * @param key1   a int key
	 * @param value1 a int value
	 * @param key2   a int key
	 * @param value2 a int value
	 * @return a new map containing the given key-value pairs
	 */
	public static Table0 withPrimitive(int key0, int value0, int key1, int value1, int key2, int value2) {
		Table0 map = new Table0(3);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This is mostly useful as an optimization for {@link #with(Number, Number, Number...)}
	 * when there's no "rest" of the keys or values. Unlike the vararg with(), this doesn't
	 * box its arguments into Number items.
	 *
	 * @param key0   a int key
	 * @param value0 a int value
	 * @param key1   a int key
	 * @param value1 a int value
	 * @param key2   a int key
	 * @param value2 a int value
	 * @param key3   a int key
	 * @param value3 a int value
	 * @return a new map containing the given key-value pairs
	 */
	public static Table0 withPrimitive(int key0, int value0, int key1, int value1, int key2, int value2, int key3, int value3) {
		Table0 map = new Table0(4);
		map.put(key0, value0);
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	/**
	 * Constructs a map given alternating keys and values.
	 * This can be useful in some code-generation scenarios, or when you want to make a
	 * map conveniently by-hand and have it populated at the start. You can also use
	 * {@link #Table0(int[], int[])}, which takes all keys and then all values.
	 * This needs all keys and all values to be primitive {@code int}s; if any are boxed,
	 * then you should call {@link #with(Number, Number, Number...)}.
	 * <br>
	 * This method has to be named differently from {@link #with(Number, Number, Number...)} to
	 * disambiguate the two, which would otherwise both be callable with all primitives
	 * (due to auto-boxing).
	 *
	 * @param key0   the first key; must not be boxed
	 * @param value0 the first value; must not be boxed
	 * @param rest   an array or varargs of primitive int elements
	 * @return a new map containing the given keys and values
	 */
	public static Table0 withPrimitive(int key0, int value0, int... rest) {
		Table0 map = new Table0(1 + (rest.length >>> 1));
		map.put(key0, value0);
		map.putPairsPrimitive(rest);
		return map;
	}

	/**
	 * Creates a new map by parsing all of {@code str},
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and
	 * the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 */
	public static Table0 parse(String str,
								  String entrySeparator,
								  String keyValueSeparator) {
		return parse(str, entrySeparator, keyValueSeparator, false);
	}

	/**
	 * Creates a new map by parsing all of {@code str} (or if {@code brackets} is true, all but the first and last
	 * chars), with entries separated by {@code entrySeparator},
	 * such as {@code ", "} and the keys separated from values by {@code keyValueSeparator}, such as {@code "="}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param brackets          if true, the first and last chars in {@code str} will be ignored
	 */
	public static Table0 parse(String str,
								  String entrySeparator,
								  String keyValueSeparator,
								  boolean brackets) {
		Table0 m = new Table0();
		if (brackets)
			m.putLegible(str, entrySeparator, keyValueSeparator, 1, str.length() - 1);
		else
			m.putLegible(str, entrySeparator, keyValueSeparator, 0, -1);
		return m;
	}

	/**
	 * Creates a new map by parsing the given subrange of {@code str},
	 * with entries separated by {@code entrySeparator}, such as {@code ", "} and the keys separated from values
	 * by {@code keyValueSeparator}, such as {@code "="}.
	 *
	 * @param str               a String containing parseable text
	 * @param entrySeparator    the String separating every key-value pair
	 * @param keyValueSeparator the String separating every key from its corresponding value
	 * @param offset            the first position to read parseable text from in {@code str}
	 * @param length            how many chars to read; -1 is treated as maximum length
	 */
	public static Table0 parse(String str,
								  String entrySeparator,
								  String keyValueSeparator,
								  int offset,
								  int length) {
		Table0 m = new Table0();
		m.putLegible(str, entrySeparator, keyValueSeparator, offset, length);
		return m;
	}

}
