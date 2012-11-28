package org.xronos.orcc.design.util;

public class XronosMathUtil {

	public static Integer nearestPowTwo(Integer num) {
		Integer n = num > 0 ? num - 1 : 0;

		n |= n >> 1;
		n |= n >> 2;
		n |= n >> 4;
		n |= n >> 8;
		n |= n >> 16;
		n++;

		return n;
	}
}
