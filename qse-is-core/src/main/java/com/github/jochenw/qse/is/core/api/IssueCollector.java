package com.github.jochenw.qse.is.core.api;

import java.util.ArrayList;
import java.util.List;

public class IssueCollector implements IssueConsumer {
	private final List<Issue> issues = new ArrayList<>();
	private int numErrors, numWarnings, numOtherIssues;

	@Override
	public void accept(Issue pIssue) {
		issues.add(new IssueBean(pIssue));
		switch(pIssue.getSeverity()) {
		  case ERROR:
			  ++numErrors;
			  break;
		  case WARN:
			  ++numWarnings;
			  break;
		  default:
			  ++numOtherIssues;
			  break;
		}
	}

	public int size() {
		return issues.size();
	}

	public List<Issue> getIssues() {
		return issues;
	}

	public int getNumErrors() {
		return numErrors;
	}

	public int getNumWarnings() {
		return numWarnings;
	}

	public int getNumOtherIssues() {
		return numOtherIssues;
	}
}
