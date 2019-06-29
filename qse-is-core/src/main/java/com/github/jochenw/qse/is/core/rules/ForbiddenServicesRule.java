package com.github.jochenw.qse.is.core.rules;

import java.util.HashSet;
import java.util.Set;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.FlowConsumer;
import com.github.jochenw.qse.is.core.api.IServiceInvocationListener;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.NSName;
import com.github.jochenw.qse.is.core.model.Node;
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
		final Set<String> serviceNameSet = new HashSet<>();
		pluginRegistry.addPlugin(FlowConsumer.class, new FlowConsumer() {
			@Override
			public IServiceInvocationListener getServiceInvocationListener(FlowConsumer.Context pCtx) {
				serviceNameSet.clear();
				for (String s : serviceNames) {
					serviceNameSet.add(s);
				}
				return new IServiceInvocationListener() {
					@Override
					public void serviceInvocation(IsPackage pSourcePackage, NSName pSourceService, NSName pTargetService) {
						// Remove the service name from the forbidden set, so that the issue pops up only once per service.
						if (serviceNameSet.remove(pTargetService.getQName())) {
							issue(pSourcePackage, pSourceService.getQName(), ErrorCodes.FORBIDDEN_SVC,
									  "Use of forbidden service: " + pTargetService.getQName());
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
