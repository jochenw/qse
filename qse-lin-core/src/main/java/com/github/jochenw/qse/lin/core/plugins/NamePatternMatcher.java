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

import com.github.jochenw.afw.core.io.DefaultMatcher;
import com.github.jochenw.qse.lin.core.api.IQLinResource;

public class NamePatternMatcher extends AbstractMatcher {
	private boolean caseSensitive;
	private String[] patterns;
	private DefaultMatcher[] matchers;

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean pCaseSensitive) {
		caseSensitive = pCaseSensitive;
	}



	public String[] getPatterns() {
		return patterns;
	}

	public void setPatterns(String[] pPatterns) {
		patterns = pPatterns;
	}

	@Override
	public void initialized() {
		if (patterns != null) {
			matchers = new DefaultMatcher[patterns.length];
			for (int i = 0;  i < patterns.length;  i++) {
				matchers[i] = new DefaultMatcher(patterns[i], caseSensitive);
			}
		}
	}

	public DefaultMatcher[] getMatchers() {
		return matchers;
	}

	public void setMatchers(DefaultMatcher[] pMatchers) {
		matchers = pMatchers;
	}



	@Override
	public boolean test(IQLinResource pRes) {
		if (matchers != null) {
			final String name;
			final String uri;
			if (caseSensitive) {
				name = pRes.getName();
				uri = pRes.getUri();
			} else {
				name = pRes.getName().toLowerCase();
				uri = pRes.getUri().toLowerCase();
			}
			for (int i = 0;  i < matchers.length;  i++) {
				if (matchers[i].test(uri)) {
					return true;
				}
				if (matchers[i].test(name)) {
					return true;
				}
			}
		}
		return false;
	}
}
