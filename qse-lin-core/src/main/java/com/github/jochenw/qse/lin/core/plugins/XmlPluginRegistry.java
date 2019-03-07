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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;

import org.xml.sax.InputSource;

import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.qse.lin.core.api.IPluginRegistry;

public class XmlPluginRegistry implements IPluginRegistry {
	private final String uri;
	private final ClassLoader classLoader;
	private final List<PluginInfo> licenses = new ArrayList<>();
	private final List<PluginInfo> families = new ArrayList<>();
	private final List<PluginInfo> matchers = new ArrayList<>();
	private final List<PluginInfo> exclusionLists = new ArrayList<>();
	private final List<PluginInfo> archiveHandlers = new ArrayList<>();

	public XmlPluginRegistry(ClassLoader pClassLoader, String pUri) {
		classLoader = pClassLoader;
		uri = pUri;
	}

	public void init() {
		try {
			final Enumeration<URL> en = classLoader.getResources(uri);
			if (!en.hasMoreElements()) {
				throw new IllegalStateException("No plugin lists found.");
			}
			do {
				final URL url = en.nextElement();
				parse(url);
			} while (en.hasMoreElements());
		} catch (IOException e) {
			throw Exceptions.show(e);
		}
	}

	protected void add(PluginInfo pPluginInfo) {
		switch (pPluginInfo.getType()) {
		case license:
			licenses.add(pPluginInfo);
			break;
		case family:
			families.add(pPluginInfo);
			break;
		case archive:
			archiveHandlers.add(pPluginInfo);
			break;
		case exclusion:
			exclusionLists.add(pPluginInfo);
			break;
		case matcher:
			matchers.add(pPluginInfo);
			break;
		}
	}

	@Override
	public List<PluginInfo> getLicenses() {
		return licenses;
	}

	@Override
	public List<PluginInfo> getFamilies() {
		return families;
	}

	@Override
	public List<PluginInfo> getMatchers() {
		return matchers;
	}

	@Override
	public List<PluginInfo> getArchiveHandlers() {
		return archiveHandlers;
	}

	@Override
	public List<PluginInfo> getExclusionLists() {
		return exclusionLists;
	}

	public void parse(URL pURL) {
		final Consumer<PluginInfo> consumer = (pi) -> add(pi);
		try (InputStream in = pURL.openStream()) {
			final InputSource isource = new InputSource(in);
			isource.setSystemId(pURL.toExternalForm());
			new QLinPluginsParser().parse(isource, consumer);
		} catch (IOException e) {
			throw Exceptions.show(e);
		}
	}
	
}
