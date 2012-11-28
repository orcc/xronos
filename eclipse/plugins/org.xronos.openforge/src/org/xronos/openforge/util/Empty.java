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
package org.xronos.openforge.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Reusable, immutable, empty collection objects.
 * 
 * @version $Id: Empty.java 2 2005-06-09 20:00:48Z imiller $
 * @author Stephen Edwards
 */
public interface Empty {

	/**
	 * Empty Set object. Throws UnsupportedOperationException if an attempt is
	 * made to modify it.
	 */
	public static final Set<?> SET = Collections.EMPTY_SET;

	/**
	 * Empty List object. Throws UnsupportedOperationException if an attempt is
	 * made to modify it.
	 */
	public static final List<?> LIST = Collections.EMPTY_LIST;

	/**
	 * Empty Collection object. Throws UnsupportedOperationException if an
	 * attempt is made to modify it.
	 */
	public static final Collection<?> COLLECTION = LIST;

	/**
	 * Empty Map object. Throws UnsupportedOperationException if an attempt is
	 * made to modify it.
	 */
	public static final Map<?, ?> MAP = Collections.EMPTY_MAP;

	/** Empty array of booleans. */
	public static final boolean[] BOOLEAN_ARRAY = new boolean[0];

	/** Empty array of bytes. */
	public static final byte[] BYTE_ARRAY = new byte[0];

	/** Empty array of characters. */
	public static final char[] CHAR_ARRAY = new char[0];

	/** Empty array of shorts. */
	public static final short[] SHORT_ARRAY = new short[0];

	/** Empty array of ints. */
	public static final int[] INT_ARRAY = new int[0];

	/** Empty array of longs. */
	public static final long[] LONG_ARRAY = new long[0];

	/** Empty array of floats. */
	public static final float[] FLOAT_ARRAY = new float[0];

	/** Empty array of doubles. */
	public static final double[] DOUBLE_ARRAY = new double[0];

	/** Empty array of Objects. */
	public static final Object[] OBJECT_ARRAY = new Object[0];

}
