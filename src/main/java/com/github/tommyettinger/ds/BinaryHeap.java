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

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A binary heap that stores nodes which each have a float value and are sorted either lowest first or highest first.
 * This can expand if its capacity is exceeded. It defaults to acting as a min-heap, sorting lowest-first.
 * The {@link Node} class can be extended to store additional information.
 * <br>
 * This isn't a direct copy from libGDX, but it's very close. It implements {@link java.util.Queue} and {@link Collection}.
 *
 * @author Nathan Sweet
 * @author Tommy Ettinger
 */
@SuppressWarnings("unchecked")
public class BinaryHeap<T extends BinaryHeap.Node> extends AbstractQueue<T> implements EnhancedCollection<T> {
	public int size;

	private Node[] nodes;
	private final boolean isMaxHeap;

	/**
	 * Constructs a BinaryHeap with 16 starting capacity, sorting lowest-first (a min-heap).
	 */
	public BinaryHeap() {
		this(16, false);
	}

	/**
	 * Constructs a BinaryHeap with the specified capacity and sorting order.
	 *
	 * @param capacity  the initial capacity
	 * @param isMaxHeap if true, this will sort highest-first; if false, it will sort lowest-first
	 */
	public BinaryHeap(int capacity, boolean isMaxHeap) {
		this.isMaxHeap = isMaxHeap;
		nodes = new Node[capacity];
	}

	/**
	 * Constructs a BinaryHeap with the contents from the given Collection of nodes, sorting lowest-first (a min-heap).
	 * If a duplicate node is present in {@code coll}, all repeats are ignored.
	 *
	 * @param coll a Collection of T (which must extend {@link Node}) or objects that subclass T
	 */
	public BinaryHeap(Collection<? extends T> coll) {
		this(false, coll);
	}

	/**
	 * Constructs a BinaryHeap with the specified sorting order and the contents from the given Collection of nodes.
	 * If a duplicate node is present in {@code coll}, all repeats are ignored.
	 *
	 * @param isMaxHeap if true, this will sort highest-first; if false, it will sort lowest-first
	 * @param coll      a Collection of T (which must extend {@link Node}) or objects that subclass T
	 */
	public BinaryHeap(boolean isMaxHeap, Collection<? extends T> coll) {
		this.isMaxHeap = isMaxHeap;
		nodes = new Node[coll.size()];
		addAll(coll);
	}

	/**
	 * Constructs a BinaryHeap with the contents from the given array of nodes, sorting lowest-first (a min-heap).
	 * If a duplicate node is present in {@code arr}, all repeats are ignored.
	 *
	 * @param arr an array of T (which must extend {@link Node}) or objects that subclass T
	 */
	public BinaryHeap(T[] arr) {
		this(false, arr);
	}

	/**
	 * Constructs a BinaryHeap with the specified sorting order and the contents from the given array of nodes.
	 * If a duplicate node is present in {@code arr}, all repeats are ignored.
	 *
	 * @param isMaxHeap if true, this will sort highest-first; if false, it will sort lowest-first
	 * @param arr       an array of T (which must extend {@link Node}) or objects that subclass T
	 */
	public BinaryHeap(boolean isMaxHeap, T[] arr) {
		this.isMaxHeap = isMaxHeap;
		nodes = new Node[arr.length];
		addAll(arr);
	}

	/**
	 * Returns true if this is a max-heap (that is, it sorts highest-first), or false if this is a min-heap
	 * (it sorts lowest-first). If not specified, this is usually false; it can be set only in the constructor,
	 * such as {@link #BinaryHeap(int, boolean)} or {@link #BinaryHeap(boolean, Collection)}.
	 *
	 * @return true if this sorts highest-first; false if it sorts lowest-first
	 */
	public boolean isMaxHeap() {
		return isMaxHeap;
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		if (c == this) {
			throw new IllegalArgumentException("A BinaryHeap cannot be added to itself.");
		}
		boolean modified = false;
		for (T t : c) {
			modified |= offer(t);
		}
		return modified;
	}

	public boolean addAll(T[] c) {
		return addAll(c, 0, c.length);
	}

	public boolean addAll(T[] c, int offset, int length) {
		boolean modified = false;
		for (int i = offset, n = offset + length; i < n; i++) {
			modified |= offer(c[i]);
		}
		return modified;
	}

	/**
	 * Adds the node to the heap using its current value. The node should not already be in the heap.
	 */
	@Override
	public boolean add(T node) {
		// Expand if necessary.
		if (size == nodes.length) {
			Node[] newNodes = new Node[size << 1];
			System.arraycopy(nodes, 0, newNodes, 0, size);
			nodes = newNodes;
		}
		// Insert at end and bubble up.
		node.index = size;
		nodes[size] = node;
		up(size++);
		return true;
	}

	/**
	 * Inserts the specified element into this queue if it is possible to do
	 * so immediately without violating capacity restrictions.
	 * You can also use {@link #add(Node)}, but if you try to add a duplicate element
	 * with that, an {@link IllegalStateException} is thrown. Here, if you try to add
	 * a duplicate element, no Exception is thrown and this returns {@code false}.
	 *
	 * @param node the element to add; must not be null
	 * @return {@code true} if the element was added to this queue, else
	 * {@code false} (typically when node is already present in this BinaryHeap)
	 * @throws ClassCastException   if the class of the specified element
	 *                              prevents it from being added to this queue
	 * @throws NullPointerException if the specified element is null
	 */
	@Override
	public boolean offer(T node) {
		if (size == nodes.length) {
			Node[] newNodes = new Node[size << 1];
			System.arraycopy(nodes, 0, newNodes, 0, size);
			nodes = newNodes;
		}
		// Insert at end and bubble up.
		node.index = size;
		nodes[size] = node;
		try {
			up(size++);
		} catch (IllegalStateException ise) {
			return false;
		}
		return true;
	}

	/**
	 * Retrieves and removes the head of this queue, or returns {@code null} if this BinaryHeap is empty.
	 * The head is the item with the lowest value (or highest value if this heap is configured as a max heap).
	 *
	 * @return the head of this BinaryHeap, or {@code null} if this queue is empty
	 * @throws ClassCastException if the class of the specified element
	 *                            prevents it from being added to this queue
	 */
	@Override
	public T poll() {
		if (size == 0)
			return null;
		Node removed = nodes[0];
		if (--size > 0) {
			nodes[0] = nodes[size];
			nodes[size] = null;
			down(0);
		} else {
			nodes[0] = null;
		}
		return (T) removed;
	}

	/**
	 * Retrieves and removes the head of this queue.  This method differs
	 * from {@link #poll()} only in that it throws an exception if this
	 * queue is empty, and won't return null.
	 *
	 * @return the head of this queue
	 * @throws NoSuchElementException if this queue is empty
	 * @throws ClassCastException     if the class of the specified element
	 *                                prevents it from being added to this queue
	 */
	@Override
	public T remove() {
		if (size == 0)
			throw new NoSuchElementException("A BinaryHeap cannot be empty when remove() is called.");
		Node removed = nodes[0];
		if (--size > 0) {
			nodes[0] = nodes[size];
			nodes[size] = null;
			down(0);
		} else {
			nodes[0] = null;
		}
		return (T) removed;
	}

	/**
	 * Sets the node's value and adds it to the heap. The node should not already be in the heap.
	 */
	public boolean add(T node, float value) {
		node.value = value;
		return add(node);
	}

	/**
	 * Returns true if the heap contains the specified node. Exactly the same as {@link #contains(Object, boolean)}
	 * with {@code identity} set to false.
	 *
	 * @param node should be a {@code T}, which must extend {@link Node}; can be some other type, which gives false
	 * @implSpec This implementation iterates over the elements in the collection,
	 * checking each element in turn for equality with the specified element via {@link Object#equals(Object)}.
	 */
	@Override
	public boolean contains(Object node) {
		for (Node other : nodes) {
			if (other.equals(node)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the heap contains the specified node.
	 *
	 * @param node     should be a {@code T}, which must extend {@link Node}; can be some other type, which gives false
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 */
	public boolean contains(Object node, boolean identity) {
		if (identity) {
			for (Node n : nodes) {
				if (n == node) {
					return true;
				}
			}
		} else {
			for (Node other : nodes) {
				if (other.equals(node)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the first item in the heap. This is the item with the lowest value (or highest value if this heap is configured as
	 * a max heap). If the heap is empty, throws an {@link NoSuchElementException}.
	 *
	 * @return the first item in the heap
	 * @throws NoSuchElementException if the heap is empty.
	 * @throws ClassCastException     if the class of the specified element
	 *                                prevents it from being added to this queue
	 */
	@Override
	public T element() {
		if (size == 0) {
			throw new NoSuchElementException("The heap is empty.");
		}
		return (T) nodes[0];
	}

	/**
	 * Returns the first item in the heap. This is the item with the lowest value (or highest value if this heap is configured as
	 * a max heap). If the heap is empty, returns null.
	 *
	 * @return the first item in the heap, or null if the heap is empty
	 * @throws ClassCastException if the class of the specified element
	 *                            prevents it from being added to this queue
	 */
	@Override
	public T peek() {
		if (size == 0) {
			return null;
		}
		return (T) nodes[0];
	}

	/**
	 * Retrieves and removes the head of this queue, or returns {@code null} if this BinaryHeap is empty.
	 * The head is the item with the lowest value (or highest value if this heap is configured as a max heap).
	 * <br>
	 * This method is identical to {@link #poll()} in this class, but because poll() is defined as part of the
	 * Queue interface, whereas this method was defined ad-hoc by libGDX, poll() should be preferred in new code.
	 *
	 * @return the head of this BinaryHeap, or {@code null} if this queue is empty
	 */
	public T pop() {
		if (size == 0)
			return null;
		Node removed = nodes[0];
		if (--size > 0) {
			nodes[0] = nodes[size];
			nodes[size] = null;
			down(0);
		} else {
			nodes[0] = null;
		}
		return (T) removed;
	}

	/**
	 * Removes the given node and returns it. If the node is not present in this BinaryHeap or is invalid, this will
	 * probably throw an Exception.
	 *
	 * @param node a {@link Node} that should be present in this already
	 * @return {@code node} after its removal
	 */
	public T remove(T node) {
		if (--size > 0) {
			Node moved = nodes[size];
			nodes[size] = null;
			nodes[node.index] = moved;
			if (moved.value < node.value ^ isMaxHeap) {
				up(node.index);
			} else {
				down(node.index);
			}
		} else {
			nodes[0] = null;
		}
		return node;
	}

	@Override
	public int size() {
		return size;
	}

	/**
	 * Returns true if the heap has one or more items.
	 */
	public boolean notEmpty() {
		return size != 0;
	}

	/**
	 * Returns true if the heap is empty.
	 */
	@Override
	public boolean isEmpty() {
		return size == 0;
	}

	/**
	 * Removes all nodes from this BinaryHeap.
	 */
	@Override
	public void clear() {
		Utilities.clear(nodes, 0, size);
		size = 0;
	}

	/**
	 * Changes the value of the node, which should already be in the heap.
	 */
	public void setValue(T node, float value) {
		float oldValue = node.value;
		node.value = value;
		if (value < oldValue ^ isMaxHeap) {
			up(node.index);
		} else {
			down(node.index);
		}
	}

	private void up(int index) {
		Node[] nodes = this.nodes;
		Node node = nodes[index];
		float value = node.value;
		while (index > 0) {
			int parentIndex = (index - 1) >> 1;
			Node parent = nodes[parentIndex];
			if (node == parent)
				throw new IllegalStateException("Duplicate nodes are not allowed in a BinaryHeap.");
			if (value < parent.value ^ isMaxHeap) {
				nodes[index] = parent;
				parent.index = index;
				index = parentIndex;
			} else {
				break;
			}
		}
		nodes[index] = node;
		node.index = index;
	}

	private void down(int index) {
		Node[] nodes = this.nodes;
		int size = this.size;

		Node node = nodes[index];
		float value = node.value;

		while (true) {
			int leftIndex = 1 + (index << 1);
			if (leftIndex >= size) {
				break;
			}
			int rightIndex = leftIndex + 1;

			// Always has a left child.
			Node leftNode = nodes[leftIndex];
			float leftValue = leftNode.value;

			// May have a right child.
			Node rightNode;
			float rightValue;
			if (rightIndex >= size) {
				rightNode = null;
				rightValue = isMaxHeap ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;
			} else {
				rightNode = nodes[rightIndex];
				rightValue = rightNode.value;
			}

			// The smallest of the three values is the parent.
			if (leftValue < rightValue ^ isMaxHeap) {
				if (leftValue == value || (leftValue > value ^ isMaxHeap)) {
					break;
				}
				nodes[index] = leftNode;
				leftNode.index = index;
				index = leftIndex;
			} else {
				if (rightValue == value || (rightValue > value ^ isMaxHeap)) {
					break;
				}
				nodes[index] = rightNode;
				if (rightNode != null) {
					rightNode.index = index;
				}
				index = rightIndex;
			}
		}

		while (index > 0) {
			int parentIndex = (index - 1) >> 1;
			Node parent = nodes[parentIndex];
			if (value < parent.value ^ isMaxHeap) {
				nodes[index] = parent;
				parent.index = index;
				index = parentIndex;
			} else {
				break;
			}
		}

		nodes[index] = node;
		node.index = index;
	}

	@Override
	public boolean remove(Object o) {
		if (o instanceof Node) {
			if (--size > 0) {
				Node moved = nodes[size];
				nodes[size] = null;
				Node node = (Node) o;
				nodes[node.index] = moved;
				if (moved.value < node.value ^ isMaxHeap) {
					up(node.index);
				} else {
					down(node.index);
				}
			} else {
				nodes[0] = null;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		for (Object o : c) {
			if (!contains(o)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Exactly like {@link #containsAll(Collection)}, but takes an array instead of a Collection.
	 *
	 * @param array array to be checked for containment in this set
	 * @return {@code true} if this set contains all the elements
	 * in the specified array
	 * @see #containsAll(Collection)
	 */
	public boolean containsAll(Object[] array) {
		for (Object o : array) {
			if (!contains(o))
				return false;
		}
		return true;
	}

	/**
	 * Like {@link #containsAll(Object[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 *
	 * @param array  array to be checked for containment in this set
	 * @param offset the index of the first item in array to check
	 * @param length how many items, at most, to check from array
	 * @return {@code true} if this set contains all the elements
	 * in the specified range of array
	 * @see #containsAll(Object[])
	 */
	public boolean containsAll(Object[] array, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			if (!contains(array[i])) return false;
		}
		return true;
	}

	/**
	 * Returns true if this set contains any of the specified values.
	 *
	 * @param values must not contain nulls, and must not be null itself
	 * @return true if this set contains any of the items in {@code values}, false otherwise
	 */
	public boolean containsAnyIterable(Iterable<?> values) {
		for (Object v : values) {
			if (contains(v)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if this set contains any of the specified values.
	 *
	 * @param values must not contain nulls, and must not be null itself
	 * @return true if this set contains any of the items in {@code values}, false otherwise
	 */
	public boolean containsAny(Object[] values) {
		for (Object v : values) {
			if (contains(v)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if this set contains any items from the specified range of values.
	 *
	 * @param values must not contain nulls, and must not be null itself
	 * @param offset the index to start checking in values
	 * @param length how many items to check from values
	 * @return true if this set contains any of the items in the given range of {@code values}, false otherwise
	 */
	public boolean containsAny(Object[] values, int offset, int length) {
		for (int i = offset, n = 0; n < length && i < values.length; i++, n++) {
			if (contains(values[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes each object in {@code other} from this heap, removing an item once if it appears once, twice if it appears twice,
	 * and so on. In this respect, this acts like {@link #removeEachIterable(Iterable)} rather than Collection's removeAll().
	 *
	 * @param other collection containing elements to be removed from this collection
	 * @return true if any elements were removed, or false otherwise
	 * @see #removeEachIterable(Iterable)
	 */
	@Override
	public boolean removeAll(Collection<?> other) {
		return removeEachIterable(other);
	}

	/**
	 * Exactly like {@link #removeAll(Collection)}, but takes an array instead of a Collection.
	 * This delegates entirely to {@link #removeEach(Object[])}, and does not act like removeAll() does in other
	 * collections if there are duplicate nodes present in the heap.
	 *
	 * @param other array containing elements to be removed from this list
	 * @return {@code true} if this list changed as a result of the call
	 * @see #removeAll(Collection)
	 */
	public boolean removeAll(Object[] other) {
		return removeEach(other);
	}

	/**
	 * Like {@link #removeAll(Object[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 * This delegates entirely to {@link #removeEach(Object[], int, int)}, and does not act like removeAll() does in other
	 * collections if there are duplicate nodes present in the heap.
	 *
	 * @param array  the elements to be removed from this list
	 * @param offset the index of the first item in array to remove
	 * @param length how many items, at most, to get from array and remove from this
	 * @return {@code true} if this list changed as a result of the call
	 * @see #removeAll(Object[])
	 * @see #removeEach(Object[], int, int)
	 */
	public boolean removeAll(Object[] array, int offset, int length) {
		return removeEach(array, offset, length);
	}

	/**
	 * Removes from this ObjectList element-wise occurrences of elements contained in the specified Iterable.
	 * Note that if a value is present more than once in this ObjectList, only one of those occurrences
	 * will be removed for each occurrence of that value in {@code other}. If {@code other} has the same
	 * contents as this ObjectList or has additional items, then removing each of {@code other} will clear this.
	 *
	 * @param other an Iterable of T items to remove one-by-one, such as another ObjectList or an ObjectSet
	 * @return true if this list was modified.
	 */
	public boolean removeEachIterable(Iterable<?> other) {
		boolean changed = false;
		for (Object item : other) {
			changed |= remove(item);
		}
		return changed;
	}

	/**
	 * Exactly like {@link #removeEachIterable(Iterable)}, but takes an array instead of a Collection.
	 *
	 * @param array array containing elements to be removed from this list
	 * @return {@code true} if this list changed as a result of the call
	 * @see #removeEachIterable(Iterable)
	 */
	public boolean removeEach(Object[] array) {
		return removeEach(array, 0, array.length);
	}

	/**
	 * Like {@link #removeEach(Object[])}, but only uses at most {@code length} items from {@code array}, starting at {@code offset}.
	 *
	 * @param array  the elements to be removed from this list
	 * @param offset the index of the first item in array to remove
	 * @param length how many items, at most, to get from array and remove from this
	 * @return {@code true} if this list changed as a result of the call
	 * @see #removeEach(Object[])
	 */
	public boolean removeEach(Object[] array, int offset, int length) {
		boolean changed = false;
		for (int i = offset, n = 0; n < length && i < array.length; i++, n++) {
			changed |= remove(array[i]);
		}
		return changed;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof BinaryHeap)) {
			return false;
		}
		BinaryHeap other = (BinaryHeap) obj;
		if (other.size != size) {
			return false;
		}
		Node[] nodes1 = this.nodes, nodes2 = other.nodes;
		for (int i = 0, n = size; i < n; i++) {
			if (nodes1[i].value != nodes2[i].value) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int hashCode() {
		int h = size;
		Node[] nodes = this.nodes;
		for (int i = 0, n = size; i < n; i++) {
			h += BitConversion.floatToRawIntBits(nodes[i].value);
			h ^= h >>> 15;
		}
		return h;
	}

	@Override
	public String toString() {
		return toString(", ", true);
	}

	/**
	 * Returns a new iterator over the elements contained in this collection.
	 *
	 * @return a new iterator over the elements contained in this collection
	 */
	@Override
	public HeapIterator<T> iterator() {
		return new HeapIterator<>(this);

	}

	public static class HeapIterator<T extends Node> implements Iterator<T> {
		private final BinaryHeap<T> heap;
		private int index;

		public HeapIterator(BinaryHeap<T> binaryHeap) {
			heap = binaryHeap;
			index = 0;
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
			return index < heap.size;
		}

		/**
		 * Returns the next element in the iteration.
		 *
		 * @return the next element in the iteration
		 */
		@Override
		public T next() {
			if (index >= heap.size) {
				throw new NoSuchElementException();
			}
			return (T) heap.nodes[index++];
		}

		public void reset() {
			index = 0;
		}
	}

	/**
	 * A binary heap node. Has a float value that is used to compare this Node with others,
	 * and an int index that is used inside BinaryHeap. This class is often extended so
	 * requisite functionality can be supplied and sorted by BinaryHeap.
	 *
	 * @author Nathan Sweet
	 */
	public static class Node {
		/**
		 * The value that is used to compare this Node with others.
		 */
		public float value;
		/**
		 * Used internally by BinaryHeap; generally not modified by external code, but may need to be read.
		 */
		public int index;

		/**
		 * @param value The initial value for the node. To change the value, use {@link BinaryHeap#add(Node, float)} if the node is
		 *              not in the heap, or {@link BinaryHeap#setValue(Node, float)} if the node is in the heap.
		 */
		public Node(float value) {
			this.value = value;
		}

		public float getValue() {
			return value;
		}

		@Override
		public String toString() {
			return Float.toString(value);
		}
	}

	/**
	 * Builds a BinaryHeap with the min-heap property from the given array or varargs of items that extend {@link Node}.
	 * This is equivalent to {@link #minHeapWith(Node[])}.
	 *
	 * @param array an array or varargs of items that extend {@link Node}
	 * @param <T>   must extend {@link Node}
	 * @return a new BinaryHeap of T with the min-heap property.
	 */
	@SafeVarargs
	public static <T extends BinaryHeap.Node> BinaryHeap<T> with(T... array) {
		return new BinaryHeap<>(false, array);
	}

	/**
	 * Builds a BinaryHeap with the min-heap property from the given array or varargs of items that extend {@link Node}.
	 * This is equivalent to {@link #with(Node[])}.
	 *
	 * @param array an array or varargs of items that extend {@link Node}
	 * @param <T>   must extend {@link Node}
	 * @return a new BinaryHeap of T with the min-heap property.
	 */
	@SafeVarargs
	public static <T extends BinaryHeap.Node> BinaryHeap<T> minHeapWith(T... array) {
		return new BinaryHeap<>(false, array);
	}

	/**
	 * Builds a BinaryHeap with the max-heap property from the given array or varargs of items that extend {@link Node}.
	 *
	 * @param array an array or varargs of items that extend {@link Node}
	 * @param <T>   must extend {@link Node}
	 * @return a new BinaryHeap of T with the max-heap property.
	 */
	@SafeVarargs
	public static <T extends BinaryHeap.Node> BinaryHeap<T> maxHeapWith(T... array) {
		return new BinaryHeap<>(true, array);
	}
}
