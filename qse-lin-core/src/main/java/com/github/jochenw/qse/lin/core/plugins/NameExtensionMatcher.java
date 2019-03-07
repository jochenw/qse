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

import com.github.jochenw.qse.lin.core.api.IQLinResource;

public class NameExtensionMatcher extends AbstractMatcher {
	private String[] extensions;
	private boolean caseSensitive = true;

	public String[] getExtensions() {
		return extensions;
	}

	public void setExtensions(String[] pExtensions) {
		if (caseSensitive) {
			extensions = pExtensions;
		} else {
			extensions = toLowerCase(pExtensions);
		}
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean pCaseSensitive) {
		caseSensitive = pCaseSensitive;
		if (!pCaseSensitive  &&  extensions != null) {
			extensions = toLowerCase(extensions);
		}
	}

	protected String[] toLowerCase(String[] pExtensions) {
		final String[] ext = new String[pExtensions.length];
		for (int i = 0;  i < ext.length;  i++) {
			ext[i] = pExtensions[i].toLowerCase();
		}
		return ext;
	}
	
	@Override
	public boolean test(IQLinResource pRes) {
		final String name = pRes.getName();
		for (String ext : extensions) {
			if (name.endsWith(ext)) {
				return true;
			}
		}
		return false;
	}
}
