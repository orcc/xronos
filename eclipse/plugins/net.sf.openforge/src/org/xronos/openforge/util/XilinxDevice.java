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

package org.xronos.openforge.util;

import java.util.ArrayList;
import java.util.List;

public class XilinxDevice {

	public static final int VIRTEX = 0;
	public static final int VIRTEX2P = 1;
	public static final int VIRTEX2 = 2;
	public static final int VIRTEXE = 3;
	public static final int SPARTAN = 4;
	public static final int SPARTAN2 = 5;
	public static final int SPARTAN2E = 6;
	public static final int SPARTANXL = 7;
	public static final int XC3000 = 8;
	public static final int XC4000 = 9;
	public static final int XC4000EX = 10;
	public static final int XC4000E = 11;
	public static final int XC4000L = 12;
	public static final int XC4000XLA = 13;
	public static final int XC4000XV = 14;
	public static final int XC5200 = 15;
	public static final int XC9500 = 16;
	public static final int XC9500XL = 17;
	public static final int XC9500XV = 18;
	public static final int SPARTAN3 = 19;
	public static final int VIRTEX4SX = 20;
	public static final int VIRTEX4FX = 21;
	public static final int VIRTEX4LX = 22;

	private static final String[] families = { "VIRTEX", "VIRTEX2P", "VIRTEX2",
			"VIRTEXE", "SPARTAN", "SPARTAN2", "SPARTAN2E", "SPARTANXL",
			"XC3000", "XC4000", "XC4000EX", "XC4000E", "XC4000L", "XC4000XLA",
			"XC4000XV", "XC5200", "XC9500", "XC9500XL", "XC9500XV", "SPARTAN3",
			"VIRTEX4SX", "VIRTEX4FX", "VIRTEX4LX" };

	private static final String[] abbreviations = { "v", "2vp", "2v", "v", "s",
			"2s", "2s", "s", "30", "40", "40", "40", "40", "40", "40", "52",
			"95", "95", "95", "3s", "4vsx", "4vfx", "4vlx" };

	private static final String[] suffixes = { "", "", "", "e", "", "", "e",
			"xl", "", "", "ex", "e", "l", "xla", "xv", "", "", "xl", "xv", "",
			"", "", "" };

	private static final XilinxDevice[] knownParts;

	private int family = -1;
	private String device = "";
	private int speed = 0;
	private String pckage = "";
	private String temp = "";
	private String input;
	private boolean isXilinxDevice = true;

	static {
		// Populate the knownParts array.

		List<XilinxDevice> al = new ArrayList<XilinxDevice>(310);

		al.add(new XilinxDevice("3s50-5-pq208C"));
		al.add(new XilinxDevice("3s50-4-pq208C"));
		al.add(new XilinxDevice("3s50-5-tq144C"));
		al.add(new XilinxDevice("3s50-4-tq144C"));
		al.add(new XilinxDevice("3s50-5-vq100C"));
		al.add(new XilinxDevice("3s50-4-vq100C"));
		al.add(new XilinxDevice("3s200-5-ft256C"));
		al.add(new XilinxDevice("3s200-4-ft256C"));
		al.add(new XilinxDevice("3s200-5-pq208C"));
		al.add(new XilinxDevice("3s200-4-pq208C"));
		al.add(new XilinxDevice("3s200-5-tq144C"));
		al.add(new XilinxDevice("3s200-4-tq144C"));
		al.add(new XilinxDevice("3s200-5-vq100C"));
		al.add(new XilinxDevice("3s200-4-vq100C"));
		al.add(new XilinxDevice("3s400-5-fg320C"));
		al.add(new XilinxDevice("3s400-4-fg320C"));
		al.add(new XilinxDevice("3s400-5-fg456C"));
		al.add(new XilinxDevice("3s400-4-fg456C"));
		al.add(new XilinxDevice("3s400-5-ft256C"));
		al.add(new XilinxDevice("3s400-4-ft256C"));
		al.add(new XilinxDevice("3s400-5-pq208C"));
		al.add(new XilinxDevice("3s400-4-pq208C"));
		al.add(new XilinxDevice("3s400-5-tq144C"));
		al.add(new XilinxDevice("3s400-4-tq144C"));
		al.add(new XilinxDevice("3s1000-5-fg320C"));
		al.add(new XilinxDevice("3s1000-4-fg320C"));
		al.add(new XilinxDevice("3s1000-5-fg456C"));
		al.add(new XilinxDevice("3s1000-4-fg456C"));
		al.add(new XilinxDevice("3s1000-5-fg676C"));
		al.add(new XilinxDevice("3s1000-4-fg676C"));
		al.add(new XilinxDevice("3s1000-5-ft256C"));
		al.add(new XilinxDevice("3s1000-4-ft256C"));
		al.add(new XilinxDevice("3s1000l-4-ft256C"));
		al.add(new XilinxDevice("3s1000l-4-fg320C"));
		al.add(new XilinxDevice("3s1000l-4-fg456C"));
		al.add(new XilinxDevice("3s1500-5-fg320C"));
		al.add(new XilinxDevice("3s1500-4-fg320C"));
		al.add(new XilinxDevice("3s1500-5-fg456C"));
		al.add(new XilinxDevice("3s1500-4-fg456C"));
		al.add(new XilinxDevice("3s1500-5-fg676C"));
		al.add(new XilinxDevice("3s1500-4-fg676C"));
		al.add(new XilinxDevice("3s1500l-4-fg320C"));
		al.add(new XilinxDevice("3s1500l-4-fg456C"));
		al.add(new XilinxDevice("3s1500l-4-fg676C"));
		al.add(new XilinxDevice("3s2000-5-fg676C"));
		al.add(new XilinxDevice("3s2000-4-fg676C"));
		al.add(new XilinxDevice("3s2000-5-fg900C"));
		al.add(new XilinxDevice("3s2000-4-fg900C"));
		al.add(new XilinxDevice("3s4000-5-fg900C"));
		al.add(new XilinxDevice("3s4000-4-fg900C"));
		al.add(new XilinxDevice("3s4000-5-fg1156C"));
		al.add(new XilinxDevice("3s4000-4-fg1156C"));
		al.add(new XilinxDevice("3s4000l-4-fg900C"));
		al.add(new XilinxDevice("3s5000-5-fg900C"));
		al.add(new XilinxDevice("3s5000-4-fg900C"));
		al.add(new XilinxDevice("3s5000-5-fg1156C"));
		al.add(new XilinxDevice("3s5000-4-fg1156C"));
		al.add(new XilinxDevice("3s100e-4-pq208C"));
		al.add(new XilinxDevice("3s100e-4-tq144C"));
		al.add(new XilinxDevice("3s100e-4-vq100C"));
		al.add(new XilinxDevice("3s250e-4-ft256C"));
		al.add(new XilinxDevice("3s250e-4-pq208C"));
		al.add(new XilinxDevice("3s250e-4-tq144C"));
		al.add(new XilinxDevice("3s250e-4-vq100C"));
		al.add(new XilinxDevice("3s500e-4-fg320C"));
		al.add(new XilinxDevice("3s500e-4-ft256C"));
		al.add(new XilinxDevice("3s500e-4-pq208C"));
		al.add(new XilinxDevice("3s1200e-4-fg320C"));
		al.add(new XilinxDevice("3s1200e-4-fg484C"));
		al.add(new XilinxDevice("3s1200e-4-ft256C"));
		al.add(new XilinxDevice("3s1200e-4-pq208C"));
		al.add(new XilinxDevice("2v40-6-cs144C"));
		al.add(new XilinxDevice("2v40-5-cs144C"));
		al.add(new XilinxDevice("2v40-4-cs144C"));
		al.add(new XilinxDevice("2v40-6-fg256C"));
		al.add(new XilinxDevice("2v40-5-fg256C"));
		al.add(new XilinxDevice("2v40-4-fg256C"));
		al.add(new XilinxDevice("2v80-6-cs144C"));
		al.add(new XilinxDevice("2v80-5-cs144C"));
		al.add(new XilinxDevice("2v80-4-cs144C"));
		al.add(new XilinxDevice("2v80-6-fg256C"));
		al.add(new XilinxDevice("2v80-5-fg256C"));
		al.add(new XilinxDevice("2v80-4-fg256C"));
		al.add(new XilinxDevice("2v250-6-cs144C"));
		al.add(new XilinxDevice("2v250-5-cs144C"));
		al.add(new XilinxDevice("2v250-4-cs144C"));
		al.add(new XilinxDevice("2v250-6-fg256C"));
		al.add(new XilinxDevice("2v250-5-fg256C"));
		al.add(new XilinxDevice("2v250-4-fg256C"));
		al.add(new XilinxDevice("2v250-6-fg456C"));
		al.add(new XilinxDevice("2v250-5-fg456C"));
		al.add(new XilinxDevice("2v250-4-fg456C"));
		al.add(new XilinxDevice("2v500-6-fg256C"));
		al.add(new XilinxDevice("2v500-5-fg256C"));
		al.add(new XilinxDevice("2v500-4-fg256C"));
		al.add(new XilinxDevice("2v500-6-fg456C"));
		al.add(new XilinxDevice("2v500-5-fg456C"));
		al.add(new XilinxDevice("2v500-4-fg456C"));
		al.add(new XilinxDevice("2v1000-6-bg575C"));
		al.add(new XilinxDevice("2v1000-5-bg575C"));
		al.add(new XilinxDevice("2v1000-4-bg575C"));
		al.add(new XilinxDevice("2v1000-6-ff896C"));
		al.add(new XilinxDevice("2v1000-5-ff896C"));
		al.add(new XilinxDevice("2v1000-4-ff896C"));
		al.add(new XilinxDevice("2v1000-6-fg256C"));
		al.add(new XilinxDevice("2v1000-5-fg256C"));
		al.add(new XilinxDevice("2v1000-4-fg256C"));
		al.add(new XilinxDevice("2v1000-6-fg456C"));
		al.add(new XilinxDevice("2v1000-5-fg456C"));
		al.add(new XilinxDevice("2v1000-4-fg456C"));
		al.add(new XilinxDevice("2v1500-6-bg575C"));
		al.add(new XilinxDevice("2v1500-5-bg575C"));
		al.add(new XilinxDevice("2v1500-4-bg575C"));
		al.add(new XilinxDevice("2v1500-6-ff896C"));
		al.add(new XilinxDevice("2v1500-5-ff896C"));
		al.add(new XilinxDevice("2v1500-4-ff896C"));
		al.add(new XilinxDevice("2v1500-6-fg676C"));
		al.add(new XilinxDevice("2v1500-5-fg676C"));
		al.add(new XilinxDevice("2v1500-4-fg676C"));
		al.add(new XilinxDevice("2v2000-6-bf957C"));
		al.add(new XilinxDevice("2v2000-5-bf957C"));
		al.add(new XilinxDevice("2v2000-4-bf957C"));
		al.add(new XilinxDevice("2v2000-6-bg575C"));
		al.add(new XilinxDevice("2v2000-5-bg575C"));
		al.add(new XilinxDevice("2v2000-4-bg575C"));
		al.add(new XilinxDevice("2v2000-6-ff896C"));
		al.add(new XilinxDevice("2v2000-5-ff896C"));
		al.add(new XilinxDevice("2v2000-4-ff896C"));
		al.add(new XilinxDevice("2v2000-6-fg676C"));
		al.add(new XilinxDevice("2v2000-5-fg676C"));
		al.add(new XilinxDevice("2v2000-4-fg676C"));
		al.add(new XilinxDevice("2v3000-6-bf957C"));
		al.add(new XilinxDevice("2v3000-5-bf957C"));
		al.add(new XilinxDevice("2v3000-4-bf957C"));
		al.add(new XilinxDevice("2v3000-6-bg728C"));
		al.add(new XilinxDevice("2v3000-5-bg728C"));
		al.add(new XilinxDevice("2v3000-4-bg728C"));
		al.add(new XilinxDevice("2v3000-6-ff1152C"));
		al.add(new XilinxDevice("2v3000-5-ff1152C"));
		al.add(new XilinxDevice("2v3000-4-ff1152C"));
		al.add(new XilinxDevice("2v3000-6-fg676C"));
		al.add(new XilinxDevice("2v3000-5-fg676C"));
		al.add(new XilinxDevice("2v3000-4-fg676C"));
		al.add(new XilinxDevice("2v4000-6-bf957C"));
		al.add(new XilinxDevice("2v4000-5-bf957C"));
		al.add(new XilinxDevice("2v4000-4-bf957C"));
		al.add(new XilinxDevice("2v4000-6-ff1152C"));
		al.add(new XilinxDevice("2v4000-5-ff1152C"));
		al.add(new XilinxDevice("2v4000-4-ff1152C"));
		al.add(new XilinxDevice("2v4000-6-ff1517C"));
		al.add(new XilinxDevice("2v4000-5-ff1517C"));
		al.add(new XilinxDevice("2v4000-4-ff1517C"));
		al.add(new XilinxDevice("2v6000-6-bf957C"));
		al.add(new XilinxDevice("2v6000-5-bf957C"));
		al.add(new XilinxDevice("2v6000-4-bf957C"));
		al.add(new XilinxDevice("2v6000-6-ff1152C"));
		al.add(new XilinxDevice("2v6000-5-ff1152C"));
		al.add(new XilinxDevice("2v6000-4-ff1152C"));
		al.add(new XilinxDevice("2v6000-6-ff1517C"));
		al.add(new XilinxDevice("2v6000-5-ff1517C"));
		al.add(new XilinxDevice("2v6000-4-ff1517C"));
		al.add(new XilinxDevice("2v8000-5-ff1152C"));
		al.add(new XilinxDevice("2v8000-4-ff1152C"));
		al.add(new XilinxDevice("2v8000-5-ff1517C"));
		al.add(new XilinxDevice("2v8000-4-ff1517C"));
		al.add(new XilinxDevice("2vp2-7-fg256C"));
		al.add(new XilinxDevice("2vp2-6-fg256C"));
		al.add(new XilinxDevice("2vp2-5-fg256C"));
		al.add(new XilinxDevice("2vp2-7-fg456C"));
		al.add(new XilinxDevice("2vp2-6-fg456C"));
		al.add(new XilinxDevice("2vp2-5-fg456C"));
		al.add(new XilinxDevice("2vp2-7-ff672C"));
		al.add(new XilinxDevice("2vp2-6-ff672C"));
		al.add(new XilinxDevice("2vp2-5-ff672C"));
		al.add(new XilinxDevice("2vp4-7-fg256C"));
		al.add(new XilinxDevice("2vp4-6-fg256C"));
		al.add(new XilinxDevice("2vp4-5-fg256C"));
		al.add(new XilinxDevice("2vp4-7-fg456C"));
		al.add(new XilinxDevice("2vp4-6-fg456C"));
		al.add(new XilinxDevice("2vp4-5-fg456C"));
		al.add(new XilinxDevice("2vp4-7-ff672C"));
		al.add(new XilinxDevice("2vp4-6-ff672C"));
		al.add(new XilinxDevice("2vp4-5-ff672C"));
		al.add(new XilinxDevice("2vp7-7-fg456C"));
		al.add(new XilinxDevice("2vp7-6-fg456C"));
		al.add(new XilinxDevice("2vp7-5-fg456C"));
		al.add(new XilinxDevice("2vp7-7-ff672C"));
		al.add(new XilinxDevice("2vp7-6-ff672C"));
		al.add(new XilinxDevice("2vp7-5-ff672C"));
		al.add(new XilinxDevice("2vp7-7-ff896C"));
		al.add(new XilinxDevice("2vp7-6-ff896C"));
		al.add(new XilinxDevice("2vp7-5-ff896C"));
		al.add(new XilinxDevice("2vp20-7-fg676C"));
		al.add(new XilinxDevice("2vp20-6-fg676C"));
		al.add(new XilinxDevice("2vp20-5-fg676C"));
		al.add(new XilinxDevice("2vp20-7-ff896C"));
		al.add(new XilinxDevice("2vp20-6-ff896C"));
		al.add(new XilinxDevice("2vp20-5-ff896C"));
		al.add(new XilinxDevice("2vp20-7-ff1152C"));
		al.add(new XilinxDevice("2vp20-6-ff1152C"));
		al.add(new XilinxDevice("2vp20-5-ff1152C"));
		al.add(new XilinxDevice("2vpx20-7-ff896C"));
		al.add(new XilinxDevice("2vpx20-6-ff896C"));
		al.add(new XilinxDevice("2vpx20-5-ff896C"));
		al.add(new XilinxDevice("2vp30-7-fg676C"));
		al.add(new XilinxDevice("2vp30-6-fg676C"));
		al.add(new XilinxDevice("2vp30-5-fg676C"));
		al.add(new XilinxDevice("2vp30-7-ff896C"));
		al.add(new XilinxDevice("2vp30-6-ff896C"));
		al.add(new XilinxDevice("2vp30-5-ff896C"));
		al.add(new XilinxDevice("2vp30-7-ff1152C"));
		al.add(new XilinxDevice("2vp30-6-ff1152C"));
		al.add(new XilinxDevice("2vp30-5-ff1152C"));
		al.add(new XilinxDevice("2vp40-7-fg676C"));
		al.add(new XilinxDevice("2vp40-6-fg676C"));
		al.add(new XilinxDevice("2vp40-5-fg676C"));
		al.add(new XilinxDevice("2vp40-7-ff1148C"));
		al.add(new XilinxDevice("2vp40-6-ff1148C"));
		al.add(new XilinxDevice("2vp40-5-ff1148C"));
		al.add(new XilinxDevice("2vp40-7-ff1152C"));
		al.add(new XilinxDevice("2vp40-6-ff1152C"));
		al.add(new XilinxDevice("2vp40-5-ff1152C"));
		al.add(new XilinxDevice("2vp50-7-ff1148C"));
		al.add(new XilinxDevice("2vp50-6-ff1148C"));
		al.add(new XilinxDevice("2vp50-5-ff1148C"));
		al.add(new XilinxDevice("2vp50-7-ff1152C"));
		al.add(new XilinxDevice("2vp50-6-ff1152C"));
		al.add(new XilinxDevice("2vp50-5-ff1152C"));
		al.add(new XilinxDevice("2vp50-7-ff1517C"));
		al.add(new XilinxDevice("2vp50-6-ff1517C"));
		al.add(new XilinxDevice("2vp50-5-ff1517C"));
		al.add(new XilinxDevice("2vp70-7-ff1517C"));
		al.add(new XilinxDevice("2vp70-6-ff1517C"));
		al.add(new XilinxDevice("2vp70-5-ff1517C"));
		al.add(new XilinxDevice("2vp70-7-ff1704C"));
		al.add(new XilinxDevice("2vp70-6-ff1704C"));
		al.add(new XilinxDevice("2vp70-5-ff1704C"));
		al.add(new XilinxDevice("2vpx70-7-ff1704C"));
		al.add(new XilinxDevice("2vpx70-6-ff1704C"));
		al.add(new XilinxDevice("2vpx70-5-ff1704C"));
		al.add(new XilinxDevice("2vp100-6-ff1696C"));
		al.add(new XilinxDevice("2vp100-5-ff1696C"));
		al.add(new XilinxDevice("2vp100-6-ff1704C"));
		al.add(new XilinxDevice("2vp100-5-ff1704C"));
		al.add(new XilinxDevice("4vfx12-11-sf363C"));
		al.add(new XilinxDevice("4vfx12-10-sf363C"));
		al.add(new XilinxDevice("4vfx12-11-ff668C"));
		al.add(new XilinxDevice("4vfx12-10-ff668C"));
		al.add(new XilinxDevice("4vlx15-11-sf363C"));
		al.add(new XilinxDevice("4vlx15-10-sf363C"));
		al.add(new XilinxDevice("4vlx15-11-ff668C"));
		al.add(new XilinxDevice("4vlx15-10-ff668C"));
		al.add(new XilinxDevice("4vlx15-11-ff676C"));
		al.add(new XilinxDevice("4vlx15-10-ff676C"));
		al.add(new XilinxDevice("4vfx20-11-ff672C"));
		al.add(new XilinxDevice("4vfx20-10-ff672C"));
		al.add(new XilinxDevice("4vlx25-11-sf363C"));
		al.add(new XilinxDevice("4vlx25-10-sf363C"));
		al.add(new XilinxDevice("4vlx25-11-ff668C"));
		al.add(new XilinxDevice("4vlx25-10-ff668C"));
		al.add(new XilinxDevice("4vlx25-11-ff676C"));
		al.add(new XilinxDevice("4vlx25-10-ff676C"));
		al.add(new XilinxDevice("4vsx25-11-ff668C"));
		al.add(new XilinxDevice("4vsx25-10-ff668C"));
		al.add(new XilinxDevice("4vsx35-11-ff668C"));
		al.add(new XilinxDevice("4vsx35-10-ff668C"));
		al.add(new XilinxDevice("4vfx40-11-ff672C"));
		al.add(new XilinxDevice("4vfx40-10-ff672C"));
		al.add(new XilinxDevice("4vfx40-11-ff1152C"));
		al.add(new XilinxDevice("4vfx40-10-ff1152C"));
		al.add(new XilinxDevice("4vlx40-11-ff668C"));
		al.add(new XilinxDevice("4vlx40-10-ff668C"));
		al.add(new XilinxDevice("4vlx40-11-ff1148C"));
		al.add(new XilinxDevice("4vlx40-10-ff1148C"));
		al.add(new XilinxDevice("4vsx55-11-ff1148C"));
		al.add(new XilinxDevice("4vsx55-10-ff1148C"));
		al.add(new XilinxDevice("4vfx60-11-ff672C"));
		al.add(new XilinxDevice("4vfx60-10-ff672C"));
		al.add(new XilinxDevice("4vfx60-11-ff1152C"));
		al.add(new XilinxDevice("4vfx60-10-ff1152C"));
		al.add(new XilinxDevice("4vlx60-11-ff668C"));
		al.add(new XilinxDevice("4vlx60-10-ff668C"));
		al.add(new XilinxDevice("4vlx60-11-ff1148C"));
		al.add(new XilinxDevice("4vlx60-10-ff1148C"));
		al.add(new XilinxDevice("4vlx80-11-ff1148C"));
		al.add(new XilinxDevice("4vlx80-10-ff1148C"));
		al.add(new XilinxDevice("4vfx100-11-ff1152C"));
		al.add(new XilinxDevice("4vfx100-10-ff1152C"));
		al.add(new XilinxDevice("4vfx100-11-ff1517C"));
		al.add(new XilinxDevice("4vfx100-10-ff1517C"));
		al.add(new XilinxDevice("4vlx100-11-ff1148C"));
		al.add(new XilinxDevice("4vlx100-10-ff1148C"));
		al.add(new XilinxDevice("4vlx100-11-ff1513C"));
		al.add(new XilinxDevice("4vlx100-10-ff1513C"));
		al.add(new XilinxDevice("4vfx140-11-ff1517C"));
		al.add(new XilinxDevice("4vfx140-10-ff1517C"));
		al.add(new XilinxDevice("4vfx140-11-ff1760C"));
		al.add(new XilinxDevice("4vfx140-10-ff1760C"));
		al.add(new XilinxDevice("4vlx160-11-ff1148C"));
		al.add(new XilinxDevice("4vlx160-10-ff1148C"));
		al.add(new XilinxDevice("4vlx160-11-ff1513C"));
		al.add(new XilinxDevice("4vlx160-10-ff1513C"));
		al.add(new XilinxDevice("4vlx200-11-ff1513C"));
		al.add(new XilinxDevice("4vlx200-10-ff1513C"));

		knownParts = new XilinxDevice[al.size()];

		for (int i = 0; i < al.size(); i++)
			knownParts[i] = al.get(i);
	}

	public XilinxDevice(String parseString) {
		input = parseString = parseString.trim();

		// First check if we were given a staight family name
		String allcaps = parseString.toUpperCase();

		for (int i = 0; i < families.length; i++) {
			if (allcaps.equals(families[i]))
				family = i;
		}

		// If the device wasn't just family form, parse it
		if (family < 0) {
			// Trim the XC if the user supplied it
			if (allcaps.startsWith("XC")) {
				if (allcaps.length() > 2)
					allcaps = allcaps.substring(2);
				else
					allcaps = "";
			}

			for (int i = 0; i < abbreviations.length; i++) {
				if (allcaps.startsWith(abbreviations[i].toUpperCase())) {
					// We found the family, could be aliased if there
					// is a suffix, but we'll fix that later
					family = i;
					break;
				}
			}

			if (family >= 0) {
				// Trim off the named family, leaving just the device
				// number

				if (allcaps.length() > abbreviations[family].length())
					allcaps = allcaps.substring(abbreviations[family].length());
				else
					allcaps = "";

				// extract the device number
				while ((allcaps.length() > 0)
						&& (Character.isDigit(allcaps.charAt(0)))) {
					device += allcaps.charAt(0);

					if (allcaps.length() > 1)
						allcaps = allcaps.substring(1);
					else
						allcaps = "";

				}

				// Grab any suffix and if present, fix the family
				// number since the above search would have found the
				// first matching family abbreviation
				for (int i = 0; i < abbreviations.length; i++) {
					if (abbreviations[family].equals(abbreviations[i])
							&& (suffixes[i].length() > 0)
							&& allcaps.startsWith(suffixes[i].toUpperCase())) {
						// Oops, the family had a suffix, update it
						family = i;

						if (allcaps.length() > suffixes[i].length())
							allcaps = allcaps.substring(suffixes[i].length());
						else
							allcaps = "";

						break;
					}
				}

				if (device.length() > 0) {
					// look for dash symbol followed by a speed grade
					if (allcaps.startsWith("-")) {
						allcaps = allcaps.substring(1);

						// extract the speed number as a string

						String speedstr = "";

						while ((allcaps.length() > 0)
								&& (Character.isDigit(allcaps.charAt(0)))) {
							speedstr += allcaps.charAt(0);

							if (allcaps.length() > 1)
								allcaps = allcaps.substring(1);
							else
								allcaps = "";
						}

						if (speedstr.length() > 0) {
							speed = Integer.parseInt(speedstr);
						}

						// look for a dash symbol followed by a
						// package string
						if (allcaps.startsWith("-")) {
							allcaps = allcaps.substring(1);
						}

						// See if whats left ends with a C or I
						// temperature range
						if (allcaps.length() > 0) {
							char lastChar = allcaps
									.charAt(allcaps.length() - 1);

							if (!Character.isDigit(lastChar)) {
								temp = String.valueOf(lastChar);

								if (allcaps.length() > 1) {
									allcaps = allcaps.substring(0,
											(allcaps.length() - 1));

									pckage = allcaps.toLowerCase();
								}
							} else {
								// extract the package string
								pckage = allcaps.toLowerCase();
							}

						}
					}

				}
			} else {
				isXilinxDevice = false;
			}

		}
	}

	public int getFamily() {
		return family;
	}

	public String getFamilyAsString() {
		if (family >= 0)
			return (families[family]);
		else
			return ("");
	}

	public String getDevice() {
		return device;
	}

	public String getSuffix() {
		if (family >= 0)
			return suffixes[family];
		else
			return ("");
	}

	public int getSpeed() {
		return speed;
	}

	public String getPackage() {
		return pckage;
	}

	public String getTemp() {
		return temp;
	}

	public static XilinxDevice[] getKnownParts() {
		return knownParts;
	}

	public String getInput() {
		return input;
	}

	public String getFullDeviceName() {
		if (temp.length() > 0)
			return getFullDeviceNameNoTemp() + temp;
		else
			return getFullDeviceNameNoTemp();
	}

	public String getFullDeviceNameNoTemp() {
		if (family >= 0) {
			String result = "xc" + abbreviations[family];

			if (device.length() > 0) {
				result += device;
			}

			result += suffixes[family];

			if (speed > 0) {
				result += "-" + speed;

				if (pckage.length() > 0) {
					result += "-" + pckage;
				}
			}

			return result;
		} else {
			return "";
		}
	}

	public boolean isXilinxDevice() {
		return isXilinxDevice;
	}

	public boolean isFullySpecified() {
		return ((family >= 0) && (device.length() > 0) && (speed != 0) && (pckage
				.length() > 0));
	}

	public boolean doSupportSRL16() {
		switch (getFamily()) {
		case VIRTEX2P:
		case VIRTEX2:
		case VIRTEX4SX:
		case VIRTEX4FX:
		case VIRTEX4LX:
		case VIRTEXE:
		case VIRTEX:
		case SPARTAN2E:
		case SPARTAN2:
		case SPARTAN3:
			return true;
		default:
			return false;
		}
	}

	@Override
	public String toString() {
		return getFullDeviceName();
	}

	public static void main(String[] args) {
		XilinxDevice[] kp = XilinxDevice.getKnownParts();

		for (int i = 0; i < kp.length; i++)
			System.out.println(kp[i]);

		if (args.length > 0) {
			XilinxDevice xd = new XilinxDevice(args[0]);

			System.out.println();
			System.out.println("Arg: " + args[0]);
			System.out.println("Input: " + xd.getInput());
			System.out.println("XL?: " + xd.isXilinxDevice());
			System.out.println("support SRL16?: " + xd.doSupportSRL16());
			System.out.println("Family: " + xd.getFamily() + " = "
					+ xd.getFamilyAsString());
			System.out.println("Device: " + xd.getDevice());
			System.out.println("Suffix: " + xd.getSuffix());
			System.out.println("Speed: " + xd.getSpeed());
			System.out.println("Package: " + xd.getPackage());
			System.out.println("Temp: " + xd.getTemp());
			System.out.println();
			System.out.println("FullName: " + xd.getFullDeviceName());
			System.out.println();
			System.out.println("isXilinxDevice: " + xd.isXilinxDevice());
			System.out.println("isFullySpecified: " + xd.isFullySpecified());
			System.out.println();
		} else
			System.out.println("please supply a string for parse testing");
	}

}

// Perl program to generate the array list initializer used in static{}
// #!/usr/bin/perl -w

// # Run partgen -arch on each family to produce

// &processFamily ("spartan3");
// &processFamily ("spartan3e");
// &processFamily ("virtex2");
// &processFamily ("virtex2p");
// &processFamily ("virtex4");

// sub processFamily
// {
// my($familyName) = @_;

// chomp(@parts=`partgen -arch $familyName`);

// shift @parts; #remove first two lines - Release X.Xi
// shift @parts; #remove Copyright

// foreach (@parts)
// {
// s/^\s*//;
// if (/(\w+)\s+SPEEDS:\s*([-\d9\s]+).*/)
// {
// $family = $1;
// @speeds = split /\s+/, $2;
// }
// else
// {
// #strip whitespace
// s/\s*//g;
// $pack=$_;

// #and print out the full device/speed/package
// # for each possible speed.
// foreach (@speeds)
// {
// print "        al.add(new XilinxDevice(\"".$family.
// $_."-".$pack."C\"));\n";
// }
// }
// }
// }

