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

import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.jochenw.afw.core.util.Sax;
import com.github.jochenw.afw.core.util.Sax.AbstractContentHandler;
import com.github.jochenw.qse.lin.core.api.QLinConfiguration;


public class QLinConfigurationParser {
	public static final String NS = "http://github.namespaces.com/jochenw/qse/lin/configuration/1.0.0";

	public static class Handler extends AbstractContentHandler {
		private QLinConfiguration config = new QLinConfiguration();
		private QLinConfiguration parsedConfig;
		private boolean inMain, inFileSets;
		private final List<String> includes = new ArrayList<>();
		private final List<String> excludes = new ArrayList<>();
		private String dir;
		private boolean caseSensitive, scanningArchives;
		private Charset charset;

		public Handler() {
		}
		
		@Override
		public void startDocument() throws SAXException {
			config = new QLinConfiguration();
			parsedConfig = null;
		}

		@Override
		public void endDocument() throws SAXException {
			if (config == null) {
				throw error("Expected startDocument() event");
			}
			parsedConfig = config;
			config = null;
		}

		@Override
		public void startElement(String pUri, String pLocalName, String pQName, Attributes pAtts) throws SAXException {
			final int l = super.incLevel();
			if (!NS.equals(pUri)) {
				throw error("Expected namespace=" + NS + ", got " + pUri);
			}
			switch (l) {
			case 1:
				if (!"qse-lin-configuration".equals(pLocalName)) {
					throw error("Expected element=qse-lin-configuration, got " + pLocalName);
				}
				return;
			case 2:
				if ("main".equals(pLocalName)) {
					inMain = true;
					return;
				} else if ("fileSets".equals(pLocalName)) {
					inFileSets = true;
					return;
				} else {
					throw error("Expected element=main, or element=fileSets, got " + pLocalName);
				}
			case 3:
				if (inMain) {
					switch (pLocalName) {
					case "numberOfThreads":
						startTextElement((s) -> {
							final int i;
							try {
								i = Integer.valueOf(s);
							} catch (Throwable t) {
								throw error("Invalid value for numberOfThreads, expected integer value, got " + s);
							}
							config.setNumberOfThreads(i);
						});
					case "approvedLicenseIds":
						startTextElement((s) -> config.setApprovedLicenseIds(asStringArray(s)));
						break;
					case "approvedLicenseFamilyIds":
						startTextElement((s) -> config.setApprovedLicenseFamilyIds(asStringArray(s)));
						break;

					case "noticeLicenseIds":
						startTextElement((s) -> config.setNoticeLicenseIds(asStringArray(s)));
						break;
					case "binaryLicenseIds":
						startTextElement((s) -> config.setBinaryLicenseIds(asStringArray(s)));
						break;
					case "generatedLicenseIds":
						startTextElement((s) -> config.setGeneratedLicenseIds(asStringArray(s)));
						break;
					case "reportTemplate":
						startTextElement((s) -> config.setReportTemplate(s));
						break;
					case "reportDateTimePattern":
						startTextElement((s) -> config.setReportDateTimePattern(s));
						break;
					case "reportLocale":
						startTextElement((s) -> {
							Locale locale;
							try {
								locale = Locale.forLanguageTag(s);
							} catch (Throwable t) {
								locale = null;
							}
							if (locale == null) {
								throw error("Invalid value for reportLocale: " + s);
							}
							config.setReportLocale(locale);
						});
						break;
					case "reportCharset":
						startTextElement((s) -> {
							Charset cs;
							try {
								cs = Charset.forName(s);
							} catch (Throwable t) {
								cs = null;
							}
							if (cs == null) {
								throw error("Invalid value for reportCharset: " + s);
							}
							config.setReportCharset(cs);
						});
					default:
						throw error("Expected element=approvedLicenseIds|approvedLicenseFamilyIds|noticeLicenseIds|binaryLicenseIds|generatedLicenseIds, got " + pLocalName);
					}
					return;
				} else if (inFileSets) {
					if ("fileSet".equals(pLocalName)) {
						includes.clear();
						excludes.clear();
						dir = pAtts.getValue("dir");
						if (dir == null  ||  dir.length() == 0) {
							throw error("Missing, or empty attribute: fileSet/@dir");
						}
						final String caseSensitiveStr = pAtts.getValue("caseSensitive");
						if (caseSensitiveStr == null) {
							caseSensitive = true;
						} else {
							caseSensitive = Boolean.valueOf(caseSensitiveStr);
						}
						final String scanningArchivesStr = pAtts.getValue("scanningArchives");
						if (scanningArchivesStr == null) {
							scanningArchives = true;
						} else {
							scanningArchives = Boolean.valueOf(scanningArchivesStr);
						}
						final String charSetStr = pAtts.getValue("charset");
						if (charSetStr == null  ||  charSetStr.length() == 0) {
							charset = StandardCharsets.UTF_8;
						} else {
							try {
								charset = Charset.forName(charSetStr);
							} catch (Throwable t) {
								throw error("Invalid character set name: " + charSetStr);
							}
						}
						return;
					} else {
						throw error("Expected element=fileSet, got " + pLocalName);
					}
				}
				break;
			case 4:
				if (dir != null) {
					if ("include".equals(pLocalName)) {
						startTextElement((s) -> includes.add(s));
						return;
					} else if ("exclude".equals(pLocalName)) {
						startTextElement((s) -> excludes.add(s));
					}
				}
				break;
			case 5:
				if (dir != null) {
					if ("include".equals(pLocalName)) {
						startTextElement((s) -> includes.add(s));
						return;
					} else if ("exclude".equals(pLocalName)) {
						startTextElement((s) -> excludes.add(s));
						return;
					}
				}
				break;
			}
			throw error("Unexpected element=" + pLocalName + " at level=" + l);
		}

		protected String[] asStringArray(String pValue) {
			final List<String> list = new ArrayList<>();
			for (StringTokenizer st = new StringTokenizer(pValue, " ");  st.hasMoreTokens();  ) {
				list.add(st.nextToken().trim());
			}
			return list.toArray(new String[list.size()]);
		}

		@Override
		public void endElement(String pUri, String pLocalName, String pQName) throws SAXException {
			final int l = decLevel();
			switch (l) {
			case 1:
				if (!"qse-lin-configuration".equals(pLocalName)) {
					throw error("Expected element=qse-lin-configuration, got " + pLocalName);
				}
				break;
			case 2:
				if (inMain) {
					if ("main".equals(pLocalName)) {
						inMain = false;
						return;
					}
				} else if (inFileSets) {
					if ("fileSets".equals(pLocalName)) {
						inFileSets = false;
						return;
					}
				}
				break;
			case 3:
				if (inFileSets) {
					if ("fileSet".equals(pLocalName)) {
						config.addFileSet(dir, includes, excludes, caseSensitive, scanningArchives, charset);
						dir = null;
						includes.clear();
						excludes.clear();
						charset = null;
					}
				}
				break;
			}
		}

		public QLinConfiguration getConfiguration() {
			if (parsedConfig == null) {
				throw new IllegalStateException("Parsing not yet done.");
			}
			return parsedConfig;
		}
	}

	public QLinConfiguration parse(Path pPath) {
		final Handler h = new Handler();
		Sax.parse(pPath, h);
		return h.getConfiguration();
	}

	public QLinConfiguration parse(URL pUrl) {
		final Handler h = new Handler();
		Sax.parse(pUrl, h);
		return h.getConfiguration();
	}

}
