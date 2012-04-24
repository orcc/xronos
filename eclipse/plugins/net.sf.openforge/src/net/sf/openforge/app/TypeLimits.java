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

package net.sf.openforge.app;

/**
 * Classname is...
 * 
 * @version $Id: TypeLimits.java 2 2005-06-09 20:00:48Z imiller $
 */
public class TypeLimits {

	public static class C {
		public static int getPointerSize() {
			return 32;
		}

		public static int getWCharSize() {
			return 16;
		}

		public static int getUCharSize() {
			return 8;
		}

		public static int getUShortSize() {
			return 16;
		}

		public static int getUIntSize() {
			return 32;
		}

		public static int getULongSize() {
			return 32;
		}

		public static int getULongLongSize() {
			return 64;
		}

		public static int getCharSize() {
			return 8;
		}

		public static int getShortSize() {
			return 16;
		}

		public static int getIntSize() {
			return 32;
		}

		public static int getLongSize() {
			return 32;
		}

		public static int getLongLongSize() {
			return 64;
		}

		public static int getFloatSize() {
			return 32;
		}

		public static int getDoubleSize() {
			return 64;
		}

		public static int getLongDoubleSize() {
			return 64;
		}
	}

	public static class Java {
		public static int getObjectReferenceSize() {
			return 32;
		}

		public static int getBooleanSize() {
			return 1;
		}

		public static int getByteSize() {
			return 8;
		}

		public static int getShortSize() {
			return 16;
		}

		public static int getIntSize() {
			return 32;
		}

		public static int getLongSize() {
			return 64;
		}

		public static int getFloatSize() {
			return 32;
		}

		public static int getDoubleSize() {
			return 64;
		}

		public static int getCharSize() {
			return 16;
		}
	}

}
