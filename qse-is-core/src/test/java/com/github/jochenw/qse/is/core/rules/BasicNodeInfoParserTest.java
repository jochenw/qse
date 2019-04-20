package com.github.jochenw.qse.is.core.rules;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

import com.github.jochenw.qse.is.core.sax.Sax;


public class BasicNodeInfoParserTest {
	@Test
	public void testFlow() {
		final URL url = getClass().getResource("BasicNodeInfo.ndf");
		assertNotNull(url);
		final BasicNodeInfoParser bnip = new BasicNodeInfoParser();
		final boolean terminated = Sax.parseTerminable(url, bnip);
		assertTrue(terminated);
		validate(bnip);
	}

	@Test
	public void testRecord() {
		final URL url = getClass().getResource("TestDocType.ndf");
		assertNotNull(url);
		final BasicNodeInfoParser bnip = new BasicNodeInfoParser();
		final boolean terminated = Sax.parseTerminable(url, bnip);
		assertTrue(terminated);
		assertEquals("record", bnip.getType());
	}

	@Test
	public void testCbxAcknowledgeEnvelopeRecord() {
		final URL url = getClass().getResource("CbxAcknowledgeEnvelope.ndf");
		assertNotNull(url);
		final BasicNodeInfoParser bnip = new BasicNodeInfoParser();
		final boolean terminated = Sax.parseTerminable(url, bnip);
		assertTrue(terminated);
		assertEquals("record", bnip.getType());
	}

	@Test
	public void testTrigger() {
		final URL url = getClass().getResource("Trigger.ndf");
		assertNotNull(url);
		final BasicNodeInfoParser bnip = new BasicNodeInfoParser();
		final boolean terminated = Sax.parseTerminable(url, bnip);
		assertTrue(terminated);
		assertEquals("webMethods/trigger", bnip.getType());
		assertNull(bnip.getServiceType());
		assertNull(bnip.getSubType());
		assertNull(bnip.getSigType());
	}

	@Test
	public void testAdapterService() {
		final URL url = getClass().getResource("AdapterService.ndf");
		assertNotNull(url);
		final BasicNodeInfoParser bnip = new BasicNodeInfoParser();
		final boolean terminated = Sax.parseTerminable(url, bnip);
		assertTrue(terminated);
		assertEquals("service", bnip.getType());
		assertEquals("AdapterService", bnip.getServiceType());
		assertEquals("unknown", bnip.getSubType());
		assertEquals("unknown", bnip.getSigType());
	}
	public static void validate(final BasicNodeInfoParser bnip) {
		assertEquals("service", bnip.getType());
		assertEquals("default", bnip.getSubType());
		assertEquals("java 3.5", bnip.getSigType());
		assertEquals("Some Comment", bnip.getComment());
	}
}
