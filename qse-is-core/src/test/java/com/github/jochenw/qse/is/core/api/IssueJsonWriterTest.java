package com.github.jochenw.qse.is.core.api;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;

import org.junit.Test;

import com.github.jochenw.qse.is.core.api.IssueConsumer.Issue;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;

public class IssueJsonWriterTest {
	@Test
	public void testEmptyIssueList() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final IssueJsonWriter ijw = new IssueJsonWriter(() -> baos);
		ijw.close();
	    assertEquals("[]", baos.toString("UTF-8"));
	}

	@Test
	public void testSingleIssue() throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final IssueJsonWriter ijw = new IssueJsonWriter(() -> baos);
		final Issue issue = new Issue() {
			@Override
			public String getRule() {
				return "MyRule";
			}

			@Override
			public String getPackage() {
				return "MyPkg";
			}

			@Override
			public String getUri() {
				return "MyPkg/manifest.v3";
			}

			@Override
			public String getErrorCode() {
				return "EC.004.002";
			}

			@Override
			public String getMessage() {
				return "An error message";
			}

			@Override
			public Severity getSeverity() {
				return Severity.WARN;
			}
		};
		ijw.accept(issue);
		ijw.close();
	    assertEquals("[[\"MyPkg\",\"EC.004.002\",\"MyRule\",\"WARN\",\"MyPkg/manifest.v3\",\"An error message\"]]", baos.toString("UTF-8"));
	}

}
