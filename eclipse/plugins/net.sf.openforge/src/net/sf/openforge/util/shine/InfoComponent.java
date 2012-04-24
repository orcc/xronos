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
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

@SuppressWarnings("serial")
class InfoComponent extends JPanel {
	private ObjectInspector oi;
	@SuppressWarnings("unused")
	private ShineTableFrame sFrame;
	private JLabel id;
	private JLabel hash;
	private JLabel type;
	private JLabel value;
	@SuppressWarnings("unused")
	private JLabel modifiers;

	InfoComponent(ShineTableFrame sFrame, ObjectInspector oi) {
		this.oi = oi;
		this.sFrame = sFrame;

		setLayout(new BorderLayout());

		Box left = new Box(BoxLayout.Y_AXIS);

		JLabel l = new JLabel("ID:", SwingConstants.RIGHT);
		l.setAlignmentX(Component.RIGHT_ALIGNMENT);
		left.add(l);

		left.add(Box.createVerticalGlue());

		l = new JLabel("Type:", SwingConstants.RIGHT);
		l.setAlignmentX(Component.RIGHT_ALIGNMENT);
		left.add(l);

		left.add(Box.createVerticalGlue());

		l = new JLabel("Value:", SwingConstants.RIGHT);
		l.setAlignmentX(Component.RIGHT_ALIGNMENT);
		left.add(l);

		Box right = new Box(BoxLayout.Y_AXIS);

		// id crap
		Box temp = new Box(BoxLayout.X_AXIS);

		id = new JLabel();
		temp.add(id);
		temp.add(Box.createHorizontalStrut(25));

		l = new JLabel("Hash:", SwingConstants.RIGHT);
		temp.add(l);
		temp.add(Box.createHorizontalStrut(5));

		hash = new JLabel();
		temp.add(hash);
		temp.add(Box.createHorizontalGlue());

		right.add(temp);

		// add spacing
		right.add(Box.createVerticalGlue());

		temp = new Box(BoxLayout.X_AXIS);
		type = new JLabel();
		temp.add(type);
		temp.add(Box.createHorizontalGlue());
		right.add(temp);

		right.add(Box.createVerticalGlue());

		temp = new Box(BoxLayout.X_AXIS);
		value = new JLabel();
		temp.add(value);
		temp.add(Box.createHorizontalGlue());
		right.add(temp);

		JPanel borders = new JPanel(new BorderLayout());
		borders.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
		borders.add(right, BorderLayout.CENTER);

		add(left, BorderLayout.WEST);
		add(borders, BorderLayout.CENTER);
		updateData();
	}

	void updateData() {
		id.setText(Integer.toString(System.identityHashCode(oi.getMyObject())));
		hash.setText(Integer.toString(oi.getMyObject().hashCode()));
		type.setText(oi.getMyObject().getClass().getName());
		value.setText(oi.getMyObject().toString());
	}
}
