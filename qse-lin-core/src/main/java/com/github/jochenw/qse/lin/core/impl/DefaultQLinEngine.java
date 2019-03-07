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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.BiFunction;

import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.io.RestartableInputStream;
import com.github.jochenw.qse.lin.core.api.IArchiveHandler;
import com.github.jochenw.qse.lin.core.api.ILicense;
import com.github.jochenw.qse.lin.core.api.ILicenseFamily;
import com.github.jochenw.qse.lin.core.api.IMatcher;
import com.github.jochenw.qse.lin.core.api.IQLinEngine;
import com.github.jochenw.qse.lin.core.api.IQLinResource;
import com.github.jochenw.qse.lin.core.api.IQLinResourceSet;
import com.github.jochenw.qse.lin.core.api.ITextMatcher;
import com.github.jochenw.qse.lin.core.api.QLinConfiguration;

public class DefaultQLinEngine implements IQLinEngine {
	private @Inject IComponentFactory componentFactory;
	private @Inject QLinConfiguration configuration;
	private @Inject List<IArchiveHandler> archiveHandlers;
	private @Inject List<IMatcher> allMatchers;
	private @Inject @Named(value="notice") Set<String> noticeLicenseIds;
	private @Inject @Named(value="binary") Set<String> binaryMatcherIds;
	private @Inject @Named(value="generated") Set<String> generatedMatcherIds;
	private @Inject @Named(value="license") Set<String> approvedMatcherIds;
	private @Inject @Named(value="family") Set<String> approvedFamilyIds;
	private BiFunction<IQLinResource,Charset,IArchiveHandler> archiveMatcher;
	private BiFunction<IQLinResource,Charset,IMatcher> licenseMatcher;
	
	private Listener listener;
	private Executor executor;

	@PostConstruct
	public void init() {
		archiveMatcher = createMatcher(archiveHandlers);
		licenseMatcher = createMatcher(allMatchers);
	}

	protected <O extends IMatcher> BiFunction<IQLinResource,Charset,O> createMatcher(List<O> pList) {
		final List<ITextMatcher> textMatchers = new ArrayList<>();
		final List<O> otherMatchers = new ArrayList<>();

		for (O o : pList) {
			if (o instanceof ITextMatcher) {
				final ITextMatcher tm = (ITextMatcher) o;
				textMatchers.add(tm);
			} else {
				otherMatchers.add(o);
			}
		}
		return (res,cs) -> {
			final RestartableInputStream ris = new RestartableInputStream(() -> res.open());
			final IQLinResource resource = new IQLinResource() {
				@Override
				public String getName() {
					return res.getName();
				}

				@Override
				public String getUri() {
					return res.getUri();
				}

				@Override
				public String getQUri() {
					return res.getQUri();
				}

				@Override
				public InputStream open() throws IOException {
					return ris.open();
				}
			};
			if (!textMatchers.isEmpty()) {
				try (final InputStream in = ris.open();
					 final InputStreamReader isr = new InputStreamReader(in, cs);
					 final BufferedReader br = new BufferedReader(isr)) {
					for (;;) {
						final String line = br.readLine();
						if (line == null) {
							break;
						} else {
							for (ITextMatcher tm : textMatchers) {
								if (tm.matches(line)) {
									@SuppressWarnings("unchecked")
									final O o = (O) tm;
									return o;
								}
							}
						}
					}
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
			if (!otherMatchers.isEmpty()) {
				for (O o : otherMatchers) {
					if (o.test(resource)) {
						return o;
					}
				}
			}
			return null;
		};
	}
	protected void run(Runnable pRunnable) {
		if (executor == null) {
			pRunnable.run();
		} else {
			executor.execute(pRunnable);
		}
	}

	protected Executor newExecutor() {
		final int numberOfThreads = configuration.getNumberOfThreads();
		if (numberOfThreads > 1) {
			return Executors.newFixedThreadPool(numberOfThreads);
		} else {
			return null;
		}
	}

	protected void scan(IQLinResourceSet pResourceSet, IQLinResource pResource) {
		final IArchiveHandler ah = archiveMatcher.apply(pResource, pResourceSet.getCharset());
		if (ah != null) {
			listener.noteArchive(pResource, ah);
			return;
		}
		final IMatcher m = licenseMatcher.apply(pResource, pResourceSet.getCharset());
		if (m != null) {
			@SuppressWarnings("null")
			final @Nonnull String id = m.getId();
			final ILicense license = componentFactory.requireInstance(ILicense.class, id);
			@SuppressWarnings("null")
			final @Nonnull String familyId = license.getFamilyId();
			final ILicenseFamily family = componentFactory.requireInstance(ILicenseFamily.class, familyId);
			if (noticeLicenseIds.contains(id)) {
				listener.noteNotice(pResource, license, family, m);
				return;
			} else if (binaryMatcherIds.contains(id)) {
				listener.noteBinary(pResource, license, family, m);
				return;
			} else if (generatedMatcherIds.contains(id)) {
				listener.noteGenerated(pResource, license, family, m);
				return;
			} else if (approvedMatcherIds.contains(id)  ||  approvedFamilyIds.contains(familyId)) {
				listener.noteApproved(pResource, license, family, m);
				return;
			}
		}
		listener.noteUnknown(pResource);
	}

	protected void scan(IQLinResourceSet pResourceSet) {
		pResourceSet.iterate((r) -> scan(pResourceSet, r));
	}
	
	@Override
	public void scan(Listener pListener, Iterable<IQLinResourceSet> resourceSets) {
		listener = pListener;
		executor = newExecutor();
		for (IQLinResourceSet rs : resourceSets) {
			run(() -> scan(rs));
		}
	}
}
