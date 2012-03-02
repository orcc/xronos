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

@SuppressWarnings("serial")
class ShineTreeFrame extends JFrame {
	private FieldTreeComponent fTreeComponent;
	private ObjectInspector oi;
	private String tag;
	private boolean wait;
	private boolean cloned;
	private ShineTracker tracker;
	private JButton clearWaitButton;
	@SuppressWarnings("unused")
	private JButton cloneButton;

	ShineTreeFrame(ShineTracker tracker, String tag, Object o, boolean wait) {
		this(tracker, null, tag, o, wait, false);
	}

	ShineTreeFrame(ShineTracker tracker, Point lastLocation, String tag,
			Object o, boolean wait, boolean cloned) {
		super(tag + " <" + tracker.getNextID() + ">");

		tracker.inc();

		if (lastLocation != null)
			setLocation(lastLocation.x + 25, lastLocation.y + 25);

		this.wait = wait;
		this.tracker = tracker;
		this.tag = tag;
		this.oi = new ObjectInspector(o);

		// add toolbar
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(createToolBar(), BorderLayout.NORTH);

		// field tree
		fTreeComponent = new FieldTreeComponent(this, oi);

		// add them together
		JPanel sub1 = new JPanel(new BorderLayout());
		sub1.add(fTreeComponent, BorderLayout.CENTER);

		// add sub1 to content pane
		getContentPane().add(sub1, BorderLayout.CENTER);

		// Finish setting up the frame, and show it.
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				ShineTreeFrame.this.tracker.dec();
			}
		});

		this.pack();
		updateData();
		this.setVisible(true);
	}

	Component createToolBar() {
		JToolBar tbar = new JToolBar();
		tbar.setFloatable(false);

		JButton b;

		b = new JButton("New Tree");
		b.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				newCurrentObject(true);
			}
		});
		tbar.add(b);

		b = new JButton("New Table");
		b.addActionListener(new ActionListener() {
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
			public void actionPerformed(ActionEvent e) {
				clearTracker();
			}
		});
		tbar.add(clearWaitButton);

		b = new JButton("Close");
		b.addActionListener(new ActionListener() {
			@SuppressWarnings("deprecation")
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

	void updateData() {
		clearWaitButton.setEnabled(wait);
	}

	void clearTracker() {
		if (wait) {
			tracker.dec();
			wait = false;
			updateData();
		}
	}

	@SuppressWarnings("rawtypes")
	void newCurrentObject(boolean makeTree) {
		Object o = fTreeComponent.getSelectedObject();
		if (o != null) {
			if (makeTree) {
				// need to be more clever
				// if something is select, create
				new ShineTreeFrame(tracker, getLocationOnScreen(), tag, o,
						wait, cloned);
			} else {
				new ShineTableFrame(tracker, getLocationOnScreen(), tag,
						new Stack(), o, wait, cloned);
			}
		}
	}


	class Sep extends JComponent {
		private int topleftGap;
		private int botrightGap;
		private int thickness;
		private boolean etchIn;

		public Sep(int thickness, boolean etchIn) {
			this(thickness, 0, 0, etchIn);
		}

		public Sep(int thickness, int gap, boolean etchIn) {
			this(thickness, gap, gap, etchIn);
		}

		public Sep(int thickness, int topleftGap, int botrightGap,
				boolean etchIn) {
			this.thickness = thickness;
			this.topleftGap = topleftGap;
			this.botrightGap = botrightGap;
			this.etchIn = etchIn;
		}

		public Dimension getPreferredSize() {
			return new Dimension(thickness + topleftGap + botrightGap,
					topleftGap + botrightGap + thickness);
		}

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
