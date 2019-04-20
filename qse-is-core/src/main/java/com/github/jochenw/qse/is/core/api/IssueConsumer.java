package com.github.jochenw.qse.is.core.api;

import javax.annotation.Nonnull;

public interface IssueConsumer {
	public enum Severity {
		ERROR, WARN, INFO, DEBUG, TRACE
	}
	public interface Issue {
		String getRule();
		String getPackage();
		String getUri();
		String getErrorCode();
		String getMessage();
		Severity getSeverity();
	}
	void accept(@Nonnull Issue pIssue);
}
