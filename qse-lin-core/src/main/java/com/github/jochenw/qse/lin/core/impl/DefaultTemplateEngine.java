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

import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import com.github.jochenw.afw.core.template.SimpleTemplateEngine;
import com.github.jochenw.qse.lin.core.api.ITemplateEngine;

public class DefaultTemplateEngine implements ITemplateEngine<Map<String,Object>> {
	@Override
	public Template<Map<String, Object>> getTemplate(Reader pReader) {
		final SimpleTemplateEngine ste = SimpleTemplateEngine.newInstance();
		final com.github.jochenw.afw.core.template.ITemplateEngine.Template<Map<String,Object>> templ = ste.getTemplate(pReader);
		return new Template<Map<String,Object>>() {
			@Override
			public void write(Map<String, Object> pModel, Writer pWriter) {
				templ.write(pModel, pWriter);
			}
		};
	}

}
