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
 * KeyGen.java
 *
 *
 */

package net.sf.openforge.util.license.util;

import net.sf.openforge.util.license.Key;

public class KeyGen
{
    static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

	private KeyGen()
	{
	}


	private static void printUsage()
	{
		System.out.println("To generate a key: month day year feature_code serial_number");
		System.out.println("To generate a block of keys: -block block_count month day year feature_code starting_serial_number");
		System.out.println("  To decode a key: key");
	}
	
	public static void main(String[] args)
	{

		if(args.length == 0)
		{
			printUsage();
			return;
		} else if(args.length == 1)
		{
			// Decode Key
			String key = args[0];
			
			System.out.println("Decoded Key: " + args[0]);
			System.out.println("      Feature Code: " + Key.getFeatureCode(key));
			System.out.println("     Serial Number: " + Key.getSerialNumber(key));
			System.out.println("   License Expires: " + Key.getExpirationAsString(key));
			System.out.println("  isLicenseCurrent: " + Key.isLicenseCurrent(key));
		} else if(args.length == 5)
		{
			// Generate a key
			int month = (new Integer(args[0])).intValue();
			int day   = (new Integer(args[1])).intValue();
			int year  = (new Integer(args[2])).intValue();
			int fc    = (new Integer(args[3])).intValue();
			int sn    = (new Integer(args[4])).intValue();
			
			String key = genKey(month,day,year,sn,fc);
			
			System.out.println("Key: " + Key.formatKey(key));			
		} else if ((args.length == 7) && (args[0].equals("-block")))
		{
			int cnt = (new Integer(args[1])).intValue();
			
			int month = (new Integer(args[2])).intValue();
			int day   = (new Integer(args[3])).intValue();
			int year  = (new Integer(args[4])).intValue();
			int fc    = (new Integer(args[5])).intValue();
			int sn    = (new Integer(args[6])).intValue();

			System.out.println("Key Block:\nindex\tkey                \tser#\tfc\texpire                      \tvalid");
			for(int i=0; i<cnt; i++)
			{
				String key = Key.formatKey(genKey(month,day,year,sn,fc));
				if(!Key.isLicenseCurrent(key))
					System.err.println("Internal Key gen error for serial number: " + sn);
				
				System.out.println(i + "\t" + key + "\t" + Key.getSerialNumber(key) +
					"\t" + Key.getFeatureCode(key) + "\t" + Key.getExpirationAsString(key) +
					"\t" + Key.isLicenseCurrent(key));

				sn++;
			}
		}						
	}

	
	public static String genKey(int month, int day, int year, int serialNumber, int featureCode)
	{

		// Take the day and make it the following week, and if necessary increment the month
		// and year if we roll.
		int week;
		
		if(day < 8)
			week = 1;
	    else if(day < 15)
			week = 2;
		else if(day < 22)
			week = 3;
		else
		{
			week = 0;
			month++;
		}

		if(month > 12)
		{
			month = 1;
			year++;
		}

		//System.out.println("Sending date: " + month + "/" + week + "/" + year);
		
		String y = Integer.toString(year);
		String mw = Integer.toString((month << 2) | (week & 0x3));
		String sn = Integer.toString(serialNumber);
		String fc = Integer.toString(featureCode);

		while(y.length() < 4)
			y = "0" + y;

		while(mw.length() < 2)
			mw = "0" + mw;

		while(sn.length() < 5)
			sn = "0" + sn;

		char[] result = new char[16];

		for(int i=0; i<16; i++)
		{
			switch(i)
			{
				case Key.CRC1_0:
				case Key.CRC1_1:
				case Key.CRC2_0:
				case Key.CRC2_1:
					result[i] = '0';
					break;
				case Key.YEAR_0:
					result[i] = Key.getDigit(y,0);
					break;
				case Key.YEAR_1:
					result[i] = Key.getDigit(y,1);
					break;
				case Key.YEAR_2:
					result[i] = Key.getDigit(y,2);
					break;
				case Key.YEAR_3:
					result[i] = Key.getDigit(y,3);
					break;
				case Key.MONTH_0:
					result[i] = Key.getDigit(mw,0);
					break;
				case Key.MONTH_1:
					result[i] = Key.getDigit(mw,1);
					break;
				case Key.FEATURE_CODE:
					result[i] = Key.getDigit(fc,0);
					break;
				case Key.SERIAL_0:
					result[i] = Key.getDigit(sn,0);
					break;
				case Key.SERIAL_1:
					result[i] = Key.getDigit(sn,1);
					break;
				case Key.SERIAL_2:
					result[i] = Key.getDigit(sn,2);
					break;
				case Key.SERIAL_3:
					result[i] = Key.getDigit(sn,3);
					break;
				case Key.SERIAL_4:
					result[i] = Key.getDigit(sn,4);
					break;
			}
		}

		
		String tmp = String.copyValueOf(result);

		//System.out.println("Key without scrambling or CRCs: " + tmp);
		
		int[] intvalues = Key.keyToInts(tmp);

		// Scramble
		for(int i=0; i<16; i++)
		{
			intvalues[i] = Key.mod10(intvalues[i] + Key.modulo[i]);
		}

		// CRC2 calculation
		int sum = 0;
		for(int i=0; i<16; i++)
		{
			sum += intvalues[i];
		}
		
		// Ensure that there are at least 2 digits in the sum
		String rep = "0" + Integer.toString(sum);

		int[] sumdigits = Key.keyToInts(rep);
		intvalues[Key.CRC2_0] = sumdigits[sumdigits.length - 2];
		intvalues[Key.CRC2_1] = sumdigits[sumdigits.length - 1];

		// CRC1 calculation
		int crc1_0 = 0;
		int crc1_1 = 0;
		
		for(int i=0; i<16; i=i+2)
		{
			crc1_0 += intvalues[i];
			crc1_1 += intvalues[i + 1];
		}

		intvalues[Key.CRC1_0] = Key.mod10(10 - Key.mod10(crc1_0));
		intvalues[Key.CRC1_1] = Key.mod10(10 - Key.mod10(crc1_1));

		
		// Convert the int array to a string
		String ret = "";
		for(int i=0; i<16; i++)
		{
			ret += Integer.toString(intvalues[i]);
		}

		return(ret);
	}


}


