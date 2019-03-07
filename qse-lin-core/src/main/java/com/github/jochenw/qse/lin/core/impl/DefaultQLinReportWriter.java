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
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import com.github.jochenw.qse.lin.core.api.ITemplateEngine;
import com.github.jochenw.qse.lin.core.api.ITemplateEngine.Template;
import com.github.jochenw.qse.lin.core.api.QLinReport;

public class DefaultQLinReportWriter extends AbstractReportWriter {
	private @Inject ITemplateEngine<Map<String,Object>> templateEngine;

	@Override
	public void write(QLinReport pReport, long pRuntimeMillis, Writer pWriter) throws IOException {
		final String uri = getConfiguration().getReportTemplate();
		final URL url = getClassLoader().getResource(uri);
		if (url == null) {
			throw new IllegalStateException("Unable to locate uri " + uri + " via ClassLoader " + getClassLoader());
		}
		try (InputStream in = url.openStream();
			 Reader r = new InputStreamReader(in, getConfiguration().getReportCharset())) {
			final Template<Map<String,Object>> template = templateEngine.getTemplate(r);
			final Map<String,Object> map = new HashMap<String,Object>();
			map.put("report", pReport);
			map.put("reportDateTime", getReportDateTime());
			map.put("reportGenerator", getReportGenerator());
			map.put("reportMillis", Long.valueOf(pRuntimeMillis));
			template.write(map, pWriter);
		}
	}

	protected String getReportGenerator() {
		return getClass().getName();
	}
}
