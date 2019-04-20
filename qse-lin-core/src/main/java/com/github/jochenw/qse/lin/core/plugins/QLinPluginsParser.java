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

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.LocatorImpl;

import com.github.jochenw.afw.core.util.Functions.FailableConsumer;
import com.github.jochenw.afw.core.util.Functions.FailableRunnable;
import com.github.jochenw.afw.core.util.Sax;
import com.github.jochenw.afw.core.util.Sax.AbstractContentHandler;
import com.github.jochenw.qse.lin.core.api.IPluginRegistry.PluginInfo;
import com.github.jochenw.qse.lin.core.api.IPluginRegistry.Type;

public class QLinPluginsParser {
	public static final String NS = "http://namespaces.github.com/jochenw/qse/lin/core/plugins/1.0.0";

	public static class LocatablePluginInfo extends PluginInfo {
		private final Locator locator;

		public LocatablePluginInfo(Locator pLocator, String pId, String pFamilyId, String pName, String pDescription, Type pType,
				String pClassName, Map<String, String> pProperties, boolean pOverriding) {
			super(pId, pFamilyId, pName, pDescription, pType, pClassName, pProperties, pOverriding);
			locator = pLocator;
		}
	}

	public static class Validator implements FailableRunnable<SAXException> {
		private final Locator locator;
		private final FailableConsumer<Locator,SAXException> validator;

		public Validator(Locator pLocator, FailableConsumer<Locator, SAXException> pValidator) {
			locator = pLocator;
			validator = pValidator;
		}

		public void run() throws SAXException {
			validator.accept(locator);
		}
	}
	
	public static class Handler extends AbstractContentHandler {
		private final Consumer<PluginInfo> pluginConsumer;
		private String id, familyId, className, name, description;
		private Map<String,String> properties;
		private Type type;
		private boolean override;
		private Locator pluginLocator;
		private final Map<Type,Map<String,LocatablePluginInfo>> plugins = new HashMap<>();
		private final List<Validator> validators = new ArrayList<>();

		public Handler(Consumer<PluginInfo> pConsumer) {
			pluginConsumer = pConsumer;
		}
		
		@Override
		public void startDocument() throws SAXException {
		}

		@Override
		public void endDocument() throws SAXException {
			for (Validator val : validators) {
				val.run();
			}
		}

		@Override
		public void startElement(String pUri, String pLocalName, String pQName, Attributes pAtts) throws SAXException {
			final int level = incLevel();
			if (!NS.equals(pUri)) {
				throw error("Expected namespace=" + NS + ", got " + pUri);
			}
			switch(level) {
			case 1:
				if (!"qse-lin-plugins".equals(pLocalName)) {
					throw error("Expected element=qse-lin-plugins, got " + pLocalName);
				}
				return;
			case 2:
				if (!"plugin".equals(pLocalName)) {
					throw error("Expected element=plugin, got " + pLocalName);
				}
				id = pAtts.getValue("id");
				if (id == null  ||  id.length() == 0) {
					throw error("Missing, or empty attribute: plugin/@id");
				}
				final String typeStr = pAtts.getValue("type");
				if (typeStr == null  ||  typeStr.length() == 0) {
					throw error("Missing, or empty attribute: plugin/@type");
				}
				try {
					type = Type.valueOf(typeStr);
				} catch (Throwable t) {
					throw error("Invalid value for attribute plugin/@type: " + typeStr);
				}
				name = pAtts.getValue("name");
				if ((name == null  ||  name.length() == 0)  &&  (type == Type.license   ||  type == Type.family)) {
					throw error("Missing, or empty attribute for plugin type " + type + ": plugin/@name");
				}
				final String overrideStr = pAtts.getValue("override");
				if (overrideStr == null  ||  overrideStr.length() == 0) {
					override = false;
				} else {
					override = Boolean.valueOf(overrideStr);
				}
				className = pAtts.getValue("class");
				familyId = pAtts.getValue("familyId");
				properties = new HashMap<>();
				pluginLocator = new LocatorImpl(getDocumentLocator());
				return;
			case 3:
				if ("description".equals(pLocalName)) {
					startTextElement((s) -> description = s);
				} else if ("property".equals(pLocalName)) {
					final String key = pAtts.getValue("name");
					if (key == null  ||  key.length() == 0) {
						throw error("Invalid value for attribute property/@name");
					}
					final String value = pAtts.getValue("value");
					startTextElement((s) -> {
						if ((s != null  &&  s.length() > 0)) {
							if (value != null  &&  value.length() > 0) {
								throw new IllegalStateException("The attribute property/@value, and embedded text within the property element are mutually exclusive.");
							} else {
								properties.put(key, s);
							}
						} else {
							properties.put(key, value);
						}
					});
				} else {
					throw error("Expected element=description, or element=property, got " + pLocalName);
				}
				return;
			}
		}

		@Override
		public void endElement(String pUri, String pLocalName, String pQName) throws SAXException {
			final int l = super.decLevel();
			switch(l) {
			case 1:
				if (!"qse-lin-plugins".equals(pLocalName)) {
					throw error("Expected element=qse-lin-plugins, got " + pLocalName);
				}
				return;
			case 2:
				if ("description".equals(pLocalName)) {
					// Nothing to do
				} else if ("plugin".equals(pLocalName)) {
					final LocatablePluginInfo pi = new LocatablePluginInfo(pluginLocator, id, familyId, name, description, type, className, properties, override);
					Map<String,LocatablePluginInfo> map = plugins.get(type);
					if (map == null) {
						map = new HashMap<>();
						plugins.put(type, map);
					}
					if (map.put(pi.getId(), pi) != null  &&  type != Type.matcher) {
						throw error("Duplicate id for plugin type=" + type + ": " + pi.getId(), pluginLocator);
					}
					switch(pi.getType()) {
					case license: {
						final String familyId = pi.getFamilyId();
						if (familyId == null  ||  familyId.length() == 0) {
							throw error("Missing, or empty, attribute for type=license: plugin/@familyId");
						}
						addValidator(pi, (loc) -> {
							if (!hasPlugin(Type.family, familyId)) {
								throw error("No plugin available with plugin/@type=family, and plugin/@id=" + familyId, loc);
							}
						});
						break;
					}
					case matcher: {
						final String licenseId = pi.getId();
						addValidator(pi, (loc) -> {
							if (!hasPlugin(Type.license, licenseId)) {
								throw error("No plugin available with plugin/@type=license, and plugin/@id=" + licenseId, loc);
							}
						});
						break;
					}
					default:
						break;
					}
					pluginConsumer.accept(pi);
					properties = null;
					description = null;
				}
			}
		}

		protected boolean hasPlugin(Type pType, String pId) {
			final Map<String,LocatablePluginInfo> map = plugins.get(pType);
			if (map == null) {
				return false;
			} else {
				return map.containsKey(pId);
			}
		}
		
		protected void addValidator(LocatablePluginInfo pPluginInfo, FailableConsumer<Locator,SAXException> pValidator) {
			validators.add(new Validator(pPluginInfo.locator, pValidator));
		}
	}

	public void parse(Path pPath, Consumer<PluginInfo> pConsumer) {
		final Handler h = new Handler(pConsumer);
		Sax.parse(pPath, h);
	}

	public void parse(InputSource pSource, Consumer<PluginInfo> pConsumer) {
		final Handler h = new Handler(pConsumer);
		Sax.parse(pSource, h);
	}

	public void parse(URL pUrl, Consumer<PluginInfo> pConsumer) {
		final Handler h = new Handler(pConsumer);
		Sax.parse(pUrl, h);
	}

}
