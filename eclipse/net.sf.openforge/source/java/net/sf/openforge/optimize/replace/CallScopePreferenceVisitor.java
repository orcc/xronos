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

package net.sf.openforge.optimize.replace;

import net.sf.openforge.app.OptionKey;
import net.sf.openforge.app.project.*;
import net.sf.openforge.lim.*;


/**
 * CallScopePreferenceVisitor is created with a preference name and
 * the value that the preference should be set to for the
 * <u>entire</u> call hierarchy visited.
 *
 * <p>Created: Thu Mar 27 15:34:29 2003
 *
 * @author imiller, last modified by $Author: imiller $
 * @version $Id: CallScopePreferenceVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class CallScopePreferenceVisitor extends DefaultVisitor 
{
    private static final String _RCS_ = "$Rev: 2 $";

    /** The name of the preference to set. */
    private OptionKey optionKey;
    /** The value of the preference to set. */
    private int value;
    
    /** The component from which the preferences will be copied */
    private Configurable replaced;

    /**
     * Create a new visitor which sets the specified value for the
     * specified preference on each procedure visited.
     *
     * @param prefName a value of type 'String'
     * @param value a value of type 'int'
     * @param comp a Component used to retrieve the default
     * preferences to apply to the replaced component.
     */
    public CallScopePreferenceVisitor (OptionKey optKey, int value, Component comp)
    {
        this.optionKey = optKey;
        this.value = value;
        this.replaced = comp;
    }

    public void visit (Call call)
    {
        if (call.getProcedure() != null)
        {
            Procedure proc = call.getProcedure();
            ReplacedSearchLabel rsl = new ReplacedSearchLabel(proc.getSearchLabel(), replaced);
            proc.setSearchLabel(rsl);
            SearchLabel tag = new CodeLabel(rsl.getSearchList().get(0).toString());
            Option op = call.getGenericJob().getOption(this.optionKey);
            op.setValue(tag, new Integer(this.value));
        }

        // Call the super so that we override the value for any sub
        // calls as well.  Otherwise the user could create recursion
        // via call to a call...
        super.visit(call);
    }
    
}// CallScopePreferenceVisitor
