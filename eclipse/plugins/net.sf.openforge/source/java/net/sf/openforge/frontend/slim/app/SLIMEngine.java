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

package net.sf.openforge.frontend.slim.app;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.openforge.app.Engine;
import net.sf.openforge.app.GenericJob;
import net.sf.openforge.frontend.slim.builder.SLIMBuilder;
import net.sf.openforge.frontend.slim.builder.SLIMBuilderException;
import net.sf.openforge.lim.Design;

import org.w3c.dom.Document;

/**
 * SLIMEngine is a stub class that extends the Engine class and is used to
 * simply initialize the Forge runtime environment with the specified
 * GenericJob. The Engine class updates the EngineThread with the current
 * thread, allowing for logging and preferences DB lookups.
 * 
 * 
 * <p>
 * Created: Wed Jul 13 10:23:13 2005
 * 
 * @author imiller, last modified by $Author: imiller $
 */
public class SLIMEngine extends Engine {

	public SLIMEngine(GenericJob job) {
		super(job);
	}

	public Design buildLim() {
		final File[] targetFiles = getGenericJob().getTargetFiles();
		assert targetFiles.length == 1 : "SLIM Compilation supports exactly one target file.  Found: "
				+ targetFiles.length;

		final File input = targetFiles[0];
		Document document = null;
		try {
			DocumentBuilderFactory domFactory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder domBuilder = domFactory.newDocumentBuilder();
			System.out.println("RUN A SCHEMA ON ME!");
			document = domBuilder.parse(input);
		} catch (ParserConfigurationException pce) {
			throw new SLIMBuilderException("error in configuration " + pce);
		} catch (org.xml.sax.SAXException saxe) {
			throw new SLIMBuilderException("sax error " + saxe);
		} catch (IOException ioe) {
			throw new SLIMBuilderException("io error " + ioe);
		}

		Design design = new SLIMBuilder().build(document);

		String fileName = input.getName();
		String rootName = fileName.substring(0, fileName.lastIndexOf("."));

		//This should be set already by the XDesignFactory
		design.setIDLogical(rootName + "_xlim");

		return design;
	}

}// SLIMEngine
