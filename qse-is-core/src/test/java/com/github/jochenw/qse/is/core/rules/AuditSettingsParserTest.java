package com.github.jochenw.qse.is.core.rules;

import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Test;

import com.github.jochenw.qse.is.core.sax.Sax;


public class AuditSettingsParserTest {
	@Test
	public void testDefaultAuditSettingsService() {
		final URL url = getClass().getResource("DefaultAuditSettingsService.ndf");
		assertNotNull(url);
		final AuditSettingsParser asp = new AuditSettingsParser();
		Sax.parseTerminable(url, asp);
		assertDefaultSettings(asp);
	}

	@Test
	public void testBasicNodeInfoService() {
		final URL url = getClass().getResource("BasicNodeInfo.ndf");
		assertNotNull(url);
		final AuditSettingsParser asp = new AuditSettingsParser();
		Sax.parseTerminable(url, asp);
		assertDefaultSettings(asp);
	}

	private void assertDefaultSettings(final AuditSettingsParser asp) {
		assertEquals("0", asp.getAuditOption());
		assertEquals("false", asp.getStartExecution());
		assertEquals("false", asp.getStopExecution());
		assertEquals("true", asp.getOnError());
	}
}
