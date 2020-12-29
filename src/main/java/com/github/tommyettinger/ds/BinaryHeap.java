/******************************************************************************
 Copyright 2011 See AUTHORS file.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.github.tommyettinger.ds;

import com.github.tommyettinger.ds.support.BitConversion;

import javax.annotation.Nullable;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A binary heap that stores nodes which each have a float value and are sorted either lowest first or highest first.
 * This can expand if its capacity is exceeded. It defaults to acting as a min-heap, sorting lowest-first.
 * The {@link Node} class can be extended to store additional information.
 * <br>
 * This isn't a direct copy from libGDX, but it's very close. It implements {@link java.util.Queue} and {@link java.util.Collection}.
 * @author Nathan Sweet
 */
@SuppressWarnings("unchecked")
public class BinaryHeap<T extends BinaryHeap.Node> extends AbstractQueue<T> {
	public int size;

	private Node[] nodes;
	private final boolean isMaxHeap;

	@Nullable
	private HeapIterator<T> iterator1 = null;
	@Nullable
	private HeapIterator<T> iterator2 = null;

	/**
	 * Constructs a BinaryHeap with 16 starting capacity, sorting lowest-first (a min-heap).
	 */
	public BinaryHeap () {
		this(16, false);
	}

	/**
	 * Constructs a BinaryHeap with the specified capacity and sorting order.
	 *
	 * @param capacity  the initial capacity
	 * @param isMaxHeap if true, this will sort highest-first; if false, it will sort lowest-first
	 */
	public BinaryHeap (int capacity, boolean isMaxHeap) {
		this.isMaxHeap = isMaxHeap;
		nodes = new Node[capacity];
	}

	/**
	 * Adds the node to the heap using its current value. The node should not already be in the heap.
	 */
	@Override
	public boolean add (T node) {
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
	 * When using a capacity-restricted queue, this method is generally
	 * preferable to {@link #add}, which can fail to insert an element only
	 * by throwing an exception.
	 *
	 * @param node the element to add
	 * @return {@code true} if the element was added to this queue, else
	 * {@code false}
	 * @throws ClassCastException       if the class of the specified element
	 *                                  prevents it from being added to this queue
	 * @throws NullPointerException     if the specified element is null and
	 *                                  this queue does not permit null elements
	 * @throws IllegalArgumentException if some property of this element
	 *                                  prevents it from being added to this queue
	 */
	@Override
	public boolean offer (T node) {
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
	 * Retrieves and removes the head of this queue, or returns {@code null} if this queue is empty.
	 * The head is the item with the lowest value (or highest value if this heap is configured as a max heap).
	 *
	 * @return the head of this queue, or {@code null} if this queue is empty
	 */
	@Override
	public T poll () {
		if(size == 0) return null;
		Node removed = nodes[0];
		if (--size > 0) {
			nodes[0] = nodes[size];
			nodes[size] = null;
			down(0);
		} else { nodes[0] = null; }
		return (T)removed;
	}

	/**
	 * Sets the node's value and adds it to the heap. The node should not already be in the heap.
	 */
	public boolean add (T node, float value) {
		node.value = value;
		return add(node);
	}

	/**
	 * Returns true if the heap contains the specified node.
	 *
	 * @param identity If true, == comparison will be used. If false, .equals() comparison will be used.
	 */
	public boolean contains (T node, boolean identity) {
		if (identity) {
			for (Node n : nodes) { if (n == node) { return true; } }
		} else {
			for (Node other : nodes) { if (other.equals(node)) { return true; } }
		}
		return false;
	}

	/**
	 * Returns the first item in the heap. This is the item with the lowest value (or highest value if this heap is configured as
	 * a max heap).
	 */
	@Override
	public T peek () {
		if (size == 0) { throw new IllegalStateException("The heap is empty."); }
		return (T)nodes[0];
	}

	/**
	 * Removes the first item in the heap and returns it. This is the item with the lowest value (or highest value if this heap is
	 * configured as a max heap). If the BinaryHeap is empty, this always returns null.
	 */
	@Nullable
	public T pop () {
		if(size == 0) return null;
		Node removed = nodes[0];
		if (--size > 0) {
			nodes[0] = nodes[size];
			nodes[size] = null;
			down(0);
		} else { nodes[0] = null; }
		return (T)removed;
	}

	/**
	 * Removes the given node and returns it. If the node is not present in this BinaryHeap or is invalid, this will
	 * probably throw an Exception.
	 *
	 * @param node a {@link Node} that should be present in this already
	 * @return {@code node} after its removal
	 */
	public T remove (T node) {
		if (--size > 0) {
			Node moved = nodes[size];
			nodes[size] = null;
			nodes[node.index] = moved;
			if (moved.value < node.value ^ isMaxHeap) { up(node.index); } else { down(node.index); }
		} else { nodes[0] = null; }
		return node;
	}

	@Override
	public int size () {
		return size;
	}

	/**
	 * Returns true if the heap has one or more items.
	 */
	public boolean notEmpty () {
		return size > 0;
	}

	/**
	 * Returns true if the heap is empty.
	 */
	@Override
	public boolean isEmpty () {
		return size == 0;
	}

	/**
	 * Removes all nodes from this BinaryHeap.
	 */
	@Override
	public void clear () {
		Arrays.fill(nodes, 0, size, null);
		size = 0;
	}

	/**
	 * Changes the value of the node, which should already be in the heap.
	 */
	public void setValue (T node, float value) {
		float oldValue = node.value;
		node.value = value;
		if (value < oldValue ^ isMaxHeap) { up(node.index); } else { down(node.index); }
	}

	private void up (int index) {
		Node[] nodes = this.nodes;
		Node node = nodes[index];
		float value = node.value;
		while (index > 0) {
			int parentIndex = (index - 1) >> 1;
			Node parent = nodes[parentIndex];
			if(node == parent)
				throw new IllegalStateException("Duplicate nodes are not allowed in a BinaryHeap.");
			if (value < parent.value ^ isMaxHeap) {
				nodes[index] = parent;
				parent.index = index;
				index = parentIndex;
			} else { break; }
		}
		nodes[index] = node;
		node.index = index;
	}

	private void down (int index) {
		Node[] nodes = this.nodes;
		int size = this.size;

		Node node = nodes[index];
		float value = node.value;

		while (true) {
			int leftIndex = 1 + (index << 1);
			if (leftIndex >= size) { break; }
			int rightIndex = leftIndex + 1;

			// Always has a left child.
			Node leftNode = nodes[leftIndex];
			float leftValue = leftNode.value;

			// May have a right child.
			Node rightNode;
			float rightValue;
			if (rightIndex >= size) {
				rightNode = null;
				rightValue = isMaxHeap ? -Float.MAX_VALUE : Float.MAX_VALUE;
			} else {
				rightNode = nodes[rightIndex];
				rightValue = rightNode.value;
			}

			// The smallest of the three values is the parent.
			if (leftValue < rightValue ^ isMaxHeap) {
				if (leftValue == value || (leftValue > value ^ isMaxHeap)) { break; }
				nodes[index] = leftNode;
				leftNode.index = index;
				index = leftIndex;
			} else {
				if (rightValue == value || (rightValue > value ^ isMaxHeap)) { break; }
				nodes[index] = rightNode;
				if (rightNode != null) { rightNode.index = index; }
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
			} else { break; }
		}

		nodes[index] = node;
		node.index = index;
	}

	@Override
	public boolean equals (Object obj) {
		if (!(obj instanceof BinaryHeap)) { return false; }
		BinaryHeap other = (BinaryHeap)obj;
		if (other.size != size) { return false; }
		Node[] nodes1 = this.nodes, nodes2 = other.nodes;
		for (int i = 0, n = size; i < n; i++) { if (nodes1[i].value != nodes2[i].value) { return false; } }
		return true;
	}

	@Override
	public int hashCode () {
		int h = 1;
		Node[] nodes = this.nodes;
		for (int i = 0, n = size; i < n; i++) {
			h += BitConversion.floatToRawIntBits(nodes[i].value);
			h ^= h >>> 15;
		}
		return h;
	}

	@Override
	public String toString () {
		if (size == 0) { return "[]"; }
		Node[] nodes = this.nodes;
		StringBuilder buffer = new StringBuilder(32);
		buffer.append('[');
		buffer.append(nodes[0].value);
		for (int i = 1; i < size; i++) {
			buffer.append(", ");
			buffer.append(nodes[i].value);
		}
		buffer.append(']');
		return buffer.toString();
	}


	/**
	 * Returns an iterator over the elements contained in this collection.
	 *
	 * @return an iterator over the elements contained in this collection
	 */
	@Override
	public Iterator<T> iterator () {
		if (iterator1 == null || iterator2 == null) {
			iterator1 = new HeapIterator<>(this);
			iterator2 = new HeapIterator<>(this);
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

	public static class HeapIterator<T extends Node> implements Iterator<T> {
		private final BinaryHeap<T> heap;
		private int index;
		private boolean valid = true;
		public HeapIterator(BinaryHeap<T> binaryHeap){
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
		public boolean hasNext () {
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			return index < heap.size;
		}

		/**
		 * Returns the next element in the iteration.
		 *
		 * @return the next element in the iteration
		 */
		@Override
		public T next () {
			if (!valid) { throw new RuntimeException("#iterator() cannot be used nested."); }
			if (index >= heap.size) { throw new NoSuchElementException(); }
			return (T)heap.nodes[index++];
		}
		public void reset(){
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
		public float value;
		public int index;

		/**
		 * @param value The initial value for the node. To change the value, use {@link BinaryHeap#add(Node, float)} if the node is
		 *              not in the heap, or {@link BinaryHeap#setValue(Node, float)} if the node is in the heap.
		 */
		public Node (float value) {
			this.value = value;
		}

		public float getValue () {
			return value;
		}

		@Override
		public String toString () {
			return Float.toString(value);
		}
	}
}
