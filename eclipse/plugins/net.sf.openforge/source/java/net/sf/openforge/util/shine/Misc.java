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

package net.sf.openforge.util.shine;

import javax.swing.*;
import java.net.URL;

public class Misc
{
    static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

    public static ImageIcon getIconResource(String resName)
    {
	return getIconResource(resName,null);
    }
    
    public static ImageIcon getIconResource(String resName,String descr)
    {
	ImageIcon icon = null;
	URL iconURL = ClassLoader.getSystemResource(resName);
	if (iconURL != null)
	{
	    if(descr!=null)
		icon = new ImageIcon(iconURL,descr);
	    else
		icon = new ImageIcon(iconURL);
	}

	return icon;
    }

}
