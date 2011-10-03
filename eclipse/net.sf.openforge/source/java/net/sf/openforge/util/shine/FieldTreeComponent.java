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
import javax.swing.tree.*;   
import java.awt.*;
import java.util.*;
import java.lang.reflect.*;

class FieldTreeComponent extends JScrollPane
{
    static final String rcs_id = "RCS_REVISION: $Rev: 2 $";
    static boolean oncet=false;
    private ObjectInspector oi;
    private ShineTreeFrame sFrame;
    private JTree tree;
    
    FieldTreeComponent(ShineTreeFrame sFrame,ObjectInspector oi)
    {
        this.sFrame=sFrame;
        this.oi=oi;
        tree=new JTree(new MyTreeNode(oi));
        tree.setToggleClickCount(2);
        tree.setCellRenderer(new MyTreeCellRenderer());
        tree.setLargeModel(true);
        setViewportView(tree);	
    }

    Object getSelectedObject()
    {
        TreePath tp=tree.getLeadSelectionPath();
        if(tp!=null)
        {
            MyTreeNode tn=(MyTreeNode)tp.getLastPathComponent();
            Object o=tn.me();
            if(tn.isInspector())
            {
                return ((ObjectInspector)o).getMyObject();
            }
            else
                return o;
        }
        return null;
    }
    
    class MyTreeNode implements TreeNode
    {
        Object me=null;
        TreeNode parent=null;
        ObjectInspector oi=null;
        String fieldName;
        boolean isInspector;
        private final int ARRAY_GLOM=25;
        private final int TOSTRING_MAX=80;
        
        public MyTreeNode(Object me)
        {
            this(null,me,true,"ROOT");
        }
        
        public MyTreeNode(TreeNode parent,Object me,boolean isInspector,String fieldName)
        {
            this.me=me;
            this.fieldName=fieldName;
            this.parent=parent;
            this.isInspector=isInspector;
            if(isInspector)
                this.oi=(ObjectInspector)me;
        } 

        public String getName()
        {
            return fieldName;
        }

        public String getData()
        {
            StringBuffer sb=new StringBuffer();
            if(isInspector)
            {
                if(oi.isArray())
                    sb.append(oi.getCount());
                sb.append(oi.getMyObject().getClass().getName());
            }
            else
                sb.append(me.getClass().getName());
            sb.append(" :: ");
            

            Object obj=(isInspector)?oi.getMyObject():me;
            
            // here we have a couple things. if it is non null, and an array
            // of primitive types, glom ARRAY_GLOM things together.
            if((obj.getClass().isArray())&&
                (obj.getClass().getComponentType().isPrimitive()))
            {
                sb.append("[ ");
                int arCount=Array.getLength(obj);
                for(int i=0;(i<ARRAY_GLOM)&&(i<arCount);i++)
                {
                    if(i!=0)
                        sb.append(" , ");
                    sb.append(Array.get(obj,i).toString());
                }
                if(arCount>ARRAY_GLOM)
                    sb.append(" ... ]");
                else
                    sb.append(" ]");
            }
            else
            {
                // choke the max length here...
                String test=obj.toString();
                if(test.length()>TOSTRING_MAX)
                {
                    sb.append(test.substring(0,TOSTRING_MAX));
                    sb.append(" ...");
                }
                else
                    sb.append(test);
            }
            return sb.toString();
        }

        public String toString()
        {
            return getName()+" :: "+getData();
        }
        
        public boolean isInspector() { return isInspector; }
 
        public Object me() { return me; }
         
        public TreeNode getChildAt(int childIndex)
        {
            if(isInspector)
            {
                Object child=oi.getValue(childIndex);
                if(child==null)
                {
                    return new MyTreeNode(this,"<null>",false,(String)oi.getName(childIndex));
                }
                if(oi.isRef(childIndex))
                {
                    return new MyTreeNode(this,new ObjectInspector(child),true,(String)oi.getName(childIndex));
                }
                else
                    return new MyTreeNode(this,child,false,(String)oi.getName(childIndex));
            }
            return null;
        }
        
        public int getChildCount()
        {
            if(isInspector)
                return oi.getCount();
            else
                return 0;
        }

        public TreeNode getParent()
        {
            return parent;
        }

        public int getIndex(TreeNode node)
        {
            if(isInspector)
            {
                MyTreeNode child=(MyTreeNode)node;
                // target object value...
                Object test=child.me();
                for(int i=0;i<oi.getCount();i++)
                {
                    if(((oi.getValue(i)!=null)&&(oi.getValue(i).equals(test))))
                        return i;
                }
            }
            return -1;
        }
        
        public boolean getAllowsChildren() { return true; }

        public boolean isLeaf()
        {
            if(isInspector)
            {
                if(oi.getCount()>0)
                    return false;
            }
            return true;
        }

        public Enumeration children()
        {
            if(isInspector)
                return new myEnumeration();
            else
                return null;
        }
        
        class myEnumeration implements Enumeration
        {
            int current=(-1);
            
            public Object nextElement()
            {
                if(++current>=getChildCount())
                    throw new NoSuchElementException("Index: "+current);
                return getChildAt(current);
            }
            
            public boolean hasMoreElements()
            {
                return ((current+1)<getChildCount());
            }        
        }
        
    }

    class MyTreeCellRenderer extends JPanel
        implements TreeCellRenderer
    {
        private JLabel name;
        private JLabel data;
        
        public MyTreeCellRenderer()
        {
            super(false);
            setLayout(new FlowLayout(FlowLayout.LEFT,0,0));

            name=new JLabel();
            name.setFont(new Font("Monospaced",Font.BOLD,12));
            name.setHorizontalAlignment(SwingConstants.CENTER);
            name.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
            
            data=new JLabel();
            data.setFont(new Font("Monospaced",Font.PLAIN,12));
            data.setHorizontalAlignment(SwingConstants.CENTER);
            data.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

            setOpaque(false);

            add(name);
            add(data);
        }

        public Component getTreeCellRendererComponent(JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            if(selected)
            {
                name.setOpaque(true);
                name.setForeground(UIManager.getColor("Tree.selectionForeground"));
                data.setOpaque(true);
                data.setForeground(UIManager.getColor("Tree.selectionForeground"));
            }
            else
            {
                name.setOpaque(false);
                name.setForeground(UIManager.getColor("Tree.textForeground"));
                data.setOpaque(false);
                data.setForeground(UIManager.getColor("Tree.textForeground"));
            }
            MyTreeNode node=(MyTreeNode)value;
            name.setText(node.getName()+": ");
            data.setText(node.getData());
            return this;
        }
    }
}


