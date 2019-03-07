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
package com.github.jochenw.qse.lin.core.plugins;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.github.jochenw.qse.lin.core.api.IPluginRegistry.PluginInfo;
import com.github.jochenw.qse.lin.core.api.IPluginRegistry.Type;

public class QLinPluginsParserTest {
	private static final String EMPTY_PLUGIN_LIST =
			"<qse-lin-plugins xmlns='" + QLinPluginsParser.NS + "'>\n"
			+ "</qse-lin-plugins>\n";

	@Test
	public void test() throws Exception {
		final List<PluginInfo> emptyList = parse(EMPTY_PLUGIN_LIST, "EMPTY_PLUGIN_LIST");
		assertNotNull(emptyList);
		assertTrue(emptyList.isEmpty());
	}

	private static final String SINGLE_PLUGIN_LIST =
			"<qse-lin-plugins xmlns='" + QLinPluginsParser.NS + "'>\n"
			+ "  <plugin type='family' id='FAM0' name='License Family 0'/>\n"
			+ "</qse-lin-plugins>\n";

	@Test
	public void testSinglePluginList() throws Exception {
		final List<PluginInfo> singlePluginList = parse(SINGLE_PLUGIN_LIST, "SINGLE_PLUGIN_LIST");
		assertNotNull(singlePluginList);
		assertEquals(1, singlePluginList.size());
		final PluginInfo pi = singlePluginList.get(0);
		assertSame(Type.family, pi.getType());
		assertEquals("FAM0", pi.getId());
		assertEquals("License Family 0", pi.getName());
		assertNull(pi.getClassName());
		assertNull(pi.getDescription());
		assertNull(pi.getFamilyId());
		final Map<String,String> props = pi.getProperties();
		assertNotNull(props);
		assertTrue(props.isEmpty());
	}

	private static final String SMALL_PLUGIN_LIST =
			"<qse-lin-plugins xmlns='" + QLinPluginsParser.NS + "'>\n"
			+ "  <plugin type='family' id='FAM0' name='License Family 0'/>\n"
			+ "  <plugin type='license' id='LIC0' familyId='FAM0' name='License 0'>\n"
			+ "    <description>Some lengthy description of License 0.</description>\n"
			+ "  </plugin>\n"
			+ "  <plugin type='matcher' class='some.class.Name' id='LIC0'>\n"
			+ "    <property name='PROP0' value='VALUE0'/>\n"
			+ "    <property name='PROP1'>\n"
			+ "      Some property value, which\n"
			+ "      extends a single line.\n"
			+ "    </property>\n"
			+ "    <property name='PROP2'>Another property value.</property>\n"
			+ "  </plugin>\n"
			+ "</qse-lin-plugins>\n";

	@Test
	public void testSmallPluginList() throws Exception {
		final List<PluginInfo> smallPluginList = parse(SMALL_PLUGIN_LIST, "SMALL_PLUGIN_LIST");
		assertNotNull(smallPluginList);
		assertEquals(3, smallPluginList.size());
		final PluginInfo pi0 = smallPluginList.get(0);
		assertSame(Type.family, pi0.getType());
		assertEquals("FAM0", pi0.getId());
		assertEquals("License Family 0", pi0.getName());
		assertNull(pi0.getClassName());
		assertNull(pi0.getDescription());
		assertNull(pi0.getFamilyId());
		final Map<String,String> props0 = pi0.getProperties();
		assertTrue(props0.isEmpty());

		final PluginInfo pi1 = smallPluginList.get(1);
		assertSame(Type.license, pi1.getType());
		assertEquals("LIC0", pi1.getId());
		assertEquals("FAM0", pi1.getFamilyId());
		assertEquals("License 0", pi1.getName());
		assertNull(pi1.getClassName());
		assertEquals("Some lengthy description of License 0.", pi1.getDescription());
		final Map<String,String> props1 = pi1.getProperties();
		assertTrue(props1.isEmpty());

		final PluginInfo pi2 = smallPluginList.get(2);
		assertSame(Type.matcher, pi2.getType());
		assertEquals("LIC0", pi1.getId());
		assertEquals("some.class.Name", pi2.getClassName());
		assertNull(pi2.getDescription());
		assertNull(pi2.getFamilyId());
		final Map<String,String> props2 = pi2.getProperties();
		assertEquals(3, props2.size());
		assertEquals("VALUE0", props2.get("PROP0"));
		final String value1 = props2.get("PROP1");
		assertTrue(value1.contains("Some property value, which"));
		assertTrue(value1.contains("extends a single line."));
		assertEquals("Another property value.", props2.get("PROP2"));
	}

	private static final String DUPLICATE_ID_ERROR =
			"<qse-lin-plugins xmlns='" + QLinPluginsParser.NS + "'>\n"
			+ "  <plugin type='family' id='FAM0' name='License Family 0'/>\n"
			+ "  <plugin type='license' id='LIC0' familyId='FAM0' name='License 0'>\n"
			+ "    <description>Some lengthy description of License 0.</description>\n"
			+ "  </plugin>\n"
			+ "  <plugin type='matcher' class='some.class.Name' id='LIC0'>\n"
			+ "    <property name='PROP0' value='VALUE0'/>\n"
			+ "    <property name='PROP1'>\n"
			+ "      Some property value, which\n"
			+ "      extends a single line.\n"
			+ "    </property>\n"
			+ "    <property name='PROP2'>Another property value.</property>\n"
			+ "  </plugin>\n"
			+ "  <plugin type='matcher' class='other.class.Name' id='LIC0'>\n"
			+ "    <property name='PROP0' value='VALUE0'/>\n"
			+ "    <property name='PROP1'>\n"
			+ "      Some property value, which\n"
			+ "      extends a single line.\n"
			+ "    </property>\n"
			+ "    <property name='PROP2'>Another property value.</property>\n"
			+ "  </plugin>\n"
			+ "  <plugin type='license' id='LIC0' name='Second instance of License 0'/>\n"
			+ "</qse-lin-plugins>\n";

	@Test
	public void testDuplicateIdError() throws Exception {
		try {
			parse(DUPLICATE_ID_ERROR, "DUPLICATE_ID_ERROR");
			fail("Expected exception");
		} catch (SAXParseException spe) {
			assertEquals("Duplicate id for plugin type=license: LIC0", spe.getMessage());
			assertEquals(22, spe.getLineNumber());
			assertTrue(spe.getSystemId().contains("DUPLICATE_ID_ERROR"));
		}
	}

	private static final String INVALID_FAMILY_ID_ERROR =
			"<qse-lin-plugins xmlns='" + QLinPluginsParser.NS + "'>\n"
			+ "  <plugin type='family' id='FAM' name='License Family 0'/>\n"
			+ "  <plugin type='license' id='LIC0' familyId='FAM0' name='License 0'>\n"
			+ "    <description>Some lengthy description of License 0.</description>\n"
			+ "  </plugin>\n"
			+ "  <plugin type='matcher' class='some.class.Name' id='LIC0'>\n"
			+ "    <property name='PROP0' value='VALUE0'/>\n"
			+ "    <property name='PROP1'>\n"
			+ "      Some property value, which\n"
			+ "      extends a single line.\n"
			+ "    </property>\n"
			+ "    <property name='PROP2'>Another property value.</property>\n"
			+ "  </plugin>\n"
			+ "</qse-lin-plugins>\n";

	@Test
	public void testInvalidFamilyIdError() throws Exception {
		try {
			parse(INVALID_FAMILY_ID_ERROR, "INVALID_FAMILY_ID_ERROR");
			fail("Expected exception");
		} catch (SAXParseException spe) {
			assertEquals("No plugin available with plugin/@type=family, and plugin/@id=FAM0", spe.getMessage());
			assertEquals(3, spe.getLineNumber());
			assertTrue(spe.getSystemId().contains("INVALID_FAMILY_ID_ERROR"));
		}
	}


	private static final String INVALID_LICENSE_ID_ERROR =
			"<qse-lin-plugins xmlns='" + QLinPluginsParser.NS + "'>\n"
			+ "  <plugin type='family' id='FAM0' name='License Family 0'/>\n"
			+ "  <plugin type='license' id='LIC0' familyId='FAM0' name='License 0'>\n"
			+ "    <description>Some lengthy description of License 0.</description>\n"
			+ "  </plugin>\n"
			+ "  <plugin type='matcher' class='some.class.Name' id='LIC0'>\n"
			+ "    <property name='PROP0' value='VALUE0'/>\n"
			+ "    <property name='PROP1'>\n"
			+ "      Some property value, which\n"
			+ "      extends a single line.\n"
			+ "    </property>\n"
			+ "    <property name='PROP2'>Another property value.</property>\n"
			+ "  </plugin>\n"
			+ "  <plugin type='matcher' class='some.class.Name' id='LIC'>\n"
			+ "    <property name='PROP0' value='VALUE0'/>\n"
			+ "    <property name='PROP1'>\n"
			+ "      Some property value, which\n"
			+ "      extends a single line.\n"
			+ "    </property>\n"
			+ "    <property name='PROP2'>Another property value.</property>\n"
			+ "  </plugin>\n"
			+ "</qse-lin-plugins>\n";

	@Test
	public void testInvalidLicenseIdError() throws Exception {
		try {
			parse(INVALID_LICENSE_ID_ERROR, "INVALID_LICENSE_ID_ERROR");
			fail("Expected exception");
		} catch (SAXParseException spe) {
			assertEquals("No plugin available with plugin/@type=license, and plugin/@id=LIC", spe.getMessage());
			assertEquals(14, spe.getLineNumber());
			assertTrue(spe.getSystemId().contains("INVALID_LICENSE_ID_ERROR"));
		}
	}

	@Test
	public void testDefaultPluginList() throws Exception {
		final String uri = "META-INF/qse/lin/qse-lin-plugins.xml";
		final URL url = Thread.currentThread().getContextClassLoader().getResource(uri);
		if (url == null) {
			throw new IllegalStateException("Unable to locate URI: " + uri);
		}
		final List<PluginInfo> plugins = new ArrayList<>();
		try (InputStream in = url.openStream()) {
			final InputSource isource = new InputSource(in);
			isource.setSystemId(url.toExternalForm());
			new QLinPluginsParser().parse(isource, (pi) -> plugins.add(pi));
		}
		assertEquals(42, plugins.size());
	}
	
	final List<PluginInfo> parse(String pXml, String pUri) throws SAXException {
		final List<PluginInfo> list = new ArrayList<>();
		final StringReader sr = new StringReader(pXml);
		final InputSource isource = new InputSource(sr);
		isource.setSystemId(pUri);
		try {
			new QLinPluginsParser().parse(isource, (pi) -> list.add(pi));
		} catch (UndeclaredThrowableException e) {
			final Throwable t = e.getCause();
			if (t != null  &&  t instanceof SAXException) {
				throw (SAXException) t;
			} else {
				throw e;
			}
		}
		return list;
	}
}
