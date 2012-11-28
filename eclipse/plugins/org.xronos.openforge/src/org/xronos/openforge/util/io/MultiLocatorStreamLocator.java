package org.xronos.openforge.util.io;

import java.io.InputStream;

/**
 * This locator defers to one or more other {@link StreamLocator} instances to
 * find a specified resource
 * 
 * @author imiller
 * 
 */
public class MultiLocatorStreamLocator implements StreamLocator {
	private StreamLocator[] locators;

	public MultiLocatorStreamLocator(StreamLocator[] locators) {
		this.locators = locators;
	}

	public InputStream getAsStream(String name) {
		for (int i = 0; i < this.locators.length; i++) {
			InputStream stream = this.locators[i].getAsStream(name);
			if (stream != null)
				return stream;
		}
		return null;
	}

}
