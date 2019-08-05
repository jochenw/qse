package com.github.jochenw.qse.is.core.rules;

import java.util.function.Predicate;

import com.github.jochenw.qse.is.core.model.Node;
import com.github.jochenw.qse.is.core.rules.RulesParser.Rule;
import com.github.jochenw.qse.is.core.util.PatternMatchedSelector;

public class NodeFilteringRule extends AbstractRule {
	@Override
	public void init(Rule pParserRule) {
		super.init(pParserRule);
		final String attributeName = pParserRule.getProperty("attributeName");
		if (attributeName == null) {
			throw new NullPointerException("Missing property: attributeName");
		}
		final String[] includedServices = pParserRule.getProperty("includedServices");
		final String[] excludedServices = pParserRule.getProperty("excludedServices");
		final Predicate<Node> predicate;
		if (includedServices == null  &&  excludedServices == null) {
			predicate = NodeFilter.ACCEPT_ALL;
		} else {
			final PatternMatchedSelector selector = new PatternMatchedSelector(includedServices, excludedServices);
			predicate = (n) -> selector.matches(n.getName().getQName());
		}
		final NodeFilter nf = new NodeFilter() {
			@Override
			public boolean test(Node pNode) {
				return predicate.test(pNode);
			}

			@Override
			public String getId() {
				return attributeName;
			}
		};
		getPluginRegistry().addPlugin(NodeFilter.class, nf);
	}
	
}
