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
package net.sf.openforge.verilog.model;

import java.util.*;

/**
 * Directive is the parent class for any Verilog directive. It is a
 * fully qualified statement starting with a backquote followed
 * by a directive token.
 *
 * <p>Created: Mon Aug 26 13:47:38 2002
 *
 * @author abk, last modified by $Author: imiller $
 * @version $Id: Directive.java 2 2005-06-09 20:00:48Z imiller $
 */
public abstract class Directive implements Statement
{
    private static final String _RCS_ = "$Rev: 2 $";

    Keyword directive;
    
    protected Directive (Keyword directive)
    {
        this.directive = directive;
    }

    /**
     * The Identifier which is the compiler directive.
     */
    public Keyword getDirectiveToken()
    {
        return directive;
    }
    
    public Collection getNets ()
    {
        return Collections.EMPTY_SET;
    }

    
    public Lexicality lexicalify ()
    {
        Lexicality lex = new Lexicality();
        lex.append(getDirectiveToken());
        return lex;
    }

    public String toString ()
    {
        return lexicalify().toString();
    }
    
    public static class Define extends Directive
    {
        ArbitraryString macro;
        ArbitraryString substitution;
        
        /**
         * Constructs a new Define directive specifying a text substitution.
         *
         * @param macro the name which will be replaced
         * @param substitution the text which will replace the macro name
         */
        public Define (String macro, String substitution)
        {
            super(Keyword.DEFINE);
            this.macro = new ArbitraryString(macro);
            this.substitution = new ArbitraryString(substitution);
        }
        
        public Lexicality lexicalify()
        {
            Lexicality lex = super.lexicalify();
            lex.append(macro);
            lex.append(substitution);
            return lex;
        }
    }
    
    public static class UnDefine extends Directive
    {
        ArbitraryString macro;
        
        /**
         * Constructs a new UnDefine directive specifying the end of a macro.
         *
         * @param macro the name of the macro which is being undefined
         */
        public UnDefine (String macro)
        {
            super(Keyword.UNDEF);
            this.macro = new ArbitraryString(macro);
        }
        
        public Lexicality lexicalify()
        {
            Lexicality lex = super.lexicalify();
            lex.append(macro);
            return lex;
        }
    }
    
    public static class IfDef extends Directive
    {
        ArbitraryString macro;
        
        /**
         * Constructs a new IfDef directive specifying conditional compilation
         * based on the defined status of a macro.
         *
         * @param macro the name of the macro to be tested
         */
        public IfDef (String macro)
        {
            super(Keyword.IFDEF);
            this.macro = new ArbitraryString(macro);
        }
        
        public Lexicality lexicalify()
        {
            Lexicality lex = super.lexicalify();
            lex.append(macro);
            return lex;
        }
    }
    
    public static class Else extends Directive
    {
        /**
         * Constructs a new Else directive, which indicates the optional (false)
         * branch of an IfDef.
         */
        public Else ()
        {
            super(Keyword.ELSE_DIRECTIVE);
        }
    }
    
    public static class EndIf extends Directive
    {
        /**
         * Constructs a new EndIf directive, which indicates the end of
         * an IfDef section.
         */
        public EndIf ()
        {
            super(Keyword.ENDIF);
        }
    }
    
    public static class DefaultNetType extends Directive
    {
        Keyword nettype;
        
        /**
         * Constructs a new DefaultNetType directive specifying the
         * default net type for implicit net declarations. The Keyword
         * used to specify the net type must pass Keyword.isWireword().
         *
         * @param nettype the net type
         */
        public DefaultNetType (Keyword nettype)
        {
            super(Keyword.DEFAULT_NETTYPE);
            assert (Keyword.isWireword(nettype)) : 
                "defaultNettype directive must use a net type, not a " + nettype.toString();
            
            this.nettype = nettype;
        }
        
        public Lexicality lexicalify()
        {
            Lexicality lex = super.lexicalify();
            lex.append(nettype);
            return lex;
        }
    }
    
        
    public static class Include extends Directive
    {
        ArbitraryString filename;
        
        protected Include()
        {
            super(Keyword.INCLUDE);
        }
        
        /**
         * Constructs a new Include directive specifying a file to include.
         *
         * @param filename the name of the file to include
         */
        public Include (String filename)
        {
            this();
            this.filename = new ArbitraryString(filename);
        }
        
        protected Token getFilename()
        {
            return filename;
        }
        
        public Lexicality lexicalify()
        {
            Lexicality lex = super.lexicalify();
            lex.append(Symbol.QUOTE);
            lex.append(getFilename());
            lex.append(Symbol.QUOTE);
            lex.append(Control.NEWLINE);
            return lex;
            
        }
    }
    
    public static class ResetAll extends Directive
    {
        /**
         * Constructs a new ResetAll directive, which resets all compiler
         * directives to their default value.
         */
        public ResetAll ()
        {
            super(Keyword.RESETALL);
        }
    }
    
    public static class TimeScale extends Directive
    {
        Constant timeUnitValue;
        Keyword timeUnitUnits;
        Constant timePrecisionValue;
        Keyword timePrecisionUnits;
        
        /**
         * Constructs a new TimeScale directive specifying the relative
         * scale of time. Both the unit and precision paramters should
         * be made up of values from 1, 10, and 100 and units from
         * s, ms, us, ns, ps, and fs.
         *
         * @param unitValue the number of units
         * @param unitUnit the units of the unit value
         * @param precisionValue the precision of time
         * @param precisionUnit the units of the precision value
         */
        public TimeScale (int unitValue, Keyword unitUnits, 
            int precisionValue, Keyword precisionUnits)
        {
            this(new Constant(unitValue), unitUnits,
                new Constant(precisionValue), precisionUnits);
        }
        
        /**
         * Constructs a new TimeScale directive specifying the relative
         * scale of time.
         *
         * @param unitValue the number of units
         * @param unitUnit the units of the unit value
         * @param precisionValue the precision of time
         * @param precisionUnit the units of the precision value
         */
        public TimeScale (Constant unitValue, Keyword unitUnits, 
            Constant precisionValue, Keyword precisionUnits)
        {
            super(Keyword.TIMESCALE);
            long value = unitValue.longValue();
            assert ((value == 1) || (value == 10) || (value == 100)) :
                "Value of timescale spec must by 1, 10, or 100, not " + unitValue.toString();
            this.timeUnitValue = unitValue;
            
            assert (Keyword.isUnitword(unitUnits)) :
                "Units must be one of s, ms, us, ns, ps or fs for timescale directive, not" +
                unitUnits.toString();
            this.timeUnitUnits = unitUnits;
            
            value = precisionValue.longValue();
            assert ((value == 1) || (value == 10) || (value == 100)) :
                "Value of timescale spec must by 1, 10, or 100, not " + precisionValue.toString();
                
            this.timePrecisionValue = precisionValue;
            
            assert (Keyword.isUnitword(precisionUnits)) :
                "Units must be one of s, ms, us, ns, ps or fs for timescale directive, not " + 
                precisionValue.toString();
            this.timePrecisionUnits = precisionUnits;
        }
        
        public Lexicality lexicalify()
        {
            Lexicality lex = super.lexicalify();
            lex.append(timeUnitValue);
            lex.append(timeUnitUnits);
            lex.append(Symbol.DIVIDE);
            lex.append(timePrecisionValue);
            lex.append(timePrecisionUnits);
            return lex;
        }
    }
    
    public static class UnconnectedPullUp extends Directive
    {
        static final ArbitraryString pull = new ArbitraryString("pull1");
        /**
         * Constructs a new UnconnectedDrive directive, specifying
         * that any unconnected input ports should be pulled up.
         */
        public UnconnectedPullUp ()
        {
            super(Keyword.UNCONNECTED_DRIVE);
        }
        
        public Lexicality lexicalify()
        {
            Lexicality lex = super.lexicalify();
            lex.append(pull);
            return lex;
        }
    }
    
    public static class UnconnectedPullDown extends Directive
    {
        static final ArbitraryString pull = new ArbitraryString("pull0");
        /**
         * Constructs a new UnconnectedDrive directive, specifying
         * that any unconnected input ports should be pulled up.
         */
        public UnconnectedPullDown ()
        {
            super(Keyword.UNCONNECTED_DRIVE);
        }
        
        public Lexicality lexicalify()
        {
            Lexicality lex = super.lexicalify();
            lex.append(pull);
            return lex;
        }
    }
    
    public static class NoUnconnectedDrive extends Directive
    {
        /**
         * Constructs a new Else directive, which indicates the optional (false)
         * branch of an IfDef.
         */
        public NoUnconnectedDrive ()
        {
            super(Keyword.NOUNCONNECTED_DRIVE);
        }
    }
    
    public static class CellDefine extends Directive
    {
        /**
         * Constructs a new Else directive, which indicates the optional (false)
         * branch of an IfDef.
         */
        public CellDefine ()
        {
            super(Keyword.CELLDEFINE);
        }
    }
    
    public static class EndCellDefine extends Directive
    {
        /**
         * Constructs a new Else directive, which indicates the optional (false)
         * branch of an IfDef.
         */
        public EndCellDefine ()
        {
            super(Keyword.ENDCELLDEFINE);
        }
    }
}// Directive
