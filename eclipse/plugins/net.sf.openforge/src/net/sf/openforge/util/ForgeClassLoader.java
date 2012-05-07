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
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

/**
 * Loads user classes to be compiled by the Forge.
 * 
 * Created: Thu Jun 20 14:14:08 2002
 * 
 * @author Christopher R.S. Schanck
 * @version $Id: ForgeClassLoader.java 2 2005-06-09 20:00:48Z imiller $
 */
public class ForgeClassLoader extends ClassLoader {

	/** Cache of loaded classes. */
	private HashMap<String, Class<?>> classes = new HashMap<String, Class<?>>();

	/** Reusable buffer for converting class file bytes to Class objects. */
	private byte[] classBytes = new byte[4 * 1024];

	private String[] searchPath;

	/**
	 * Construct a new ForgeClassLoader which uses the paths contained in
	 * 'classpath' when finding classes. Any classes not found in these paths
	 * will use the system class loader. Any system and/or API classes will also
	 * use the system class loader.
	 * 
	 * @param classpath
	 *            a value of type 'String[]'
	 */
	public ForgeClassLoader(String[] classpath) {
		super();
		searchPath = classpath;
	}

	/**
	 * Locates and loads a named class.
	 * 
	 * @param name
	 *            the fully qualified name of the class
	 * @param resolve
	 *            true if the class is to be resolved after loading
	 * @return the loaded class
	 * @throws ClassNotFoundException
	 *             if the class is not found
	 */
	@Override
	public synchronized Class<?> loadClass(String name, boolean resolve)
			throws ClassNotFoundException {
		//
		// If the class has not already been loaded, attempt to find it.
		//
		Class<?> cl = classes.get(name);
		if (cl == null) {

			// System.out.println("\nForgeClassLoader.loadClass(" + name +
			// ",resolve: " + resolve + ")");
			// Thread.dumpStack();

			//
			// If the class belongs to one of the system packages, or
			// one of the API classes load it using the default
			// ClassLoader behavior.
			//
			if (ForgeKnownClasses.isSystemClass(name)
					|| ForgeKnownClasses.isAPIClass(name)) {
				try {
					return super.loadClass(name, resolve);
				} catch (Exception e) {
				}
			}

			try {
				//
				// Find the file containing the named class.
				//
				// File f = Globals.pathManager.findClassFile(name);
				File f = findClassFile(name);
				if (f.exists()) {
					//
					// Make sure the buffer has room to hold the
					// class info.
					//
					int length = (int) f.length();
					ensureRoom(length);

					//
					// Read the class file into the buffer.
					//
					FileInputStream fis = new FileInputStream(f);
					if (fis.read(classBytes, 0, length) == length) {
						//
						// Define the class.
						//
						cl = defineClass(name, classBytes, 0, length);
					}
				}
			} catch (IOException e) {
			}

			if (cl == null) {
				// One more try to the system class loader, we only get
				// here for classes that are user specified that are really
				// not in the forgecpath, or JVM internal classes that are
				// not expicitly specified in the systemclasses list, such
				// as sun.net. and sun.misc. We do it this way, cause we
				// don't really know the JVM internal packages, only the
				// defined library packages like java. and javax. etc.
				try {
					return super.loadClass(name, resolve);
				} catch (Exception e) {
				}

				throw new ClassNotFoundException(name);
			}

			//
			// Save the new class in the cache.
			//
			classes.put(name, cl);
		}

		//
		// Resolve the class if required.
		//
		if (resolve) {
			resolveClass(cl);
		}

		return cl;
	}

	/**
	 * Finds the URL of a named file.
	 * 
	 * @param name
	 *            the file name
	 * @return the URL of the file, or null if not found
	 */
	@Override
	protected URL findResource(String name) {
		if (ForgeKnownClasses.isSystemClass(name)) {
			try {
				return super.findResource(name);
			} catch (Exception e) {
			}
		}

		if (name.endsWith(".class")) {
			try {
				String className = name.substring(0, name.length() - 6);
				// File file = Globals.pathManager.findClassFile(className);
				File file = findClassFile(className);

				if (file.exists()) {
					return file.toURI().toURL();
				}
			} catch (IOException e) {
			}
		}

		// Fall back, it wasn't a system class and we couldn't find
		// it in our class path, try the system classpath
		try {
			return super.findResource(name);
		} catch (Exception e) {
		}

		return null;
	}

	/**
	 * Adjusts the size of the class bytes buffer.
	 * 
	 * @param size
	 *            the number of bytes that must be accommodated in the buffer
	 */
	private void ensureRoom(int size) {
		if (size >= classBytes.length) {
			int newsize = (size + classBytes.length + 4 * 1024) / (4 * 1024);
			newsize = newsize * 4 * 1024;
			classBytes = new byte[newsize];
		}
	}

	private File findClassFile(String rootFilename) throws IOException {
		String className = rootFilename.replace('.', File.separatorChar)
				+ ".class";
		return findFile(className);
	}

	private File findFile(String filename) throws IOException {
		for (int i = 0; i < searchPath.length; i++) {
			String fname = searchPath[i] + File.separator + filename;
			File f = new File(fname);

			if (f.exists()) {
				return f;
			}
		}

		throw new IOException("File not found! " + filename);
	}

}
