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

package net.sf.openforge.lim.io;

import java.util.*;

import net.sf.openforge.app.EngineThread;

/**
 * BlockIOInterface holds information about all block io interfaces
 *
 * @author Jim Jensen & Ian Miller
 * @version $Id: BlockIOInterface.java 129 2006-04-05 13:40:46Z imiller $
 */
public class BlockIOInterface
{
    private static final String _RCS_ = "$Rev: 129 $";

    /** map of String function name to List of BlockDescriptors */
    static private HashMap functionToDescriptors=new HashMap();

    /**
     * Returns a list of BlockDescriptors for the specific
     * function passed in (names are available from getFunctionNames)
     */
    public static List getDescriptors (String functionName) 
    {
        List l=(List) functionToDescriptors.get(functionName);
        if (l == null)
        {
            return Collections.EMPTY_LIST;
        }
        
        return Collections.unmodifiableList(l);
    }
    
    /**
     * add a BlockDescriptor
     */
    public static void addBlockDescriptor (String functionName, BlockDescriptor bd)
    {
        List l=(List) functionToDescriptors.get(functionName);
        if (l == null)
        {
            l=new ArrayList();
            functionToDescriptors.put(functionName, l);
        }
        l.add(bd);
    }
    
    /**
     * clear all known functions
     */
    public static void clear ()
    {
        functionToDescriptors.clear();
    }

    /**
     * Returns the names of the entry functions
     */
    public static Set getFunctionNames() 
    {
        return functionToDescriptors.keySet();
    }



    
    // sample population of a BlockIODescriptor
    // for C source (courtesy of Ian):
    //
    //  struct foo
    //  {
    //      int z[3];
    //      int x,y;
    //  };
    //
    //  int boo (struct foo *x, short a, long long b)
    //  {
    //      int temp = x->z[0] + x->z[1] + x->z[2];
    //      x->z[0] = -1;
    //      return temp + x->x + x->y + a + b;
    //  }
    //
    public static void generateTest (final int fifoWidth)
    {
        // currently fifo width must be 4 for this test generator
        if (fifoWidth != 4)
        {
        	EngineThread.getEngine().fatalError("fifo width must be 4");
        }
        


        // first the input block... 3 elements
        // takes 5 cycles to transfer struct foo...
        // 2 cycles to transfer short a
        // 2 cycles to transfer long long b
        BlockIOInterface.addBlockDescriptor("boo",
            new BlockDescriptor()
            {
                public DeclarationGenerator getFunctionDeclaredType () { return new tempDeclGen("int"); }
                public String getFunctionName () { return "boo"; }
                public String getInterfaceID () { return "0"; }
                public boolean isSlave () { return true; }
                public int[] getBlockOrganization () { return new int[] {0,0,0,0,0,1,2,2}; }
                public int getByteWidth () { return fifoWidth; }
                private BlockDescriptor getThis () { return this; } // Hack to get 'this'
                
                public BlockElement[] getBlockElements ()
                {
                    return new BlockElement[] 
                    {
                        new BlockElement () 
                        {
                            public int getAllocatedSize () { return 5; }
                            public BlockDescriptor getBlockDescriptor () { return getThis(); }
                            public DeclarationGenerator getDeclaredType () { return new tempDeclGen("struct foo *"); }
                            public String getFormalName () { return "x"; }
                            public byte[] getStreamFormat () { return new byte[]{1,1,1,1, 1,1,1,1, 1,1,1,1, 1,1,1,1, 1,1,1,1}; }
                            public void deleteStreamBytes (int start, int length) {;}
                        },
                        new BlockElement ()
                        {
                            public int getAllocatedSize () { return 2; }
                            public BlockDescriptor getBlockDescriptor () { return getThis(); }
                            public DeclarationGenerator getDeclaredType () { return new tempDeclGen("short"); }
                            public String getFormalName () { return "a"; }
                            public byte[] getStreamFormat () { return new byte[]{1,0,0,0,1,0,0,0}; }
                            public void deleteStreamBytes (int start, int length) {;}
                        },
                        new BlockElement ()
                        {
                            public int getAllocatedSize () { return 8; }
                            public BlockDescriptor getBlockDescriptor () { return getThis(); }
                            public DeclarationGenerator getDeclaredType () { return new tempDeclGen("long long"); }
                            public String getFormalName () { return "b"; }
                            public byte[] getStreamFormat () { return new byte[]{1,1,1,1, 1,1,1,1}; }
                            public void deleteStreamBytes (int start, int length) {;}
                        }
                    };
                }
            }
                                            ); // end of addBlockDescriptor
        
        

        // then the output block...  4 elements out
        BlockIOInterface.addBlockDescriptor("boo",
            new BlockDescriptor()
            {
                public DeclarationGenerator getFunctionDeclaredType () { return new tempDeclGen("int"); }
                public String getFunctionName () { return "boo"; }
                public String getInterfaceID () { return "1"; }
                public boolean isSlave () { return false; }
                public int[] getBlockOrganization () { return new int[] {0,0,0,0,0,1,2,2,3}; }
                public int getByteWidth () { return fifoWidth; }
                private BlockDescriptor getThis () { return this; } // Hack to get 'this'
   
                public BlockElement[] getBlockElements ()
                {
                    return new BlockElement[] 
                    {
                        new BlockElement () 
                        {
                            public int getAllocatedSize () { return 5; }
                            public BlockDescriptor getBlockDescriptor () { return getThis(); }
                            public DeclarationGenerator getDeclaredType () { return new tempDeclGen("struct foo *"); }
                            public String getFormalName () { return "x"; }
                            public byte[] getStreamFormat () { return new byte[]{1,1,1,1, 1,1,1,1, 1,1,1,1, 1,1,1,1, 1,1,1,1}; }
                            public void deleteStreamBytes (int start, int length) {;}
                        },
                        new BlockElement ()
                        {
                            public int getAllocatedSize () { return 2; }
                            public BlockDescriptor getBlockDescriptor () { return getThis(); }
                            public DeclarationGenerator getDeclaredType () { return new tempDeclGen("short"); }
                            public String getFormalName () { return "a"; }
                            public byte[] getStreamFormat () { return new byte[]{1,0,0,0,1,0,0,0}; }
                            public void deleteStreamBytes (int start, int length) {;}
                        },
                        new BlockElement ()
                        {
                            public int getAllocatedSize () { return 8; }
                            public BlockDescriptor getBlockDescriptor () { return getThis(); }
                            public DeclarationGenerator getDeclaredType () { return new tempDeclGen("long long"); }
                            public String getFormalName () { return "b"; }
                            public byte[] getStreamFormat () { return new byte[]{1,1,1,1, 1,1,1,1}; }
                            public void deleteStreamBytes (int start, int length) {;}
                        },
                        new BlockElement ()
                        {
                            public int getAllocatedSize () { return 4; }
                            public BlockDescriptor getBlockDescriptor () { return getThis(); }
                            public DeclarationGenerator getDeclaredType () { return new tempDeclGen("int"); }
                            public String getFormalName () { return "return"; }
                            public byte[] getStreamFormat () { return new byte[]{1,1,1,1}; }
                            public void deleteStreamBytes (int start, int length) {;}
                        }
                    };
                }
            }
                                            );// end of addBlockDescriptor
    }
    
}

class tempDeclGen implements DeclarationGenerator
{
    private String type;
    tempDeclGen (String type)
    {
        this.type = type;
    }

    public String getDeclaration (String id)
    {
        return this.type + " " + id;
    }
    
}
