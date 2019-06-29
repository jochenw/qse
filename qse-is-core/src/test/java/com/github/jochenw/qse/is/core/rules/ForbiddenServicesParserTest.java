package com.github.jochenw.qse.is.core.rules;

import static org.junit.Assert.*;

import java.util.ArrayList;

import java.net.URL;
import java.util.List;

import org.junit.Test;

import com.github.jochenw.qse.is.core.sax.Sax;


public class ForbiddenServicesParserTest {
	@Test
	public void testServiceUsingDebugLog() throws Exception {
		runTest("ServiceUsingDebugLog.xml");
	}

	@Test
	public void testServiceUsingDebugLogInTransformer() throws Exception {
		runTest("ServiceUsingDebugLogInTransformer.xml");
	}

	@Test
	public void testServiceUsingDebugLogTwice() throws Exception {
		runTest("ServiceUsingDebugLogTwice.xml", "pub.flow:debugLog");
	}

	@Test
	public void testServiceUsingBothAppendToServices() throws Exception {
		runTest("ServiceUsingBothAppendToServices.xml", "pub.list:appendToStringList", "pub.list:appendToDocumentList");
	}

	private void runTest(String pUri) {
		runTest(pUri, "pub.flow:debugLog");
	}
	
	private void runTest(String pUri, String... pServiceNames) {
		final URL url = getClass().getResource(pUri);
		assertNotNull(url);
		final List<String> services = new ArrayList<>();
		final ServiceInvocationParser sip = new ServiceInvocationParser() {
			@Override
			protected void serviceInvocation(String pServiceName) {
				for (String s : pServiceNames) {
					if (s.equals(pServiceName)  &&  !services.contains(pServiceName)) {
						services.add(pServiceName);
					}
				}
			}
		};
		Sax.parseTerminable(url, sip);
		assertEquals(pServiceNames.length, services.size());
		for (int i = 0;  i < pServiceNames.length;  i++) {
			assertEquals(pServiceNames[i], services.get(i));
		}
	}
}
