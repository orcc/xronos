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

import java.io.File;
import java.io.IOException;

/**
 * FileUtils is a place to put file handling methods. These methods are platform
 * independent methods.
 * 
 * <p>
 * Created: Mon Aug 16 12:09:50 2004
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: FileUtils.java 552 2008-02-12 22:40:15Z imiller $
 */
public class FileUtils {

	/**
	 * No instances please...
	 */
	private FileUtils() {
	}

	/**
	 * Create and return the File for a system specific unique temporary
	 * directory. Because it is possible that two instances of the JVM could
	 * create the same temp file if we create a temp file, delete it, then
	 * create a directory with that name, we will use the temp file as a lock
	 * file, then start trying to create a directory with that name, then
	 * continue to append unique numbers to it until we successfully create the
	 * directory.
	 * 
	 * @param basename
	 *            a String on which to base the directory name.
	 * @return a value of type 'File'
	 * @throws IOException
	 *             on any IO error condition
	 * @throws FileUtilException
	 *             if directory could not be created after 5000 tries.
	 */
	public static File getTemporaryDirectory(String basename)
			throws IOException, FileUtilException {
		String baseNameExt = basename;
		for (int i = baseNameExt.length(); i < 3; i++)
			baseNameExt = baseNameExt + "_";
		File outDir = File.createTempFile(baseNameExt, "");
		outDir.deleteOnExit();
		final String buildName = outDir.getName();
		int protectionCount = 0;

		while (!outDir.mkdirs()) {
			if (protectionCount > 5000) {
				throw new FileUtilException("Could not create temp directory "
						+ outDir.getAbsolutePath());
			}

			assert outDir.getAbsoluteFile().getParent() != null : "Could not determine location of temp directory";
			outDir = new File(outDir.getAbsoluteFile().getParent(), buildName
					+ protectionCount);
			outDir.deleteOnExit();

			protectionCount++;
		}

		return outDir;
	}

	public static class FileUtilException extends Exception {
		public FileUtilException(String msg) {
			super(msg);
		}
	}

}// FileUtils
