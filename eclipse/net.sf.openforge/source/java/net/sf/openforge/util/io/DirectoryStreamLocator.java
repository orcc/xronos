package net.sf.openforge.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;


public class DirectoryStreamLocator implements StreamLocator {

	public InputStream getAsStream(String name) {
		File f = new File(dirpath + name);
		try {
			InputStream is = new FileInputStream(f);
			
			return is;
		}
		catch (Exception e) {
			return null;
		}
	}
	
	public DirectoryStreamLocator(String dirpath) {
		this.dirpath = dirpath + File.separator;
	}

	private String dirpath;
	
	public String toString ()
	{
	    return super.toString() + "[" + this.dirpath + "]";
	}
}
