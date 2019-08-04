package com.github.jochenw.qse.is.core;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Binder;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.Scopes;
import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.IssueConsumer;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Issue;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.api.PrintStreamLogger;
import com.github.jochenw.qse.is.core.scan.IWorkspaceScanner;
import com.github.jochenw.qse.is.core.scan.DefaultWorkspaceScanner;


public class ScannerTest {
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
	public void testPackagesDir() {
		final List<MyIssue> issues = new ArrayList<>();
		final Path path = Paths.get("src/test/resources/packages");
		assertTrue(Files.isDirectory(path));
		final PrintStreamLogger logger = new PrintStreamLogger(System.err);
		logger.setDebugEnabled(true);
		logger.setTraceEnabled(true);
		final Module module = new Module() {
			@Override
			public void configure(Binder pBinder) {
				final DefaultWorkspaceScanner.DefaultWSContext context = new DefaultWorkspaceScanner.DefaultWSContext(path);
				pBinder.bind(IWorkspaceScanner.class).to(DefaultWorkspaceScanner.class).in(Scopes.SINGLETON);
				pBinder.bind(IWorkspaceScanner.Context.class).toInstance(context);
			}
		};
		final Scanner scanner = Scanner.newInstance(null, logger, module);
		final IssueConsumer ic = (i) -> issues.add(new MyIssue(i));
		scanner.getWorkspace().addListener(ic);
		scanner.run();
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
		assertIssue(issues, Severity.WARN, "JwiScratch", "DependencyCheckingRule", ErrorCodes.DEPENDENCY_MISSING, "jwi.scratch.messageCatalog:serviceOverridingSeverity",  "The flow service jwi.scratch.messageCatalog:serviceOverridingSeverity in package JwiScratch invokes the service wx.log.pub:logMessageFromCatalog, but neither of the following packages is declared as a dependency: WxLog");
		assertIssue(issues, Severity.WARN, "JwiScratch", "DependencyCheckingRule", ErrorCodes.DEPENDENCY_MISSING, "jwi.scratch.missingDependencies:serviceUsingWxConfig",  "The flow service jwi.scratch.missingDependencies:serviceUsingWxConfig in package JwiScratch invokes the service wx.config.pub:getValue, but neither of the following packages is declared as a dependency: WxConfig");

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
		assertNull(unexpectedIssue);
		assertEquals(20, issues.size());
	}

	private void assertIssue(List<MyIssue> pIssues, Severity pSeverity, String pPackage, String pRule, String pErrorCode, String pPath, String pMessage) {
		for (Iterator<MyIssue> iter = pIssues.iterator();  iter.hasNext();  ) {
			final MyIssue issue = iter.next();
			if (pPackage.equals(issue.getPackage())
					&&  pErrorCode.equals(issue.getErrorCode())
					&&  pPath.equals(issue.getUri())
					&&  pRule.equals(issue.getRule())
					&&  pSeverity == issue.getSeverity()) {
				if (pMessage.equals(issue.getMessage())) {
					issue.expected = true;
					return;
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
}
