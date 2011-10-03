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


package net.sf.openforge.frontend.xlim.builder;

import java.io.*;
import java.util.*;

import net.sf.openforge.app.*;
import net.sf.openforge.frontend.slim.builder.SLIMBuilder;
import net.sf.openforge.frontend.slim.builder.SLIMConstants;
import net.sf.openforge.frontend.slim.builder.XDesignFactory;
import net.sf.openforge.lim.*;
import net.sf.openforge.util.io.ClassLoaderStreamLocator;
import net.sf.openforge.util.xml.Util;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.*;
import javax.xml.transform.Transformer;
import javax.xml.validation.*;

import static net.sf.openforge.util.xml.Util.xpathEvalNode;

/**
 * XLIMBuilder, creates LIM from XLIM document.
 *
 *
 * @author jwj@acm.org 
 */
public class XLIMBuilder extends org.xml.sax.helpers.DefaultHandler
{

    public XLIMBuilder ()
    {
        super();
        slimBuilder = new SLIMBuilder();
    }
    
    public Design build (Document doc)
    {    	
        // Resources are all direct children of the 'design' element
        // There had better be only one design element!
        NodeList designTags = doc.getElementsByTagName(SLIMConstants.DESIGN);
        assert designTags.getLength() == 1 : "Wrong number of design tags found.  Must be 1.  " + designTags.getLength();
        
//        return build ((Element)designTags.item(0));
        
        return build(xpathEvalNode("/", doc));

    }
    
    public Design build (Node design) {

    	Node n = null;
    	try {
			n = xlim2slim(design);
		} catch (Exception e) {
			throw new RuntimeException("Error generating SLIM from XLIM.", e);
		}
		
		Element slimDesignElement = (n instanceof Element) ? (Element) n :
									 (n instanceof Document) ? ((Document) n).getDocumentElement() :
								     null;
									 
		return slimBuilder.build(slimDesignElement);
    }
    
    public Node xlim2slim(Node n) throws Exception {
    	return Util.applyTransforms(n, xlimTransforms);
    }

    
    private SLIMBuilder  slimBuilder;
    
    
    private String[] xlimTransformPaths = {
            "net/sf/openforge/transforms/xlim/XLIMLoopFix.xslt",
            "net/sf/openforge/transforms/xlim/XLIMFixSelector.xslt",
            "net/sf/openforge/transforms/xlim/XLIMTagify.xslt",
            "net/sf/openforge/transforms/xlim/XLIMMakePortNames.xslt",
            "net/sf/openforge/transforms/xlim/XLIMSizeAndType.xslt",
            "net/sf/openforge/transforms/xlim/XLIMAddVarReads.xslt",
            "net/sf/openforge/transforms/xlim/XLIMInsertCasts.xslt",
            "net/sf/openforge/transforms/xlim/XLIMAddVarReadScope.xslt",
            "net/sf/openforge/transforms/xlim/XLIMBuildControl.xslt",
            "net/sf/openforge/transforms/xlim/XLIMRoutePorts.xslt",
            "net/sf/openforge/transforms/xlim/XLIMFixNames.xslt",
            "net/sf/openforge/transforms/xlim/XLIMMakeDeps.xslt",
            "net/sf/openforge/transforms/xlim/XLIMProcessPHI.xslt",
            "net/sf/openforge/transforms/xlim/XLIMFixNames.xslt",
            "net/sf/openforge/transforms/xlim/XLIMCreateExits.xslt",
            "net/sf/openforge/transforms/xlim/XLIMTagify.xslt",
            "net/sf/openforge/transforms/xlim/XLIMAddControlDeps.xslt"
        };
    
    private Transformer [] xlimTransforms = 
    	Util.getTransformersAsResources(xlimTransformPaths, 
    									Util.getSaxonImplementation(),
    									new ClassLoaderStreamLocator(XLIMBuilder.class.getClassLoader()));
}
