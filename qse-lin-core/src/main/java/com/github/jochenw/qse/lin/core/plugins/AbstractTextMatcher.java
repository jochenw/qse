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
import com.github.jochenw.qse.lin.core.api.ITextMatcher;

public abstract class AbstractTextMatcher extends AbstractMatcher implements ITextMatcher {
	@Override
	public boolean test(IQLinResource pT) {
		throw new IllegalStateException("Not implemented, use matches(String)."); 
	}
}
