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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;

/**
 * Description of the Class
 * 
 * @author cschanck created July 24, 2002
 */
@SuppressWarnings("serial")
class ShineTableFrame extends JFrame {
	private FieldTableComponent fTableComponent;
	private InfoComponent infoComponent;
	private ObjectInspector oi;
	private String tag;
	private Stack<Object> history;
	private boolean wait;
	private boolean cloned;
	// private boolean tree;
	private ShineTracker tracker;
	private JButton clearWaitButton;

	// private JButton cloneButton;

	/**
	 * Constructor for the ShineTableFrame object
	 * 
	 * @param tracker
	 *            Description of the Parameter
	 * @param tag
	 *            Description of the Parameter
	 * @param o
	 *            Description of the Parameter
	 * @param wait
	 *            Description of the Parameter
	 */
	ShineTableFrame(ShineTracker tracker, String tag, Object o, boolean wait) {
		this(tracker, null, tag, new Stack<Object>(), o, wait, false);
	}

	/**
	 * Constructor for the ShineTableFrame object
	 * 
	 * @param tracker
	 *            Description of the Parameter
	 * @param lastLocation
	 *            Description of the Parameter
	 * @param tag
	 *            Description of the Parameter
	 * @param history
	 *            Description of the Parameter
	 * @param o
	 *            Description of the Parameter
	 * @param wait
	 *            Description of the Parameter
	 * @param cloned
	 *            Description of the Parameter
	 */
	ShineTableFrame(ShineTracker tracker, Point lastLocation, String tag,
			Stack<Object> history, Object o, boolean wait, boolean cloned) {
		super(tag + " <" + tracker.getNextID() + ">");

		tracker.inc();

		if (lastLocation != null) {
			setLocation(lastLocation.x + 25, lastLocation.y + 25);
		}

		this.wait = wait;
		this.tracker = tracker;
		this.tag = tag;
		this.history = history;
		oi = new ObjectInspector(o);

		// add toolbar
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(createToolBar(), BorderLayout.NORTH);

		// field table
		fTableComponent = new FieldTableComponent(this, oi);

		// info component
		infoComponent = new InfoComponent(this, oi);

		// add them together
		JPanel sub1 = new JPanel(new BorderLayout());
		sub1.add(infoComponent, BorderLayout.NORTH);
		sub1.add(fTableComponent, BorderLayout.CENTER);

		// add sub1 to content pane
		getContentPane().add(sub1, BorderLayout.CENTER);

		// Finish setting up the frame, and show it.
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				ShineTableFrame.this.tracker.dec();
			}
		});

		pack();
		updateData();
		setVisible(true);
	}

	/**
	 * Description of the Method
	 * 
	 * @return Description of the Return Value
	 */
	Component createToolBar() {
		JToolBar tbar = new JToolBar();
		tbar.setFloatable(false);

		JButton b;
		b = new JButton("1st");
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goFirstObject();
			}
		});
		tbar.add(b);

		b = new JButton("Prev");
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goPreviousObject();
			}
		});
		tbar.add(b);

		b = new JButton("Goto");
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				goSelectedObject();
			}
		});
		tbar.add(b);

		b = new JButton("New Table");
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				newCurrentObject(true);
			}
		});
		tbar.add(b);

		b = new JButton("New Tree");
		b.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				newCurrentObject(false);
			}
		});
		tbar.add(b);

		Box temp = new Box(BoxLayout.X_AXIS);

		temp.add(tbar);
		temp.add(Box.createHorizontalGlue());

		tbar = new JToolBar();
		tbar.setFloatable(false);

		clearWaitButton = new JButton("Clear Wait");
		clearWaitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				clearTracker();
			}
		});
		tbar.add(clearWaitButton);

		b = new JButton("Close");
		b.addActionListener(new ActionListener() {
			@SuppressWarnings("deprecation")
			@Override
			public void actionPerformed(ActionEvent e) {
				clearTracker();
				hide();
				dispose();
			}
		});
		tbar.add(b);

		temp.add(tbar);

		JPanel withSep = new JPanel(new BorderLayout());

		withSep.add(temp, BorderLayout.CENTER);
		withSep.add(new Sep(1, 0, false), BorderLayout.SOUTH);

		return withSep;
	}

	/** Description of the Method */
	void updateData() {
		fTableComponent.updateTableData();
		infoComponent.updateData();
		clearWaitButton.setEnabled(wait);
	}

	/** Description of the Method */
	void clearTracker() {
		if (wait) {
			tracker.dec();
			wait = false;
			updateData();
		}
	}

	/** Description of the Method */
	void goFirstObject() {
		if (!history.empty()) {
			Object go = null;
			while (!history.empty()) {
				go = history.pop();
			}
			oi.reuse(go);
			updateData();
		}
	}

	/** Description of the Method */
	void goPreviousObject() {
		if (!history.empty()) {
			Object o = history.pop();
			oi.reuse(o);
			updateData();
		}
	}

	/** Description of the Method */
	void goSelectedObject() {
		int next = fTableComponent.getSelectedIndex();
		if ((next >= 0) && (next < oi.getCount())) {
			if ((oi.isRef(next)) && (oi.getValue(next) != null)) {
				history.push(oi.getMyObject());
				oi.reuse(oi.getValue(next));
				updateData();
			}
		}
	}

	/**
	 * Description of the Method
	 * 
	 * @param makeTable
	 *            Description of the Parameter
	 */
	void newCurrentObject(boolean makeTable) {
		if (makeTable) {
			Stack<Object> newStack = (Stack<Object>) history.clone();
			new ShineTableFrame(tracker, getLocationOnScreen(), tag, newStack,
					oi.getMyObject(), wait, cloned);
		} else {
			new ShineTreeFrame(tracker, getLocationOnScreen(), tag,
					oi.getMyObject(), wait, cloned);

		}
	}

	/** Description of the Method */
	void openSelectedObject() {
		int next = fTableComponent.getSelectedIndex();
		if ((next >= 0) && (next < oi.getCount())) {
			if ((oi.isRef(next)) && (oi.getValue(next) != null)) {
				Stack<Object> newStack = (Stack<Object>) history.clone();
				newStack.push(oi.getMyObject());
				new ShineTableFrame(tracker, getLocationOnScreen(), tag,
						newStack, oi.getValue(next), wait, cloned);
			}
		}
	}

	/*
	 * void cloneCurrentObject() { / can't clone a cloned ... / not so tough.
	 * add the current objec to our stack history.push(oi.getMyObject()); / now
	 * clone it. try { Stack newStack=(Stack)cloneWithEngine(history); / get rid
	 * of the prior object history.pop(); / get the new current object Object
	 * o=newStack.pop(); / create the new frame new ShineTableFrame(tracker,
	 * getLocationOnScreen(), tag+" [cloned]", newStack, o, false, true); }
	 * catch(GraphIOException gie) { System.out.println(gie); } } private static
	 * GraphClone cloneEngine=new GraphClone(); private static synchronized
	 * Object cloneWithEngine(Object o) throws GraphIOException { return
	 * cloneEngine.cloneGraph(o); }
	 */
	class Sep extends JComponent {
		private int topleftGap;
		private int botrightGap;
		private int thickness;
		private boolean etchIn;

		/**
		 * Constructor for the Sep object
		 * 
		 * @param thickness
		 *            Description of the Parameter
		 * @param etchIn
		 *            Description of the Parameter
		 */
		public Sep(int thickness, boolean etchIn) {
			this(thickness, 0, 0, etchIn);
		}

		/**
		 * Constructor for the Sep object
		 * 
		 * @param thickness
		 *            Description of the Parameter
		 * @param gap
		 *            Description of the Parameter
		 * @param etchIn
		 *            Description of the Parameter
		 */
		public Sep(int thickness, int gap, boolean etchIn) {
			this(thickness, gap, gap, etchIn);
		}

		/**
		 * Constructor for the Sep object
		 * 
		 * @param thickness
		 *            Description of the Parameter
		 * @param topleftGap
		 *            Description of the Parameter
		 * @param botrightGap
		 *            Description of the Parameter
		 * @param etchIn
		 *            Description of the Parameter
		 */
		public Sep(int thickness, int topleftGap, int botrightGap,
				boolean etchIn) {
			this.thickness = thickness;
			this.topleftGap = topleftGap;
			this.botrightGap = botrightGap;
			this.etchIn = etchIn;
		}

		@Override
		public Dimension getPreferredSize() {
			return new Dimension(thickness + topleftGap + botrightGap,
					topleftGap + botrightGap + thickness);
		}

		@Override
		public void paint(Graphics g) {
			Dimension d = getSize();
			Color from = etchIn ? SystemColor.controlDkShadow
					: SystemColor.controlHighlight;
			Color to = etchIn ? SystemColor.controlHighlight
					: SystemColor.controlDkShadow;

			if (d.width > d.height) {
				// horizontal
				g.setColor(from);
				int half = thickness / 2;
				for (int i = 0; i < half; i++) {
					g.drawLine(0, i + topleftGap, d.width, i + topleftGap);
				}
				g.setColor(to);
				for (int i = half; i < thickness; i++) {
					g.drawLine(0, i + topleftGap, d.width, i + topleftGap);
				}
			} else {
				// vertical
				g.setColor(from);
				int half = thickness / 2;
				for (int i = 0; i < half; i++) {
					g.drawLine(i + botrightGap, 0, i + botrightGap, d.height);
				}
				g.setColor(to);
				for (int i = half; i < thickness; i++) {
					g.drawLine(i + botrightGap, 0, i + botrightGap, d.height);
				}
			}
			super.paint(g);
		}
	}
}
