package com.github.jochenw.qse.is.core.stax;

import static org.junit.Assert.*;

import javax.xml.stream.Location;

import org.junit.Test;

public class StaxTest {
	@Test
	public void test() {
		validate("Some error", null, -1, -1);
		validate("At SomeFile: Some error", "Some error", "SomeFile", -1, -1);
		validate("At SomeFile, line 15: Some error", "Some error", "SomeFile", 15, -1);
		validate("At SomeFile, column 31: Some error", "Some error", "SomeFile", -1, 31);
		validate("At SomeFile, line 14, column 61: Some error", "Some error", "SomeFile", 14, 61);
		validate("At line 15: Some error", "Some error", null, 15, -1);
		validate("At column 31: Some error", "Some error", null, -1, 31);
		validate("At line 14, column 61: Some error", "Some error", null, 14, 61);
	}

	protected void validate(String pMsg, String pSystemId, int pLineNumber, int pColumnNumber) {
		validate(pMsg, pMsg, pSystemId, pLineNumber, pColumnNumber);
	}

	protected void validate(String pExpect, String pMsg, String pSystemId, int pLineNumber, int pColumnNumber) {
		assertEquals(pExpect, Stax.asLocalizedMessage(pMsg, pSystemId, pLineNumber, pColumnNumber));
		final Location loc = new Location() {
			@Override
			public int getLineNumber() {
				return pLineNumber;
			}

			@Override
			public int getColumnNumber() {
				return pColumnNumber;
			}

			@Override
			public int getCharacterOffset() {
				return -1;
			}

			@Override
			public String getPublicId() {
				return null;
			}

			@Override
			public String getSystemId() {
				return pSystemId;
			}
		};
		assertEquals(pExpect, Stax.asLocalizedMessage(loc, pMsg));
	}
}
