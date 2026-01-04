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

import com.github.tommyettinger.digital.Base;
import com.github.tommyettinger.digital.BitConversion;
import com.github.tommyettinger.ds.support.util.CharAppender;
import com.github.tommyettinger.ds.support.util.CharIterator;
import com.github.tommyettinger.ds.support.util.IntIterator;
import com.github.tommyettinger.function.CharPredicate;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * A bit set, which can be seen as a set of char positions in the Unicode Basic Multilingual Plane (the first
 * 65536 chars in Unicode). Allows comparison via bitwise operators to other bit sets. This is similar to
 * {@link CharBitSet}, but this class always uses an internal table 8KiB in size (2048 ints), and can avoid
 * some extra math as a result at the expense of (slightly) greater memory usage.
 * <br>
 * This was originally Bits in libGDX. Many methods have been renamed to more-closely match the Collection API.
 * This was changed from using {@code long} to store 64 bits in
 * one value, to {@code int} to store 32 bits in one value, because GWT is so slow at handling {@code long}.
 *
 * @author mzechner
 * @author jshapcott
 * @author tommyettinger
 */
public class CharBitSetFixedSize implements PrimitiveSet.SetOfChar, CharPredicate {

	/**
	 * The raw bits, each one representing the presence or absence of a char at a position.
	 */
	protected int[] bits;

	/**
	 * Creates a bit set with an initial size that can store positions between 0 and 65535, inclusive, without
	 * needing to resize. This won't ever resize.
	 */
	public CharBitSetFixedSize() {
		bits = new int[2048];
	}

	/**
	 * Creates a bit set from another bit set. This will copy the raw bits.
	 *
	 * @param toCopy bitset to copy
	 */
	public CharBitSetFixedSize(CharBitSetFixedSize toCopy) {
		this.bits = new int[2048];
		System.arraycopy(toCopy.bits, 0, this.bits, 0, 2048);
	}

	/**
	 * Creates a bit set from a CharSequence, such as a {@link CharList} or {@link String}.
	 *
	 * @param toCopy the char sequence to copy
	 */
	public CharBitSetFixedSize(CharSequence toCopy) {
		bits = new int[2048];
		if (toCopy.length() == 0) return;
		activateSeq(toCopy);
	}

	/**
	 * Creates a bit set from an entire char array.
	 *
	 * @param toCopy the non-null char array to copy
	 */
	public CharBitSetFixedSize(char[] toCopy) {
		this(toCopy, 0, toCopy.length);
	}

	/**
	 * Creates a bit set from a char array, starting reading at an offset and continuing for a given length.
	 *
	 * @param toCopy the char array to copy
	 * @param off    which index to start copying from toCopy
	 * @param length how many items to copy from toCopy
	 */
	public CharBitSetFixedSize(char[] toCopy, int off, int length) {
		bits = new int[2048];
		if (toCopy.length == 0) {
			return;
		}
		activateAll(toCopy, off, length);
	}

	/**
	 * Meant primarily for offline use to store the results of a CharPredicate on one target platform so those results
	 * can be recalled identically on all platforms. This can be relevant because of changing Unicode versions on newer
	 * JDK versions, or partial implementations of JDK predicates like {@link Character#isLetter(char)} on GWT.
	 *
	 * @param predicate a CharPredicate, which could be a method reference like {@code Character::isLetter}
	 * @see #toJavaCode() Once you have a CharBitSetFixedSize on a working target platform, you can store it with toJavaCode().
	 */
	public CharBitSetFixedSize(CharPredicate predicate) {
		this();
		if(predicate != null) {
			for (int i = 0; i < 65536; i++) {
				if (predicate.test((char) i))
					bits[i >>> 5] |= 1 << i;
			}
		}
	}

	/**
	 * Allows passing an int array either to be treated as char contents to enter (ignoring any ints outside the valid
	 * char range) or as the raw bits that are used internally (which can be accessed with {@link #getRawBits()}.
	 * Note that {@code ints} should always have a length of 1 or more; otherwise, it won't be used directly (or if
	 * {@code useAsRawBits} is false, it won't have any contents copied out).
	 *
	 * @param ints depending on {@code useAsRawBits}, this will be used as either char items or raw bits
	 * @param useAsRawBits if true, {@code ints} will be used as raw bits and used directly, not copied as char items
	 */
	public CharBitSetFixedSize(int[] ints, boolean useAsRawBits) {
		if (ints != null) {
			if (useAsRawBits) {
				if (ints.length == 2048) {
					this.bits = ints;
				} else {
					this.bits = new int[2048];
					System.arraycopy(ints, 0, this.bits, 0, Math.min(2048, ints.length));
				}
			} else {
				this.bits = new int[2048];
				activateAll(ints);
			}
		} else {
			this.bits = new int[2048];
		}
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
	 * This allows setting the internal {@code int[]} used to store bits in bulk. This is not meant for typical usage;
	 * it may be useful for serialization or other code that would typically need reflection to access the internals
	 * here. Be very careful with this method. If bits is null, it is ignored. If the length of bits is not exactly
	 * 2048, it will not be used directly, but its contents will be copied into the bits here, up to the first reached
	 * of 2048 ints or {@code bits.length}.
	 *
	 * @param bits a non-null, length-2048 int array storing positions, typically obtained from {@link #getRawBits()}
	 */
	public void setRawBits(int[] bits) {
		if (bits != null) {
			if (bits.length == 2048) {
				this.bits = bits;
			} else {
				System.arraycopy(bits, 0, this.bits, 0, Math.min(2048, bits.length));
			}
		}
	}

	/**
	 * Returns true if the given char is contained in this bit set.
	 *
	 * @param index the index of the bit
	 * @return whether the bit is set
	 */
	public boolean contains(char index) {
		return (bits[index >>> 5] & (1 << index)) != 0;
	}

	/**
	 * Returns true if the given position is contained in this bit set.
	 *
	 * @param index the index of the bit
	 * @return whether the bit is set
	 */
	public boolean contains(int index) {
		if (index < 0 || index >= 65536) return false;
		return (bits[index >>> 5] & (1 << index)) != 0;
	}

	/**
	 * Deactivates the given position and returns true if the bit set was modified
	 * in the process.
	 *
	 * @param index the index of the bit
	 * @return true if this modified the bit set
	 */
	public boolean remove(char index) {
		final int word = index >>> 5;
		int oldBits = bits[word];
		return (bits[word] = oldBits & ~(1 << index)) != oldBits;
	}

	/**
	 * Deactivates the given position and returns true if the bit set was modified
	 * in the process.
	 *
	 * @param index the index of the bit
	 * @return true if this modified the bit set
	 */
	public boolean remove(int index) {
		if (index < 0 || index >= 65536) return false;
		final int word = index >>> 5;
		int oldBits = bits[word];
		return (bits[word] = oldBits & ~(1 << index)) != oldBits;
	}

	/**
	 * Activates the given position and returns true if the bit set was modified
	 * in the process.
	 *
	 * @param index the index of the bit
	 * @return true if this modified the bit set
	 */
	public boolean add(char index) {
		final int word = index >>> 5;
		int oldBits = bits[word];
		return (bits[word] = oldBits | 1 << index) != oldBits;
	}

	/**
	 * Activates the given position and returns true if the bit set was modified
	 * in the process.
	 *
	 * @param index the index of the bit
	 * @return true if this modified the bit set
	 */
	public boolean add(int index) {
		if (index < 0 || index >= 65536) return false;
		final int word = index >>> 5;
		int oldBits = bits[word];
		return (bits[word] = oldBits | 1 << index) != oldBits;
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
			changed |= add((char)indices[i]);
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
			changed |= add((char)(indices[i] & 0xFF));
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

	public boolean addSeq(CharSequence indices) {
		return addSeq(indices, 0, indices.length());
	}

	public boolean addSeq(CharSequence indices, int off, int length) {
		if (length <= 0 || off < 0 || off + length > indices.length())
			return false;
		boolean changed = false;
		for (int i = off, n = off + length; i < n; i++) {
			changed |= add(indices.charAt(i));
		}
		return changed;
	}

	public boolean addAll(OfInt indices) {
		IntIterator it = indices.iterator();
		boolean changed = false;
		while (it.hasNext()) {
			changed |= add(it.nextInt());
		}
		return changed;
	}

	public void activateAll(char[] indices) {
		activateAll(indices, 0, indices.length);
	}

	public void activateAll(char[] indices, int off, int length) {
		if (length <= 0 || off < 0 || off + length > indices.length)
			return;
		for (int i = off, n = off + length; i < n; i++) {
			activate(indices[i]);
		}
	}


	public void activateAll(int[] indices) {
		activateAll(indices, 0, indices.length);
	}

	public void activateAll(int[] indices, int off, int length) {
		if (length <= 0 || off < 0 || off + length > indices.length)
			return;
		for (int i = off, n = off + length; i < n; i++) {
			activate(indices[i]);
		}
	}

	/**
	 * Like {@link #activateAll(char[])}, but takes a CharSequence.
	 * Named differently to avoid ambiguity between {@link #activateAll(OfChar)} when a type is both a CharSequence and
	 * a PrimitiveCollection.OfChar .
	 * @param indices the CharSequence to read distinct chars from
	 */
	public void activateSeq(CharSequence indices) {
		activateSeq(indices, 0, indices.length());
	}

	/**
	 * Like {@link #activateAll(char[], int, int)}, but takes a CharSequence.
	 * Named differently to avoid ambiguity between {@link #activateAll(OfChar)} when a type is both a CharSequence and
	 * a PrimitiveCollection.OfChar .
	 * @param indices the CharSequence to read distinct chars from
	 * @param off the first position to read from {@code indices}
	 * @param length how many chars to read from {@code indices}; because the CharSequence may have duplicates, this is
	 *                  not necessarily the length that will be added
	 */
	public void activateSeq(CharSequence indices, int off, int length) {
		if (length <= 0 || off < 0 || off + length > indices.length())
			return;
		for (int i = off, n = off + length; i < n; i++) {
			activate(indices.charAt(i));
		}
	}

	/**
	 * Adds another PrimitiveCollection.OfChar, such as a CharList, to this set.
	 * If you have another CharBitSetFixedSize, you can use {@link #or(CharBitSetFixedSize)}, which is faster.
	 * @param indices another primitive collection of char
	 */
	public void activateAll(PrimitiveCollection.OfChar indices) {
		CharIterator it = indices.iterator();
		while (it.hasNext()) {
			activate(it.nextChar());
		}
	}

	/**
	 * Returns true if the given char is contained in this bit set, or false otherwise.
	 *
	 * @param value the char to check
	 * @return true if the char is present, or false otherwise
	 */
	@Override
	public boolean test(char value) {
		return (bits[value >>> 5] & (1 << value)) != 0;

	}

	/**
	 * Returns a new iterator for the keys in the set; remove is supported.
	 * @return a new iterator for the keys in the set; remove is supported
	 */
	@Override
	public CharBitSetFixedSizeIterator iterator() {
		return new CharBitSetFixedSizeIterator(this);
	}

	/**
	 * Sets the given int position to true.
	 *
	 * @param index the index of the bit to set
	 */
	public void activate(char index) {
		bits[index >>> 5] |= 1 << index;
	}

	/**
	 * Sets the given int position to false.
	 *
	 * @param index the index of the bit to clear
	 */
	public void deactivate(char index) {
		bits[index >>> 5] &= ~(1 << index);
	}

	/**
	 * Changes the given int position from true to false, or from false to true.
	 *
	 * @param index the index of the bit to flip
	 */
	public void toggle(char index) {
		bits[index >>> 5] ^= 1 << index;
	}

	/**
	 * Sets the given int position to true, unless the position is outside char range
	 * (then it does nothing).
	 *
	 * @param index the index of the bit to set
	 */
	public void activate(int index) {
		if (index < 0 || index >= 65536) return;
		bits[index >>> 5] |= 1 << index;
	}

	/**
	 * Sets the given int position to false, unless the position is outside char range
	 * (then it does nothing).
	 *
	 * @param index the index of the bit to clear
	 */
	public void deactivate(int index) {
		if (index < 0 || index >= 65536) return;
		bits[index >>> 5] &= ~(1 << index);
	}

	/**
	 * Changes the given int position from true to false, or from false to true,
	 * unless the position is outside char range (then it does nothing).
	 *
	 * @param index the index of the bit to flip
	 */
	public void toggle(int index) {
		if (index < 0 || index >= 65536) return;
		bits[index >>> 5] ^= 1 << index;
	}

	/**
	 * Clears the entire bitset, removing all contained ints. Doesn't change the capacity.
	 */
	public void clear() {
		Arrays.fill(bits, 0);
	}

	/**
	 * Gets the capacity in bits, including both true and false values, and including any false values that may be
	 * after the last contained position. Runs in O(1) time.
	 *
	 * @return the number of bits currently stored, <b>not</b> the highest set bit
	 */
	public int numBits() {
		return bits.length << 5;
	}

	/**
	 * Returns the "logical extent" of this bitset: the index of the highest set bit in the bitset plus one. Returns zero if the
	 * bitset contains no set bits. Runs in O(n) time.
	 *
	 * @return the logical extent of this bitset
	 */
	public int length() {
		int[] bits = this.bits;
		for (int word = 2047; word >= 0; --word) {
			int bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return (word + 1 << 5) - BitConversion.countLeadingZeros(bitsAtWord);
			}
		}
		return 0;
	}

	/**
	 * Returns the size of the set, or its cardinality; this is the count of distinct activated positions in the set.
	 * Note that while this runs in O(1) time, there is a constant factor of 2048 there, because the size here is always
	 * 2048.
	 *
	 * @return the count of distinct activated positions in the set.
	 */
	public int size() {
		int[] bits = this.bits;
		int count = 0;
		for (int word = 0; word < 2048; word++) {
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
		int[] bits = this.bits;
		for (int i = 0; i < 2048; i++) {
			if (bits[i] != 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if there are no positions contained in this at all. Run in O(n) time, but usually takes less.
	 *
	 * @return true if this bitset contains no bits that are set to true
	 */
	public boolean isEmpty() {
		int[] bits = this.bits;
		for (int i = 0; i < 2048; i++) {
			if (bits[i] != 0) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns the index of the first bit that is set to true that occurs on or after the specified starting index.
	 * If no such bit exists then {@code -1} is returned.
	 *
	 * @param fromIndex the index to start looking at
	 * @return the first position that is set to true that occurs on or after the specified starting index
	 */
	public int nextSetBit(int fromIndex) {
		if (fromIndex < 0) return -1;
		int[] bits = this.bits;
		int word = fromIndex >>> 5;
		if (word >= 2048)
			return -1;
		int bitsAtWord = bits[word] & -1 << fromIndex; // shift implicitly is masked to bottom 31 bits
		if (bitsAtWord != 0) {
			// countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
			return BitConversion.countTrailingZeros(bitsAtWord) + (word << 5);
		}
		for (word++; word < 2048; word++) {
			bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return BitConversion.countTrailingZeros(bitsAtWord) + (word << 5);
			}
		}
		return -1;
	}

	/**
	 * Returns the index of the first bit that is set to false that occurs on or after the specified starting index. If no such bit
	 * exists then {@link #numBits()}  is returned.
	 *
	 * @param fromIndex the index to start looking at
	 * @return the first position that is set to true that occurs on or after the specified starting index
	 */
	public int nextClearBit(int fromIndex) {
		if (fromIndex < 0) return 65536;
		int[] bits = this.bits;
		int word = fromIndex >>> 5;
		if (word >= 2048) return 65536;
		int bitsAtWord = bits[word] | (1 << fromIndex) - 1; // shift implicitly is masked to bottom 31 bits
		if (bitsAtWord != -1) {
			// countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
			return BitConversion.countTrailingZeros(~bitsAtWord) + (word << 5);
		}
		for (word++; word < 2048; word++) {
			bitsAtWord = bits[word];
			if (bitsAtWord != -1) {
				// countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
				return BitConversion.countTrailingZeros(~bitsAtWord) + (word << 5);
			}
		}
		return (bits.length << 5);
	}

	/**
	 * Performs a logical <b>AND</b> of this target bit set with the argument bit set. This bit set is modified so that each bit
	 * in it has the value true if and only if it both initially had the value true and the corresponding bit in the bit set
	 * argument also had the value true.
	 *
	 * @param other another CharBitSetFixedSize
	 */
	public void and(CharBitSetFixedSize other) {
		for (int i = 0; i < 2048; i++) {
			bits[i] &= other.bits[i];
		}
	}

	/**
	 * Clears all the bits in this bit set whose corresponding bit is set in the specified bit set.
	 * This can be seen as an optimized version of {@link OfInt#removeAll(OfInt)}.
	 *
	 * @param other another CharBitSetFixedSize
	 */
	public void andNot(CharBitSetFixedSize other) {
		for (int i = 0; i < 2048; i++) {
			bits[i] &= ~other.bits[i];
		}
	}

	/**
	 * Performs a logical <b>OR</b> of this bit set with the bit set argument.
	 * This bit set is modified so that a bit in it has the value true if and only if
	 * it either already had the value true or the corresponding bit in {@code other}
	 * has the value true.
	 *
	 * @param other another CharBitSetFixedSize
	 */
	public void or(CharBitSetFixedSize other) {
		for (int i = 0; i < 2048; i++) {
			bits[i] |= other.bits[i];
		}
	}

	/**
	 * Performs a logical <b>XOR</b> of this bit set with the bit set argument. This bit set is modified so that
	 * a bit in it has the value true if and only if one of the following statements holds:
	 * <ul>
	 * <li>The bit initially has the value true, and the corresponding bit in the argument has the value false.</li>
	 * <li>The bit initially has the value false, and the corresponding bit in the argument has the value true.</li>
	 * </ul>
	 *
	 * @param other another CharBitSetFixedSize
	 */
	public void xor(CharBitSetFixedSize other) {
		for (int i = 0; i < 2048; i++) {
			bits[i] ^= other.bits[i];
		}
	}

	/**
	 * Returns true if the specified CharBitSetFixedSize has any bits set to true that are also
	 * set to true in this CharBitSetFixedSize.
	 *
	 * @param other another CharBitSetFixedSize
	 * @return true if this bit set shares any set bits with the specified bit set
	 */
	public boolean intersects(CharBitSetFixedSize other) {
		int[] bits = this.bits;
		int[] otherBits = other.bits;
		for (int i = 0; i < 2048; i++) {
			if ((bits[i] & otherBits[i]) != 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if this bit set is a super set of the specified set, i.e. it has all bits set
	 * to true that are also set to true in the specified CharBitSetFixedSize.
	 *
	 * @param other another CharBitSetFixedSize
	 * @return boolean indicating whether this bit set is a super set of the specified set
	 */
	public boolean containsAll(CharBitSetFixedSize other) {
		int[] bits = this.bits;
		int[] otherBits = other.bits;

		for (int i = 0; i < 2048; i++) {
			int o = otherBits[i];
			if ((bits[i] & o) != o) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		for (int i = 0; i < 2048; i++) {
			hash += bits[i];
		}
		return hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;

		CharBitSetFixedSize other = (CharBitSetFixedSize) obj;
		int[] otherBits = other.bits;

		for (int i = 0; i < 2048; i++) {
			if (bits[i] != otherBits[i]) return false;
		}
		return true;
	}

	/**
	 * Given a StringBuilder, this appends part of the toString() representation of this CharBitSetFixedSize, without
	 * allocating a String. This does not include the opening {@code [} and closing {@code ]} chars, and only appends
	 * the int positions in this CharBitSetFixedSize, each pair separated by the given delimiter String. You can use
	 * this to choose a different delimiter from what toString() uses.
	 *
	 * @param builder   a StringBuilder that will be modified in-place and returned
	 * @param delimiter the String that separates every pair of integers in the result
	 * @return the given StringBuilder, after modifications
	 */
	public StringBuilder appendContents(StringBuilder builder, String delimiter) {
		int curr = nextSetBit(0);
		builder.append(curr);
		while ((curr = nextSetBit(curr + 1)) != -1) {
			builder.append(delimiter).append(curr);
		}
		return builder;
	}

	/**
	 * Given a StringBuilder, this appends the toString() representation of this CharBitSetFixedSize, without allocating
	 * a String. This includes the opening {@code [} and closing {@code ]} chars; it uses {@code ", "} as its delimiter.
	 *
	 * @param builder a StringBuilder that will be modified in-place and returned
	 * @return the given StringBuilder, after modifications
	 */
	public StringBuilder appendTo(StringBuilder builder) {
		return appendContents(builder.append('['), ", ").append(']');
	}

	/**
	 * Appends to a StringBuilder from the contents of this PrimitiveCollection, but uses the given {@link CharAppender}
	 * to convert each item to a customizable representation and append them to a StringBuilder. To use
	 * the default String representation, you can use {@link CharAppender#DEFAULT} as an appender.
	 *
	 * @param sb        a StringBuilder that this can append to
	 * @param separator how to separate items, such as {@code ", "}
	 * @param brackets  true to wrap the output in square brackets, or false to omit them
	 * @param appender  a function that takes a StringBuilder and an int, and returns the modified StringBuilder
	 * @return {@code sb}, with the appended items of this PrimitiveCollection
	 */
	@Override
	public <S extends CharSequence & Appendable> S appendTo(S sb, String separator, boolean brackets, CharAppender appender) {
		try {
			if (isEmpty()) {
				if(brackets) sb.append("[]");
				return sb;
			}
			if (brackets) {
				sb.append('[');
			}
			int curr = nextSetBit(0);
			appender.apply(sb, (char) curr);
			while ((curr = nextSetBit(curr + 1)) != -1) {
				sb.append(separator);
				appender.apply(sb, (char) curr);
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

	/**
	 * A convenience method that returns a String of Java source that constructs this CharBitSetFixedSize directly from
	 * its raw bits, without any extra steps involved.
	 * <br>
	 * This is intended to allow tests on one platform to set up CharBitSetFixedSize values that store the results of
	 * some test, such as {@link Character#isLetter(char)}, and to load those results on any platform without having to
	 * recalculate the results (potentially with incorrect results on other platforms). Notably, GWT doesn't calculate
	 * many Unicode queries correctly (at least according to their JVM documentation), and this can store their results
	 * for a recent Unicode version by running on the most recent desktop JDK, and storing to be loaded on other
	 * platforms.
	 *
	 * @return a String of Java code that can be used to construct an exact copy of this CharBitSetFixedSize
	 */
	public String toJavaCode() {
		return "new CharBitSetFixedSize(new int[]{" + Base.joinReadable(",", bits) + "}, true)";
	}

	public static class CharBitSetFixedSizeIterator implements CharIterator {
		static private final int INDEX_ILLEGAL = -1, INDEX_ZERO = -1;

		public boolean hasNext;

		final CharBitSetFixedSize set;
		int nextIndex, currentIndex;

		public CharBitSetFixedSizeIterator(CharBitSetFixedSize set) {
			this.set = set;
			reset();
		}

		public void reset() {
			currentIndex = INDEX_ILLEGAL;
			nextIndex = INDEX_ZERO;
			findNextIndex();
		}

		void findNextIndex() {
			nextIndex = set.nextSetBit(nextIndex + 1);
			hasNext = nextIndex != INDEX_ILLEGAL;
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
			return hasNext;
		}

		@Override
		public void remove() {
			if (currentIndex < 0) {
				throw new IllegalStateException("next must be called before remove.");
			}
			set.deactivate(currentIndex);
			currentIndex = INDEX_ILLEGAL;
		}

		@Override
		public char nextChar() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			char key = (char)nextIndex;
			currentIndex = nextIndex;
			findNextIndex();
			return key;
		}

		/**
		 * Returns a new {@code char[]} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public char[] toArray() {
			char[] arr = new char[set.size()];
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			int i = 0;
			while (hasNext) {
				arr[i++] = nextChar();
			}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return arr;
		}

		/**
		 * Returns a new {@link CharList} containing the remaining items.
		 * Does not change the position of this iterator.
		 */
		public CharList toList() {
			CharList list = new CharList(set.size());
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {
				list.add(nextChar());
			}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return list;
		}

		/**
		 * Append the remaining items that this can iterate through into the given PrimitiveCollection.OfChar.
		 * Does not change the position of this iterator.
		 *
		 * @param coll any modifiable PrimitiveCollection.OfChar; may have items appended into it
		 * @return the given primitive collection
		 */
		public OfChar appendInto(OfChar coll) {
			int currentIdx = currentIndex, nextIdx = nextIndex;
			boolean hn = hasNext;
			while (hasNext) {
				coll.add(nextChar());
			}
			currentIndex = currentIdx;
			nextIndex = nextIdx;
			hasNext = hn;
			return coll;
		}

	}

	/**
	 * Static builder for a CharBitSetFixedSize; this overload does not allocate an
	 * array for the index/indices, but only takes one index.
	 *
	 * @param index the one char to place in the built bit set
	 * @return a new CharBitSetFixedSize with the given item
	 */
	public static CharBitSetFixedSize with(char index) {
		CharBitSetFixedSize s = new CharBitSetFixedSize();
		s.add(index);
		return s;
	}

	/**
	 * Static builder for a CharBitSetFixedSize; this overload allocates an array for
	 * the indices unless given an array already, and can take many indices.
	 *
	 * @param indices the positions to place in the built bit set; must be non-negative
	 * @return a new CharBitSetFixedSize with the given items
	 */
	public static CharBitSetFixedSize with(char... indices) {
		CharBitSetFixedSize s = new CharBitSetFixedSize();
		s.addAll(indices);
		return s;
	}

	/**
	 * Calls {@link #parse(String, String, boolean)} with brackets set to false.
	 * @param str a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @return a new collection parsed from str
	 */
	public static CharBitSetFixedSize parse(String str, String delimiter) {
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
	public static CharBitSetFixedSize parse(String str, String delimiter, boolean brackets) {
		CharBitSetFixedSize c = new CharBitSetFixedSize();
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
	public static CharBitSetFixedSize parse(String str, String delimiter, int offset, int length) {
		CharBitSetFixedSize c = new CharBitSetFixedSize();
		c.addLegible(str, delimiter, offset, length);
		return c;
	}
}
