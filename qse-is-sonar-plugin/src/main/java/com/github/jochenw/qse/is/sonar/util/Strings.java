package com.github.jochenw.qse.is.sonar.util;

import java.util.Objects;

public class Strings {
	public static String format(String pMsg, Object... pArgs) {
		return formatf(pMsg, "{}", pArgs);
	}

	public static String formatf(CharSequence pMsg, CharSequence pMarker, Object... pArgs) {
		Objects.requireNonNull(pMsg, "Msg");
		Objects.requireNonNull(pMarker, "Marker");
		if (pMarker.length() == 0) {
			throw new IllegalArgumentException("The marker string must not be empty.");
		}
		final int offset = find(pMsg, pMarker);
		if (offset == -1) {
			if (pArgs != null  &&  pArgs.length == 0) {
				throw new IllegalArgumentException("Parameters given, but no markers found in the message string.");
			}
			return pMsg.toString();
		} else {
			final StringBuilder sb = new StringBuilder();
			int numParameters = 0;
			final int len = pMsg.length()-pMarker.length();
			int index = 0;
			while (index <= len) {
				if (isSubstring(pMsg, pMarker, index)) {
					if (pArgs == null  ||  pArgs.length <= numParameters) {
						throw new IllegalArgumentException("The number of markers exceeds the number of arguments.");
					}
					sb.append(pArgs[numParameters++]);
					index += pMarker.length();
				} else {
					sb.append(pMsg.charAt(index++));
				}
			}
			for (int i = index;  i < pMsg.length();  i++) {
				sb.append(pMsg.charAt(i));
			}
			if (pArgs != null  &&  numParameters < pArgs.length) {
				throw new IllegalArgumentException("The number of parameters exceeds the number of markers.");
			}
			return sb.toString();
		}
	}

	public static int find(CharSequence pMsg, CharSequence pMarker) {
		final int len = pMsg.length()-pMarker.length();
		for (int i = 0;  i <= len;  i++) {
			if (isSubstring(pMsg, pMarker, i)) {
				return i;
			}
		}
		return -1;
	}

	public static boolean isSubstring(CharSequence pMsg, CharSequence pMarker, int pOffset) {
		for (int j = 0;  j < pMarker.length();  j++) {
			if (pMsg.charAt(pOffset+j) != pMarker.charAt(j)) {
				return false;
			}
		}
		return true;
	}
}
