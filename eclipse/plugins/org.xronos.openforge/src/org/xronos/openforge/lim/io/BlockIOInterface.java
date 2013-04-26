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

package org.xronos.openforge.lim.io;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.xronos.openforge.app.EngineThread;

/**
 * BlockIOInterface holds information about all block io interfaces
 * 
 * @author Jim Jensen & Ian Miller
 * @version $Id: BlockIOInterface.java 129 2006-04-05 13:40:46Z imiller $
 */
public class BlockIOInterface {

	/** map of String function name to List of BlockDescriptors */
	static private HashMap<String, List<BlockDescriptor>> functionToDescriptors = new HashMap<String, List<BlockDescriptor>>();

	/**
	 * add a BlockDescriptor
	 */
	public static void addBlockDescriptor(String functionName,
			BlockDescriptor bd) {
		List<BlockDescriptor> l = functionToDescriptors.get(functionName);
		if (l == null) {
			l = new ArrayList<BlockDescriptor>();
			functionToDescriptors.put(functionName, l);
		}
		l.add(bd);
	}

	/**
	 * clear all known functions
	 */
	public static void clear() {
		functionToDescriptors.clear();
	}

	// sample population of a BlockIODescriptor
	// for C source (courtesy of Ian):
	//
	// struct foo
	// {
	// int z[3];
	// int x,y;
	// };
	//
	// int boo (struct foo *x, short a, long long b)
	// {
	// int temp = x->z[0] + x->z[1] + x->z[2];
	// x->z[0] = -1;
	// return temp + x->x + x->y + a + b;
	// }
	//
	public static void generateTest(final int fifoWidth) {
		// currently fifo width must be 4 for this test generator
		if (fifoWidth != 4) {
			EngineThread.getEngine().fatalError("fifo width must be 4");
		}

		// first the input block... 3 elements
		// takes 5 cycles to transfer struct foo...
		// 2 cycles to transfer short a
		// 2 cycles to transfer long long b
		BlockIOInterface.addBlockDescriptor("boo", new BlockDescriptor() {
			@Override
			public BlockElement[] getBlockElements() {
				return new BlockElement[] { new BlockElement() {
					@Override
					public void deleteStreamBytes(int start, int length) {
					}

					@Override
					public int getAllocatedSize() {
						return 5;
					}

					@Override
					public BlockDescriptor getBlockDescriptor() {
						return getThis();
					}

					@Override
					public DeclarationGenerator getDeclaredType() {
						return new tempDeclGen("struct foo *");
					}

					@Override
					public String getFormalName() {
						return "x";
					}

					@Override
					public byte[] getStreamFormat() {
						return new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
								1, 1, 1, 1, 1, 1, 1, 1 };
					}
				}, new BlockElement() {
					@Override
					public void deleteStreamBytes(int start, int length) {
					}

					@Override
					public int getAllocatedSize() {
						return 2;
					}

					@Override
					public BlockDescriptor getBlockDescriptor() {
						return getThis();
					}

					@Override
					public DeclarationGenerator getDeclaredType() {
						return new tempDeclGen("short");
					}

					@Override
					public String getFormalName() {
						return "a";
					}

					@Override
					public byte[] getStreamFormat() {
						return new byte[] { 1, 0, 0, 0, 1, 0, 0, 0 };
					}
				}, new BlockElement() {
					@Override
					public void deleteStreamBytes(int start, int length) {
					}

					@Override
					public int getAllocatedSize() {
						return 8;
					}

					@Override
					public BlockDescriptor getBlockDescriptor() {
						return getThis();
					}

					@Override
					public DeclarationGenerator getDeclaredType() {
						return new tempDeclGen("long long");
					}

					@Override
					public String getFormalName() {
						return "b";
					}

					@Override
					public byte[] getStreamFormat() {
						return new byte[] { 1, 1, 1, 1, 1, 1, 1, 1 };
					}
				} };
			}

			@Override
			public int[] getBlockOrganization() {
				return new int[] { 0, 0, 0, 0, 0, 1, 2, 2 };
			}

			@Override
			public int getByteWidth() {
				return fifoWidth;
			}

			@Override
			public DeclarationGenerator getFunctionDeclaredType() {
				return new tempDeclGen("int");
			}

			@Override
			public String getFunctionName() {
				return "boo";
			}

			@Override
			public String getInterfaceID() {
				return "0";
			}

			private BlockDescriptor getThis() {
				return this;
			} // Hack to get 'this'

			@Override
			public boolean isSlave() {
				return true;
			}
		}); // end of addBlockDescriptor

		// then the output block... 4 elements out
		BlockIOInterface.addBlockDescriptor("boo", new BlockDescriptor() {
			@Override
			public BlockElement[] getBlockElements() {
				return new BlockElement[] { new BlockElement() {
					@Override
					public void deleteStreamBytes(int start, int length) {
					}

					@Override
					public int getAllocatedSize() {
						return 5;
					}

					@Override
					public BlockDescriptor getBlockDescriptor() {
						return getThis();
					}

					@Override
					public DeclarationGenerator getDeclaredType() {
						return new tempDeclGen("struct foo *");
					}

					@Override
					public String getFormalName() {
						return "x";
					}

					@Override
					public byte[] getStreamFormat() {
						return new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
								1, 1, 1, 1, 1, 1, 1, 1 };
					}
				}, new BlockElement() {
					@Override
					public void deleteStreamBytes(int start, int length) {
					}

					@Override
					public int getAllocatedSize() {
						return 2;
					}

					@Override
					public BlockDescriptor getBlockDescriptor() {
						return getThis();
					}

					@Override
					public DeclarationGenerator getDeclaredType() {
						return new tempDeclGen("short");
					}

					@Override
					public String getFormalName() {
						return "a";
					}

					@Override
					public byte[] getStreamFormat() {
						return new byte[] { 1, 0, 0, 0, 1, 0, 0, 0 };
					}
				}, new BlockElement() {
					@Override
					public void deleteStreamBytes(int start, int length) {
					}

					@Override
					public int getAllocatedSize() {
						return 8;
					}

					@Override
					public BlockDescriptor getBlockDescriptor() {
						return getThis();
					}

					@Override
					public DeclarationGenerator getDeclaredType() {
						return new tempDeclGen("long long");
					}

					@Override
					public String getFormalName() {
						return "b";
					}

					@Override
					public byte[] getStreamFormat() {
						return new byte[] { 1, 1, 1, 1, 1, 1, 1, 1 };
					}
				}, new BlockElement() {
					@Override
					public void deleteStreamBytes(int start, int length) {
					}

					@Override
					public int getAllocatedSize() {
						return 4;
					}

					@Override
					public BlockDescriptor getBlockDescriptor() {
						return getThis();
					}

					@Override
					public DeclarationGenerator getDeclaredType() {
						return new tempDeclGen("int");
					}

					@Override
					public String getFormalName() {
						return "return";
					}

					@Override
					public byte[] getStreamFormat() {
						return new byte[] { 1, 1, 1, 1 };
					}
				} };
			}

			@Override
			public int[] getBlockOrganization() {
				return new int[] { 0, 0, 0, 0, 0, 1, 2, 2, 3 };
			}

			@Override
			public int getByteWidth() {
				return fifoWidth;
			}

			@Override
			public DeclarationGenerator getFunctionDeclaredType() {
				return new tempDeclGen("int");
			}

			@Override
			public String getFunctionName() {
				return "boo";
			}

			@Override
			public String getInterfaceID() {
				return "1";
			}

			private BlockDescriptor getThis() {
				return this;
			} // Hack to get 'this'

			@Override
			public boolean isSlave() {
				return false;
			}
		});// end of addBlockDescriptor
	}

	/**
	 * Returns a list of BlockDescriptors for the specific function passed in
	 * (names are available from getFunctionNames)
	 */
	public static List<BlockDescriptor> getDescriptors(String functionName) {
		List<BlockDescriptor> l = functionToDescriptors.get(functionName);
		if (l == null) {
			return Collections.emptyList();
		}

		return Collections.unmodifiableList(l);
	}

	/**
	 * Returns the names of the entry functions
	 */
	public static Set<String> getFunctionNames() {
		return functionToDescriptors.keySet();
	}

}

class tempDeclGen implements DeclarationGenerator {
	private String type;

	tempDeclGen(String type) {
		this.type = type;
	}

	@Override
	public String getDeclaration(String id) {
		return type + " " + id;
	}

}
