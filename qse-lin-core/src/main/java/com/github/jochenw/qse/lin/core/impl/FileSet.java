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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import com.github.jochenw.afw.core.io.DefaultMatcher;
import com.github.jochenw.afw.core.io.DirectoryScanner;
import com.github.jochenw.afw.core.io.DirectoryScanner.Context;
import com.github.jochenw.afw.core.io.DirectoryScanner.Listener;
import com.github.jochenw.afw.core.io.IMatcher;
import com.github.jochenw.qse.lin.core.api.IQLinResource;
import com.github.jochenw.qse.lin.core.api.IQLinResourceSet;

public class FileSet implements IQLinResourceSet {
	private final @Nonnull Path dir;
	private final String[] includes, excludes;
	private final boolean caseSensitive;
	private final boolean scanningArchives;
	private final Charset charset;

	public FileSet(@Nonnull Path pDir, String[] pIncludes, String[] pExcludes, boolean pCaseSensitive,
			boolean pScanningArchives, Charset pCharset) {
		dir = pDir;
		includes = pIncludes;
		excludes = pExcludes;
		caseSensitive = pCaseSensitive;
		scanningArchives = pScanningArchives;
		charset = pCharset;
	}

	@Override
	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	@Override
	public boolean isScanningArchives() {
		return scanningArchives;
	}

	@Override
	public Charset getCharset() {
		return charset;
	}

	private static class MutableResource implements IQLinResource {
		private final String baseDir;
		String name, uri, qUri;
		Path file;

		MutableResource(String pBaseDir) {
			if (pBaseDir.endsWith("/")  ||  pBaseDir.endsWith("\\")) {
				baseDir = pBaseDir;
			} else {
				baseDir = pBaseDir + '/';
			}
		}
		void setContext(Context pContext) {
			file = pContext.getFile();
			name = file.getFileName().toString();
			uri = pContext.getUri();
			qUri = baseDir + uri;
		}
		@Override
		public String getName() {
			return name;
		}
		@Override
		public String getUri() {
			return uri;
		}
		@Override
		public String getQUri() {
			return qUri;
		}
		@Override
		public InputStream open() throws IOException {
			return Files.newInputStream(file);
		}
	}

	@Override
	public void iterate(Consumer<IQLinResource> pConsumer) {
		final MutableResource mr = new MutableResource(dir.toString());
		final IMatcher matcher = DefaultMatcher.newMatcher(includes, excludes, isCaseSensitive());
		final DirectoryScanner ds = new DirectoryScanner();
		ds.scan(dir, matcher, new Listener() {
			@Override
			public void accept(Context pContext) {
				mr.setContext(pContext);
				pConsumer.accept(mr);
			}
		});
	}

}
