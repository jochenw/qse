package com.github.jochenw.qse.is.core.rules;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;

import com.github.jochenw.qse.is.core.sax.Sax;


public class InvocationValidatingServiceParserTest {
	@Test
	public void testServiceUsingLogMessageFromCatalog() throws Exception {
		runTest("ServiceUsingLogMessageFromCatalog.xml", (list) -> {
			assertEquals(3*5, list.size());
			assertInvocation(list, 0, "wx.log.pub:logMessageFromCatalog", "FOO", "100", "0001", null);
			assertInvocation(list, 1, "wx.log.pub:logMessageFromCatalog", "FOO", "100", "0002", null);
			assertInvocation(list, 2, "wx.log.pub:logMessageFromCatalog", "BAR", "100", "0001", null);
		});
	}

	@Test
	public void testServiceUsingLogMessageFromCatalogOverridingSeverity() throws Exception {
		runTest("ServiceUsingLogMessageFromCatalogOverridingSeverity.xml", (list) -> {
			assertEquals(1*5, list.size());
			assertInvocation(list, 0, "wx.log.pub:logMessageFromCatalog", "ONE", "002", "0003", "INFO");
		});
	}
	
	private void runTest(String pUri, Consumer<List<String>> pValidator) {
		final URL url = getClass().getResource(pUri);
		assertNotNull(url);
		final List<String> list = new ArrayList<>();
		final InvocationValidatingServiceParser parser = new InvocationValidatingServiceParser() {
			@Override
			protected void note(Invocation pInvocation) {
				list.add(pInvocation.getServiceName());
				list.add(pInvocation.getParameterValue("componentKey"));
				list.add(pInvocation.getParameterValue("facilityKey"));
				list.add(pInvocation.getParameterValue("messageKey"));
				list.add(pInvocation.getParameterValue("severity"));
			}
		};
		Sax.parseTerminable(url, parser);
		if (pValidator != null) {
			pValidator.accept(list);
		}
		//assertInvocation(list, 3, "wx.log.pub:logMessageFromCatalog", "", "", "", null);
	}
	
	protected void assertInvocation(List<String> pGot, int pIndex, String pServiceName, String pComponentKey, String pFacilityKey, String pMessageKey, String pSeverity) {
		final int offset = pIndex*5;
		assertTrue(pGot.size() >= offset+4);
		assertEquals("Service name", pServiceName, pGot.get(offset)); 
		assertEquals("Component key", pComponentKey, pGot.get(offset+1)); 
		assertEquals("Facility key", pFacilityKey, pGot.get(offset+2)); 
		assertEquals("Message key", pMessageKey, pGot.get(offset+3)); 
		assertEquals("Severity", pSeverity, pGot.get(offset+4)); 
	}
}
