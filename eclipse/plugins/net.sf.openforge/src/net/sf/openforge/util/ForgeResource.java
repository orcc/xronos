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

package net.sf.openforge.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

/**
 * ForgeResource.java
 * 
 * 
 * Created: Thu Sep 10 16:03:48 1998
 * 
 * @author Andy Kollegger
 * @version 0.1
 */
public class ForgeResource extends ListResourceBundle {

	private static final String REP_DIV = "OP_REPLACE_DIV";
	private static final String REP_REM = "OP_REPLACE_REM";

	/**
	 * Individual resource items.
	 */
	// JWJFIXME: Create XLIM versions for DIV and REM.
	protected static final Object[][] contents = {
			{ "VERSION_TEXT", "text/version.txt" },
			{ "RELEASE_NOTES_TEXT", "text/release_notes.txt" },
			{ "LICENSE_FILE", "license/license.dat" },
			{ "FORGE_C_HEADER", "c/forge/include/forge.h" },
			{ REP_DIV, "lib/ops/DIV.xlim" }, { REP_REM, "lib/ops/REM.xlim" } };

	/**
	 * Overrides getContents from ListResourceBundle
	 * 
	 * @return the two dimensional array of resource items
	 */
	@Override
	public Object[][] getContents() {
		return contents;
	}

	/**
	 * Returns an URL for the resource associated with the given key
	 * 
	 * 
	 * @param key
	 *            the key of the resource
	 */
	public URL getResourceURL(String key) {
		String keyPath = "/net/sf/openforge/resources/" + getString(key);
		return ForgeResource.class.getResource(keyPath);
	} // getResourceURL()

	/**
	 * Gets an InputStream for a resource.
	 * 
	 * @param key
	 *            the resource identifier
	 * @return an InputStream to the resource
	 */
	public InputStream loadResourceStream(String key) {

		InputStream result = null;

		try {
			if (getResourceURL(key) != null) {
				return getResourceURL(key).openStream();
			}
		} catch (Exception ex) {
			result = null;
		}

		return result;
	} // loadResourceStream()

	public static InputStream loadForgeResourceStream(String key) {
		// When integrated in an eclipse plugin the resource lookup fails. This
		// is hack to provide the baseline implementations that are needed for
		// DIV
		// and REM operators
		if (key.equals(REP_DIV)) {
			return new ByteArrayInputStream(DIV_SRC.getBytes());
		}
		if (key.equals(REP_REM)) {
			return new ByteArrayInputStream(REM_SRC.getBytes());
		}

		ForgeResource resources = (ForgeResource) ResourceBundle
				.getBundle(ForgeResource.class.getName());
		return resources.loadResourceStream(key);
	}

	// Copied from DIV.c on 2008_03_31
	private static final String DIV_SRC = " \n        unsigned char divUCUC(unsigned char num, unsigned char den)\n        {\n            unsigned char result = 0;\n                \n            unsigned char remainder = num;\n                \n            unsigned char mask = 0x80;\n            int i;\n            \n            for (i=0; i < 8; i++)\n            {\n                unsigned char numer = ((long)(remainder >> (7 - i)));\n                \n                if (numer >= den)\n                {\n                    result |= mask;\n                        \n                    remainder = (remainder - (den << (7 - i)));\n                }      \n                mask = mask >> 1;\n            }\n                \n            return result;\n        }"
			+ "\n        unsigned short divUSUS(unsigned short num, unsigned short den)\n        {\n            unsigned short result = 0;\n                \n            unsigned short remainder = num;\n                \n            unsigned short mask = 0x8000;\n            int i;\n            \n            for (i=0; i < 16; i++)\n            {\n                unsigned short numer = ((long)(remainder >> (15 - i)));\n                \n                if (numer >= den)\n                {\n                    result |= mask;\n                        \n                    remainder = (remainder - (den << (15 - i)));\n                }      \n                mask = mask >> 1;\n            }\n                \n            return result;\n        }"
			+
			/* private static final String _RCS_ = "$Revision: 570 $"; */
			/**
			 * Implements a functionally correct unsigned divide according to
			 * the C specification. The only deviation is in the handling of
			 * divide by 0. No exception is thrown in that case, instead the
			 * result of -1 is returned. When forging this code constant
			 * propagation must be turned on and, optionally, loop unrolling. If
			 * loop unrolling is turned on, then this method may be balance
			 * pipeline scheduled.
			 * 
			 * @param a
			 *            a value of type 'unsigned int'
			 * @param b
			 *            a value of type 'unsigned int'
			 * @return a value of type 'unsigned int'
			 */
			"\n        unsigned int divUIUI(unsigned int num, unsigned int den)\n        {\n            unsigned int result = 0;\n                \n            unsigned int remainder = num;\n                \n            unsigned int mask = 0x80000000;\n            int i;\n            \n            for (i=0; i < 32; i++)\n            {\n                unsigned int numer = ((long)(remainder >> (31 - i)));\n                \n                if (numer >= den)\n                {\n                    result |= mask;\n                        \n                    remainder = (remainder - (den << (31 - i)));\n                }      \n                mask = mask >> 1;\n            }\n                \n            return result;\n        }"
			+ "\n        unsigned long long divULLULL (unsigned long long num, unsigned long long den)\n        {\n            unsigned long long result = 0ULL;\n                \n            unsigned long long remainder = num;\n                \n            unsigned long long mask = 0x8000000000000000ULL;\n            int i;\n            \n            for (i=0; i < 64; i++)\n            {\n                unsigned long long numer = ((unsigned long long)(remainder >> (63 - i)));\n                \n                if (numer >= den)\n                {\n                    result |= mask;\n                        \n                    remainder = (remainder - (den << (63 - i)));\n                }\n                    \n                mask = mask >> 1;\n            }\n                \n            return result;\n        }"
			+ "\n        char divCC(char num, char den)\n        {\n            char result = 0;\n            int i;\n            \n            /* If true, then the result must be negative. */\n            int flipResult = 0;\n\n            char remainder;\n            short denom;\n            int mask;\n            short numer;\n            \n            if (num < 0)\n            {\n                num = -num;\n                flipResult ^= 1;\n            }\n            \n            if (den < 0)\n            {\n                den = -den;\n                flipResult ^= 1;\n            }\n            \n            remainder = num;\n            \n            /* Cast the denominator to a long so that MIN_INT looks like\n               a positive number.(We need 33 bits to represent -(MIN_INT)).\n            */\n            denom = ((short)den) & 0x000000FF;\n            \n            mask = 0x80;\n            \n            for (i=0; i < 8; i++)\n            {\n                /* Cast the numerator to a long so that -(MIN_INT) appears as\n                   a positive value (we need 33 bits to represent it). */\n                numer = ((short)(((short)remainder & 0x000000ff) >> (7 - i)));\n                \n                if (numer >= denom)\n                {\n                    result |= mask;\n                    \n                    remainder = (remainder - (den << (7 - i)));\n                }\n                \n                mask = (mask >> 1) & 0x7fffffff;\n            }\n            \n            /* If the signs of the inputs did not agree, then make the result\n               negative. */\n            if (flipResult != 0)\n            {\n                result = -result;\n            }\n            \n            return result;\n        }"
			+ "\n        short divSS(short num, short den)\n        {\n            short result = 0;\n            int i;\n            \n            /* If true, then the result must be negative. */\n            int flipResult = 0;\n\n            short remainder;\n            int denom;\n            int mask;\n            int numer;\n            \n            if (num < 0)\n            {\n                num = -num;\n                flipResult ^= 1;\n            }\n            \n            if (den < 0)\n            {\n                den = -den;\n                flipResult ^= 1;\n            }\n            \n            remainder = num;\n            \n            /* Cast the denominator to a long so that MIN_INT looks like\n               a positive number.(We need 33 bits to represent -(MIN_INT)).\n            */\n            denom = ((int)den) & 0x0000FFFF;\n            \n            mask = 0x8000;\n            \n            for (i=0; i < 16; i++)\n            {\n                /* Cast the numerator to a long so that -(MIN_INT) appears as\n                   a positive value (we need 33 bits to represent it). */\n                numer = ((int)(((int)remainder & 0x0000ffff) >> (15 - i)));\n                \n                if (numer >= denom)\n                {\n                    result |= mask;\n                    \n                    remainder = (remainder - (den << (15 - i)));\n                }\n                \n                mask = (mask >> 1) & 0x7fffffff;\n            }\n            \n            /* If the signs of the inputs did not agree, then make the result\n               negative. */\n            if (flipResult != 0)\n            {\n                result = -result;\n            }\n            \n            return result;\n        }"
			+
			/**
			 * Implements a functionally correct signed divide according to the
			 * JVM specification. The only deviation is in the handling of
			 * divide by 0. No exception is thrown in that case, instead the
			 * result of -1 is returned. When forging this code constant
			 * propagation must be turned on and, optionally, loop unrolling. If
			 * loop unrolling is turned on, then this method may be balance
			 * pipeline scheduled.
			 * 
			 * @param a
			 *            a value of type 'int'
			 * @param b
			 *            a value of type 'int'
			 * @return a value of type 'int'
			 */
			"\n        int divII(int num, int den)\n        {\n            int result = 0;\n            int i;\n            \n            /* If true, then the result must be negative. */\n            int flipResult = 0;\n\n            int remainder;\n            long long denom;\n            int mask;\n            long long numer;\n            \n            if (num < 0)\n            {\n                num = -num;\n                flipResult ^= 1;\n            }\n            \n            if (den < 0)\n            {\n                den = -den;\n                flipResult ^= 1;\n            }\n            \n            remainder = num;\n            \n            /* Cast the denominator to a long so that MIN_INT looks like\n               a positive number.(We need 33 bits to represent -(MIN_INT)).\n            */\n            denom = ((long long)den) & 0xFFFFFFFFLL;\n            \n            mask = 0x80000000L;\n            \n            for (i=0; i < 32; i++)\n            {\n                /* Cast the numerator to a long so that -(MIN_INT) appears as\n                   a positive value (we need 33 bits to represent it). */\n                numer = ((long long)(((long long)remainder & 0xffffffffLL) >> (31 - i)));\n                \n                if (numer >= denom)\n                {\n                    result |= mask;\n                    \n                    remainder = (remainder - (den << (31 - i)));\n                }\n                \n                mask = (mask >> 1) & 0x7fffffffL;\n            }\n            \n            /* If the signs of the inputs did not agree, then make the result\n               negative. */\n            if (flipResult != 0)\n            {\n                result = -result;\n            }\n            \n            return result;\n        }"
			+
			/**
			 * A fully functional impelentation of a 'long' (64 bit) signed
			 * divider. This implementation conforms to the JVM specification
			 * for long division except for divide by 0, in which case the
			 * result -1 is returned.
			 * 
			 * @param a
			 *            a 'long'
			 * @param b
			 *            a 'long'
			 * @return a 'long'
			 */
			"\n        long long divLLLL(long long num, long long den)\n        {\n            long long result = 0;\n            \n            /* If true, the result is to be negative. */\n            int flipResult = 0;\n            long long remainder, upperDen, lowerDen, mask;\n            int i;\n            long long numer, upperNum, lowerNum;\n\n            if (num < 0)\n            {\n                num = -num;\n                flipResult ^= 1;\n            }\n            \n            if (den < 0)\n            {\n                den = -den;\n                flipResult ^= 1;\n            }\n            \n            remainder = num;\n            \n            /* Break the denominator into 2 halves for comparison.  This allows us\n               to represent the value -(MIN_LONG) as a positive value and get it\n               to compare correctly. */\n            upperDen = (den >> 32) & 0xffffffffLL;\n            lowerDen = den & 0x00000000FFFFFFFFLL;\n            \n            mask = 0x8000000000000000LL;\n            \n            for (i=0; i < 64; i++)\n            {\n                numer = (long long)(((unsigned long long)remainder) >> (63 - i));\n                \n                /* Split the numerator into 2 halves for comparison.  This allows us\n                   to represent the value -(MIN_LONG) as a positive value. */\n                upperNum = (numer >> 32) & 0xffffffffLL;\n                lowerNum = numer & 0x00000000FFFFFFFFLL;\n                \n                \n                if ((upperNum > upperDen) ||\n                    ((upperNum == upperDen) && (lowerNum >= lowerDen)))\n                {\n                    result |= mask;\n                    \n                    remainder = (remainder - (den << (63 - i)));\n                }\n                \n                mask = (mask >> 1) & 0x7fffffffffffffffLL;\n            }\n            \n            /* If the signs do not agree then negate the result. */\n            if (flipResult != 0)\n            {\n                result = -result;\n            }\n            \n            return result;\n        }"
			+ "";
	/*
	 * #ifdef TESTING
	 * 
	 * #include<limits.h> #define ULLONG_MAX 18446744073709551615ULL
	 * 
	 * int main(int argc, char * argv[]) { char valuesC[]={CHAR_MAX-1,
	 * CHAR_MIN+1, CHAR_MAX, CHAR_MIN, CHAR_MAX-10,
	 * (CHAR_MAX/2),(CHAR_MAX/2)-1,(CHAR_MIN/2),(CHAR_MIN/2)+1,(CHAR_MAX/2)+2,
	 * (CHAR_MAX/2)-10,(CHAR_MAX/2)+10, CHAR_MAX/4, CHAR_MIN/4, CHAR_MIN/8,
	 * 64,63,62, -61, -39, 10,9,8,7,6, 5,4,3,2,1}; unsigned char
	 * valuesUC[]={UCHAR_MAX-1,UCHAR_MAX-2,UCHAR_MAX-3,UCHAR_MAX, UCHAR_MAX-100,
	 * (
	 * UCHAR_MAX/2),(UCHAR_MAX/2)-1,(UCHAR_MAX/2)-2,(UCHAR_MAX/2)+1,(UCHAR_MAX/2
	 * )+2, (UCHAR_MAX/2)-10,(UCHAR_MAX/2)+10, UCHAR_MAX/4, UCHAR_MAX/8,
	 * UCHAR_MAX/16, 64,63,62, 61, 39, 10,9,8,7,6, 5,4,3,2,1}; short
	 * valuesS[]={SHRT_MAX-1, SHRT_MIN+1, SHRT_MAX, SHRT_MIN, SHRT_MAX-100,
	 * (SHRT_MAX/2),(SHRT_MAX/2)-1,(SHRT_MIN/2),(SHRT_MIN/2)+1,(SHRT_MAX/2)+2,
	 * (SHRT_MAX/2)-100,(SHRT_MAX/2)+100, SHRT_MAX/4, SHRT_MIN/4, SHRT_MIN/8,
	 * 1024,1023,1021, 513, 39, 10,9,8,7,6, 5,4,3,2,1}; unsigned short
	 * valuesUS[]={USHRT_MAX-1,USHRT_MAX-2,USHRT_MAX-3,USHRT_MAX, USHRT_MAX-100,
	 * (
	 * USHRT_MAX/2),(USHRT_MAX/2)-1,(USHRT_MAX/2)-2,(USHRT_MAX/2)+1,(USHRT_MAX/2
	 * )+2, (USHRT_MAX/2)-100,(USHRT_MAX/2)+100, USHRT_MAX/4, USHRT_MAX/8,
	 * USHRT_MAX/1024, 1024,1023,1021, 513, 39, 10,9,8,7,6, 5,4,3,2,1}; unsigned
	 * int valuesUI[]={UINT_MAX-1,UINT_MAX-2,UINT_MAX-3,UINT_MAX, UINT_MAX-100,
	 * (UINT_MAX/2),(UINT_MAX/2)-1,(UINT_MAX/2)-2,(UINT_MAX/2)+1,(UINT_MAX/2)+2,
	 * (UINT_MAX/2)-100,(UINT_MAX/2)+100, UINT_MAX/4, UINT_MAX/8, UINT_MAX/1024,
	 * 1024,1023,1021, 513, 39, 10,9,8,7,6, 5,4,3,2,1}; unsigned long long
	 * valuesULL[]={ULLONG_MAX-1ULL,ULLONG_MAX-2ULL,ULLONG_MAX-3ULL,ULLONG_MAX,
	 * ULLONG_MAX-100ULL,
	 * (ULLONG_MAX/2ULL),(ULLONG_MAX/2ULL)-1ULL,(ULLONG_MAX/2ULL
	 * )-2ULL,(ULLONG_MAX/2ULL)+1ULL,(ULLONG_MAX/2ULL)+2ULL,
	 * (ULLONG_MAX/2ULL)-100ULL,(ULLONG_MAX/2ULL)+100ULL, ULLONG_MAX/4ULL,
	 * ULLONG_MAX/8ULL, ULLONG_MAX/1024ULL, 1024ULL,1023ULL,1021ULL, 513ULL,
	 * 39ULL, 10ULL,9ULL,8ULL,7ULL,6ULL, 5ULL,4ULL,3ULL,2ULL,1ULL};
	 * 
	 * unsigned long long i,j;
	 * 
	 * 
	 * for (i=0; i<sizeof(valuesUI)/4; i++) { printf(\"i=%8llx valuesS %x
	 * valuesUS %x valuesUI %x valuesULL %llx
	 * \n\",i,valuesS[i],valuesUS[i],valuesUI[i],valuesULL[i]);
	 * 
	 * for (j=0; j<sizeof(valuesUI)/4; j++) {
	 * 
	 * if ((valuesC[i]/valuesC[j]) != divCC(valuesC[i],valuesC[j])) {
	 * printf(\"ERROR char: %x %x %x / %x = %x, div= %x\n\
	 * ",i,j,valuesC[i],valuesC[j],(valuesC[i]/valuesC[j]),divCC(valuesC[i],valuesC[j]));
	 * } if ((valuesUC[i]/valuesUC[j]) != divUCUC(valuesUC[i],valuesUC[j])) {
	 * printf(\"ERROR uchar: %x %x %x / %x = %x, div= %x\n\
	 * ",i,j,valuesUC[i],valuesUC[j],(valuesUC[i]/valuesUC[j]),divUCUC(valuesUC[i],valuesUC[j]));
	 * }
	 * 
	 * if ((valuesS[i]/valuesS[j]) != divSS(valuesS[i],valuesS[j])) {
	 * printf(\"ERROR short: %x %x %x / %x = %x, div= %x\n\
	 * ",i,j,valuesS[i],valuesS[j],(valuesS[i]/valuesS[j]),divSS(valuesS[i],valuesS[j]));
	 * } if ((valuesUS[i]/valuesUS[j]) != divUSUS(valuesUS[i],valuesUS[j])) {
	 * printf(\"ERROR ushort: %x %x %x / %x = %x, div= %x\n\
	 * ",i,j,valuesUS[i],valuesUS[j],(valuesUS[i]/valuesUS[j]),divUSUS(valuesUS[i],valuesUS[j]));
	 * }
	 * 
	 * if ((valuesUI[i]/valuesUI[j]) != divUIUI(valuesUI[i],valuesUI[j])) {
	 * printf(\"ERROR int: %x %x %x / %x = %x, div= %x\n\
	 * ",i,j,valuesUI[i],valuesUI[j],(valuesUI[i]/valuesUI[j]),divUIUI(valuesUI[i],valuesUI[j]));
	 * }
	 * 
	 * if ((valuesULL[i]/valuesULL[j]) != divULLULL(valuesULL[i],valuesULL[j]))
	 * { printf(\"ERRORlong: %u %u %llx / %llx = %llx, div= %llx\n\
	 * ",i,j,valuesULL[i],valuesULL[j],(valuesULL[i]/valuesULL[j]),divULLULL(valuesULL[i],valuesULL[j]));
	 * }
	 * 
	 * } if ((0 / valuesS[i]) != (divSS(0, valuesS[i]))) { printf(\"ERROR short0
	 * %x / %x = %x, div= %x\n\",0,valuesS[i],0/valuesS[i],divSS(0,valuesS[i]));
	 * } if ((0U / valuesUS[i]) != (divUSUS(0U, valuesUS[i]))) { printf(\"ERROR
	 * short0 %x / %x = %x, div=
	 * %x\n\",0U,valuesUS[i],0U/valuesUS[i],divUSUS(0U,valuesUS[i])); } if ((0U
	 * / valuesUI[i]) != (divUIUI(0U, valuesUI[i]))) { printf(\"ERROR int0 %x /
	 * %x = %x, div=
	 * %x\n\",0U,valuesUI[i],0U/valuesUI[i],divUIUI(0U,valuesUI[i])); } if
	 * ((0ULL / valuesULL[i]) != (divULLULL(0ULL, valuesULL[i]))) {
	 * printf(\"ERROR longlong0 %llx / %llx = %llx, div=
	 * %llx\n\",0ULL,valuesULL[i],0ULL/valuesULL[i],divULLULL(0ULL,valuesULL[i]));
	 * }
	 * 
	 * } }
	 * 
	 * 
	 * #endif
	 */

	// Copied from REM.c on 2008_03_31
	private static final String REM_SRC = " \n        unsigned char remUCUC (unsigned char num, unsigned char den)\n        {\n            unsigned char remainder = num;\n            unsigned int i;\n            \n            for (i=0; i < 8; i++)\n            {\n                unsigned char numer = (remainder >> (7 - i));\n                \n                if (numer >= den)\n                {\n                    remainder = (remainder - (den << (7 - i)));\n                }\n                \n            }\n            return remainder;\n        }"
			+ "\n        unsigned short remUSUS (unsigned short num, unsigned short den)\n        {\n            unsigned short remainder = num;\n            unsigned int i;\n            \n            for (i=0; i < 16; i++)\n            {\n                unsigned short numer = (remainder >> (15 - i));\n                \n                if (numer >= den)\n                {\n                    remainder = (remainder - (den << (15 - i)));\n                }\n                \n            }\n            return remainder;\n        }"
			+ "\n        unsigned int remUIUI (unsigned int num, unsigned int den)\n        {\n            unsigned int remainder = num;\n            unsigned int i;\n            \n            for (i=0; i < 32; i++)\n            {\n                unsigned int numer = (remainder >> (31 - i));\n                \n                if (numer >= den)\n                {\n                    remainder = (remainder - (den << (31 - i)));\n                }\n                \n            }\n            return remainder;\n        }"
			+ "\n        unsigned long long remULLULL (unsigned long long num, unsigned long long den)\n        {\n            unsigned long long remainder = num;\n                \n            unsigned int i;\n            \n            for (i=0; i < 64; i++)\n            {\n                unsigned long long numer = remainder >> (63 - i);\n                \n                if (numer >= den)\n                {\n                    remainder = (remainder - (den << (63 - i)));\n                }\n                    \n            }\n            return remainder;\n        }"
			+ "\n        char remCC(char num, char den)\n        {\n            /* If true, then the result needs to be negative. */\n            int flipResult = 0;\n\n            int remainder;\n            int denom;\n            int i;\n            int numer;\n            \n            if (num < 0)\n            {\n                num = -num;\n                flipResult ^= 1;\n            }\n            \n            if (den < 0)\n            {\n                den = -den;\n            }\n            \n            remainder = num;\n            /* Cast the denominator so we can use 9 bits.  This gets us\n               around the MIN problem since 9 bits can represent\n               -(MIN). */\n            denom = ((int)den) & 0x000000FF;\n            \n            for (i=0; i < 8; i++)\n            {\n                /* Cast the numerator so we can use 9 bits.  This gets us\n                   around the MIN problem since 9 bits can represent\n                   -(MIN). */\n                numer = ((int)(((int)remainder & 0x000000ff) >> (7 - i)));\n                \n                if (numer >= denom)\n                {\n                    remainder = (remainder - (den << (7 - i)));\n                }        \n            }\n            \n            /* If the numerator was negative, make the remainder negative. */\n            if (flipResult)\n            {\n                remainder = -remainder;\n            }\n            \n            return remainder;\n        }"
			+ "\n        short remSS(short num, short den)\n        {\n            /* If true, then the result needs to be negative. */\n            int flipResult = 0;\n\n            short remainder;\n            int denom;\n            int i;\n            int numer;\n            \n            if (num < 0)\n            {\n                num = -num;\n                flipResult ^= 1;\n            }\n            \n            if (den < 0)\n            {\n                den = -den;\n            }\n            \n            remainder = num;\n            /* Cast the denominator so we can use 17 bits.  This gets us\n               around the MIN problem since 17 bits can represent\n               -(MIN). */\n            denom = ((int)den) & 0x0000FFFF;\n            \n            for (i=0; i < 16; i++)\n            {\n                /* Cast the numerator so we can use 17 bits.  This gets us\n                   around the MIN problem since 17 bits can represent\n                   -(MIN). */\n                numer = ((int)(((int)remainder & 0x0000ffff) >> (15 - i)));\n                \n                if (numer >= denom)\n                {\n                    remainder = (remainder - (den << (15 - i)));\n                }        \n            }\n            \n            /* If the numerator was negative, make the remainder negative. */\n            if (flipResult)\n            {\n                remainder = -remainder;\n            }\n            \n            return remainder;\n        }"
			+ "\n        int remII(int num, int den)\n        {\n            /* If true, then the result needs to be negative. */\n            int flipResult = 0;\n\n            int remainder;\n            long long denom;\n            int i;\n            long long numer;\n            \n            if (num < 0)\n            {\n                num = -num;\n                flipResult ^= 1;\n            }\n            \n            if (den < 0)\n            {\n                den = -den;\n            }\n            \n            remainder = num;\n            /* Cast the denominator to a long so we can use 33 bits.  This gets us\n               around the MIN_INT problem since 33 bits can represent\n               -(MIN_INT). */\n            denom = ((long long)den) & 0xFFFFFFFFLL;\n            \n            for (i=0; i < 32; i++)\n            {\n                /* Cast the numerator to a long so we can use 33 bits.  This gets us\n                   around the MIN_INT problem since 33 bits can represent\n                   -(MIN_INT). */\n                numer = ((long long)(((long long)remainder & 0xffffffffLL) >>\n                                     (31 - i)));\n                \n                if (numer >= denom)\n                {\n                    remainder = (remainder - (den << (31 - i)));\n                }        \n            }\n            \n            /* If the numerator was negative, make the remainder negative. */\n            if (flipResult)\n            {\n                remainder = -remainder;\n            }\n            \n            return remainder;\n        }"
			+
			/**
			 * A fully functional impelentation of a 'long' (64 bit) signed
			 * remainder (%). This implementation conforms to the JVM
			 * specification for long remainder.
			 */
			"\n        long long remLLLL(long long num, long long den)\n        {\n            /* If the numerator is negative then negate the result. */\n            int flipResult = 0;\n            int i;\n            long long remainder;\n            long long upperDen, lowerDen;\n            long long numer;\n            long long upperNum, lowerNum;\n            \n            if (num < 0)\n            {\n                num = -num;\n                flipResult ^= 1;\n            }\n            \n            if (den < 0)\n            {\n                den = -den;\n            }\n            \n            remainder = num;\n            \n            /* Split the denominator into 2 halves so that we can\n               correctly represent -(MIN_LONG) as a positive value. */\n            upperDen = (den >> 32) & 0xffffffffLL;\n            lowerDen = den & 0x00000000FFFFFFFFLL;\n            \n            for (i=0; i < 64; i++)\n            {\n                numer = (long long)(((unsigned long long)remainder) >> (63 - i));\n                \n                /* Split the numerator into 2 halves so that we can\n                   correctly represent -(MIN_LONG) as a positive value. */\n                upperNum = (long long)(((unsigned long long)numer) >> 32);\n                lowerNum = numer & 0x00000000FFFFFFFFLL;\n                \n                if ((upperNum > upperDen) ||\n                    ((upperNum == upperDen) && (lowerNum >= lowerDen)))\n                {\n                    remainder = (remainder - (den << (63 - i)));\n                }\n                \n            }\n            \n            /* If the numerator was negative then negate the remainder. */\n            if (flipResult)\n            {\n                remainder = -remainder;\n            }\n            \n            return remainder;\n        }"
			+ "";
	/*
	 * #if 0 // Testing code below
	 * 
	 * #include<limits.h> #define ULLONG_MAX 18446744073709551615ULL
	 * 
	 * int main(int argc, char * argv[]) { char valuesC[]={CHAR_MAX-1,
	 * CHAR_MIN+1, CHAR_MAX, CHAR_MIN, CHAR_MAX-10,
	 * (CHAR_MAX/2),(CHAR_MAX/2)-1,(CHAR_MIN/2),(CHAR_MIN/2)+1,(CHAR_MAX/2)+2,
	 * (CHAR_MAX/2)-10,(CHAR_MAX/2)+10, CHAR_MAX/4, CHAR_MIN/4, CHAR_MIN/8,
	 * 64,63,62, -61, -39, 10,9,8,7,6, 5,4,3,2,1}; unsigned char
	 * valuesUC[]={UCHAR_MAX-1,UCHAR_MAX-2,UCHAR_MAX-3,UCHAR_MAX, UCHAR_MAX-100,
	 * (
	 * UCHAR_MAX/2),(UCHAR_MAX/2)-1,(UCHAR_MAX/2)-2,(UCHAR_MAX/2)+1,(UCHAR_MAX/2
	 * )+2, (UCHAR_MAX/2)-10,(UCHAR_MAX/2)+10, UCHAR_MAX/4, UCHAR_MAX/8,
	 * UCHAR_MAX/16, 64,63,62, 61, 39, 10,9,8,7,6, 5,4,3,2,1}; short
	 * valuesS[]={SHRT_MAX-1, SHRT_MIN+1, SHRT_MAX, SHRT_MIN, SHRT_MAX-100,
	 * (SHRT_MAX/2),(SHRT_MAX/2)-1,(SHRT_MIN/2),(SHRT_MIN/2)+1,(SHRT_MAX/2)+2,
	 * (SHRT_MAX/2)-100,(SHRT_MAX/2)+100, SHRT_MAX/4, SHRT_MIN/4, SHRT_MIN/8,
	 * 1024,1023,1021, 513, 39, 10,9,8,7,6, 5,4,3,2,1}; unsigned short
	 * valuesUS[]={USHRT_MAX-1,USHRT_MAX-2,USHRT_MAX-3,USHRT_MAX, USHRT_MAX-100,
	 * (
	 * USHRT_MAX/2),(USHRT_MAX/2)-1,(USHRT_MAX/2)-2,(USHRT_MAX/2)+1,(USHRT_MAX/2
	 * )+2, (USHRT_MAX/2)-100,(USHRT_MAX/2)+100, USHRT_MAX/4, USHRT_MAX/8,
	 * USHRT_MAX/1024, 1024,1023,1021, 513, 39, 10,9,8,7,6, 5,4,3,2,1}; unsigned
	 * int valuesI[]={UINT_MAX-1,UINT_MAX-2,UINT_MAX-3,UINT_MAX, UINT_MAX-100,
	 * (UINT_MAX/2),(UINT_MAX/2)-1,(UINT_MAX/2)-2,(UINT_MAX/2)+1,(UINT_MAX/2)+2,
	 * (UINT_MAX/2)-100,(UINT_MAX/2)+100, UINT_MAX/4, UINT_MAX/8, UINT_MAX/1024,
	 * 1024,1023,1021, 513, 39, 10,9,8,7,6, 5,4,3,2,1}; unsigned long long
	 * valuesLL[]={ULLONG_MAX-1ULL,ULLONG_MAX-2ULL,ULLONG_MAX-3ULL,ULLONG_MAX,
	 * ULLONG_MAX-100ULL,
	 * (ULLONG_MAX/2ULL),(ULLONG_MAX/2ULL)-1ULL,(ULLONG_MAX/2ULL
	 * )-2ULL,(ULLONG_MAX/2ULL)+1ULL,(ULLONG_MAX/2ULL)+2ULL,
	 * (ULLONG_MAX/2ULL)-100ULL,(ULLONG_MAX/2ULL)+100ULL, ULLONG_MAX/4ULL,
	 * ULLONG_MAX/8ULL, ULLONG_MAX/1024ULL, 1024ULL,1023ULL,1021ULL, 513ULL,
	 * 39ULL, 10ULL,9ULL,8ULL,7ULL,6ULL, 5ULL,4ULL,3ULL,2ULL,1ULL};
	 * 
	 * unsigned long long i,j;
	 * 
	 * 
	 * for (i=0; i<sizeof(valuesI)/4; i++) {
	 * printf("i=%u valuesI %x valuesLL %llx \n",i,valuesI[i],valuesLL[i]);
	 * 
	 * 
	 * for (j=0; j<sizeof(valuesI)/4; j++) {
	 * 
	 * if ((valuesC[i]%valuesC[j]) != remCC(valuesC[i],valuesC[j])) {
	 * printf("int: %x %x %x %% %x = %x, mod %x\n"
	 * ,i,j,valuesC[i],valuesC[j],(valuesC
	 * [i]%valuesC[j]),remCC(valuesC[i],valuesC[j])); } if
	 * ((valuesUC[i]%valuesUC[j]) != remUCUC(valuesUC[i],valuesUC[j])) {
	 * printf("int: %x %x %x %% %x = %x, mod %x\n"
	 * ,i,j,valuesUC[i],valuesUC[j],(valuesUC
	 * [i]%valuesUC[j]),remUCUC(valuesUC[i],valuesUC[j])); }
	 * 
	 * if ((valuesS[i]%valuesS[j]) != remSS(valuesS[i],valuesS[j])) {
	 * printf("int: %x %x %x %% %x = %x, mod %x\n"
	 * ,i,j,valuesS[i],valuesS[j],(valuesS
	 * [i]%valuesS[j]),remSS(valuesS[i],valuesS[j])); } if
	 * ((valuesUS[i]%valuesUS[j]) != remUSUS(valuesUS[i],valuesUS[j])) {
	 * printf("int: %x %x %x %% %x = %x, mod %x\n"
	 * ,i,j,valuesUS[i],valuesUS[j],(valuesUS
	 * [i]%valuesUS[j]),remUSUS(valuesUS[i],valuesUS[j])); }
	 * 
	 * if ((valuesI[i]%valuesI[j]) != remUIUI(valuesI[i],valuesI[j])) {
	 * printf("int: %x %x %x %% %x = %x, mod %x\n"
	 * ,i,j,valuesI[i],valuesI[j],(valuesI
	 * [i]%valuesI[j]),remUIUI(valuesI[i],valuesI[j])); } if
	 * ((valuesLL[i]%valuesLL[j]) != remULLULL(valuesLL[i],valuesLL[j])) {
	 * printf
	 * ("long: %u %u %llx %% %llx = %llx, rem= %llx\n",i,j,valuesLL[i],valuesLL
	 * [j],(valuesLL[i]%valuesLL[j]),remULLULL(valuesLL[i],valuesLL[j])); }
	 * 
	 * } if ((0U % valuesI[i]) != (remUIUI(0U, valuesI[i]))) {
	 * printf("%x %% %x = %x, rem= %x\n"
	 * ,0U,valuesI[i],0U%valuesI[i],remUIUI(0U,valuesI[i])); } if ((0ULL %
	 * valuesLL[i]) != (remULLULL(0ULL, valuesLL[i]))) {
	 * printf("%llx %% %llx = %llx, rem= %llx\n"
	 * ,0ULL,valuesLL[i],0ULL%valuesLL[i],remULLULL(0ULL,valuesLL[i])); }
	 * 
	 * } }
	 * 
	 * 
	 * #endif
	 */

} // ForgeResource

