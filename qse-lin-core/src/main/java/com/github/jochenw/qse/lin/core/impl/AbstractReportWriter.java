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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.afw.core.util.Streams;
import com.github.jochenw.qse.lin.core.api.IQLinReportWriter;
import com.github.jochenw.qse.lin.core.api.QLinConfiguration;


public abstract class AbstractReportWriter implements IQLinReportWriter {
	@Inject IComponentFactory componentFactory;
	@Inject QLinConfiguration configuration;
	@Inject ClassLoader classLoader;

	protected QLinConfiguration getConfiguration() {
		return configuration;
	}

	protected ClassLoader getClassLoader() {
		return classLoader;
	}

	protected IComponentFactory getComponentFactory() {
		return componentFactory;
	}

	protected URL getReportTemplate() {
		final String uri = getConfiguration().getReportTemplate();
		final URL url = classLoader.getResource(uri);
		if (url == null) {
			throw new IllegalStateException("Unable to locate URI: " + uri + " via ClassLoader " + classLoader);
		}
		return url;
	}

	protected String getReportTemplateText() {
		final URL url = getReportTemplate();
		try (InputStream in = url.openStream();
			 Reader r = new InputStreamReader(in, getConfiguration().getReportCharset())) {
			return Streams.read(r);
		} catch (IOException e) {
			throw Exceptions.show(e);
		}
	}

	protected String getReportDateTime() {
		final String reportDateTimePattern = getConfiguration().getReportDateTimePattern();
		ZonedDateTime zdt = getComponentFactory().getInstance(ZonedDateTime.class);
		if (zdt == null) {
			zdt = ZonedDateTime.now();
		}
		final DateTimeFormatter dtf = DateTimeFormatter.ofPattern(reportDateTimePattern);
		final String reportDateTimeStr = dtf.format(zdt);
		return reportDateTimeStr;
	}
}
