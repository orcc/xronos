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

import java.util.Collection;
import java.util.Collections;

/**
 * InlineComment represents a verilog comment which is placed anywhere that
 * whitespace can appear. These styled comments are usually short, and can be
 * bounded by {@link Symbol#OPEN_COMMENT} and {@link Symbol#CLOSE_COMMENT} or by
 * a {@link Symbol#SHORT_COMMENT} depending on which constructor is being used.
 * <P>
 * 
 * Created: Mon Feb 12 2001
 * 
 * @author abk
 * @version $Id: InlineComment.java 2 2005-06-09 20:00:48Z imiller $
 */
public class InlineComment extends Token implements Statement {

	Comment comment;

	public static final int TYPE = 6;

	public InlineComment(String comment) {
		this.comment = new Comment(comment);
	}

	public InlineComment(String comment, int type) {
		this.comment = new Comment(comment, type);
	}

	@Override
	public String getToken() {
		if (comment.getType() == Comment.LONG) {
			return comment.toLongComment();
		} else {
			return comment.toShortComment();
		}
	}

	public void setWidth(int width) {
		comment.setWidth(width);
	}

	@Override
	public int getType() {
		return TYPE;
	}

	@Override
	public Collection<Net> getNets() {
		return Collections.emptyList();
	}

	@Override
	public Lexicality lexicalify() {
		Lexicality lex = new Lexicality();
		lex.append(this);
		return lex;
	}

} // class InlineComment
