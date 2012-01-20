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


package net.sf.openforge.frontend.slim.builder;

import java.io.*;
import java.util.*;

import net.sf.openforge.app.*;
import net.sf.openforge.lim.*;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.*;
import javax.xml.validation.*;

/**
 * SLIMBuilder contains methods to turn SLIM source document into a fully
 * constructed, structurally correct, LIM {@link Design} object.
 *
 *
 * <p>Created: Fri Jun 10 13:07:24 2005
 *
 * @author imiller
 * @author jwj@acm.org
 */
public class SLIMBuilder extends org.xml.sax.helpers.DefaultHandler
{

    public SLIMBuilder ()
    {
        super();
    }
    
    public Design build (Document doc)
    {    	
        // Resources are all direct children of the 'design' element
        // There had better be only one design element!
        NodeList designTags = doc.getElementsByTagName(SLIMConstants.DESIGN);
        assert designTags.getLength() == 1 : "Wrong number of design tags found.  Must be 1.  " + designTags.getLength();
        
        return build ((Element)designTags.item(0));

    }
    
    public Design build (Element designElement) {
    	
        XDesignFactory designFactory = new XDesignFactory();
        Design design = designFactory.buildDesign(designElement);
        
        return design;
    }
    
}// SLIMBuilder
