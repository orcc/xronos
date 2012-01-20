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

package net.sf.openforge.lim;

import java.util.*;

/**
 * MatchingNodesVisitor is visitor which provides a DefaultVisitor which
 * can be used to record matching nodes in either a fifo or lifo manner.
 *
 *
 * Created: Thu Jul 11 14:32:46 2002
 *
 * @author cschanck
 * @version $Id: MatchingVisitor.java 2 2005-06-09 20:00:48Z imiller $
 */
public class MatchingVisitor extends DefaultVisitor
{
    private static final String _RCS_ = "$Rev: 2 $";

    public static final boolean FIFO=true;
    public static final boolean LIFO=false;

    private boolean isFifo;
    private LinkedList ll=new LinkedList();
    
    public MatchingVisitor (boolean isFifo)
    {
        super();
        this.isFifo=isFifo;
    }

    public boolean isFifo() { return isFifo; }
    
    public void addMatchingNode(Object o)
    {
        if(isFifo)
        {
            ll.addLast(o);
        }
        else
        {
            ll.addFirst(o);
        }
    }

    public List getMatchingNodes()
    {
        return Collections.unmodifiableList(ll);
    }

    public void clear()
    {
        ll.clear();
    }
    

}
