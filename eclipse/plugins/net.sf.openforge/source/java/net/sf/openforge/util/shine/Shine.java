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

package net.sf.openforge.util.shine;

import java.util.ArrayList;

import javax.swing.UIManager;

/**
 * Shine a light on those bugs!
 * 
 * Let's you walk around an object hierarchy.
 */
public class Shine {
	static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

	/**
	 * Default shine invocation. Given a tag and an object, displays a tree for
	 * the object hierarchy. Execution of debugee is paused until this window
	 * (and all its created windows) are closed or cleared.
	 * 
	 * @param tag
	 *            ID Tag for this group of windows
	 * @param o
	 *            root object for the object hierarchy to browse
	 */
	public static void shine(String tag, Object o) {
		shine(tag, o, true, true);
	}

	/**
	 * Given a tag and an object, displays a treefor the object hierarchy. Does
	 * not pause program execution.
	 * 
	 * @param tag
	 *            ID Tag for this group of windows
	 * @param o
	 *            root object for the object hierarchy to browse
	 */
	public static void shineNoWait(String tag, Object o) {
		shine(tag, o, false, true);
	}

	/**
	 * Given a tag and an object, displays a table broswer for the object
	 * hierarchy. Execution of debugee is paused until this window (and all its
	 * created windows) are closed or cleared.
	 * 
	 * @param tag
	 *            ID Tag for this group of windows
	 * @param o
	 *            root object for the object hierarchy to browse
	 */
	public static void shineTable(String tag, Object o) {
		shine(tag, o, true, false);
	}

	/**
	 * Given a tag and an object, displays a table browser for the object
	 * hierarchy. Execution of debugee is NOT paused.
	 * 
	 * @param tag
	 *            ID Tag for this group of windows
	 * @param o
	 *            root object for the object hierarchy to browse
	 */
	public static void shineTableNoWait(String tag, Object o) {
		shine(tag, o, false, false);
	}

	public static void shine(Object o) {
		shine(null, o, true, true);
	}

	public static void shineNoWait(Object o) {
		shine(null, o, false, true);
	}

	public static void shineTable(Object o) {
		shine(null, o, true, false);
	}

	public static void shineTableNoWait(Object o) {
		shine(null, o, false, false);
	}

	/**
	 * This is the generi entry for creating a shine.
	 * 
	 * @param tag
	 *            ID Tag for this group of windows
	 * @param o
	 *            Root object to examine
	 * @param wait
	 *            do we wait?
	 * @param tree
	 *            tree or table? true if tree
	 */
	public static void shine(String tag, Object o, boolean wait, boolean tree) {
		ShineTracker bbt = new ShineTracker();
		if (tag == null)
			tag = o.getClass().getName();
		if (tree)
			new ShineTreeFrame(bbt, tag, o, wait);
		else
			new ShineTableFrame(bbt, tag, o, wait);
		if (wait) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException ie) {
			}
			for (; bbt.getCounter() != 0;) {
				try {
					Thread.sleep(250);
				} catch (InterruptedException ie) {
				}
			}
		}
	}

	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager
					.getCrossPlatformLookAndFeelClassName());
		} catch (Exception e) {
		}

		Integer[] iarr1 = { new Integer(1), new Integer(2) };
		Integer[] iarr2 = { new Integer(3), new Integer(4) };
		Integer[][] i2arr = { iarr1, null, iarr2 };

		ArrayList<Integer[][]> al = new ArrayList<Integer[][]>();
		al.add(i2arr);
		shine("2d", al);
		shine("Test - NOWAIT", new Integer(10), false, false);
		shine("Test - WAIT", new Integer(10));
		System.exit(0);
	}
}
