/**
 * XRONOS, High Level Synthesis of Streaming Applications
 * 
 * Copyright (C) 2014 EPFL SCI STI MM
 * 
 * This file is part of XRONOS.
 * 
 * XRONOS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * XRONOS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with XRONOS.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additional permission under GNU GPL version 3 section 7
 * 
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or
 * an Eclipse library), containing parts covered by the terms of the
 * Eclipse Public License (EPL), the licensors of this Program grant you
 * additional permission to convey the resulting work.  Corresponding Source
 * for a non-source form of such a combination shall include the source code
 * for the parts of Eclipse libraries used as well as that of the  covered work.
 */
package org.xronos.systemc;

import java.math.BigInteger;
import net.sf.orcc.backends.c.CTemplate;
import net.sf.orcc.ir.ExprInt;
import net.sf.orcc.ir.TypeBool;
import net.sf.orcc.ir.TypeInt;
import net.sf.orcc.ir.TypeString;
import net.sf.orcc.ir.TypeUint;
import org.eclipse.xtend2.lib.StringConcatenation;

/**
 * @author Endri Bezati
 */
@SuppressWarnings("all")
public class SystemCTemplate extends CTemplate {
  public CharSequence caseTypeBool(final TypeBool type) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("bool");
    return _builder;
  }
  
  public CharSequence caseTypeInt(final TypeInt type) {
    StringConcatenation _builder = new StringConcatenation();
    {
      int _sizeInBits = type.getSizeInBits();
      boolean _lessEqualsThan = (_sizeInBits <= 64);
      if (_lessEqualsThan) {
        _builder.append("sc_int");
      } else {
        _builder.append("sc_bigint");
      }
    }
    _builder.append("<");
    int _size = type.getSize();
    _builder.append(_size, "");
    _builder.append(">");
    return _builder;
  }
  
  public CharSequence caseTypeUint(final TypeUint type) {
    StringConcatenation _builder = new StringConcatenation();
    {
      int _sizeInBits = type.getSizeInBits();
      boolean _lessEqualsThan = (_sizeInBits <= 64);
      if (_lessEqualsThan) {
        _builder.append("sc_uint");
      } else {
        _builder.append("sc_biguint");
      }
    }
    _builder.append("<");
    int _size = type.getSize();
    _builder.append(_size, "");
    _builder.append(">");
    return _builder;
  }
  
  public CharSequence caseTypeString(final TypeString type) {
    StringConcatenation _builder = new StringConcatenation();
    _builder.append("string");
    return _builder;
  }
  
  public CharSequence caseExprInt(final ExprInt object) {
    CharSequence _xblockexpression = null;
    {
      final BigInteger value = object.getValue();
      CharSequence _xifexpression = null;
      boolean _or = false;
      BigInteger _valueOf = BigInteger.valueOf(Integer.MIN_VALUE);
      int _compareTo = value.compareTo(_valueOf);
      boolean _lessThan = (_compareTo < 0);
      if (_lessThan) {
        _or = true;
      } else {
        BigInteger _valueOf_1 = BigInteger.valueOf(Integer.MAX_VALUE);
        int _compareTo_1 = value.compareTo(_valueOf_1);
        boolean _greaterThan = (_compareTo_1 > 0);
        _or = _greaterThan;
      }
      if (_or) {
        StringConcatenation _builder = new StringConcatenation();
        _builder.append(value, "");
        _builder.append("L");
        _xifexpression = _builder;
      } else {
        StringConcatenation _builder_1 = new StringConcatenation();
        _builder_1.append(value, "");
        _xifexpression = _builder_1;
      }
      _xblockexpression = _xifexpression;
    }
    return _xblockexpression;
  }
}
