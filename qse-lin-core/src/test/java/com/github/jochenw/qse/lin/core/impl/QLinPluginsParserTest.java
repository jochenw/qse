/*
 *    Copyright 2019 Jochen Wiedmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.github.jochenw.qse.lin.core.impl;

import static org.junit.Assert.*;

import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.xml.sax.InputSource;

import com.github.jochenw.afw.core.util.MutableInteger;
import com.github.jochenw.qse.lin.core.api.IPluginRegistry;
import com.github.jochenw.qse.lin.core.api.IPluginRegistry.PluginInfo;
import com.github.jochenw.qse.lin.core.plugins.QLinPluginsParser;
import com.github.jochenw.qse.lin.core.plugins.XmlPluginRegistry;

public class QLinPluginsParserTest {
	private static final String EMPTY_PLUGIN_LIST =
		"<qse-lin-plugins xmlns='" + QLinPluginsParser.NS + "'>\n"
		+ "</qse-lin-plugins>\n";

	@Test
	public void testEmptyPluginList() {
		final List<PluginInfo> list = parse(EMPTY_PLUGIN_LIST, "EMPTY_PLUGIN_LIST");
		assertTrue(list.isEmpty());
	}

	private static final String SINGLE_PLUGIN_LIST =
			"<qse-lin-plugins xmlns='" + QLinPluginsParser.NS + "'>\n"
			+ "<plugin id='Fam0' name='Name0' type='family'/>\n"
			+ "</qse-lin-plugins>\n";

	@Test
	public void testSinglePluginList() {
		final List<PluginInfo> list = parse(SINGLE_PLUGIN_LIST, "SINGLE_PLUGIN_LIST");
		assertEquals(1, list.size());
		final PluginInfo pi0 = list.get(0);
		assertEquals("Fam0", pi0.getId());
		assertNull(pi0.getFamilyId());
		assertEquals("Name0", pi0.getName());
		assertEquals(IPluginRegistry.Type.family, pi0.getType());
		assertNull(pi0.getClassName());
		assertNull(pi0.getDescription());
		assertTrue(pi0.getProperties().isEmpty());
	}

	private List<PluginInfo> parse(String pXml, String pUri) {
		final InputSource isource = new InputSource(new StringReader(pXml));
		isource.setSystemId(pUri);
		final List<PluginInfo> list = new ArrayList<>();
		new QLinPluginsParser().parse(isource, (pi) -> list.add(pi));
		return list;
	}

	@Test
	public void testBuiltinPluginList() {
		final String uri =  "META-INF/qse/lin/qse-lin-plugins.xml";
		final URL url = getClass().getClassLoader().getResource(uri);
		assertNotNull(url);
		final List<PluginInfo> plugins = new ArrayList<>();
		final MutableInteger numberOfLicenseFamilies = new MutableInteger();
		final MutableInteger numberOfLicenses = new MutableInteger();
		final MutableInteger numberOfMatchers = new MutableInteger();
		final MutableInteger numberOfArchiveHandlers = new MutableInteger();
		final MutableInteger numberOfExclusionLists = new MutableInteger();
		new QLinPluginsParser().parse(url, (pi) -> {
			plugins.add(pi);
			switch (pi.getType()) {
			case family:
				numberOfLicenseFamilies.inc();
				break;
			case license:
				numberOfLicenses.inc();
				break;
			case matcher:
				numberOfMatchers.inc();
				break;
			case archive:
				numberOfArchiveHandlers.inc();
				break;
			case exclusion:
				numberOfExclusionLists.inc();
				break;
			default:
				throw new IllegalStateException("Invalid type: " + pi.getType());
			}
		});
		assertEquals(10, numberOfLicenseFamilies.getValue());
		assertEquals(14, numberOfLicenses.getValue());
		assertEquals(15, numberOfMatchers.getValue());
		assertEquals(0, numberOfArchiveHandlers.getValue());
		assertEquals(5, numberOfExclusionLists.getValue());
	}

	@Test
	public void testPluginRegistry() {
		final XmlPluginRegistry xpr = new XmlPluginRegistry(getClass().getClassLoader(),  "META-INF/qse/lin/qse-lin-plugins.xml");
		xpr.init();
		assertEquals(10, xpr.getFamilies().size());
		assertEquals(14, xpr.getLicenses().size());
		assertEquals(15, xpr.getMatchers().size());
		assertEquals(0, xpr.getArchiveHandlers().size());
		assertEquals(5, xpr.getExclusionLists().size());
	}
}
