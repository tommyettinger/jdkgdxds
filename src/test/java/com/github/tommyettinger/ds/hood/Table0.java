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

import com.github.tommyettinger.ds.Utilities;

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
public class Table0 {
	public long[] slots;
	public int mask;
	public int count;
	public int defaultValue = 0;

	public Table0() {
		this(16);
	}

	public Table0(int capacity){
		mask = Utilities.tableSize(capacity, 0.75f) - 1;
		slots = new long[mask+2];
		count = 0;
	}

	public boolean containsKey(int key) {
		if(key == 0){
			return (int) slots[mask + 1] == 0;
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
			if((int)slot == 0)
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

	public int getOrDefault(int key, int defaultValue) {
		if(key == 0){
			long slot = slots[mask+1];
			if((int)slot == 0)
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
			resize(count);
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
}
