package com.github.jochenw.qse.is.core.model;

import static org.junit.Assert.*;

import org.junit.Test;

public class NSNameTest {
	@Test
	public void testValueOf() {
		assertNSName("pub.flow:debugLog", "pub.flow", "debugLog");
		assertNSName(NSName.valueOf("pub.flow.debugLog"), "pub.flow:debugLog", "pub.flow", "debugLog");
		assertNSName("MyService", "", "MyService");
	}

	protected void assertNSName(String pQName, String pPackageName, String pSvcName) {
		assertNSName(NSName.valueOf(pQName), pQName, pPackageName, pSvcName);
	}

	protected void assertNSName(NSName pName, String pQName, String pPackageName, String pSvcName) {
		assertEquals(pQName, pName.getQName());
		assertEquals(pPackageName, pName.getPackageName());
		assertEquals(pSvcName, pName.getNodeName());
	}
}
