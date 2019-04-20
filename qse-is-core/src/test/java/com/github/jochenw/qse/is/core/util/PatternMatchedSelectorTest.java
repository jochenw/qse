package com.github.jochenw.qse.is.core.util;

import org.junit.Assert;
import org.junit.Test;

public class PatternMatchedSelectorTest {
	@Test
	public void testPublicServices() {
		final String[] includes = { "^.*\\.pub(\\..*\\:|\\:).*$",
				                    "^.*\\.ws\\.provider(\\..*\\:|\\:).*$",
				                    "^.*\\:(_get|_post|_put|_delete|_default)$" };
		final PatternMatchedSelector pms = new PatternMatchedSelector(includes, null);
		assertFalse(pms, "jwi.scratch.impl:SomeService");
		assertTrue(pms, "jwi.scratch.pub:AnotherService");
		assertTrue(pms, "jwi.scratch.pub.foo:AnotherService");
		assertFalse(pms, "jwi.scratch.ws:MyService");
		assertFalse(pms, "jwi.scratch.ws.consumer:FooService");
		assertTrue(pms, "jwi.scratch.ws.provider:YetAnotherService");
		assertTrue(pms, "jwi.scratch.rst:_post");
		assertTrue(pms, "jwi.scratch.rst.resource:_post");
		assertTrue(pms, "jwi.scratch.rst:_get");
		assertTrue(pms, "jwi.scratch.rst.resource:_get");
		assertTrue(pms, "jwi.scratch.rst:_put");
		assertTrue(pms, "jwi.scratch.rst.resource:_put");
		assertTrue(pms, "jwi.scratch.rst:_delete");
		assertTrue(pms, "jwi.scratch.rst.resource:_delete");
		assertTrue(pms, "jwi.scratch.rst:_default");
		assertTrue(pms, "jwi.scratch.rst.resource:_default");
	}

	private void assertFalse(PatternMatchedSelector pPms, String pValue) {
		Assert.assertFalse(pValue, pPms.matches(pValue));
	}

	private void assertTrue(PatternMatchedSelector pPms, String pValue) {
		Assert.assertTrue(pValue, pPms.matches(pValue));
	}
}
