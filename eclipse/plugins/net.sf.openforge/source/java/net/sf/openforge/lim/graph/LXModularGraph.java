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

package net.sf.openforge.lim.graph;

import java.text.SimpleDateFormat;

import java.io.*;
import java.util.*;

import net.sf.openforge.lim.*;
import net.sf.openforge.lim.io.*;
import net.sf.openforge.lim.memory.*;
import net.sf.openforge.util.naming.ID;

/**
 * LXModularGraph is a specialized Visitor which creates a seperate
 * dot graph for each Module and ties them together via an HTML
 * interface for perusing the graphs.  It execs a 'dot' process to
 * convert the graphs to PNG format.
 */
public class LXModularGraph extends DefaultVisitor
{
    private static final String _RCS_ = "$Rev: 108 $";

    private boolean doPNGConvert = true;
    
    PrintStream callListPS;
    File directory;
    Stack visitableStack = new Stack();
    Stack printStreamStack = new Stack();

    private int indentDepth = 0;

    // Captures the name of the top most level image.
    private String graphRoot = null;
    
    public LXModularGraph (File directory)
    {
        this.directory = directory;
        System.err.println("Generating modular graph to directory " + directory);
        System.err.println("\tBe patient, this may take a while for large designs");
        
        this.callListPS = null;
        try
        {
            // The list of calls, ie the call tree hierarchy
            File callList = new File(directory, "callList.html");
            callList.createNewFile();
            this.callListPS = new PrintStream(new FileOutputStream(callList));
            openCallList(this.callListPS);

            // The top level, may be design, or whatever module the
            // use starts with.
            File rootFile = new File(directory, "root.html");
            printStreamStack.push(openCallStream(rootFile));
            writeCallListEntry(rootFile.getName(), "Root");
            
            File index = new File(directory, "index.html");
            index.createNewFile();
            PrintStream ps = new PrintStream(new FileOutputStream(index));
            ps.println("<html><head></head>");
            ps.println("<frameset cols=20%,80%>");
            ps.println("<frame src=\"" + callList.getName() + "\" name=\"indexFrame\">");
            ps.println("<frame src=\"" + rootFile.getName() + "\" name=\"targetFrame\">");
            ps.println("</frameset>");
            ps.println("</html>");
        }
        catch (Exception e)
        {
            assert false : e;
        }
        
        doPNGConvert = !Boolean.getBoolean("LXM_NOCONVERT");
    }

    public void visit (Design design)
    {
        graphVisitable(design);
        super.visit(design);
        exitVisitable();
    }

    public void visit (AbsoluteMemoryRead module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (AbsoluteMemoryWrite module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (ArrayRead module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (ArrayWrite module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (Block module)
    {
        // If it is a procedure body, then LXGraph wrote it out as
        // part of the call, so we don't need to here.
        if (module.isProcedureBody())
        {
            super.visit(module);
        }
        else
        {
            graphVisitable(module);
            super.visit(module);
            exitVisitable();
        }
    }
    public void visit (Branch module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    
    /**
     * Each call is written in its own HTML file to keep things small
     * enough for the browser to render.
     */
    public void visit (Call call)
    {
        String baseName = getBaseName(call);
        String callName = baseName + ".html";
        File callIndex = new File(this.directory, callName);
        PrintStream ps = openCallStream(callIndex);
        writeCallListEntry(callIndex.getName(), baseName);

        int oldDepth = this.indentDepth;
        this.indentDepth = 0;
        printStreamStack.push(ps);
        
        graphVisitable(call);
        super.visit(call);
        exitVisitable();
        
        printStreamStack.pop();
        this.indentDepth = oldDepth;
        
        closeCallStream(ps, baseName + ".png");
    }
    public void visit (Decision module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (ForBody module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (HeapRead module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (HeapWrite module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (Kicker module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (Latch module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (Loop module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (TaskCall module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (SimplePinAccess module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (FifoAccess module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (FifoRead module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (FifoWrite module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (MemoryGateway module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (MemoryRead mr)
    {
        super.visit(mr);
//         if (mr.getPhysicalComponent() != null)
//         {
//             graphVisitable(mr.getPhysicalComponent());
//             exitVisitable();
//         }
    }
    public void visit (MemoryWrite mr)
    {
        super.visit(mr);
//         if (mr.getPhysicalComponent() != null)
//         {
//             graphVisitable(mr.getPhysicalComponent());
//             exitVisitable();
//         }
    }
    public void visit (MemoryReferee module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (PinReferee module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (PinRead acc)
    {
        super.visit(acc);
        if (acc.getPhysicalComponent() != null)
        {
            graphVisitable(acc.getPhysicalComponent());
            exitVisitable();
        }
    }
    public void visit (PinWrite acc)
    {
        super.visit(acc);
        if (acc.getPhysicalComponent() != null)
        {
            graphVisitable(acc.getPhysicalComponent());
            exitVisitable();
        }
    }
    public void visit (PriorityMux module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (RegisterGateway module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (RegisterReferee module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (Scoreboard module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (Switch module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (UntilBody module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }
    public void visit (WhileBody module)
    {
        graphVisitable(module);
        super.visit(module);
        exitVisitable();
    }

    private void graphVisitable (Visitable module)
    {
        PrintStream ps = (PrintStream)printStreamStack.peek();
        visitableStack.push(module);
        this.indentDepth++;
        
        String baseName = getBaseName(module);
        String imgName = baseName + ".png";
        File dotFile = new File(directory, baseName + ".dot");
        File imgFile = new File(directory, imgName);

        if (this.graphRoot == null)
            this.graphRoot = imgName;

        LXGraph.graphTo(module, dotFile.getPath(), 1); // graph only 1 layer
        indent(ps);
        ps.println("<a href=javascript:void(); onclick=\"setSrc('" + imgName + "')\">" + baseName + "</a><br>");

        if (doPNGConvert)
        {
            String command = "dot " + dotFile.getName() + " -Tpng -o " + imgFile.getName();
            //System.out.println("Execing " + command + " in " + directory);
            try
            {
                Runtime.getRuntime().exec(command, new String[]{}, directory);
            }
            catch (Exception e)
            {
                System.err.println("Could not convert dot graph to PNG: " + e);
            }
        }
    }

    private void indent (PrintStream ps)
    {
        for (int i=1; i < indentDepth; i++)
        {
            ps.print("&nbsp;&nbsp;&nbsp;&nbsp;");
        }
    }

    private void exitVisitable()
    {
        visitableStack.pop();
        this.indentDepth--;
        
        if (visitableStack.isEmpty())
        {
            closeCallList(this.callListPS);
            assert printStreamStack.size() == 1;
            closeCallStream((PrintStream)printStreamStack.pop(), this.graphRoot);
            //convertDotToPNG();
        }
    }

    private void writeCallListEntry (String file, String name)
    {
        for (int i=1; i < printStreamStack.size(); i++)
            callListPS.print("&nbsp;&nbsp;");
        callListPS.println("<a href=\"" + file + "\" target=\"targetFrame\">" + name + "</a><br>");

        PrintStream ps = (PrintStream)printStreamStack.peek();
        indent(ps);
        ps.println("<a href=\"" + file + "\" target=\"targetFrame\">" + name + "</a><br>");
    }
    
    private void openCallList (PrintStream ps)
    {
        ps.println("<html><head>");
        ps.println("</head>");
        ps.println("<body>");
        ps.println("<center><h2>Call Hierarchy</h2></center>");

        SimpleDateFormat df = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
        ps.println("<center><font size=-1>Generated: " + df.format(new Date()) + "</font></center>");
    }
    private void closeCallList (PrintStream ps)
    {
        ps.println("</body>");
        ps.println("</html>");
        ps.flush();
        ps.close();
    }
    
    
    private PrintStream openCallStream (File file)
    {
        PrintStream ps = null;
        try
        {
            ps = new PrintStream(new FileOutputStream(file));
        }
        catch (Exception e)
        {
            assert false : "Could not open stream " + e;
        }
        openHTML (ps);
        return ps;
    }

    private void openHTML (PrintStream ps)
    {
        ps.println("<html><head>");
        ps.println("<script type=\"text/javascript\">");
        ps.println("function setSrc(imgName)");
        ps.println("{");
        ps.println("  document.getElementById(\"target\").src=imgName");
        ps.println("}");
        ps.println("function shrink()");
        ps.println("{");
        ps.println("  var img = document.getElementById(\"target\");");
        ps.println("  var x = (img.height * 4) / 5;");
        ps.println("  var y = (img.width * 4) / 5;");
        ps.println("  img.height = x;");
        ps.println("  img.width = y;");
        ps.println("}");
        ps.println("function grow()");
        ps.println("{");
        ps.println("  var img = document.getElementById(\"target\");");
        ps.println("  var x = (img.height * 5) / 4;");
        ps.println("  var y = (img.width * 5) / 4;");
        ps.println("  img.height = x;");
        ps.println("  img.width = y;");
        ps.println("}");
        ps.println("</script>");
        
        ps.println("</head>");
        ps.println("<body>");
        ps.print("<center><a href=\"javascript:void();\" onclick=\"shrink()\">Smaller Image</a>");
        ps.print("&nbsp;&nbsp");
        ps.println("<a href=\"javascript:void();\" onclick=\"grow()\">Larger Image</a></center>");
        ps.println("<table border=1><tr><td valign=top>");
    }

    private void closeCallStream (PrintStream ps, String img)
    {
        ps.println("</td><td valign=top><img id=\"target\" src=\"" + img + "\"></td></tr></table>");
        ps.println("</body>");
        ps.println("</html>");
        ps.flush();
        ps.close();
    }
    
    private String getBaseName (Object o)
    {
        return ID.showLogical(o) + "_" + Integer.toHexString(o.hashCode());
    }

    /**
     * Doesn't work, and I don't know why...
     */
    private void convertDotToPNG ()
    {
        String command = "find ./* -name \"*.dot\" -exec dot {} -Tpng -o \\{}.png \\;";
        //String command = "dot " + dotFile.getName() + " -Tpng -o " + imgFile.getName();
        System.out.println("Execing " + command + " in " + directory);
        try
        {
            Runtime.getRuntime().exec(command, new String[]{}, this.directory);
        }
        catch (Exception e)
        {
            System.err.println("Could not convert dot graph to PNG: " + e);
        }
    }
    
    
}// LXModularGraph
