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

/**
 * The <code>Tester</code> interface is implemented by classes that
 * are capable of testing a {@link Design}.  When a {@link Design} is
 * created, an appropriate <code>Tester</code> is created which can be
 * accessed by the {@link net.sf.openforge.verilog.testbench.TestbenchWriter} 
 * to generate a self verifying HDL test bench.  This interface
 * doesn't put any requirements on the implementation about how the
 * testing should occur, only that it be able to supply the number of
 * vectors created, and report the {@link Task} the vector applies to
 * along with the arguments to supply, the expected result (if any)
 * and wether to verify the result.  It is expected that if the
 * testing process will disturb the Forge memory image of the user's
 * design (i.e. running the program using reflection would change
 * field values) then the test run should not occur until the
 * <code>runTests()</code> method is called.
 *
 * @author <a href="mailto:Jonathan.Harris@xilinx.com">Jonathan
 * C. Harris</a>
 *
 */
public interface Tester
{

    /**
     * Sets the Design on which to apply this tester.
     */
    public void setDesign (Design design);
    
   /**
     * Causes this <code>Tester</code> to execute its tests.
     *
     */
    public void runTests();

    
    /**
     * @return number of vectors contained in the test set.
     */
    public int getVectorCount();

    
    /**
     * @param index of the {@link Task} vector to retrieve
     * @return {@link Task} accessed on the retrieved vector
     */
    public Task getTaskVector(int index);

    
    /**
     * @param index of the argument vector to retrieve
     * @return <code>Object[]</code> of arguments for the given
     * vector.  Each element of the array will be an encapsulating
     * Object such as <code>java.lang.Integer</code> for an argument
     * of type </code>int</code>.
     */
    public Object[] getArgsVector(int index);

    
    /**
     * @param index of the result vector to retrieve
     * @return <code>Object</code> encapsulating the expected result
     * (i.e. java.lang.Integer).  <code>null</code> if the result
     * isn't valid, as indicated by the method <code>getResultValidVector</code>
     */
    public Object getResultVector(int index);

    
    /**
     * @param index of the result valid vector to retrieve
     * @return <code>boolean</code> indicating if the result obtained
     * with <code>getResultVector</code> is valid.  If this method
     * returns <code>false</code> then the vector should complete in
     * hardware, but the result is unknown (i.e. divide by zero).
     */
    public boolean getResultValidVector(int index);
}
