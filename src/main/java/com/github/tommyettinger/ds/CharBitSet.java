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
 * {@link CharBitSetResizable}, but this class always uses an internal table 8KiB in size (2048 ints), and can avoid
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
public class CharBitSet implements PrimitiveSet.SetOfChar, CharPredicate {

	/**
	 * The raw bits, each one representing the presence or absence of a char at a position.
	 */
	protected int[] bits;

	protected transient CharBitSetIterator iterator1;
	protected transient CharBitSetIterator iterator2;

	/**
	 * Creates a bit set with an initial size that can store positions between 0 and 65535, inclusive, without
	 * needing to resize. This won't ever need to resize for any char input.
	 */
	public CharBitSet() {
		bits = new int[2048];
	}

	/**
	 * Creates a bit set from another bit set. This will copy the raw bits and will have the same offset.
	 *
	 * @param toCopy bitset to copy
	 */
	public CharBitSet(CharBitSet toCopy) {
		this.bits = new int[2048];
		System.arraycopy(toCopy.bits, 0, this.bits, 0, toCopy.bits.length);
	}

	/**
	 * Creates a bit set from any primitive char collection, such as a {@link CharList} or {@link CharDeque}.
	 *
	 * @param toCopy the primitive int collection to copy
	 */
	public CharBitSet(CharSequence toCopy) {
		bits = new int[2048];
		if (toCopy.length() == 0) return;
		addAll(toCopy);
	}

	/**
	 * Creates a bit set from an entire char array.
	 *
	 * @param toCopy the non-null char array to copy
	 */
	public CharBitSet(char[] toCopy) {
		this(toCopy, 0, toCopy.length);
	}

	/**
	 * Creates a bit set from a char array, starting reading at an offset and continuing for a given length.
	 *
	 * @param toCopy the char array to copy
	 * @param off    which index to start copying from toCopy
	 * @param length how many items to copy from toCopy
	 */
	public CharBitSet(char[] toCopy, int off, int length) {
		bits = new int[2048];
		if (toCopy.length == 0) {
			return;
		}
		addAll(toCopy, off, length);
	}

	/**
	 * Meant primarily for offline use to store the results of a CharPredicate on one target platform so those results
	 * can be recalled identically on all platforms. This can be relevant because of changing Unicode versions on newer
	 * JDK versions, or partial implementations of JDK predicates like {@link Character#isLetter(char)} on GWT.
	 *
	 * @param predicate a CharPredicate, which could be a method reference like {@code Character::isLetter}
	 * @see #toJavaCode() Once you have a CharBitSet on a working target platform, you can store it with toJavaCode().
	 */
	public CharBitSet(CharPredicate predicate) {
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
	public CharBitSet(int[] ints, boolean useAsRawBits) {
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
				addAll(ints);
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
	 * Returns true if the given char is contained in this bit set.
	 *
	 * @param index the index of the bit
	 * @return whether the bit is set
	 */
	public boolean contains(char index) {
		final int word = index >>> 5;
		if (word >= bits.length) return false;
		return (bits[word] & (1 << index)) != 0;
	}

	/**
	 * Returns true if the given position is contained in this bit set.
	 *
	 * @param index the index of the bit
	 * @return whether the bit is set
	 */
	public boolean contains(int index) {
		if (index < 0 || index >= 65536) return false;
		final int word = index >>> 5;
		if (word >= bits.length) return false;
		return (bits[word] & (1 << index)) != 0;
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
		if (word >= bits.length) return false;
		int oldBits = bits[word];
		bits[word] &= ~(1 << index);
		return bits[word] != oldBits;
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
		if (word >= bits.length) return false;
		int oldBits = bits[word];
		bits[word] &= ~(1 << index);
		return bits[word] != oldBits;
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
		checkCapacity(word);
		int oldBits = bits[word];
		bits[word] |= 1 << index;
		return bits[word] != oldBits;
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

	public boolean addAll(CharSequence indices) {
		return addAll(indices, 0, indices.length());
	}

	public boolean addAll(CharSequence indices, int off, int length) {
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

	/**
	 * Evaluates this predicate on the given argument.
	 *
	 * @param value the input argument
	 * @return {@code true} if the input argument matches the predicate,
	 * otherwise {@code false}
	 */
	@Override
	public boolean test(char value) {
		return contains(value);
	}

	/**
	 * Returns an iterator for the keys in the set. Remove is supported.
	 * <p>
	 * Use the {@link CharBitSetIterator} constructor for nested or multithreaded iteration.
	 */
	@Override
	public CharBitSetIterator iterator() {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new CharBitSetIterator(this);
			iterator2 = new CharBitSetIterator(this);
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
	 * Sets the given int position to true, unless the position is outside char range
	 * (then it does nothing).
	 *
	 * @param index the index of the bit to set
	 */
	public void activate(int index) {
		if (index < 0 || index >= 65536) return;
		final int word = index >>> 5;
		checkCapacity(word);
		bits[word] |= 1 << index;
	}

	/**
	 * Sets the given int position to false, unless the position is outside char range
	 * (then it does nothing).
	 *
	 * @param index the index of the bit to clear
	 */
	public void deactivate(int index) {
		if (index < 0 || index >= 65536) return;
		final int word = index >>> 5;
		if (word >= bits.length) return;
		bits[word] &= ~(1 << index);
	}

	/**
	 * Changes the given int position from true to false, or from false to true,
	 * unless the position is outside char range (then it does nothing).
	 *
	 * @param index the index of the bit to flip
	 */
	public void toggle(int index) {
		if (index < 0 || index >= 65536) return;
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
	 * bitset contains no set bits. Runs in O(n) time.
	 *
	 * @return the logical extent of this bitset
	 */
	public int length() {
		int[] bits = this.bits;
		for (int word = bits.length - 1; word >= 0; --word) {
			int bitsAtWord = bits[word];
			if (bitsAtWord != 0) {
				return (word + 1 << 5) - BitConversion.countLeadingZeros(bitsAtWord);
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
		int bitsLength = bits.length;
		if (word >= bitsLength)
			return -1;
		int bitsAtWord = bits[word] & -1 << fromIndex; // shift implicitly is masked to bottom 31 bits
		if (bitsAtWord != 0) {
			// countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
			return BitConversion.countTrailingZeros(bitsAtWord) + (word << 5);
		}
		for (word++; word < bitsLength; word++) {
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
		if (fromIndex < 0) return (bits.length << 5);
		int[] bits = this.bits;
		int word = fromIndex >>> 5;
		int bitsLength = bits.length;
		if (word >= bitsLength) return (bits.length << 5);
		int bitsAtWord = bits[word] | (1 << fromIndex) - 1; // shift implicitly is masked to bottom 31 bits
		if (bitsAtWord != -1) {
			// countTrailingZeros() uses an intrinsic candidate, and should be extremely fast
			return BitConversion.countTrailingZeros(~bitsAtWord) + (word << 5);
		}
		for (word++; word < bitsLength; word++) {
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
	 * @param other another CharBitSet
	 */
	public void and(CharBitSet other) {
		int commonWords = Math.min(bits.length, other.bits.length);
		for (int i = 0; commonWords > i; i++) {
			bits[i] &= other.bits[i];
		}

		if (bits.length > commonWords) {
			for (int i = commonWords, s = bits.length; s > i; i++) {
				bits[i] = 0;
			}
		}
	}

	/**
	 * Clears all the bits in this bit set whose corresponding bit is set in the specified bit set.
	 * This can be seen as an optimized version of {@link OfInt#removeAll(OfInt)}.
	 *
	 * @param other another CharBitSet
	 */
	public void andNot(CharBitSet other) {
		for (int i = 0, j = bits.length, k = other.bits.length; i < j && i < k; i++) {
			bits[i] &= ~other.bits[i];
		}
	}

	/**
	 * Performs a logical <b>OR</b> of this bit set with the bit set argument.
	 * This bit set is modified so that a bit in it has the value true if and only if
	 * it either already had the value true or the corresponding bit in {@code other}
	 * has the value true.
	 *
	 * @param other another CharBitSet
	 */
	public void or(CharBitSet other) {
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
	}

	/**
	 * Performs a logical <b>XOR</b> of this bit set with the bit set argument. This bit set is modified so that
	 * a bit in it has the value true if and only if one of the following statements holds:
	 * <ul>
	 * <li>The bit initially has the value true, and the corresponding bit in the argument has the value false.</li>
	 * <li>The bit initially has the value false, and the corresponding bit in the argument has the value true.</li>
	 * </ul>
	 *
	 * @param other another CharBitSet
	 */
	public void xor(CharBitSet other) {
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
	}

	/**
	 * Returns true if the specified CharBitSet has any bits set to true that are also
	 * set to true in this CharBitSet.
	 *
	 * @param other another CharBitSet
	 * @return true if this bit set shares any set bits with the specified bit set
	 */
	public boolean intersects(CharBitSet other) {
		int[] bits = this.bits;
		int[] otherBits = other.bits;
		for (int i = Math.min(bits.length, otherBits.length) - 1; i >= 0; i--) {
			if ((bits[i] & otherBits[i]) != 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if this bit set is a super set of the specified set, i.e. it has all bits set
	 * to true that are also set to true in the specified CharBitSet.
	 *
	 * @param other another CharBitSet
	 * @return boolean indicating whether this bit set is a super set of the specified set
	 */
	public boolean containsAll(CharBitSet other) {
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
	}

	@Override
	public int hashCode() {
		int hash = 1;
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

		CharBitSet other = (CharBitSet) obj;
		int[] otherBits = other.bits;

		int commonWords = Math.min(bits.length, otherBits.length);
		for (int i = 0; i < commonWords; i++) {
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
	 * Given a StringBuilder, this appends part of the toString() representation of this CharBitSet, without allocating a String.
	 * This does not include the opening {@code [} and closing {@code ]} chars, and only appends the int positions in this CharBitSet,
	 * each pair separated by the given delimiter String. You can use this to choose a different delimiter from what toString() uses.
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
	 * Given a StringBuilder, this appends the toString() representation of this CharBitSet, without allocating a String.
	 * This includes the opening {@code [} and closing {@code ]} chars; it uses {@code ", "} as its delimiter.
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
	 * A convenience method that returns a String of Java source that constructs this CharBitSet directly from its raw
	 * bits, without any extra steps involved.
	 * <br>
	 * This is intended to allow tests on one platform to set up CharBitSet values that store the results of some test,
	 * such as {@link Character#isLetter(char)}, and to load those results on any platform without having to recalculate
	 * the results (potentially with incorrect results on other platforms). Notably, GWT doesn't calculate many Unicode
	 * queries correctly (at least according to their JVM documentation), and this can store their results for a recent
	 * Unicode version by running on the most recent desktop JDK, and storing to be loaded on other platforms.
	 *
	 * @return a String of Java code that can be used to construct an exact copy of this CharBitSet
	 */
	public String toJavaCode() {
		return "new CharBitSet(new int[]{" + Base.joinReadable(",", bits) + "}, true)";
	}

	public static class CharBitSetIterator implements CharIterator {
		static private final int INDEX_ILLEGAL = -1, INDEX_ZERO = -1;

		public boolean hasNext;

		final CharBitSet set;
		int nextIndex, currentIndex;
		boolean valid = true;

		public CharBitSetIterator(CharBitSet set) {
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
			currentIndex = INDEX_ILLEGAL;
		}

		@Override
		public char nextChar() {
			if (!hasNext) {
				throw new NoSuchElementException();
			}
			if (!valid) {
				throw new RuntimeException("#iterator() cannot be used nested.");
			}
			char key = (char)nextIndex;
			currentIndex = nextIndex;
			findNextIndex();
			return key;
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
	 * Static builder for a CharBitSet; this overload does not allocate an
	 * array for the index/indices, but only takes one index. This always has
	 * an offset of 0.
	 *
	 * @param index the one char to place in the built bit set
	 * @return a new CharBitSet with the given item
	 */
	public static CharBitSet with(char index) {
		CharBitSet s = new CharBitSet(index + 1);
		s.add(index);
		return s;
	}

	/**
	 * Static builder for a CharBitSet; this overload allocates an array for
	 * the indices unless given an array already, and can take many indices. This
	 * always has an offset of 0.
	 *
	 * @param indices the positions to place in the built bit set; must be non-negative
	 * @return a new CharBitSet with the given items
	 */
	public static CharBitSet with(char... indices) {
		CharBitSet s = new CharBitSet();
		s.addAll(indices);
		return s;
	}

	/**
	 * Calls {@link #parse(String, String, boolean)} with brackets set to false.
	 * @param str a String that will be parsed in full
	 * @param delimiter the delimiter between items in str
	 * @return a new collection parsed from str
	 */
	public static CharBitSet parse(String str, String delimiter) {
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
	public static CharBitSet parse(String str, String delimiter, boolean brackets) {
		CharBitSet c = new CharBitSet();
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
	public static CharBitSet parse(String str, String delimiter, int offset, int length) {
		CharBitSet c = new CharBitSet();
		c.addLegible(str, delimiter, offset, length);
		return c;
	}
}
