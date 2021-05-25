package com.github.jochenw.qse.is.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import org.junit.Test;

import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.IssueConsumer;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Issue;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.api.IssueJsonWriter;
import com.github.jochenw.qse.is.core.api.Logger;
import com.github.jochenw.qse.is.core.api.PrintStreamLogger;
import com.github.jochenw.qse.is.core.scan.NullWorkspaceScanner;
import com.github.jochenw.qse.is.core.scan.SonarWorkspaceScanner;
import com.github.jochenw.qse.is.core.scan.DefaultWorkspaceScanner;


public class ScannerTest {
	private final long seed = 1564926475992l; // System.currentTimeMillis() at the time, this test was written.
	private final Random random = new Random(seed);

	public static class MyIssue implements Issue {
		private boolean expected;
		private final Severity severity;
		private final String rule, packageName, uri, errorCode, message;
		MyIssue(Issue pIssue) {
			severity = pIssue.getSeverity();
			rule = pIssue.getRule();
			packageName = pIssue.getPackage();
			uri = pIssue.getUri();
			message = pIssue.getMessage();
			errorCode = pIssue.getErrorCode();
		}
		@Override
		public String getRule() {
			return rule;
		}
		@Override
		public String getPackage() {
			return packageName;
		}
		@Override
		public String getUri() {
			return uri;
		}
		@Override
		public String getErrorCode() {
			return errorCode;
		}
		@Override
		public String getMessage() {
			return message;
		}
		@Override
		public Severity getSeverity() {
			return severity;
		}
	}

	@Test
	public void testPackagesDirWithDefaultScanner() {
		final Path path = Paths.get("src/test/resources/packages");
		assertTrue(Files.isDirectory(path));
		final PrintStreamLogger logger = new PrintStreamLogger(System.err);
		logger.setDebugEnabled(true);
		logger.setTraceEnabled(true);
		final Scanner scanner = new ScannerBuilder(null).logger(logger).workspaceScanner(new DefaultWorkspaceScanner(path)).build();
		validate(scanner, null);
	}

	@Test
	public void testPackagesDirWithSonarScanner() {
		final Path path = Paths.get("src/test/resources/packages");
		assertTrue(Files.isDirectory(path));
		final List<File> allFiles = findFilesForSonarScanner(path);
		final PrintStreamLogger logger = new PrintStreamLogger(System.err);
		logger.setDebugEnabled(true);
		logger.setTraceEnabled(true);
		final Scanner scanner = new ScannerBuilder(null).logger(logger).workspaceScanner(new SonarWorkspaceScanner(() -> allFiles)).build();
		validate(scanner, null);
	}
	
	@Test
	public void testHtmlGeneration() throws Exception {
		final Path path = Paths.get("src/test/resources/packages");
		assertTrue(Files.isDirectory(path));
		final PrintStreamLogger logger = new PrintStreamLogger(System.err);
		logger.setDebugEnabled(true);
		logger.setTraceEnabled(true);
		final Scanner scanner = new ScannerBuilder(null).logger(logger).workspaceScanner(new DefaultWorkspaceScanner(path)).build();
		final Path outputFile = Paths.get("target/unit-tests/htmlGeneration/output.json");
		final Path outputDir = outputFile.getParent();
		if (outputDir != null) {
			Files.createDirectories(outputDir);
		}
		try (OutputStream os = Files.newOutputStream(outputFile);
			 IssueJsonWriter ijw = new IssueJsonWriter(() -> os)) {
			scanner.getWorkspace().addListener(ijw);
			scanner.run();
		}
	}

	@Test
	public void testTemplateFile() throws Exception {
		final Path path = Paths.get("src/test/resources/packages");
		assertTrue(Files.isDirectory(path));
		final PrintStreamLogger logger = new PrintStreamLogger(System.err);
		logger.setDebugEnabled(true);
		logger.setTraceEnabled(true);
		final Path outputFile = Paths.get("target/unit-tests/templates/output.html");
		final Scanner scanner = new ScannerBuilder(null)
				                .logger(logger)
				                .outputFile(outputFile)
				                .templateFile("default:result-table.html")
				                .templateTitle("Test scan result")
				                .workspaceScanner(new DefaultWorkspaceScanner(path))
				                .build();
		final Path outputDir = outputFile.getParent();
		if (outputDir != null) {
			Files.createDirectories(outputDir);
		}
		try (OutputStream os = Files.newOutputStream(outputFile);
			 IssueJsonWriter ijw = new IssueJsonWriter(() -> os)) {
			scanner.getWorkspace().addListener(ijw);
			scanner.run();
		}
	}
	@Test
	public void testTemplateFileViaMain() throws Exception {
		Main.main(new String[]{"-scanDir", "src/test/resources/packages",
				  			   "-outFile", "target/unit-tests/templates/result-table.html",
				  			   "-templateFile", "default:result-table.html",
				  			   "-templateTitle", "Test scan result",
				  			   "-rulesFile", "src/main/resources/com/github/jochenw/qse/is/core/rules.xml"});
	}

	private List<File> findFilesForSonarScanner(final Path path) {
		final List<File> files = com.github.jochenw.qse.is.core.util.Files.findFiles(path);
		final List<File> randomFiles = new ArrayList<>(files.size());
		while (!files.isEmpty()) {
			final int index = random.nextInt(files.size());
			randomFiles.add(files.remove(index));
		}
		return randomFiles;
	}

	private void validate(Scanner pScanner, Consumer<List<MyIssue>> pIssueConsumer) {
		final List<MyIssue> issues = new ArrayList<>();
		final IssueConsumer ic = (i) -> {
			System.out.println("Issue: package=" + i.getPackage()
			                   + ", severity=" + i.getSeverity()
			                   + ", rule=" + i.getRule()
			                   + ", errorCode=" + i.getErrorCode()
			                   + ", message=" + i.getMessage());
			issues.add(new MyIssue(i));
		};
		pScanner.getWorkspace().addListener(ic);
		pScanner.run();
		assertIssue(issues, Severity.ERROR, "JwiScratch", "PipelineDebugRule", ErrorCodes.PIPELINE_DEBUG_USE, "jwi.scratch.pipelineDebug:pipelineDebugSave", "A flow service must have Pipeline debug=None");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "DebugLogRule", ErrorCodes.FORBIDDEN_SVC, "jwi.scratch.forbiddenServices:serviceUsingDebugLog", "Use of forbidden service: pub.flow:debugLog");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "DebugLogRule", ErrorCodes.FORBIDDEN_SVC, "jwi.scratch.forbiddenServices:serviceUsingDebugLogInTransformer", "Use of forbidden service: pub.flow:debugLog");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "DebugLogRule", ErrorCodes.FORBIDDEN_SVC, "jwi.scratch.forbiddenServices:serviceUsingDebugLogTwice", "Use of forbidden service: pub.flow:debugLog");
		assertIssue(issues, Severity.WARN, "JwiScratch", "AppendToListRule", ErrorCodes.FORBIDDEN_SVC, "jwi.scratch.forbiddenServices:serviceUsingAppendToDocumentList", "Use of forbidden service: pub.list:appendToDocumentList");
		assertIssue(issues, Severity.WARN, "JwiScratch", "AppendToListRule", ErrorCodes.FORBIDDEN_SVC, "jwi.scratch.forbiddenServices:serviceUsingAppendToStringList", "Use of forbidden service: pub.list:appendToStringList");
		assertIssue(issues, Severity.WARN, "JwiScratch", "AppendToListRule", ErrorCodes.FORBIDDEN_SVC, "jwi.scratch.forbiddenServices:serviceUsingBothAppendToServices", "Use of forbidden service: pub.list:appendToStringList");
		assertIssue(issues, Severity.WARN, "JwiScratch", "AppendToListRule", ErrorCodes.FORBIDDEN_SVC, "jwi.scratch.forbiddenServices:serviceUsingBothAppendToServices", "Use of forbidden service: pub.list:appendToDocumentList");
		assertIssue(issues, Severity.WARN, "JwiScratch", "LogMessageCatalogRule", ErrorCodes.SEVERITY_EXPLICIT, "jwi.scratch.messageCatalog:serviceOverridingSeverity", "Overriding a message severity is discouraged, because the severity from the message catalog should be used.");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "LogMessageCatalogRule", ErrorCodes.LOG_MESSAGE_CATALOG_MISSING, "jwi.scratch.messageCatalog:serviceUsingLogMessageFromCatalogDev", "No such entry in the message catalog: NEWFOO|110|0003");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "LogMessageCatalogRule", ErrorCodes.LOG_MESSAGE_CATALOG_MISSING, "jwi.scratch.messageCatalog:serviceOverridingSeverity", "No such entry in the message catalog: ONE|002|0003");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "LogMessageCatalogRule", ErrorCodes.LOG_MESSAGE_CATALOG_MISSING, "jwi.scratch.messageCatalog:serviceUsingUnknownLogMessage", "No such entry in the message catalog: FOO|100|0001");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "LogMessageCatalogRule", ErrorCodes.LOG_MESSAGE_CATALOG_MISSING, "jwi.scratch.messageCatalog:serviceUsingUnknownLogMessage", "No such entry in the message catalog: FOO|100|0002");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "LogMessageCatalogRule", ErrorCodes.LOG_MESSAGE_CATALOG_MISSING, "jwi.scratch.messageCatalog:serviceUsingUnknownLogMessage", "No such entry in the message catalog: BAR|100|0001");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "AuditSettingsRule", ErrorCodes.AUDIT_SETTTING_ENABLE, "jwi.scratch.auditSettings.pub:pubServiceFail", "Invalid value for Audit/Enable auditing: Expected 1, got 0");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "AuditSettingsRule", ErrorCodes.AUDIT_SETTTING_INCLUDE_PIPELINE, "jwi.scratch.auditSettings.restServices:_post", "Invalid value for Audit/Include pipeline: Expected 1, got 2");
		assertIssue(issues, Severity.ERROR, "JwiScratch", "AuditSettingsRule", ErrorCodes.AUDIT_SETTTING_LOG_ON, "jwi.scratch.auditSettings.ws.provider:wsServiceFail", "Invalid value for Audit/Log On: Expected 0, got 1");
		assertIssue(issues, Severity.ERROR, "StartupServicePackage", "StartupServiceRule", ErrorCodes.STARTUP_SERVICE_UNKNOWN, "StartupServicePackage/manifest.v3", "Startup service com.foo.mypkg.admin:startup is not present in package StartupServicePackage");
		assertIssue(issues, Severity.WARN, "JwiScratch", "DependencyCheckingRule", ErrorCodes.DEPENDENCY_MISSING,
				    "jwi.scratch.messageCatalog:serviceOverridingSeverity",
				    "The flow service(s) jwi.scratch.messageCatalog:serviceOverridingSeverity,"
				    + " jwi.scratch.messageCatalog:serviceUsingLogMessageFromCatalogDev,"
				    + " jwi.scratch.messageCatalog:serviceUsingUnknownLogMessage in package"
				    + " JwiScratch seem to be referencing either of the following packages,"
				    + " none of which is declared as a dependency: WxLog");
		assertIssue(issues, Severity.WARN, "JwiScratch", "DependencyCheckingRule", ErrorCodes.DEPENDENCY_MISSING,
				    "jwi.scratch.missingDependencies:serviceUsingWxConfig",
				    "The flow service(s) jwi.scratch.missingDependencies:serviceUsingWxConfig"
				    + " in package JwiScratch seem to be referencing either of the following packages,"
				    + " none of which is declared as a dependency: WxConfig");
		assertIssue(issues, Severity.WARN, "JwiScratch", "DisabledStepsRule", ErrorCodes.DISABLED_STEP,
				    "jwi.scratch.flowParserExample:service1",
				    "The flow service jwi.scratch.flowParserExample:service1 contains 1 disabled step(s).");
				    
		if (pIssueConsumer != null) {
			/* Some issues may vary, depending on the workspace scanner, because they depend on the order
			 * of events, which in turn depends on the order of files, that are being scanned.
			 * So we support checking for additional issues here.
			 */
			pIssueConsumer.accept(issues);
		}

		for (MyIssue issue : issues) {
			if (!issue.expected) {
				System.err.println("Unexpected issue: errorCode=" + issue.getErrorCode()
									+ ", package=" + issue.getPackage()
									+ ", rule=" + issue.getRule()
									+ ", severity=" + issue.getSeverity()
									+ ", path=" + issue.getUri()
									+ ", message=" + issue.getMessage());
			}
		}
		MyIssue unexpectedIssue = null;
		for (MyIssue issue:issues) {
			if (!issue.expected) {
				if (unexpectedIssue == null) {
					unexpectedIssue = issue;
				}
			}
		}
		if (unexpectedIssue != null) {
			assertNull("Unexpected issue: package=" + unexpectedIssue.getPackage()
			           + ", errorCode=" + unexpectedIssue.getErrorCode()
			           + ", rule=" + unexpectedIssue.getRule()
			           + ", severity=" + unexpectedIssue.getSeverity()
			           + ", path=" + unexpectedIssue.getUri()
			           + ", message=" + unexpectedIssue.getMessage(),
			           unexpectedIssue);
		}
		assertEquals(21, issues.size());
	}

	private void assertIssue(List<MyIssue> pIssues, Severity pSeverity, String pPackage, String pRule, String pErrorCode, String pPath, String pMessage) {
		for (Iterator<MyIssue> iter = pIssues.iterator();  iter.hasNext();  ) {
			final MyIssue issue = iter.next();
			if (pPackage.equals(issue.getPackage())
				&&  pErrorCode.equals(issue.getErrorCode())) {
				if (pPath.equals(issue.getUri())
					&&  pRule.equals(issue.getRule())
					&&  pSeverity == issue.getSeverity()) {
					if (pMessage.equals(issue.getMessage())) {
						issue.expected = true;
						return;
					}
				}
			}
		}
		showIssues(pIssues);
		fail("Expected issue not found: package=" + pPackage + ", errorCode=" + pErrorCode + ", rule=" + pRule
		     + ", path=" + pPath + ", severity=" + pSeverity + ", message=" + pMessage);
		
	}

	private void showIssues(List<MyIssue> pIssues) {
		for (MyIssue issue: pIssues) {
			System.err.println("Issue: package=" + issue.getPackage()
			                   + ", errorCode=" +issue.getErrorCode()
			                   + ", rule=" + issue.getRule()
			                   + ", severity=" + issue.getSeverity()
			                   + ", path=" + issue.getUri()
			                   + ", expected=" + issue.expected
			                   + ", message=" + issue.getMessage());
		}
	}

	@Test
	public void testNullWorkspaceScanner() {
		final Scanner scanner = new ScannerBuilder(null).logger(Logger.getNullLogger()).workspaceScanner(new NullWorkspaceScanner()).build();
		assertNotNull(scanner);
	}
}
