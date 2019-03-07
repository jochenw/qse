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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.afw.core.util.Strings;

public class QLinConfiguration {
	public static class FileSet {
		private final String dir;
		private final String[] includes, excludes;
		private final boolean caseSensitive, scanningArchives;
		private final Charset charSet;

		public FileSet(String dir, String[] includes, String[] excludes, boolean caseSensitive,
				boolean scanningArchives, Charset charSet) {
			this.dir = Strings.requireNonEmpty(dir, "Directory");
			this.includes = includes;
			this.excludes = excludes;
			this.caseSensitive = caseSensitive;
			this.scanningArchives = scanningArchives;
			this.charSet = charSet;
		}

		public String getDir() {
			return dir;
		}
		public String[] getIncludes() {
			return includes;
		}
		public String[] getExcludes() {
			return excludes;
		}
		public boolean isCaseSensitive() {
			return caseSensitive;
		}
		public boolean isScanningArchives() {
			return scanningArchives;
		}
		public Charset getCharSet() {
			return charSet;
		}
	}
	public static final int DEFAULT_NUMBER_OF_THREADS = 5;
	public static final String[] DEFAULT_ARCHIVE_HANDLER_IDS = new String[] { "TAR", "ZIP" };
	public static final String[] DEFAULT_NOTICE_MATCHER_IDS = new String[] {"NOTC"};
	public static final String[] DEFAULT_BINARY_MATCHER_IDS = new String[] {"BIN"};
	public static final String[] DEFAULT_GENERATED_MATCHER_IDS = new String[] {"GEN"};
	public static final String DEFAULT_REPORT_TEMPLATE = "com/github/jochenw/qse/lin/core/impl/qse-lin-report-template.fm";
	public static final String DEFAULT_REPORT_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final Charset DEFAULT_REPORT_CHARSET = StandardCharsets.UTF_8;
	public static final Locale DEFAULT_REPORT_LOCALE = Locale.US;

	private int numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
	private String[] archiveHandlerIds = DEFAULT_ARCHIVE_HANDLER_IDS;
	private String[] noticeLicenseIds = DEFAULT_NOTICE_MATCHER_IDS;
	private String[] binaryLicenseIds = DEFAULT_BINARY_MATCHER_IDS;
	private String[] generatedLicenseIds = DEFAULT_GENERATED_MATCHER_IDS;
	private String[] defaultExcludes = new String[] {"all"};
	private String[] approvedLicenseIds;
	private String[] approvedLicenseFamilyIds;
	private final List<FileSet> fileSets = new ArrayList<>();
	private String reportTemplate = DEFAULT_REPORT_TEMPLATE;
	private String reportDateTimePattern = DEFAULT_REPORT_DATE_TIME_PATTERN;
	private Locale reportLocale = DEFAULT_REPORT_LOCALE;
	private Charset reportCharset = DEFAULT_REPORT_CHARSET;

	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	public void setNumberOfThreads(int pNumberOfThreads) {
		if (pNumberOfThreads < 0) {
			numberOfThreads = DEFAULT_NUMBER_OF_THREADS;
		} else {
			numberOfThreads = pNumberOfThreads;
		}
	}

	public String[] getNoticeLicenseIds() {
		return noticeLicenseIds;
	}

	public void setNoticeLicenseIds(String[] pIds) {
		noticeLicenseIds = Objects.notNull(pIds, DEFAULT_NOTICE_MATCHER_IDS);
	}


	public String[] getBinaryLicenseIds() {
		return binaryLicenseIds;
	}

	public void setBinaryLicenseIds(String[] pIds) {
		binaryLicenseIds = Objects.notNull(pIds, DEFAULT_BINARY_MATCHER_IDS);
	}

	public String[] getGeneratedLicenseIds() {
		return generatedLicenseIds;
	}

	public void setGeneratedLicenseIds(String[] pIds) {
		generatedLicenseIds = Objects.notNull(pIds, DEFAULT_GENERATED_MATCHER_IDS);
	}

	public String[] getApprovedLicenseIds() {
		return approvedLicenseIds;
	}

	public void setApprovedLicenseIds(String[] pIds) {
		approvedLicenseIds = pIds;
	}

	public String[] getApprovedLicenseFamilyIds() {
		return approvedLicenseFamilyIds;
	}

	public void setApprovedLicenseFamilyIds(String[] pIds) {
		approvedLicenseFamilyIds = pIds;
	}

	public String[] getArchiveHandlerIds() {
		return archiveHandlerIds;
	}

	public void setArchiveHandlerIds(String[] pIds) {
		archiveHandlerIds = Objects.notNull(pIds, DEFAULT_ARCHIVE_HANDLER_IDS);
	}

	public List<FileSet> getFileSets() {
		return fileSets;
	}
	
	public void addFileSet(String pDir, List<String> pIncludes, List<String> pExcludes, boolean pCaseSensitive,
			boolean pScanningArchives, Charset pCharset) {
		fileSets.add(new FileSet(pDir, asArray(pIncludes), asArray(pExcludes), pCaseSensitive, pScanningArchives, pCharset));
	}

	protected String[] asArray(List<String> pList) {
		if (pList == null  ||  pList.isEmpty()) {
			return null;
		} else {
			return pList.toArray(new String[pList.size()]);
		}
	}

	public String getReportTemplate() {
		return reportTemplate;
	}

	public void setReportTemplate(String pReportTemplate) {
		if (pReportTemplate == null) {
			reportTemplate = DEFAULT_REPORT_TEMPLATE;
		} else {
			reportTemplate = pReportTemplate;
		}
	}

	public Locale getReportLocale() {
		return reportLocale;
	}

	public void setReportLocale(Locale pReportLocale) {
		reportLocale = Objects.notNull(pReportLocale, DEFAULT_REPORT_LOCALE);
	}

	public Charset getReportCharset() {
		return reportCharset;
	}

	public void setReportCharset(Charset pCharset) {
		reportCharset = Objects.notNull(pCharset, DEFAULT_REPORT_CHARSET);
	}

	public String getReportDateTimePattern() {
		return reportDateTimePattern;
	}

	public void setReportDateTimePattern(String pPattern) {
		reportDateTimePattern = Strings.notEmpty(pPattern, DEFAULT_REPORT_DATE_TIME_PATTERN);
	}

	public String[] getDefaultExcludes() {
		return defaultExcludes;
	}

	public void setDefaultExcludes(String[] pExcludes) {
		if (pExcludes == null  ||  pExcludes.length == 0) {
			defaultExcludes = new String[] {"none"};
		} else {
			defaultExcludes = pExcludes;
		}
	}
}
