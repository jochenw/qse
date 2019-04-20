package com.github.jochenw.qse.is.core.rules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.FlowConsumer;
import com.github.jochenw.qse.is.core.rules.RulesParser.Rule;


public class ForbiddenServicesRule extends AbstractRule {
	String[] serviceNames;

	@Override
	public void init(Rule pParserRule) {
		super.init(pParserRule);
		serviceNames = pParserRule.requireProperty("serviceNames");
	}

	@Override
	public void accept(Scanner pScanner) {
		super.accept(pScanner);
		final IPluginRegistry pluginRegistry = pScanner.getPluginRegistry();
		pluginRegistry.addPlugin(FlowConsumer.class, new FlowConsumer() {
			@Override
			public ContentHandler getContentHandler(final FlowConsumer.Context pNodeInfo) {
				return new ForbiddenServicesParser() {
					private Set<String> serviceNameSet = new HashSet<>();

					@Override
					public void startDocument() throws SAXException {
						super.startDocument();
						serviceNameSet.clear();
						serviceNameSet.addAll(Arrays.asList(serviceNames));
					}

					@Override
					protected boolean isForbiddenService(String pServiceName) {
						return serviceNameSet.remove(pServiceName);
					}

					@Override
					protected void noteForbiddenService(String pServiceName) {
						issue(pNodeInfo.getPackage(), pNodeInfo.getNode().getName().getQName(), ErrorCodes.FORBIDDEN_SVC,
							  "Use of forbidden service: " + pServiceName);
						if (serviceNameSet.isEmpty()) {
							terminate();
						}
					}
				};
			}

			@Override
			public void accept(FlowConsumer.Context pNodeInfo) {
			}
		});
	}
	
}
