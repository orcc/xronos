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

package org.xronos.openforge.frontend.xlim.builder;

import static org.xronos.openforge.util.xml.Util.xpathEvalNode;

import javax.xml.transform.Transformer;


import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xronos.openforge.frontend.slim.builder.SLIMBuilder;
import org.xronos.openforge.frontend.slim.builder.SLIMConstants;
import org.xronos.openforge.lim.Design;
import org.xronos.openforge.util.io.ClassLoaderStreamLocator;
import org.xronos.openforge.util.xml.Util;

/**
 * XLIMBuilder, creates LIM from XLIM document.
 * 
 * 
 * @author jwj@acm.org
 */
public class XLIMBuilder extends org.xml.sax.helpers.DefaultHandler {

	public XLIMBuilder() {
		super();
		slimBuilder = new SLIMBuilder();
	}

	public Design build(Document doc) {
		// Resources are all direct children of the 'design' element
		// There had better be only one design element!
		NodeList designTags = doc.getElementsByTagName(SLIMConstants.DESIGN);
		assert designTags.getLength() == 1 : "Wrong number of design tags found.  Must be 1.  "
				+ designTags.getLength();

		// return build ((Element)designTags.item(0));

		return build(xpathEvalNode("/", doc));

	}

	public Design build(Node design) {

		Node n = null;
		try {
			n = xlim2slim(design);
		} catch (Exception e) {
			throw new RuntimeException("Error generating SLIM from XLIM.", e);
		}

		Element slimDesignElement = (n instanceof Element) ? (Element) n
				: (n instanceof Document) ? ((Document) n).getDocumentElement()
						: null;

		return slimBuilder.build(slimDesignElement);
	}

	public Node xlim2slim(Node n) throws Exception {
		return Util.applyTransforms(n, xlimTransforms);
	}

	private final SLIMBuilder slimBuilder;

	private final String[] xlimTransformPaths = {
			"net/sf/openforge/frontend/xlim/transforms/XLIMLoopFix.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMFixSelector.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMTagify.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMMakePortNames.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMSizeAndType.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMAddVarReads.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMInsertCasts.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMAddVarReadScope.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMBuildControl.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMRoutePorts.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMFixNames.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMMakeDeps.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMProcessPHI.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMFixNames.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMCreateExits.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMTagify.xslt",
			"net/sf/openforge/frontend/xlim/transforms/XLIMAddControlDeps.xslt" };

	private final Transformer[] xlimTransforms = Util
			.getTransformersAsResources(xlimTransformPaths, Util
					.getSaxonImplementation(), new ClassLoaderStreamLocator(
					XLIMBuilder.class.getClassLoader()));
}
