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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class QLinReport implements IQLinEngine.Listener {
	public abstract static class Resource {
		private final String name, uri, qUri;
		protected Resource(String pName, String pUri, String pQUri) {
			name = pName;
			uri = pUri;
			qUri = Objects.requireNonNull(pQUri);
		}
		protected Resource(IQLinResource pResource) {
			this(pResource.getName(), pResource.getUri(), pResource.getQUri());
		}

		public String getName() { return name; }
		public String getUri() { return uri; }
		public String getQUri() { return qUri; }
	}
	public static class Archive extends Resource {
		private final String archiveHandlerId;
		public Archive(String pName, String pUri, String pQUri, String pArchiveHandlerId) {
			super(pName, pUri, pQUri);
			archiveHandlerId = pArchiveHandlerId;
		}
		public Archive(IQLinResource pResource, IArchiveHandler pArchiveHandler) {
			super(pResource);
			archiveHandlerId = pArchiveHandler.getId();
		}
		public String getArchiveHandlerId() {
			return archiveHandlerId;
		}
	}
	public abstract static class MatchedResource extends Resource {
		private final String licenseId, licenseFamilyId;
		private final String licenseName, licenseFamilyName;
		private final String licenseDescription, licenseFamilyDescription;
		private final String matcherId;

		protected MatchedResource(String pName, String pUri, String pQUri, String pLicenseId, String pLicenseFamilyId,
				                  String pLicenseName, String pLicenseFamilyName, String pLicenseDescription,
				                  String pLicenseFamilyDescription, String pMatcherId) {
			super(pName, pUri, pQUri);
			licenseId = pLicenseId;
			licenseFamilyId = pLicenseFamilyId;
			licenseName = pLicenseName;
			licenseFamilyName = pLicenseFamilyName;
			licenseDescription = pLicenseDescription;
			licenseFamilyDescription = pLicenseFamilyDescription;
			matcherId = pMatcherId;
		}
		protected MatchedResource(IQLinResource pResource, ILicense pLicense, ILicenseFamily pFamily, IMatcher pMatcher) {
			this(pResource.getName(), Objects.requireNonNull(pResource.getUri()), Objects.requireNonNull(pResource.getQUri()), pLicense.getId(), pFamily.getId(),
			     pLicense.getName(), pFamily.getName(), pLicense.getDescription(), pFamily.getDescription(),
			     pMatcher.getId());
		}
		public String getMatcherId() { return matcherId; }
		public String getLicenseId() { return licenseId; }
		public String getLicenseFamilyId() { return licenseFamilyId; }
		public String getLicenseName() { return licenseName; }
		public String getLicenseFamilyName() { return licenseFamilyName; }
		public String getLicenseDescription() { return licenseDescription; }
		public String getLicenseFamilyDescription() { return licenseFamilyDescription; }
	}
	public static class Notice extends MatchedResource {
		public Notice(IQLinResource pResource, ILicense pLicense, ILicenseFamily pFamily, IMatcher pMatcher) {
			super(pResource, pLicense, pFamily, pMatcher);
		}
	}
	public static class Binary extends MatchedResource {
		public Binary(IQLinResource pResource, ILicense pLicense, ILicenseFamily pFamily, IMatcher pMatcher) {
			super(pResource, pLicense, pFamily, pMatcher);
		}
	}
	public static class Generated extends MatchedResource {
		public Generated(IQLinResource pResource, ILicense pLicense, ILicenseFamily pFamily, IMatcher pMatcher) {
			super(pResource, pLicense, pFamily, pMatcher);
		}
	}
	public static class Approved extends MatchedResource {
		public Approved(IQLinResource pResource, ILicense pLicense, ILicenseFamily pFamily, IMatcher pMatcher) {
			super(pResource, pLicense, pFamily, pMatcher);
		}
	}
	public static class Unknown extends Resource {
		public Unknown(String pName, String pUri, String pQUri) {
			super(pName, pUri, pQUri);
		}
		public Unknown(IQLinResource pResource) {
			super(pResource);
		}
	}

	private final List<Resource> resources = new ArrayList<>();
	private final List<Archive> archives = new ArrayList<>();
	private final List<Notice> noticeFiles = new ArrayList<>();
	private final List<Binary> binaryFiles = new ArrayList<>();
	private final List<Generated> generatedFiles = new ArrayList<>();
	private final List<Approved> approvedFiles = new ArrayList<>();
	private final List<Unknown> unknownFiles = new ArrayList<>();

	@Override
	public void noteArchive(IQLinResource pResource, IArchiveHandler pHandler) {
		final Archive archive = new Archive(pResource, pHandler);
		synchronized (resources) {
			resources.add(archive);
			archives.add(archive);
		}
	}

	@Override
	public void noteNotice(IQLinResource pResource, ILicense pLicense, ILicenseFamily pFamily, IMatcher pMatcher) {
		final Notice notice = new Notice(pResource, pLicense, pFamily, pMatcher);
		synchronized (resources) {
			resources.add(notice);
			noticeFiles.add(notice);
		}
	}

	@Override
	public void noteBinary(IQLinResource pResource, ILicense pLicense, ILicenseFamily pFamily, IMatcher pMatcher) {
		final Binary binary = new Binary(pResource, pLicense, pFamily, pMatcher);
		synchronized (resources) {
			resources.add(binary);
			binaryFiles.add(binary);
		}
	}

	@Override
	public void noteGenerated(IQLinResource pResource, ILicense pLicense, ILicenseFamily pFamily, IMatcher pMatcher) {
		final Generated gen = new Generated(pResource, pLicense, pFamily, pMatcher);
		synchronized (resources) {
			resources.add(gen);
			generatedFiles.add(gen);
		}
	}

	@Override
	public void noteApproved(IQLinResource pResource, ILicense pLicense, ILicenseFamily pFamily, IMatcher pMatcher) {
		final Approved approved = new Approved(pResource, pLicense, pFamily, pMatcher);
		synchronized (resources) {
			resources.add(approved);
			approvedFiles.add(approved);
		}
	}

	@Override
	public void noteUnknown(IQLinResource pResource) {
		final Unknown unknown = new Unknown(pResource);
		synchronized (resources) {
			resources.add(unknown);
			unknownFiles.add(unknown);
		}
	}

	public int getNumberOfUnknownFiles() {
		return unknownFiles.size();
	}
	public int getNumberOfArchiveFiles() {
		return archives.size();
	}
	public int getNumberOfNoticeFiles() {
		return noticeFiles.size();
	}
	public int getNumberOfBinaryFiles() {
		return binaryFiles.size();
	}
	public int getNumberOfGeneratedFiles() {
		return generatedFiles.size();
	}
	public int getNumberOfApprovedFiles() {
		return approvedFiles.size();
	}

	public Archive[] getArchives() {
		return archives.toArray(new Archive[archives.size()]);
	}

	public Notice[] getNoticeFiles() {
		return noticeFiles.toArray(new Notice[noticeFiles.size()]);
	}

	public Binary[] getBinaryFiles() {
		return binaryFiles.toArray(new Binary[binaryFiles.size()]);
	}

	public Generated[] getGeneratedFiles() {
		return generatedFiles.toArray(new Generated[generatedFiles.size()]);
	}

	public Approved[] getApprovedFiles() {
		return approvedFiles.toArray(new Approved[approvedFiles.size()]);
	}

	public Unknown[] getUnknownFiles() {
		return unknownFiles.toArray(new Unknown[unknownFiles.size()]);
	}
}
