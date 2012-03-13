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

package net.sf.openforge.frontend.slim.builder;

/**
 * XLIMConstants contains the definitions of the recognized XLIM
 * tags/attributes.
 * 
 * 
 * <p>
 * Created: Fri Jun 10 14:20:54 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */
public class SLIMConstants {

	public static final int MAX_ADDR_WIDTH = 32;

	public static final String TRUE = "true";
	public static final String FALSE = "true";

	public static final String ELEMENT_KIND = "kind";

	public static final String DESIGN = "design";

	public static final String IF = "if";
	public static final String DECISION_TEST = "test";
	public static final String DECISION = "decision";
	public static final String THEN = "then";
	public static final String ELSE = "else";

	public static final String LOOP = "loop";
	public static final String LOOP_BODY = "body";

	public static final String NAME = "name";
	public static final String MODULE = "module";
	public static final String MODULE_STYLE = ELEMENT_KIND;
	public static final String MODULE_MUTEX = "mutex";
	public static final String OPERATION = "operation";
	public static final String ACTOR_PORT = "actor-port";
	public static final String INTERNAL_PORT = "internal-port";

	public static final String PORT = "port";
	public static final String PORT_NAME = NAME;
	public static final String PORT_DIRECTION = "dir";
	public static final String PORT_SIZE = "size";
	public static final String PORT_ACCESS_STYLE = "style";
	public static final String PORT_ACCESS_BLOCKING_STYLE = "blocking";
	public static final String EXIT = "exit";
	public static final String EXIT_KIND = ELEMENT_KIND;
	public static final String EXIT_DONE = "done";
	public static final String EXIT_FEEDBACK = "feedback";

	public static final String DEPENDENCY = "dependency";
	public static final String DEP_SOURCE = "source";
	public static final String DEP_TARGET = "dest";
	public static final String DEP_GROUP = "group";

	public static final String CONNECTION = "connection";
	public static final String CONN_SOURCE = "source";
	public static final String CONN_DEST = "dest";

	public static final String RESOURCE_TARGET = "target";
	public static final String STATE_VAR = "stateVar";
	public static final String IVALUE = "initValue";
	public static final String IVALUE_LIST_TYPE = "list";
	public static final String IVALUE_VALUE = "value";
	public static final String IVALUE_SIZE = "size";

	public static final String TYPENAME = "typeName";
	public static final String PORT_TYPE = ELEMENT_KIND;
	public static final String CONTROL_TYPE = "control";

	//
	// Named element attributes for configuring component behaviors
	//
	public static final String CONFIG_OPTION = "config_option"; // Tag name for
																// configurable
																// options
	public static final String CONFIG_NAME = NAME;
	public static final String CONFIG_VALUE = "value";
	public static final String CONFIG_MAXGATEDEPTH = "maxgatedepth"; // takes a
																		// positive
																		// value.
																		// 0
																		// indicates
																		// no
																		// pipelining
	public static final String CONFIG_LOOPUNROLL = "loopunroll"; // takes a
																	// value
																	// from -1
																	// to max
																	// int
																	// indicating
																	// max # of
																	// iterations
																	// if the
																	// loop is
																	// to be
																	// considered
																	// unrollable
	public static final String CONFIG_MINSRLLENGTH = "minsrllength"; // takes a
																		// value
																		// from
																		// 2 to
																		// max
																		// int
																		// indicating
																		// minimum
																		// # of
																		// registers
																		// to be
																		// merged
																		// into
																		// an
																		// SRL

	public static final String REMOVABLE = "removable"; // legal values are yes
														// or no
	public static final String AUTOSTART = "autostart"; // legal values are true
														// or false

	// ///////////////////////////
	//
	// Operation types
	//
	// ///////////////////////////
	public static final String PIN_READ = "pinRead";
	public static final String PIN_WRITE = "pinWrite";
	public static final String PIN_COUNT = "pinTokenCount";
	public static final String PIN_STALL = "pinStall";
	public static final String PIN_PEEK = "pinPeek";
	public static final String PIN_STATUS = "pinStatus";
	public static final String TASKCALL = "taskCall";
	public static final String VAR_REF = "var_ref";
	public static final String ASSIGN = "assign";
	public static final String CONSTANT = "$literal_Integer";
	public static final String ADD = "$add";
	public static final String AND = "$and";
	public static final String DIV = "$div";
	public static final String EQ = "$eq";
	public static final String GE = "$ge";
	public static final String GT = "$gt";
	public static final String LE = "$le";
	public static final String LT = "$lt";
	public static final String MUL = "$mul";
	public static final String NE = "$ne";
	public static final String NEGATE = "$negate";
	public static final String NOT = "$not"; // These two are the same thing...
	public static final String BITNOT = "bitnot"; // These two are the same
													// thing...
	public static final String OR = "$or";
	public static final String SUB = "$sub";
	public static final String XOR = "$xor";
	public static final String LSHIFT = "lshift";
	public static final String RSHIFT = "rshift";
	public static final String URSHIFT = "urshift";
	public static final String NOOP = "noop";
	public static final String CAST = "cast";

	private SLIMConstants() {
	}

}// XLIMConstants
