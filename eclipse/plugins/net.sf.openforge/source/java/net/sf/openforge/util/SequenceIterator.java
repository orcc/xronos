/*******************************************************************************
 * Copyright 2002-2009  Xilinx Inc.
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
 ******************************************************************************/
/*
 * 
 *
 * 
 */

package net.sf.openforge.util;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A <code>SequenceIterator</code> represents the logical concatentation of
 * other {@link Iterator Iterators}. When the elements of one iterator have been
 * exhausted, iteration proceeds with the next iterator, until all iterators
 * have been exhausted.
 * 
 * @version $Id: SequenceIterator.java 2 2005-06-09 20:00:48Z imiller $
 */
public class SequenceIterator implements Iterator {

	/** Sequence of Iterators used to construct this iterator */
	private LinkedList iterators = new LinkedList();

	/** The current iterator being traversed */
	private Iterator currentIterator = Collections.EMPTY_LIST.iterator();

	/**
	 * Constructs a new <code>SequenceIterator</code> with two {@link Iterator
	 * Iterators} to be traversed in sequence.
	 * 
	 * @param iter1
	 *            the first iterator to be traversed
	 * @param iter2
	 *            the second iterator to be traversed
	 * @throws NullPointerException
	 *             if <code>iter1</code> or <code>iter2</code> is null
	 */
	public SequenceIterator(Iterator iter1, Iterator iter2) {
		if (iter1 == null || iter2 == null) {
			throw new NullPointerException("null argument");
		}

		this.iterators.add(iter1);
		this.iterators.add(iter2);
		advanceToNext();
	}

	/**
	 * Constructs a new <code>SequenceIterator</code> with a list of
	 * {@link Iterator Iterators} to be traversed in sequence.
	 * 
	 * @param iterators
	 *            a list of {@link Iterator}
	 * @throws NullPointerException
	 *             if <code>iterators</code> or any of its elements is null
	 * @throws ClassCastException
	 *             if any element of <code>iterators</code> is not an instance
	 *             of {@link Iterator}
	 */
	public SequenceIterator(List iterators) {
		for (Iterator iter = iterators.iterator(); iter.hasNext();) {
			final Iterator nextIterator = (Iterator) iter.next();
			if (nextIterator == null) {
				throw new NullPointerException("null iterator");
			}
			this.iterators.add(nextIterator);
		}
		advanceToNext();
	}

	/**
	 * Returns <code>true</code> if the iteration has more elements. (In other
	 * words, returns <code>true</code> if {@link #next()} would return an
	 * element rather than throwing an exception).
	 * 
	 * @return <code>true</code> if the iterator has more elements
	 */
	public boolean hasNext() {
		return currentIterator.hasNext();
	}

	/**
	 * Returns the next element in the iteration.
	 * 
	 * @return the next element in the iteration
	 * @throws NoSuchElementException
	 *             if the iteration has no more elements
	 */
	public Object next() {
		final Object nextObject = currentIterator.next();
		advanceToNext();
		return nextObject;
	}

	/**
	 * An unsupported optional method.
	 * 
	 * @throws UnsupportedOperationException
	 *             always
	 */
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Advances <code>currentIterator</code> to the next element to be returned.
	 */
	private void advanceToNext() {
		while (!currentIterator.hasNext() && !iterators.isEmpty()) {
			currentIterator = (Iterator) iterators.removeFirst();
			if (currentIterator.hasNext()) {
				break;
			}
		}
	}
}
