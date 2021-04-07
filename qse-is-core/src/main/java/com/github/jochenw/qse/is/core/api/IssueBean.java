package com.github.jochenw.qse.is.core.api;

import com.github.jochenw.qse.is.core.api.IssueConsumer.Issue;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;

public class IssueBean implements Issue {
	private final String rule, pkg, uri, errorCode, msg;
	private final Severity severity;

	public IssueBean(Issue pIssue) {
		rule = pIssue.getRule();
		pkg = pIssue.getPackage();
		uri = pIssue.getUri();
		errorCode = pIssue.getErrorCode();
		msg = pIssue.getMessage();
		severity = pIssue.getSeverity();
	}

	public IssueBean(String pRule, String pPackage, String pUri, String pErrorCode, String pMsg, Severity pSeverity) {
		rule = pRule;
		pkg = pPackage;
		uri = pUri;
		errorCode = pErrorCode;
		msg = pMsg;
		severity = pSeverity;
	}

	@Override
	public String getRule() {
		return rule;
	}

	@Override
	public String getPackage() {
		return pkg;
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
		return msg;
	}

	@Override
	public Severity getSeverity() {
		// TODO Auto-generated method stub
		return null;
	}

}
