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
 * Created on Oct 6, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package net.sf.openforge.lim;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts tags from Procedure names and labeled components within a design.
 * Based on Andy's LIMProject.
 * @author sb
 */
public class SearchLabelExtractor extends FilteredVisitor
{	
	List tagList = new ArrayList();
    
    private Procedure currentProcedure;    
    
    /**
     * Gets the tags extracted from any visited designs.
     */
    public List getTags()
    {
        return tagList;
    }
    
    /**
     * Adds tags for all permutations of the procedure name using
     * CodeLabel's searchList.
     */
    public void visit (Procedure procedure)
    {
        CodeLabel procedureLabel = new CodeLabel(procedure, null);
        List procList = procedureLabel.getSearchList();
        for(int idx = 0; idx < procList.size(); idx ++){
        	String tag = (String)procList.get(idx);
            if (!tagList.contains(tag)) tagList.add(tag);
        }            
        currentProcedure = procedure;
        super.visit(procedure);
    }
    
    /**
     * Adds tags for all permutations of the component's label (if any) using
     * CodeLabel's searchList.
     */
    public void filterAny(Component c)
    {
        String optionLabel = c.getOptionLabel();
        if (optionLabel != null)
        {
            CodeLabel label = new CodeLabel(currentProcedure, optionLabel);
            List list = label.getSearchList(optionLabel);
            for(int idx = 0; idx < list.size(); idx ++){
            	String tag = (String)list.get(idx);
                if (!tagList.contains(tag)) tagList.add(tag);
            }
        }
    }	
}
