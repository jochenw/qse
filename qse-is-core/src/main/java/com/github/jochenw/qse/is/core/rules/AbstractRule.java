package com.github.jochenw.qse.is.core.rules;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.api.Rule;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.IsWorkspace;


public class AbstractRule implements Rule {
	private String id;
	private Severity severity;
	private Scanner scanner;
	private Map<String,Object> properties;
	private RulesParser.Rule parserRule;

	public void init(@Nonnull RulesParser.Rule pParserRule) {
		if (pParserRule.getId() == null) {
			String className = pParserRule.getClassName();
			final int dollarOffset = className.lastIndexOf('$');
			if (dollarOffset == -1) {
				final int dotOffset = className.lastIndexOf('.');
				if (dotOffset == -1) {
					id = className;
				} else {
					id = className.substring(dotOffset+1);
				}
			} else {
				id = className.substring(dollarOffset+1);
			}
		} else {
			id = pParserRule.getId();
		}
		parserRule = pParserRule;
		severity = pParserRule.getSeverity();
		properties = pParserRule.getProperties();
	}


	protected void accept(@Nonnull IPluginRegistry pRegistry) {
	}

	@Override
	public void accept(@Nonnull Scanner pScanner) {
		scanner = pScanner;
		accept(pScanner.getPluginRegistry());
	}

	public Scanner getScanner() {
		return scanner;
	}

	public Severity getSeverity() {
		return severity;
	}

	public Object getProperty(@Nonnull String pKey) {
		return properties.get(pKey);
	}

	public String getId() {
		return id;
	}

	public RulesParser.Rule getParserRule() {
		return parserRule;
	}

	public IsWorkspace getWorkspace() {
		return getScanner().getWorkspace();
	}

	public IPluginRegistry getPluginRegistry() {
		return getScanner().getPluginRegistry();
	}

	protected void issue(@Nonnull IsPackage pPackage, @Nonnull String pUri, @Nonnull String pErrorCode, @Nonnull String pDetails) {
		issue(pPackage, pUri, pErrorCode, pDetails, getSeverity());
	}

	protected void issue(@Nonnull IsPackage pPackage, @Nonnull String pUri, @Nonnull String pErrorCode, @Nonnull String pDetails, @Nonnull Severity pSeverity) {
		getWorkspace().issue(this, pPackage, pUri, pErrorCode, pSeverity, pDetails);
	}

	public static class ServiceListDescription {
		private final String firstService, serviceNameListDescription;
		public ServiceListDescription(String pFirstService, String pServiceNameListDescription) {
			firstService = pFirstService;
			serviceNameListDescription = pServiceNameListDescription;
		}
		public String getFirstService() {
			return firstService;
		}
		public String getServiceNameListDescription() {
			return serviceNameListDescription;
		}
	}
	protected ServiceListDescription getServiceListDescription(Collection<String> pServiceNames) {
		final List<String> list = new ArrayList<>(pServiceNames);
		Collections.sort(list, Collator.getInstance(Locale.US));
		if (list.isEmpty()) {
			throw new IllegalStateException("The service name list is empty.");
		} else {
			final String firstServiceName = list.get(0);
			if (list.size() == 1) {
				return new ServiceListDescription(firstServiceName, firstServiceName);
			} else {
				final StringBuilder sb = new StringBuilder();
				for (int i = 0;  i < Math.min(list.size(), 3);  i++) {
					if (i > 0) {
						sb.append(", ");
					}
					sb.append(list.get(i));
				}
				if (list.size() > 3) {
					sb.append(", and ");
					sb.append(list.size()-3);
					sb.append(" others");
				}
				return new ServiceListDescription(firstServiceName, sb.toString());
			}
		}
	}
}
