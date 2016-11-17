/* 
 * XRONOS-EXELIXI
 * 
 * Copyright (C) 2011-2016 EPFL SCI STI MM
 *
 * This file is part of XRONOS-EXELIXI.
 *
 * XRONOS-EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * XRONOS-EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with XRONOS-EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or 
 * an Eclipse library), containing parts covered by the terms of the 
 * Eclipse Public License (EPL), the licensors of this Program grant you 
 * additional permission to convey the resulting work.  Corresponding Source 
 * for a non-source form of such a combination shall include the source code 
 * for the parts of Eclipse libraries used as well as that of the covered work.
 * 
 */

package org.xronos.orcc.backend.transform.pipelining.coloring;

import java.io.BufferedWriter;
import java.io.IOException;

/**
 * This class defines different parameters for an operator
 * 
 * @author Anatoly Prihozhy
 * 
 */
public class OperatorType {

	/**
	 * The operators name
	 */
	private String name;

	/**
	 * The operators time cost
	 */
	private float time;

	/**
	 * The operators cost
	 */
	private float cost;

	/**
	 * Count the copies
	 */
	private int count;

	public OperatorType() {
		name = "t0";
		time = 0.0f;
		cost = 0.0f;
		count = 0;
	}

	public OperatorType(String name, float time, float cost) {
		this.name = name;
		this.time = time;
		this.cost = cost;
		count = 0;
	}

	/**
	 * Add copy
	 */

	public void addCopy() {
		count++;
	}

	/**
	 * Get this operator name
	 * 
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get this operator time
	 * 
	 * @return
	 */
	public float getTime() {
		return this.time;
	}

	/**
	 * Print this operator type
	 * 
	 * @param out
	 * @return
	 */
	public boolean print(BufferedWriter out) {
		try {
			out.write(name + " - " + time + " - " + cost + " - " + count);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Remove copy
	 */
	public void removeCopy() {
		count--;
	}

	@Override
	public String toString() {
		return name + " - " + time + " - " + cost + " - " + count;
	}

}
