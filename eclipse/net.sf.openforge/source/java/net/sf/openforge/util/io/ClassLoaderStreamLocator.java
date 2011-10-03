package net.sf.openforge.util.io;

import java.io.InputStream;


public class ClassLoaderStreamLocator implements StreamLocator {

	public InputStream getAsStream(String name) {
		InputStream s = loader.getResourceAsStream(name);
		return s;
	}
	
	public ClassLoaderStreamLocator(ClassLoader loader) {
		this.loader = loader;
	}
	
	private ClassLoader loader;

}
