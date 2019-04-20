package com.github.jochenw.qse.is.core.util;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PatternMatchedSelector {
	private final String[] includedValues;
	private final String[] excludedValues;
	private final Pattern[] includedPatterns;
	private final Pattern[] excludedPatterns;

	public PatternMatchedSelector(@Nullable String[] pIncludedValues, @Nullable String[] pExcludedValues) {
		if (pIncludedValues == null  ||  pIncludedValues.length == 0) {
			includedValues = null;
			includedPatterns = null;
		} else {
			includedValues = pIncludedValues;
			includedPatterns = asPatterns(includedValues);
		}
		if (pExcludedValues == null  ||  pExcludedValues.length == 0) {
			excludedValues = null;
			excludedPatterns = null;
		} else {
			excludedValues = pExcludedValues;
			excludedPatterns = asPatterns(excludedValues);
		}
	}

	private Pattern[] asPatterns(@Nonnull String[] pValues) {
		final Pattern[] patterns = new Pattern[pValues.length];
		for (int i = 0;  i < patterns.length;  i++) {
			try {
				patterns[i] = Pattern.compile(pValues[i]);
			} catch (PatternSyntaxException e) {
				throw new IllegalArgumentException("Invalid regexp pattern: " + pValues[i], e);
			}
		}
		return patterns;
	}

	public boolean matches(@Nonnull String pValue) {
		if (includedPatterns == null) {
			if (excludedPatterns == null) {
				return true;
			} else {
				return !matches(pValue, excludedPatterns);
			}
		} else {
			if (excludedPatterns == null) {
				return matches(pValue, includedPatterns);
			} else {
				return matches(pValue, includedPatterns)  &&  !matches(pValue, excludedPatterns);
			}
		}
	}

	protected boolean matches(@Nonnull String pValue, @Nonnull Pattern[] pPatterns) {
		for (Pattern p : pPatterns) {
			if (p.matcher(pValue).matches()) {
				return true;
			}
		}
		return false;
	}
}
