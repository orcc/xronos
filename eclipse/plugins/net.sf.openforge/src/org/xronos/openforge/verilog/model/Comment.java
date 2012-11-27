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
package org.xronos.openforge.verilog.model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Comment represents a verilog comment.
 * 
 * <P>
 * 
 * Created: Mon Feb 12 2001
 * 
 * @author abk
 * @version $Id: Comment.java 284 2006-08-15 15:43:34Z imiller $
 */
public class Comment extends PrintWriter {

	/** A short comment. */
	public static final int SHORT = 0;
	/** A long comment. */
	public static final int LONG = 1;
	/** A blank line comment. */
	public static final int BLANK = 2;

	/** The type of comment (LONG or SHORT). */
	int type = SHORT;

	/**
	 * The width in characters of text per line in a comment (excluding the
	 * comment delimiters).
	 */
	int width = 72;

	static final int DEFAULT_SHORT_WIDTH = 40;
	static final int DEFAULT_LONG_WIDTH = 72;

	public Comment() {
		super(new StringWriter());
	}

	public Comment(int type) {
		this();
		this.type = type;

		switch (type) {
		case SHORT:
			width = DEFAULT_SHORT_WIDTH;
			break;
		default:
			width = DEFAULT_LONG_WIDTH;
		}
	} // Comment(int)

	public Comment(String text) {
		this();

		if (text.length() > DEFAULT_SHORT_WIDTH) {
			type = LONG;
		} else {
			type = SHORT;
		}

		println(text);

	} // Comment

	public Comment(String text, int type) {
		this();

		this.type = type;

		println(text);
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	/**
	 * Get the line width used to output the comment.
	 * 
	 * @return value of width.
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * Set the line width used to output the comment.
	 * 
	 * @param v
	 *            Value to assign to width.
	 */
	public void setWidth(int v) {
		width = v;
	}

	public String toShortComment() {
		StringBuffer comment = new StringBuffer();

		for (Iterator<Object> it = iterator(); it.hasNext();) {
			String line = (String) it.next();
			comment.append(Symbol.SHORT_COMMENT);
			comment.append(" ");
			comment.append(line);
			comment.append(Control.NEWLINE.toString());
		}

		return comment.toString();
	} // toShortComment()

	public String toLongComment() {
		StringBuffer comment = new StringBuffer();

		comment.append(Symbol.OPEN_COMMENT);
		comment.append(Control.NEWLINE.toString());

		for (Iterator<Object> it = iterator(); it.hasNext();) {
			String line = (String) it.next();
			comment.append(" ");
			comment.append(Symbol.CONTINUE_COMMENT);
			comment.append(" ");
			comment.append(line);
			comment.append(Control.NEWLINE.toString());
		}

		comment.append(" ");
		comment.append(Symbol.CLOSE_COMMENT);

		return comment.toString();
	} // toLongComment()

	public String toBlankComment() {
		StringBuffer comment = new StringBuffer();

		comment.append(Control.NEWLINE.toString());
		return comment.toString();
	} // toLongComment()

	@Override
	public String toString() {
		switch (type) {
		case SHORT:
			return toShortComment();
		case BLANK:
			return toBlankComment();
		default:
			return toLongComment();
		}
	} // toString()

	public Iterator<Object> iterator() {
		return new LineIterator();
	}

	public class LineIterator implements Iterator<Object> {
		String buffer;
		List<Object> lines;
		int offset = 0;

		int iterator_count = 0;

		public LineIterator() {
			buffer = ((StringWriter) out).toString();
			lines = new ArrayList<Object>();

			breakBuffer();
		}

		private void breakBuffer() {
			int index = 0;
			int prev_index = 0;

			int newline_length = Control.NEWLINE.toString().length();

			while (index < buffer.length()) {
				index = buffer.indexOf(Control.NEWLINE.toString(), index);

				if (index < 0) {
					index = buffer.length() - 1;
				}

				String line = buffer.substring(prev_index, index);

				if (line.length() > width) {
					breakLine(line);
				} else {
					lines.add(line);
				}

				prev_index = index;
				index += newline_length;
			}

		} // breakBuffer()

		private void breakLine(String line) {
			int line_length = 0;
			int line_start = 0;
			int line_end = 0;
			char c = ' ';

			while (line_start < line.length()) {
				line_length = 0;

				while (line_length < width) {
					if (line_start + line_length == line.length()) {
						line_end = line_start + line_length;
						break;
					}

					c = line.charAt(line_start + line_length);

					if (Character.isWhitespace(c)) {
						line_end = line_start + line_length;
					}

					line_length++;
				}

				lines.add(line.substring(line_start, line_end));

				line_start = line_end + 1;
			}

		}

		@Override
		public boolean hasNext() {
			return (iterator_count < lines.size());
		}

		@Override
		public Object next() {
			return lines.get(iterator_count++);
		}

		@Override
		public void remove() {

		}

	} // nested class LineIterator()

} // end of class Comment
