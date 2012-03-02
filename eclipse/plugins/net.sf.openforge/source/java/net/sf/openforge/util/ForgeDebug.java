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
package net.sf.openforge.util;

public interface ForgeDebug {
	/**
	 * Used for older debug statements you wish to keep, but not display. If no
	 * level is specified, these will not display.
	 */
	public final static long OLD = GlobalDebug.OLD;
	public final static long LEGACY = OLD;

	/**
	 * Default set of levels enabled by default ;-)
	 */
	public final static long DEFAULT_LEVELS = GlobalDebug.DEFAULT_LEVELS;

	// --------------------------------------------------------
	// cmodel
	// --------------------------------------------------------

	// cmodel (new)
	public final static boolean _cmodel = false;
	public final static GlobalDebug cmodel = new GlobalDebug("CModel", _cmodel,
			DEFAULT_LEVELS);

	// clinker (new)
	public final static boolean _clinker = false;
	public final static GlobalDebug clinker = new GlobalDebug("CLinker",
			_clinker, DEFAULT_LEVELS);

	// gloablapp (new) (Global app, i.e. hllc.app)
	public final static boolean _gapp = false;
	public final static GlobalDebug gapp = new GlobalDebug("Global App", _gapp,
			DEFAULT_LEVELS);
}
