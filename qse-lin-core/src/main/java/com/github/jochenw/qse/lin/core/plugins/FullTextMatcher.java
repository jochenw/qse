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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FullTextMatcher extends AbstractTextMatcher {
	private String text1, text2, text3;

	protected class Pruner {
		private int offset;
		private String text;
		public Pruner(int pOffset, String pText) {
			offset = pOffset;
			text = pText;
		}
		public int matches(char pChar) {
			if (pChar == text.charAt(offset)) {
				if (++offset >= text.length()) {
					return 1;
				} else {
					return 0;
				}
			} else {
				return -1;
			}
		}
	}
	private List<Pruner> pruners = new ArrayList<Pruner>();
	private String copyright;
	
	@Override
	public boolean matches(String pLine) {
		for (int i = 0;  i < pLine.length();  i++) {
			final char c = pLine.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				for (Iterator<Pruner> it = pruners.iterator();  it.hasNext();  ) {
					final Pruner p = it.next();
					switch (p.matches(c)) {
					case 0:
						// Do nothing
						break;
					case 1:
						return true;
					case -1:
						it.remove();
					}
				}
				if (text1 != null) {
					if (c == text1.charAt(0)) {
						pruners.add(new Pruner(1, text1));
					}
				}
				if (text2 != null) {
					if (c == text2.charAt(0)) {
						pruners.add(new Pruner(1, text2));
					}
				}
				if (text3 != null) {
					if (c == text3.charAt(0)) {
						pruners.add(new Pruner(1, text3));
					}
				}
			}
		}
		return false;
	}

	public void setText1(String pText) {
		text1 = prune(pText);
	}
	
	public void setText2(String pText) {
		text2 = prune(pText);
	}

	public void setText3(String pText) {
		text3 = prune(pText);
	}
	

	protected String prune(String pIn) {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0;  i < pIn.length();  i++) {
			final char c = pIn.charAt(i);
			if (Character.isLetterOrDigit(c)) {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	public void setCopyright(String pCopyright) {
		copyright = pCopyright;
	}

	public String getCopyright() {
		return copyright;
	}
}
