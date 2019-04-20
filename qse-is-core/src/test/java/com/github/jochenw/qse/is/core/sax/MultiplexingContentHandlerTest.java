package com.github.jochenw.qse.is.core.sax;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.Arrays;

import org.junit.Test;
import org.xml.sax.ContentHandler;

import com.github.jochenw.qse.is.core.rules.BasicNodeInfoParser;
import com.github.jochenw.qse.is.core.rules.BasicNodeInfoParserTest;


public class MultiplexingContentHandlerTest {
	@Test
	public void test() {
		final URL url = BasicNodeInfoParserTest.class.getResource("BasicNodeInfo.ndf");
		assertNotNull(url);
		final BasicNodeInfoParser bnip = new BasicNodeInfoParser();
		final ContentHandler ch = (ContentHandler) bnip;
		final boolean terminated = Sax.parseTerminable(url, Arrays.asList(ch));
		assertTrue(terminated);
		BasicNodeInfoParserTest.validate(bnip);
	}
}
