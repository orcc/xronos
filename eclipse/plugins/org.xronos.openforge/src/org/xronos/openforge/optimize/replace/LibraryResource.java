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

package org.xronos.openforge.optimize.replace;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.ListResourceBundle;

import org.xronos.openforge.optimize._optimize;


/**
 * LibraryResource creates accessible resources for each user defined library by
 * looking first in the source path (defined by project) and then the classpath
 * from which Forge was launched.
 * 
 * <p>
 * Created: Fri Aug 30 11:07:54 2002
 * 
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: LibraryResource.java 2 2005-06-09 20:00:48Z imiller $
 */
public class LibraryResource extends ListResourceBundle {

	/**
	 * Individual resource items. Populate with the libraries defined by the
	 * user.
	 */
	protected Object[][] contents;

	/* The source path in which to look first for libraries. */
	private String[] sourcePath;

	/**
	 * Create a new library resource locator populated with the default
	 * libraries as well as any user defined libraries, supplied as Strings in
	 * the argument list.
	 * 
	 * @param libraries
	 *            a List of Strings, one for each user defined library.
	 * @param sourcePath
	 *            an array of Strings, identifying the users defined source
	 *            path.
	 */
	public LibraryResource(List<String> libraries, String[] sourcePath) {
		int size = libraries.size();
		contents = new Object[size][];
		for (int i = 0; i < libraries.size(); i++) {
			String next = libraries.get(i);
			contents[i] = new Object[] { next, next };
		}
		this.sourcePath = sourcePath;
	}

	@Override
	public Object[][] getContents() {
		return contents;
	}

	/**
	 * Retrieves the URL used to locate the library named by key by retrieving
	 * it as a resource via the default class loader. Note that this could be
	 * modified to use the ForgeClassLoader in order to include searching of the
	 * users sourcepath as well.
	 */
	public URL getResourceURL(String key) {
		String keyName = getString(key);
		URL url = findInSourcePath(keyName);
		if (url == null) {
			url = LibraryResource.class.getResource("/" + keyName);
		}
		return url;
	}

	/**
	 * Attempts to find the specified library file name in the source path
	 * defined when this class was constructed.
	 */
	public URL findInSourcePath(String key) {
		File f = new File(key);
		if (f.isAbsolute()) {
			// the user pointed directly to the file, obey their
			// command
			if (f.exists()) {
				try {
					return f.getCanonicalFile().toURI().toURL();
				} catch (MalformedURLException e) {
					if (_optimize.db)
						_optimize.ln(_optimize.REPLACE, "Malformed URL? " + e);
				} catch (IOException e) {
					if (_optimize.db)
						_optimize.ln(_optimize.REPLACE, "IOException? " + e);
				}
			}
		} else {
			// handle non absolute files by searching the sourcePath

			for (int i = 0; i < sourcePath.length; i++) {
				File file = new File(sourcePath[i], key);
				if (file.exists()) {
					try {
						return file.getCanonicalFile().toURI().toURL();
					} catch (MalformedURLException e) {
						if (_optimize.db)
							_optimize.ln(_optimize.REPLACE, "Malformed URL? "
									+ e);
					} catch (IOException e) {
						if (_optimize.db)
							_optimize
									.ln(_optimize.REPLACE, "IOException? " + e);
					}
				}
			}
		}

		return null;
	}

}// LibraryResource
