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
package com.github.jochenw.qse.lin.core.api;

import com.github.jochenw.qse.lin.core.impl.QLinEngineBuilder;

public interface IQLinEngine {
	public interface Listener {
		void noteArchive(IQLinResource pResource, IArchiveHandler pHandler);
		void noteNotice(IQLinResource pResource, ILicense license, ILicenseFamily family, IMatcher m);
		void noteBinary(IQLinResource pResource, ILicense license, ILicenseFamily family, IMatcher m);
		void noteGenerated(IQLinResource pResource, ILicense license, ILicenseFamily family, IMatcher m);
		void noteApproved(IQLinResource pResource, ILicense license, ILicenseFamily family, IMatcher m);
		void noteUnknown(IQLinResource pResource);
	}
	public static IQLinEngineBuilder builder() {
		return new QLinEngineBuilder();
	}

	public void scan(Listener pListener, Iterable<IQLinResourceSet> resourceSets);
}
