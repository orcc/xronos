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

package net.sf.openforge.util.license;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

/*
 * A class to deal with licensing keys.  We find the key file and
 * check for key integrety, and return if the tool is licensed to
 * run.
 *
 */

public final class Key
{
    private static final String rcs_id = "RCS_REVISION: $Rev: 2 $";

        public static final int PRODUCT_CODE_INVALID = 0;
    public static final int PRODUCT_CODE_MODULE_BUILDER = 1;

    /*
     * How our key is built:
     *
     *  1. Input 4 digit year, 2 digit month and week of month (1-4) for expiration;
     *     use, September 1, 3949 as perpetual number which requires
     *     the keygen program to input 8/30/3949 as the expiration date
     *  2. Input 5 digit serial number and 1 digit feature code.
     *  3. Build month/week code by multiplying the week by the month creating a
     *     2 digit result.
     *  4. Extract the decimal digits from all numbers input and organize into an
     *     array according to the following locations chart:
     *
     *     Key digit locations:
     *
     *     For multi digit numbers, digit 0 is the msd, so we process left to right
     *
     *      0 = CRC1   - 0
     *      1 = CRC2   - 1
     *      2 = Year   - 0
     *      3 = Serial - 3
     *      4 = Feature Code
     *      5 = Year   - 2
     *      6 = Serial - 4
     *      7 = Month  - 1
     *      8 = Year   - 3
     *      9 = Month  - 0
     *     10 = Serial - 1
     *     11 = Serial - 0
     *     12 = CRC2   - 0
     *     13 = CRC1   - 1
     *     14 = Year   - 1
     *     15 = Serial - 2
     *
     *  5. Scramble the array with the modulo array, do this by adding each element of the
     *     modulo array to the key array and wrap modulo 10.
     *  6. Caclulate the CRC2 digits by summing up the digits and extracting the bottom
     *     two decimal digits of the sum and loading them into the CRC2 locations
     *  7. Calculate the CRC1 digits by xoring all the even key digits and all the odd ones
     *     separately, and storing the 2 digit result in the two CRC1 locations.
     *
     *  To recover a key:
     *
     *  1. During Installation, perform the CRC1 calculation of the given key and if the
     *     the result is 0, then the value is valid, unless all the input numbers are 0.
     *  2. Perform the CRC2 summing, ignoring the CRC1 and CRC2 locations and compare
     *     the result against the CRC2 digits in the key, if they don't match, its invalid
     *  3. Perform unscrambling using the modulo array by inverting each location and adding
     *     it to the key and use the modulo10 routine.
     *  4. Extract the date, serial number and feature code by rearranging the digits.
     *  5. Turn the Month/week into Month/day by assigning week 1 to be day 1, week 2 to
     *     be day 8, week 3 to be day 15, and week 4 to be day 22.
     *
     *  You can now check the recovered data for perpetual or expiration Data information.
     *
     *  Keep in mind that months are 1-12 and weeks are 1-4, so we don't get any * 0!
     */

    public static final int CRC1_0  = 0;
    public static final int CRC1_1  = 13;
    public static final int CRC2_0  = 12;
    public static final int CRC2_1  = 1;
    public static final int YEAR_0  = 2;
    public static final int YEAR_1  = 14;
    public static final int YEAR_2  = 5;
    public static final int YEAR_3  = 8;
    public static final int MONTH_0 = 9;
    public static final int MONTH_1 = 7;
    public static final int FEATURE_CODE = 4;
    public static final int SERIAL_0 = 11;
    public static final int SERIAL_1 = 10;
    public static final int SERIAL_2 = 15;
    public static final int SERIAL_3 = 3;
    public static final int SERIAL_4 = 6;

    public static final int[] modulo = {0,0,3,4,1,-2,-3,1,-2,4,-5,6,0,0,2,-2};


    
    /*
     * This method determines if the current license key is valid and
     * if the product is still licensed to run by checking the key expiration
     * time.
     *
     */
    public static boolean isLicenseCurrent(String key)
    {
        // Parse and validate license, then check if the current data is after the
        // data in the license, and if so, return false, otherwise true.

        int day = getDay(key);
        int month = getMonth(key);
        int year = getYear(key);
        int fc = getFeatureCode(key);
        int sn = getSerialNumber(key);

        if(!areCRCsValid(key))
            return false;

        if(fc != 2)
            return(false);

        if(sn == 0)
            return(false);
        
        if(isPerpetual(key))
            return(true);
        
        if((year < 2004) || (year > 2010))
            return(false);

        Date expire = getExpirationDate(key);
        Date now = new Date();

        return(!now.after(expire));

    }

    
    public static String getExpirationAsString(String key)
    {
        if(!areCRCsValid(key))
            return("Malformed Key, cannot determine expiration date");

        int sn = getSerialNumber(key);
        if(sn == 0)
            return("Malformed Key, cannot determine expiration date");

        if(isPerpetual(key))
            return("Never");

        int year = getYear(key);
        if((year < 2000) || (year > 2100))
            return("Malformed Key, cannot determine expiration date");

        return(getExpirationDate(key).toString());
    }

    
    public static int getSerialNumber(String key)
    {
        int[] ukey = getUnscrambledKey(key);

        int result = 0;

        for(int i=0; i<5; i++)
        {
            switch(i)
            {
                case 0:
                    result += ukey[SERIAL_0];
                    break;
                case 1:
                    result = (result * 10) + ukey[SERIAL_1];
                    break;
                case 2:
                    result = (result * 10) + ukey[SERIAL_2];
                    break;
                case 3:
                    result = (result * 10) + ukey[SERIAL_3];
                    break;
                case 4:
                    result = (result * 10) + ukey[SERIAL_4];
                    break;
            }
        }

        return(result);
    }
    

    public static int getFeatureCode(String key)
    {
        int[] ukey = getUnscrambledKey(key);
        
        return(ukey[FEATURE_CODE]);
    }

    
    public static String formatKey(String key)
    {
        key = parseKey(key);
        
        String result = "";

        for(int i=0; i<4; i++)
            result += Key.getDigit(key,i);

        result += "-";

        for(int i=0; i<4; i++)
            result += Key.getDigit(key,i + 4);

        result += "-";

        for(int i=0; i<4; i++)
            result += Key.getDigit(key,i + 8);

        result += "-";

        for(int i=0; i<4; i++)
            result += Key.getDigit(key,i + 12);

        return result;
    }
        

    private static boolean isPerpetual(String key)
    {
        int day = getDay(key);
        int month = getMonth(key);
        int year = getYear(key);

        if((day == 1) && (month == 9) && (year == 3949))
            return true;

        return false;
    }

    
    private static Date getExpirationDate(String key)
    {
        Calendar cal = new GregorianCalendar();

        cal.set(getYear(key),(getMonth(key) - 1),getDay(key),0,0,0);

        return(cal.getTime());
    }

    
    private static int getYear(String key)
    {
        int[] ukey = getUnscrambledKey(key);

        int result = 0;

        for(int i=0; i<4; i++)
        {
            switch(i)
            {
                case 0:
                    result += ukey[YEAR_0];
                    break;
                case 1:
                    result = (result * 10) + ukey[YEAR_1];
                    break;
                case 2:
                    result = (result * 10) + ukey[YEAR_2];
                    break;
                case 3:
                    result = (result * 10) + ukey[YEAR_3];
                    break;
            }
        }

        return result;
    }

    
    private static int getMonth(String key)
    {
        int[] ukey = getUnscrambledKey(key);

        int result = (ukey[MONTH_0] * 10) + ukey[MONTH_1];

        // Get rid of the week scale.
        result >>>= 2;
        return(result);
    }


    private static int getDay(String key)
    {
        int[] ukey = getUnscrambledKey(key);

        int result = (ukey[MONTH_0] * 10) + ukey[MONTH_1];

        result &= 3;

        switch(result)
        {
            case 0:
                return(1);
            case 1:
                return(8);
            case 2:
                return(15);
            case 3:
                return(22);
            default:
                return(1);
        }
    }

    
    private static int[] getUnscrambledKey(String key)
    {
        int[] intvalues = keyToInts(key);
        int[] badresult = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        
        if(intvalues.length != 16)
            return(badresult);

        for(int i=0; i<16; i++)
            intvalues[i] = mod10(intvalues[i] - modulo[i]);

        return intvalues;
    }

    
    private static boolean areCRCsValid(String key)
    {
        // There are two CRCs, one used during installation as a quick check
        // and a second one to catch tampering with the license.dat file after
        // installation.
        return(isCRC1Valid(key) && isCRC2Valid(key));
    }

    
    private static boolean isCRC1Valid(String key)
    {
        // CRC1 is used during installation.
        int[] keyints = keyToInts(key);

        if(keyints.length != 16)
            return false;

        int sum = 0;
        for(int i=0; i<16; i++)
            sum += keyints[i];

        // Catch the all zeros case.
        if(sum == 0)
            return(false);
            
        int crctop = 0;
        int crcbot = 0;
        
        for(int i=0; i<16; i=i+2)
        {
            //System.out.println("isCRC1Valid: keyints[" + i + "] = " + keyints[i]);
            //System.out.println("isCRC1Valid: keyints[" + (i + 1) + "] = " + keyints[i + 1]);
            
            crctop += keyints[i];
            crcbot += keyints[i+1];
        }

        // System.out.println("isCRC1Valid: crctop sum: " + crctop + ", crcbot sum: " + crcbot);
        
        return((mod10(crcbot) == 0) && (mod10(crctop) == 0));
    }

    
    private static boolean isCRC2Valid(String key)
    {
        int[] keyints = keyToInts(key);

        if(keyints.length != 16)
            return false;

        int crcsum = 0;
        for(int i=0; i<16; i++)
        {
            switch(i)
            {
                case CRC1_0:
                case CRC1_1:
                case CRC2_0:
                case CRC2_1:
                    break;
                default:
                    crcsum += keyints[i];
            }
        }

        String rep = Integer.toString(crcsum);

        int[] sumdigits = keyToInts(rep);

        int crctop = 0;
        int crcbot = sumdigits[sumdigits.length - 1];

        if(sumdigits.length > 1)
            crctop = sumdigits[sumdigits.length - 2];

        return((crctop == keyints[CRC2_0]) && (crcbot == keyints[CRC2_1]));
    }

    
    public static int[] keyToInts(String key)
    {
        key = parseKey(key);
        int[] result = new int[key.length()];

        for(int i=0; i<result.length; i++)
        {
            result[i] = digitToInt(getDigit(key,i));
        }

        return result;
    }

    
    public static String parseKey(String key)
    {
        // assume the entire key file is given to us, extract the key, which is
        // just the characters representing decimal digits in the first line of the file.
        String result = "";

        int len = key.length();

        for(int i=0; i<len; i++)
        {
            if(key.charAt(i) == '\n')
                break;
            else
            {
                if(isDigit(key.charAt(i)))
                    result += key.charAt(i);
            }
        }

        return result;
    }

    
    public static char getDigit(String number, int whichOne)
    {
        if((whichOne < 0) || (whichOne >= number.length()))
            return(' ');

        return(number.charAt(whichOne));
    }

    
    private static int digitToInt(char c)
    {
        if(c == '0')
            return 0;
        
        if(c == '1')
            return 1;

        if(c == '2')
            return 2;

        if(c == '3')
            return 3;

        if(c == '4')
            return 4;

        if(c == '5')
            return 5;

        if(c == '6')
            return 6;

        if(c == '7')
            return 7;

        if(c == '8')
            return 8;

        if(c == '9')
            return 9;

        return 0;
    }

    
    public static int mod10(int in)
    {
        // Use modulo arithmetic to get the input into the -9 to +9 range
        in = (in % 10);

        // If negative, wrap to the highest positive range
        if(in < 0)
            in = in + 10;

        return in;
    }

    
    private static boolean isDigit(char c)
    {
        if(c == '0')
            return true;
        
        if(c == '1')
            return true;

        if(c == '2')
            return true;

        if(c == '3')
            return true;

        if(c == '4')
            return true;

        if(c == '5')
            return true;

        if(c == '6')
            return true;

        if(c == '7')
            return true;

        if(c == '8')
            return true;

        if(c == '9')
            return true;

        return false;
    }
}






