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
/* $Rev: 2 $ */

package net.sf.openforge.forge.api.ucf;

/**
 * The IOStandard attribute for pins. For a full description of the
 * IOSTANDARD constraints, please see the Constraints Guide section
 * of the online documentation located
 * at:"<path-to-ISE>/doc/usenglish/manuals.pdf".
 */
public final class IOStandard implements UCFAttribute
{
    private String value;

    public final static IOStandard AGP = new IOStandard("AGP");
    public final static IOStandard GTL = new IOStandard("GTL");
    public final static IOStandard GTL_DCI = new IOStandard("GTL_DCI");
    public final static IOStandard GTLP = new IOStandard("GTLP");
    public final static IOStandard GTLP_DCI = new IOStandard("GTLP_DCI");
    public final static IOStandard HSTL_I = new IOStandard("HSTL_I");
    public final static IOStandard HSTL_I_18 = new IOStandard("HSTL_I_18");
    public final static IOStandard HSTL_I_DCI = new IOStandard("HSTL_I_DCI");
    public final static IOStandard HSTL_I_DCI_18 = new IOStandard("HSTL_I_DCI_18");
    public final static IOStandard HSTL_II = new IOStandard("HSTL_II");
    public final static IOStandard HSTL_II_18 = new IOStandard("HSTL_II_18");
    public final static IOStandard HSTL_II_DCI = new IOStandard("HSTL_II_DCI");
    public final static IOStandard HSTL_II_DCI_18 = new IOStandard("HSTL_II_DCI_18");
    public final static IOStandard HSTL_III = new IOStandard("HSTL_III");
    public final static IOStandard HSTL_III_18 = new IOStandard("HSTL_III_18");
    public final static IOStandard HSTL_III_DCI = new IOStandard("HSTL_III_DCI");
    public final static IOStandard HSTL_III_DCI_18 = new IOStandard("HSTL_III_DCI_18");
    public final static IOStandard HSTL_IV = new IOStandard("HSTL_IV");
    public final static IOStandard HSTL_IV_18 = new IOStandard("HSTL_IV_18");
    public final static IOStandard HSTL_IV_DCI = new IOStandard("HSTL_IV_DCI");
    public final static IOStandard HSTL_IV_DCI_18 = new IOStandard("HSTL_IV_DCI_18");
    public final static IOStandard LVCMOS2 = new IOStandard("LVCMOS2");
    public final static IOStandard LVCMOS12 = new IOStandard("LVCMOS12");
    public final static IOStandard LVCMOS15 = new IOStandard("LVCMOS15");
    public final static IOStandard LVCMOS18 = new IOStandard("LVCMOS18");
    public final static IOStandard LVCMOS25 = new IOStandard("LVCMOS25");
    public final static IOStandard LVCMOS33 = new IOStandard("LVCMOS33");
    public final static IOStandard LVDCI_15 = new IOStandard("LVDCI_15");
    public final static IOStandard LVDCI_18 = new IOStandard("LVDCI_18");
    public final static IOStandard LVDCI_25 = new IOStandard("LVDCI_25");
    public final static IOStandard LVDCI_33 = new IOStandard("LVDCI_33");
    public final static IOStandard LVDCI_DV2_15 = new IOStandard("LVDCI_DV2_15");
    public final static IOStandard LVDCI_DV2_18 = new IOStandard("LVDCI_DV2_18");
    public final static IOStandard LVDCI_DV2_25 = new IOStandard("LVDCI_DV2_25");
    public final static IOStandard LVDCI_DV2_33 = new IOStandard("LVDCI_DV2_33");
    public final static IOStandard LVDS = new IOStandard("LVDS");
    public final static IOStandard LVTTL = new IOStandard("LVTTL");
    public final static IOStandard PCI33_3 = new IOStandard("PCI33_3");
    public final static IOStandard PCI33_5 = new IOStandard("PCI33_5");
    public final static IOStandard PCI66_3 = new IOStandard("PCI66_3");
    public final static IOStandard PCIX = new IOStandard("PCIX");
    public final static IOStandard PCIX66_3 = new IOStandard("PCIX66_3");
    public final static IOStandard SSTL18_I = new IOStandard("SSTL18_I");
    public final static IOStandard SSTL18_I_DCI = new IOStandard("SSTL18_I_DCI");
    public final static IOStandard SSTL18_II = new IOStandard("SSTL18_II");
    public final static IOStandard SSTL18_II_DCI = new IOStandard("SSTL18_II_DCI");
    public final static IOStandard SSTL2_I = new IOStandard("SSTL2_I");
    public final static IOStandard SSTL2_I_DCI = new IOStandard("SSTL2_I_DCI");
    public final static IOStandard SSTL2_II = new IOStandard("SSTL2_II");
    public final static IOStandard SSTL2_II_DCI = new IOStandard("SSTL2_II_DCI");
    public final static IOStandard SSTL3_I = new IOStandard("SSTL3_I");
    public final static IOStandard SSTL3_I_DCI = new IOStandard("SSTL3_I_DCI");
    public final static IOStandard SSTL3_II = new IOStandard("SSTL3_II");
    public final static IOStandard SSTL3_II_DCI = new IOStandard("SSTL_II_DCI");

    
    public IOStandard(String value)
    {
        this.value = value;
    }
    
    /**
     * Produces the ucf attribute "IOSTANDARD=value".
     */
    public String toString()
    {
        return "IOSTANDARD="+value;
    }
    
    /**
     * @see net.sf.openforge.forge.api.ucf.UCFAttribute#getBit
     */
    public int getBit()
    {
        return -1;
    }
    
}


