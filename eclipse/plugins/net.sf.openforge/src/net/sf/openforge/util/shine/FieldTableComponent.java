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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.reflect.Array;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

@SuppressWarnings("serial")
class FieldTableComponent extends JScrollPane {
	@SuppressWarnings("unused")
	private ObjectInspector oi;
	private ShineTableFrame sFrame;
	private MyTableModel myModel;
	private JTable table;
	private final static int ARRAY_GLOM = 25;

	FieldTableComponent(ShineTableFrame sFrame, ObjectInspector oi) {
		this.oi = oi;
		this.sFrame = sFrame;

		myModel = new MyTableModel(oi);
		table = new JTable(myModel);
		table.setFont(new Font("Monospaced", Font.PLAIN, 12));
		table.setPreferredScrollableViewportSize(new Dimension(700, 200));

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

		table.addMouseListener(new MouseAdapter() {
			// double click opens it, right click opens new window
			@Override
			public void mouseClicked(MouseEvent me) {
				if (((me.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)
						&& (me.getClickCount() == 2)) {
					goSelectedObject();
				} else if (((me.getModifiers() & MouseEvent.BUTTON3_MASK) != 0)
						&& (me.getClickCount() == 1)) {
					int row = table.rowAtPoint(me.getPoint());
					table.getSelectionModel().setSelectionInterval(row, row);
					openSelectedObject();
				}
			}
		});

		setViewportView(table);

		// Set up column sizes.
		initColumnSizes(table, myModel);

	}

	void goPreviousObject() {
		sFrame.goPreviousObject();
	}

	void goSelectedObject() {
		sFrame.goSelectedObject();
	}

	void openSelectedObject() {
		sFrame.openSelectedObject();
	}

	int getSelectedIndex() {
		return table.getSelectionModel().getMinSelectionIndex();
	}

	void updateTableData() {
		myModel.fireTableDataChanged();
	}

	/*
	 * This method picks good column sizes. If all column heads are wider than
	 * the column's cells' contents, then you can just use
	 * column.sizeWidthToFit().
	 */
	private void initColumnSizes(JTable table, MyTableModel model) {
		int headerWidth = 0;
		int cellWidth = 0;

		for (int i = 0; i < 4; i++) {
			TableColumn column = table.getColumnModel().getColumn(i);
			if (i != 3) {
				TableCellRenderer headerRenderer = table.getTableHeader()
						.getDefaultRenderer();

				Component comp = headerRenderer.getTableCellRendererComponent(
						null, column.getHeaderValue(), false, false, 0, 0);
				headerWidth = comp.getPreferredSize().width;

				cellWidth = model.getMyPreferredWidth(i);
				int columnWidth = Math.max(headerWidth, cellWidth);
				columnWidth += 2; // not sure why this is.... but you really
									// need it for mods
				column.setPreferredWidth(columnWidth);
				if (i == 0)
					column.setMaxWidth(columnWidth);
			} else {
				column.setPreferredWidth(275);
			}
		}
	}

	class MyTableModel extends AbstractTableModel {
		private ObjectInspector oi;
		private String[] columnNames = { "Mods", "Type", "Name", "Value" };

		MyTableModel(ObjectInspector oi) {
			reuse(oi);
		}

		private void reuse(ObjectInspector oi) {
			this.oi = oi;
		}

		@Override
		public int getColumnCount() {
			return 4;
		} // modifier, name, type, value

		@Override
		public int getRowCount() {
			return oi.getCount();
		}

		@Override
		public String getColumnName(int col) {
			return columnNames[col];
		}

		@Override
		public Object getValueAt(int row, int col) {
			switch (col) {
			case 0:
				// modifiers
				return oi.getModifierString(row);
			case 1:
				// type
				if (oi.isArray(row))
					return Array.getLength(oi.getValue(row)) + ""
							+ oi.getType(row);
				return oi.getType(row);
			case 2:
				// name
				return oi.getName(row);
			case 3:
				// val
				// here we have a couple things. if it is non null, and an array
				// of primitive types, glom ARRAY_GLOM things together.
				Object obj = oi.getValue(row);
				if (obj != null) {
					if ((obj.getClass().isArray())
							&& (obj.getClass().getComponentType().isPrimitive())) {
						StringBuffer sb = new StringBuffer();
						sb.append("[ ");
						int arCount = Array.getLength(obj);
						for (int i = 0; (i < ARRAY_GLOM) && (i < arCount); i++) {
							if (i != 0)
								sb.append(" , ");
							sb.append(Array.get(obj, i).toString());
						}
						if (arCount > ARRAY_GLOM)
							sb.append(" ... ]");
						else
							sb.append(" ]");
						return sb;
					}
				}
				return obj == null ? "<null>" : oi.getValue(row);
			}
			return null;
		}

		/**
		 * Gets preferred width for a column
		 * 
		 * @param col
		 *            a value of type 'int'
		 * @return a value of type 'int'
		 */
		int getMyPreferredWidth(int col) {
			int w = 0;
			for (int i = 0; i < getRowCount(); i++) {
				Component comp = table.getCellRenderer(i, col)
						.getTableCellRendererComponent(table,
								getValueAt(i, col), false, false, 0, i);
				w = Math.max(w, comp.getPreferredSize().width);
			}
			return w;
		}

	}
}
