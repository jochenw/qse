package com.github.jochenw.qse.is.sonar.util;

import org.junit.Assert;
import org.junit.Test;

public class StringsTest {
	@Test
	public void testFormat() {
		Assert.assertEquals("Hello, Nicki!", Strings.format("Hello, {}!", "Nicki"));
		try {
			Strings.format(null);
		} catch (NullPointerException e) {
			Assert.assertEquals("Msg", e.getMessage());
		}
		try {
			Strings.formatf("", null);
		} catch (NullPointerException e) {
			Assert.assertEquals("Marker", e.getMessage());
		}
		try {
			Strings.format("Hello, {}!", (Object[]) null);
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("The number of markers exceeds the number of arguments.", e.getMessage());
		}
		try {
			Strings.format("Hello, {}!", "foo", "bar");
		} catch (IllegalArgumentException e) {
			Assert.assertEquals("The number of parameters exceeds the number of markers.", e.getMessage());
		}
		Assert.assertEquals("Running test: test", Strings.format("Running test: {}", "test"));
	}

	@Test
	public void testFind() {
		final int offset = Strings.find("Running test: {}", "{}");
		Assert.assertEquals(14, offset);
	}
}
