package com.github.jochenw.qse.is.core.rules;

import org.junit.Assert;
import org.junit.Test;

import com.github.jochenw.qse.is.core.rules.DependencyCheckingRule.DependencySpec;


public class DependencyCheckingRuleTest {
	@Test
	public void testDependencySpecification() {
		try {
			new DependencySpec(null);
			Assert.fail("Expected NullPointerException");
		} catch (NullPointerException npe) {
			Assert.assertEquals("A dependency specification must not be null.", npe.getMessage());
		}
		try {
			new DependencySpec("foo,bar");
			Assert.fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException npe) {
			Assert.assertEquals("Invalid dependency specification: foo,bar", npe.getMessage());
		}
		final DependencySpec ds = new DependencySpec("WxConfig:^wx.config[.\\:].*");
		Assert.assertEquals(1, ds.getPackageNames().length);
		Assert.assertEquals("WxConfig", ds.getPackageNames()[0]);
		Assert.assertTrue(ds.matches("wx.config.pub:getValue"));
		Assert.assertTrue(ds.matches("wx.config:startUp"));
		Assert.assertFalse(ds.matches("wx.log.pub:logMessage"));
		final DependencySpec ds2 = new DependencySpec("Foo,Bar|Baz:^wx.config[.\\:].*");
		Assert.assertEquals(3, ds2.getPackageNames().length);
		Assert.assertEquals("Foo", ds2.getPackageNames()[0]);
		Assert.assertEquals("Bar", ds2.getPackageNames()[1]);
		Assert.assertEquals("Baz", ds2.getPackageNames()[2]);
		try {
			new DependencySpec(":");
			Assert.fail("Expected IllegalArgumentException");
		} catch (IllegalArgumentException npe) {
			Assert.assertEquals("Invalid dependency specification: :", npe.getMessage());
		}
	}
}
