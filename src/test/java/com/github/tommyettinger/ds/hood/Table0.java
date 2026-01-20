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

import com.github.tommyettinger.ds.EnhancedCollection;
import com.github.tommyettinger.ds.PrimitiveCollection;
import com.github.tommyettinger.ds.PrimitiveSet;
import com.github.tommyettinger.ds.Utilities;
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
	}
}
