package com.github.jochenw.qse.is.core.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.github.jochenw.qse.is.core.api.IssueConsumer;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Issue;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.api.Rule;


public class IsWorkspace {
	private Map<String, IsPackage> packages = new HashMap<String,IsPackage>(); 
	private List<IssueConsumer> issueConsumers = new ArrayList<>();

	public IsPackage addPackage(@Nonnull String pPackageName, @Nonnull String pUri) {
		Objects.requireNonNull(pPackageName, "Package Name");
		Objects.requireNonNull(pUri, "URI");
		if (packages.containsKey(pPackageName)) {
			throw new IllegalStateException("Duplicate package name: " + pPackageName
					+ ", original URI=" + packages.get(pPackageName).getUri()
					+ ", duplicates URI=" + pUri);
		}
		final IsPackage pkg = new IsPackage(pPackageName, pUri);
		packages.put(pPackageName, pkg);
		return pkg;
	}

	public IsPackage getPackage(@Nonnull String pPackageName) {
		return packages.get(pPackageName);
	}

	public IsPackage requirePackage(@Nonnull String pPackageName) {
		final IsPackage pkg = getPackage(pPackageName);
		if (pkg == null) {
			throw new NoSuchElementException("No such package available: " + pPackageName);
		}
		return pkg;
	}

	public Collection<IsPackage> getPackages() {
		return packages.values();
	}

	public void issue(@Nonnull Rule pRule, @Nonnull IsPackage pPackage,
			          @Nonnull String pUri, @Nonnull String pErrorCode,
			          @Nonnull Severity pSeverity, @Nonnull String pDetails) {
		final String id = pRule.getId();
		final String packageName = pPackage.getName();
		final Issue issue = new Issue() {
			@Override
			public String getRule() {
				return id;
			}

			@Override
			public String getPackage() {
				return packageName;
			}

			@Override
			public String getUri() {
				return pUri;
			}

			@Override
			public String getErrorCode() {
				return pErrorCode;
			}

			@Override
			public String getMessage() {
				return pDetails;
			}

			@Override
			public Severity getSeverity() {
				return pSeverity;
			}
		};
		for (IssueConsumer ic : issueConsumers) {
			ic.accept(issue);
		}
	}

	public void addListener(@Nonnull IssueConsumer pListener) {
		issueConsumers.add(pListener);
	}

	public List<IssueConsumer> getIssueConsumers() {
		return issueConsumers;
	}
}
