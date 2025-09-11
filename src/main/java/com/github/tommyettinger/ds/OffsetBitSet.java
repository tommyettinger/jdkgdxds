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

import com.github.tommyettinger.ds.support.util.IntAppender;
import com.github.tommyettinger.ds.support.util.IntIterator;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * A bit set, which can be seen as a set of integer positions greater than some starting number,
 * that has changeable offset, or starting position. If you know the integer positions will all
 * be greater than or equal to some minimum value, such as -128, 0, or 1000, then you can use an offset
 * of that minimum value to save memory. This is important because every possible integer position, whether
 * contained in the bit set or not, takes up one bit of memory (rounded up to a multiple of 32), but
 * positions less than the offset simply aren't stored, and the bit set can grow to fit positions arbitrarily
 * higher than the offset. Allows comparison via bitwise operators to other bit sets, as long as the offsets
 * are the same.
 * <br>
 * This was originally Bits in libGDX. Many methods have been renamed to more-closely match the Collection API.
 * This has also had the offset functionality added. It was changed from using {@code long} to store 64 bits in
 * one value, to {@code int} to store 32 bits in one value, because GWT is so slow at handling {@code long}.
 *
 * @author mzechner
 * @author jshapcott
 * @author tommyettinger
 */
public class OffsetBitSet implements PrimitiveSet.OfInt {

	/**
	 * The raw bits, each one representing the presence or absence of an integer at a position.
	 */
	protected int[] bits;

	/**
	 * This is the lowest integer position that this OffsetBitSet can store.
	 * If all positions are at least equal to some value, using that for the offset can save space.
	 */
	protected int offset = 0;

	@Nullable
	protected transient OffsetBitSetIterator iterator1;
	@Nullable
	protected transient OffsetBitSetIterator iterator2;

	/**
	 * Creates a bit set with an initial size that can store positions between 0 and 31, inclusive, without
	 * needing to resize. This has an offset of 0 and can resize to fit larger positions.
	 */
	public OffsetBitSet() {
		bits = new int[1];
	}

	/**
	 * Creates a bit set whose initial size is large enough to explicitly represent bits with indices in the range 0 through
	 * bitCapacity-1. This has an offset of 0 and can resize to fit larger positions.
	 *
	 * @param bitCapacity the initial size of the bit set
	 */
	public OffsetBitSet(int bitCapacity) {
		bits = new int[bitCapacity + 31 >>> 5];
	}

	/**
	 * Creates a bit set whose initial size is large enough to explicitly represent bits with indices in the range {@code start} through
	 * {@code end-1}. This has an offset of {@code start} and can resize to fit larger positions.
	 *
	 * @param start the lowest value that can be stored in the bit set
	 * @param end   the initial end of the range of the bit set
	 */
	public OffsetBitSet(int start, int end) {
		offset = start;
		bits = new int[end + 31 - start >>> 5];
	}

	/**
	 * Creates a bit set from another bit set. This will copy the raw bits and will have the same offset.
	 *
	 * @param toCopy bitset to copy
	 */
	public OffsetBitSet(OffsetBitSet toCopy) {
		this.bits = new int[toCopy.bits.length];
		System.arraycopy(toCopy.bits, 0, this.bits, 0, toCopy.bits.length);
		this.offset = toCopy.offset;
	}

	/**
	 * Creates a bit set from any primitive int collection, such as a {@link IntList} or {@link IntSet}.
	 * The offset of the new bit set will be the lowest int in the collection, which you should be aware of
	 * if you intend to use the bitwise methods such as {@link #and(OffsetBitSet)} and {@link #or(OffsetBitSet)}.
	 *
	 * @param toCopy the primitive int collection to copy
	 */
	public OffsetBitSet(PrimitiveCollection.OfInt toCopy) {
		if (toCopy.isEmpty()) {
			offset = 0;
			bits = new int[1];
			return;
		}
		int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
		for (IntIterator it = toCopy.iterator(); it.hasNext(); ) {
			int n = it.next();
			start = Math.min(start, n);
			end = Math.max(end, n + 1);
		}
		offset = start;
		bits = new int[end + 31 - start >>> 5];
		addAll(toCopy);
	}

	/**
	 * Creates a bit set from an entire int array.
	 * The offset of the new bit set will be the lowest int in the collection, which you should be aware of
	 * if you intend to use the bitwise methods such as {@link #and(OffsetBitSet)} and {@link #or(OffsetBitSet)}.
	 *
	 * @param toCopy the non-null int array to copy
	 */
	public OffsetBitSet(int[] toCopy) {
		this(toCopy, 0, toCopy.length);
	}

	/**
	 * Creates a bit set from an int array, starting reading at an offset and continuing for a given length.
	 * The offset of the new bit set will be the lowest int in the collection, which you should be aware of
	 * if you intend to use the bitwise methods such as {@link #and(OffsetBitSet)} and {@link #or(OffsetBitSet)}.
	 *
	 * @param toCopy the int array to copy
	 * @param off    which index to start copying from toCopy
	 * @param length how many items to copy from toCopy
	 */
	public OffsetBitSet(int[] toCopy, int off, int length) {
		if (toCopy.length == 0) {
			offset = 0;
			bits = new int[1];
			return;
		}
		int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;
		for (int i = off, e = off + length; i < e; i++) {
			int n = toCopy[i];
			start = Math.min(start, n);
			end = Math.max(end, n + 1);
		}
		offset = start;
		bits = new int[end + 31 - start >>> 5];
		addAll(toCopy, off, length);
	}

	/**
	 * Gets the lowest integer position that this OffsetBitSet can store.
	 * If all positions are at least equal to some value, using that for the offset can save space.
	 */
	public int getOffset() {
		return offset;
	}

	/**
	 * Changes the offset without considering the previous value. This effectively adds {@code newOffset - getOffset()}
	 * to every int stored in this, in constant time. This also changes the minimum value in the process.
	 *
	 * @param newOffset the value to use instead of the current offset
	 */
	public void setOffset(int newOffset) {
		this.offset = newOffset;
	}

	/**
	 * Adds {@code addend} to the current offset, effectively adding to every int stored in this, in constant time.
	 * This also changes the minimum value in the process.
	 *
	 * @param addend the value to add to the current offset
	 */
	public void changeOffset(int addend) {
		this.offset += addend;
	}

	/**
	 * This gets the internal {@code int[]} used to store bits in bulk. This is not meant for typical usage; it may be
	 * useful for serialization or other code that would typically need reflection to access the internals here. This
	 * may and often does include padding at the end.
	 *
	 * @return the raw int array used to store positions, one bit per on and per off position
	 */
	public int[] getRawBits() {
		return bits;
	}

	/**
	 * This allows setting the internal {@code int[]} used to store bits in bulk. This is not meant for typical usage; it
	 * may be useful for serialization or other code that would typically need reflection to access the internals here.
	 * Be very careful with this method. If bits is null or empty, it is ignored; this is the only error validation this does.
	 *
	 * @param bits a non-null, non-empty int array storing positions, typically obtained from {@link #getRawBits()}
	 */
	public void setRawBits(int[] bits) {
		if (bits != null && bits.length != 0) {
			this.bits = bits;
		}
	}

	/**
	 * Returns true if the given position is contained in this bit set.
	 * If the index is less than the {@link #getOffset() offset}, this returns false.
	 *
	 * @param index the index of the bit
	 * @return whether the bit is set
	 */
	public boolean contains(int index) {
		index -= offset;
		if (index < 0) return false;
		final int word = index >>> 5;
		if (word >= bits.length) return false;
		return (bits[word] & (1 << index)) != 0;
	}

	/**
	 * Deactivates the given position and returns true if the bit set was modified
	 * in the process. If the index is less than the {@link #getOffset() offset},
	 * this does not modify the bit set and returns false.
	 *
	 * @param index the index of the bit
	 * @return true if this modified the bit set
	 */
	public boolean remove(int index) {
		index -= offset;
		if (index < 0) return false;
		final int word = index >>> 5;
		if (word >= bits.length) return false;
		int oldBits = bits[word];
		bits[word] &= ~(1 << index);
		return bits[word] != oldBits;
	}

	/**
	 * Activates the given position and returns true if the bit set was modified
	 * in the process. If the index is less than the {@link #getOffset() offset},
	 * this does not modify the bit set and returns false.
	 *
	 * @param index the index of the bit
	 * @return true if this modified the bit set
	 */
	public boolean add(int index) {
		index -= offset;
		if (index < 0) return false;
		final int word = index >>> 5;
		checkCapacity(word);
		int oldBits = bits[word];
		bits[word] |= 1 << index;
		return bits[word] != oldBits;
	}

	public boolean addAll(int[] indices) {
		return addAll(indices, 0, indices.length);
	}

	public boolean addAll(int[] indices, int off, int length) {
		if (length <= 0 || off < 0 || off + length > indices.length)
			return false;
		boolean changed = false;
		for (int i = off, n = off + length; i < n; i++) {
			changed |= add(indices[i]);
		}
		return changed;
	}

	public boolean addAll(short[] indices) {
		return addAll(indices, 0, indices.length);
	}

	public boolean addAll(short[] indices, int off, int length) {
		if (length <= 0 || off < 0 || off + length > indices.length)
			return false;
		boolean changed = false;
		for (int i = off, n = off + length; i < n; i++) {
			changed |= add(indices[i]);
		}
		return changed;
	}

	public boolean addAll(byte[] indices) {
		return addAll(indices, 0, indices.length);
	}

	public boolean addAll(byte[] indices, int off, int length) {
		if (length <= 0 || off < 0 || off + length > indices.length)
			return false;
		boolean changed = false;
		for (int i = off, n = off + length; i < n; i++) {
			changed |= add(indices[i]);
		}
		return changed;
	}

	public boolean addAll(char[] indices) {
		return addAll(indices, 0, indices.length);
	}

	public boolean addAll(char[] indices, int off, int length) {
		if (length <= 0 || off < 0 || off + length > indices.length)
			return false;
		boolean changed = false;
		for (int i = off, n = off + length; i < n; i++) {
			changed |= add(indices[i]);
		}
		return changed;
	}

	public boolean addAll(PrimitiveCollection.OfInt indices) {
		IntIterator it = indices.iterator();
		boolean changed = false;
		while (it.hasNext()) {
			changed |= add(it.nextInt());
		}
		return changed;
	}

	/**
	 * Returns an iterator for the keys in the set. Remove is supported.
	 * <p>
	 * Use the {@link OffsetBitSetIterator} constructor for nested or multithreaded iteration.
	 */
	@Override
	public OffsetBitSetIterator iterator() {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new OffsetBitSetIterator(this);
			iterator2 = new OffsetBitSetIterator(this);
		}
		if (!iterator1.valid) {
			iterator1.reset();
			iterator1.valid = true;
			iterator2.valid = false;
			return iterator1;
		}
		iterator2.reset();
		iterator2.valid = true;
		iterator1.valid = false;
		return iterator2;
	}


	/**
	 * Sets the given int position to true, unless the position is less
	 * than the {@link #getOffset() offset} (then it does nothing).
	 *
	 * @param index the index of the bit to set
	 */
	public void activate(int index) {
		index -= offset;
		if (index < 0) return;
		final int word = index >>> 5;
		checkCapacity(word);
		bits[word] |= 1 << index;
	}

	/**
	 * Sets the given int position to false, unless the position is less
	 * than the {@link #getOffset() offset} (then it does nothing).
	 *
	 * @param index the index of the bit to clear
	 */
	public void deactivate(int index) {
		index -= offset;
		if (index < 0) return;
		final int word = index >>> 5;
		if (word >= bits.length) return;
		bits[word] &= ~(1 << index);
	}

	/**
	 * Changes the given int position from true to false, or from false to true,
	 * unless the position is less than the {@link #getOffset() offset} (then it
	 * does nothing).
	 *
	 * @param index the index of the bit to flip
	 */
	public void toggle(int index) {
		index -= offset;
		if (index < 0) return;
		final int word = index >>> 5;
		checkCapacity(word);
		bits[word] ^= 1 << index;
	}

	private void checkCapacity(int index) {
		if (index >= bits.length) {
			int[] newBits = new int[1 << -BitConversion.countLeadingZeros(index)]; // resizes to next power of two size that can fit index
			System.arraycopy(bits, 0, newBits, 0, bits.length);
			bits = newBits;
		}
	}

	/**
	 * Clears the entire bitset, removing all contained ints. Doesn't change the capacity.
	 */
	public void clear() {
		Arrays.fill(bits, 0);
	}

	/**
	 * Gets the capacity in bits, including both true and false values, and including any false values that may be
	 * after the last contained position, but does not include the offset. Runs in O(1) time.
	 *
	 * @return the number of bits currently stored, <b>not</b> the highest set bit; doesn't include offset either
	 */
	public int numBits() {
		return bits.length << 5;
	}

	/**
	 * Returns the "logical extent" of this bitset: the index of the highest set bit in the bitset plus one. Returns zero if the
	 * bitset contains no set bits. If this has any set bits, it will return an int at least equal to {@code offset}.
	 * Runs in O(n) time.
	 *
	 * @return the logical extent of this bitset
	 */
	public int length() {
		int[] bits = this.bits;
		for (int word = bits.length - 1; word >= 0; --word) {
			int bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return (word + 1 << 5) - BitConversion.countLeadingZeros(bitsAtWord) + offset;
			}
		}
		return 0;
	}

	/**
	 * Returns the size of the set, or its cardinality; this is the count of distinct activated positions in the set.
	 * Note that unlike most Collection types, which typically have O(1) size() runtime, this runs in O(n) time, where
	 * n is on the order of the capacity.
	 *
	 * @return the count of distinct activated positions in the set.
	 */
	public int size() {
		int[] bits = this.bits;
		int count = 0;
		for (int word = bits.length - 1; word >= 0; --word) {
			count += Integer.bitCount(bits[word]);
		}
		return count;
	}

	/**
	 * Checks if there are any positions contained in this at all. Run in O(n) time, but usually takes less.
	 *
	 * @return true if this bitset contains at least one bit set to true
	 */
	public boolean notEmpty() {
		return !isEmpty();
	}

	/**
	 * Checks if there are no positions contained in this at all. Run in O(n) time, but usually takes less.
	 *
	 * @return true if this bitset contains no bits that are set to true
	 */
	public boolean isEmpty() {
		int[] bits = this.bits;
		int length = bits.length;
		for (int i = 0; i < length; i++) {
			if (bits[i] != 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the index of the first bit that is set to true that occurs on or after the specified starting index. If no such bit
	 * exists then {@code getOffset() - 1} is returned.
	 *
	 * @param fromIndex the index to start looking at
	 * @return the first position that is set to true that occurs on or after the specified starting index
	 */
	public int nextSetBit(int fromIndex) {
		fromIndex -= offset;
		if (fromIndex < 0) return offset - 1;
		int[] bits = this.bits;
		int word = fromIndex >>> 5;
		int bitsLength = bits.length;
		if (word >= bitsLength)
			return offset - 1;
		int bitsAtWord = bits[word] & -1 << fromIndex; // shift implicitly is masked to bottom 31 bits
		if (bitsAtWord != 0) {
			return BitConversion.countTrailingZeros(bitsAtWord) + (word << 5) + offset; // countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
		}
		for (word++; word < bitsLength; word++) {
			bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return BitConversion.countTrailingZeros(bitsAtWord) + (word << 5) + offset;
			}
		}
		return offset - 1;
	}

	/**
	 * Returns the index of the first bit that is set to false that occurs on or after the specified starting index. If no such bit
	 * exists then {@code numBits() + getOffset()}  is returned.
	 *
	 * @param fromIndex the index to start looking at
	 * @return the first position that is set to true that occurs on or after the specified starting index
	 */
	public int nextClearBit(int fromIndex) {
		fromIndex -= offset;
		if (fromIndex < 0) return (bits.length << 5) + offset;
		int[] bits = this.bits;
		int word = fromIndex >>> 5;
		int bitsLength = bits.length;
		if (word >= bitsLength) return (bits.length << 5) + offset;
		int bitsAtWord = bits[word] | (1 << fromIndex) - 1; // shift implicitly is masked to bottom 31 bits
		if (bitsAtWord != -1) {
			return BitConversion.countTrailingZeros(~bitsAtWord) + (word << 5) + offset; // countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
		}
		for (word++; word < bitsLength; word++) {
			bitsAtWord = bits[word];
			if (bitsAtWord != -1) {
				return BitConversion.countTrailingZeros(~bitsAtWord) + (word << 5) + offset; // countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
			}
		}
		return (bits.length << 5) + offset;
	}

	/**
	 * Performs a logical <b>AND</b> of this target bit set with the argument bit set. This bit set is modified so that each bit
	 * in it has the value true if and only if it both initially had the value true and the corresponding bit in the bit set
	 * argument also had the value true. Both this OffsetBitSet and {@code other} must have the same offset.
	 *
	 * @param other another OffsetBitSet; must have the same offset as this
	 */
	public void and(OffsetBitSet other) {
		if (offset == other.offset) {
			int commonWords = Math.min(bits.length, other.bits.length);
			for (int i = 0; commonWords > i; i++) {
				bits[i] &= other.bits[i];
			}

			if (bits.length > commonWords) {
				for (int i = commonWords, s = bits.length; s > i; i++) {
					bits[i] = 0;
				}
			}
		} else {
			throw new UnsupportedOperationException("The offset of both OffsetBitSet objects must be the same to call and().");
		}
	}

	/**
	 * Clears all the bits in this bit set whose corresponding bit is set in the specified bit set.
	 * This can be seen as an optimized version of {@link PrimitiveCollection.OfInt#removeAll(OfInt)} that only works if
	 * both OffsetBitSet objects have the same {@link #offset}. Both this OffsetBitSet and {@code other} must have the same offset.
	 *
	 * @param other another OffsetBitSet; must have the same offset as this
	 */
	public void andNot(OffsetBitSet other) {
		if (offset == other.offset) {
			for (int i = 0, j = bits.length, k = other.bits.length; i < j && i < k; i++) {
				bits[i] &= ~other.bits[i];
			}
		} else {
			throw new UnsupportedOperationException("The offset of both OffsetBitSet objects must be the same to call andNot().");
		}

	}

	/**
	 * Performs a logical <b>OR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has
	 * the value true if and only if it either already had the value true or the corresponding bit in the bit set argument has the
	 * value true. Both this OffsetBitSet and {@code other} must have the same offset.
	 *
	 * @param other another OffsetBitSet; must have the same offset as this
	 */
	public void or(OffsetBitSet other) {
		if (offset == other.offset) {
			int commonWords = Math.min(bits.length, other.bits.length);
			for (int i = 0; commonWords > i; i++) {
				bits[i] |= other.bits[i];
			}

			if (commonWords < other.bits.length) {
				checkCapacity(other.bits.length);
				for (int i = commonWords, s = other.bits.length; s > i; i++) {
					bits[i] = other.bits[i];
				}
			}
		} else {
			throw new UnsupportedOperationException("The offset of both OffsetBitSet objects must be the same to call or().");
		}
	}

	/**
	 * Performs a logical <b>XOR</b> of this bit set with the bit set argument. This bit set is modified so that a bit in it has
	 * the value true if and only if one of the following statements holds:
	 * <ul>
	 * <li>The bit initially has the value true, and the corresponding bit in the argument has the value false.</li>
	 * <li>The bit initially has the value false, and the corresponding bit in the argument has the value true.</li>
	 * </ul>
	 * Both this OffsetBitSet and {@code other} must have the same offset.
	 *
	 * @param other another OffsetBitSet; must have the same offset as this
	 */
	public void xor(OffsetBitSet other) {
		if (offset == other.offset) {
			int commonWords = Math.min(bits.length, other.bits.length);
			for (int i = 0; commonWords > i; i++) {
				bits[i] ^= other.bits[i];
			}
			if (commonWords < other.bits.length) {
				checkCapacity(other.bits.length);
				for (int i = commonWords, s = other.bits.length; s > i; i++) {
					bits[i] = other.bits[i];
				}
			}
		} else {
			throw new UnsupportedOperationException("The offset of both OffsetBitSet objects must be the same to call xor().");
		}
	}

	/**
	 * Returns true if the specified OffsetBitSet has any bits set to true that are also set to true in this OffsetBitSet.
	 * Both this OffsetBitSet and {@code other} must have the same offset.
	 *
	 * @param other another OffsetBitSet; must have the same offset as this
	 * @return boolean indicating whether this bit set intersects the specified bit set
	 */
	public boolean intersects(OffsetBitSet other) {
		if (offset == other.offset) {
			int[] bits = this.bits;
			int[] otherBits = other.bits;
			for (int i = Math.min(bits.length, otherBits.length) - 1; i >= 0; i--) {
				if ((bits[i] & otherBits[i]) != 0) {
					return true;
				}
			}
			return false;
		} else {
			throw new UnsupportedOperationException("The offset of both OffsetBitSet objects must be the same to call intersects().");
		}
	}

	/**
	 * Returns true if this bit set is a super set of the specified set, i.e. it has all bits set to true that are also set to
	 * true in the specified OffsetBitSet. If this OffsetBitSet and {@code other} have the same offset, this is much more efficient, but
	 * it will work even if the offsets are different.
	 *
	 * @param other another OffsetBitSet
	 * @return boolean indicating whether this bit set is a super set of the specified set
	 */
	public boolean containsAll(OffsetBitSet other) {
		if (offset == other.offset) {
			int[] bits = this.bits;
			int[] otherBits = other.bits;
			int otherBitsLength = otherBits.length;
			int bitsLength = bits.length;

			for (int i = bitsLength; i < otherBitsLength; i++) {
				if (otherBits[i] != 0) {
					return false;
				}
			}
			for (int i = Math.min(bitsLength, otherBitsLength) - 1; i >= 0; i--) {
				if ((bits[i] & otherBits[i]) != otherBits[i]) {
					return false;
				}
			}
			return true;
		} else return ((PrimitiveCollection.OfInt) this).containsAll(other);
	}

	@Override
	public int hashCode() {
		int hash = offset;
		for (int i = 0, n = bits.length; i < n; i++) {
			hash += bits[i];
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		OffsetBitSet other = (OffsetBitSet) obj;
		if (offset != other.offset) return false;
		int[] otherBits = other.bits;

		int commonWords = Math.min(bits.length, otherBits.length);
		for (int i = 0; commonWords > i; i++) {
			if (bits[i] != otherBits[i]) return false;
		}

		if (bits.length == otherBits.length) return true;

		if(commonWords < otherBits.length) {
			for (int i = commonWords, n = otherBits.length; i < n; i++) {
				if (0 != otherBits[i]) return false;
			}
		} else {
			for (int i = commonWords, n = bits.length; i < n; i++) {
				if (0 != bits[i]) return false;
			}
		}

		return true;
	}

	/**
	 * Given a StringBuilder, this appends part of the toString() representation of this OffsetBitSet, without allocating a String.
	 * This does not include the opening {@code [} and closing {@code ]} chars, and only appends the int positions in this OffsetBitSet,
	 * each pair separated by the given delimiter String. You can use this to choose a different delimiter from what toString() uses.
	 *
	 * @param builder   a StringBuilder that will be modified in-place and returned
	 * @param delimiter the String that separates every pair of integers in the result
	 * @return the given StringBuilder, after modifications
	 */
	public StringBuilder appendContents(StringBuilder builder, String delimiter) {
		int curr = nextSetBit(offset);
		builder.append(curr);
		while ((curr = nextSetBit(curr + 1)) != offset - 1) {
			builder.append(delimiter).append(curr);
		}
		return builder;
	}

	/**
	 * Given a StringBuilder, this appends the toString() representation of this OffsetBitSet, without allocating a String.
	 * This includes the opening {@code [} and closing {@code ]} chars; it uses {@code ", "} as its delimiter.
	 *
	 * @param builder a StringBuilder that will be modified in-place and returned
	 * @return the given StringBuilder, after modifications
	 */
	public StringBuilder appendTo(StringBuilder builder) {
		return appendContents(builder.append('['), ", ").append(']');
	}

	/**
	 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link IntAppender}
	 * to convert each item to a customizable representation and append them to a StringBuilder. To use
	 * the default String representation, you can use {@link IntAppender#DEFAULT} as an appender.
	 *
	 * @param sb        a StringBuilder that this can append to
	 * @param separator how to separate items, such as {@code ", "}
	 * @param brackets  true to wrap the output in square brackets, or false to omit them
	 * @param appender  a function that takes a StringBuilder and an int, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended items of this PrimitiveCollection
	 */
	@Override
	public <S extends CharSequence & Appendable> S appendTo(S sb, String separator, boolean brackets, IntAppender appender) {
		try {
			if (isEmpty()) {
				if(brackets) sb.append("[]");
				return sb;
			}
			if (brackets) {
				sb.append('[');
			}
			int curr = nextSetBit(offset);
			appender.apply(sb, curr);
			while ((curr = nextSetBit(curr + 1)) != offset - 1) {
				sb.append(separator);
				appender.apply(sb, curr);
			}
			if (brackets) {
				sb.append(']');
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return sb;
	}

	@Override
	public String toString() {
		return toString(", ", true);
	}


	public static class OffsetBitSetIterator implements IntIterator {
		static private final int INDEX_ILLEGAL = -1, INDEX_ZERO = -1;

		public boolean hasNext;

		final OffsetBitSet set;
		int nextIndex, currentIndex;
		boolean valid = true;

		public OffsetBitSetIterator(OffsetBitSet set) {
			this.set = set;
			reset();
		}

		public void reset() {
			currentIndex = INDEX_ILLEGAL + set.offset;
			nextIndex = INDEX_ZERO + set.offset;
			findNextIndex();
		}

		void findNextIndex() {
			nextIndex = set.nextSetBit(nextIndex + 1);
			hasNext = nextIndex != INDEX_ILLEGAL + set.offset;
		}

		/**
		 * Returns {@code true} if the iteration has more elements.
		 * (In other words, returns {@code true} if {@link #next} would
		 * return an element rather than throwing an exception.)
		 *
		 * @return {@code true} if the iteration has more elements
		 */
		@Override
		public boolean hasNext() {
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			return hasNext;
		}

		@Override
		public void remove() {
			if (currentIndex < 0) {
				throw new IllegalStateException("next must be called before remove.");
			}
			set.deactivate(currentIndex);
			currentIndex = INDEX_ILLEGAL + set.offset;
		}

		@Override
		public int nextInt() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			int key = nextIndex;
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		/**
		 * Returns a new {@link IntList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public IntList toList() {
			IntList list = new IntList(set.size());
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {
				list.add(nextInt());
			}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given PrimitiveCollection.OfInt.
		 * Does not change the position of this iterator.
		 *
		 * @param coll any modifiable PrimitiveCollection.OfInt; may have items appended into it
		 * @return the given primitive collection
		 */
		public PrimitiveCollection.OfInt appendInto(PrimitiveCollection.OfInt coll) {
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {
				coll.add(nextInt());
			}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return coll;
		}

	}

	/**
	 * Static builder for an OffsetBitSet; this overload does not allocate an
	 * array for the index/indices, but only takes one index. This always has
	 * an offset of 0.
	 *
	 * @param index the one position to place in the built bit set; must be non-negative
	 * @return a new OffsetBitSet with the given item
	 */
	public static OffsetBitSet with(int index) {
		OffsetBitSet s = new OffsetBitSet(index + 1);
		s.add(index);
		return s;
	}

	/**
	 * Static builder for an OffsetBitSet; this overload allocates an array for
	 * the indices unless given an array already, and can take many indices. This
	 * always has an offset of 0.
	 *
	 * @param indices the positions to place in the built bit set; must be non-negative
	 * @return a new OffsetBitSet with the given items
	 */
	public static OffsetBitSet with(int... indices) {
		OffsetBitSet s = new OffsetBitSet();
		s.addAll(indices);
		return s;
	}

	/**
	 * Calls {@link #parse(String, String, boolean)} with brackets set to false.
	 * @param str a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @return a new collection parsed from str
	 */
	public static OffsetBitSet parse(String str, String delimiter) {
		return parse(str, delimiter, false);
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, int, int)} on either all of
	 * {@code str} (if {@code brackets} is false) or {@code str} without its first and last chars (if {@code brackets}
	 * is true). Each item is expected to be separated by {@code delimiter}.
	 * @param str a String that will be parsed in full (depending on brackets)
	 * @param delimiter the delimiter between items in str
	 * @param brackets if true, the first and last chars in str will be ignored
	 * @return a new collection parsed from str
	 */
	public static OffsetBitSet parse(String str, String delimiter, boolean brackets) {
		OffsetBitSet c = new OffsetBitSet();
		if(brackets)
			c.addLegible(str, delimiter, 1, str.length() - 1);
		else
			c.addLegible(str, delimiter);
		return c;
	}

	/**
	 * Creates a new collection and fills it by calling {@link #addLegible(String, String, int, int)} with the given
	 * four parameters as-is.
	 * @param str a String that will have the given section parsed
	 * @param delimiter the delimiter between items in str
	 * @param offset the first position to parse in str, inclusive
	 * @param length how many chars to parse, starting from offset
	 * @return a new collection parsed from str
	 */
	public static OffsetBitSet parse(String str, String delimiter, int offset, int length) {
		OffsetBitSet c = new OffsetBitSet();
		c.addLegible(str, delimiter, offset, length);
		return c;
	}
}
