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

/**
 * A SimpleExpression is an expression without any qualifications or operations.
 * In other words, just a wire name. This is useful when the syntax allows for
 * any expression, but synthesis or simulation has trouble with anything other
 * than a wire name.
 * <P>
 * This was created directly in response to xst complaining about wire ranges in
 * a sensitivity list.s
 * 
 * @author abk
 * @version $Id: SimpleExpression.java 2 2005-06-09 20:00:48Z imiller $
 */
public interface SimpleExpression extends Expression {

}
