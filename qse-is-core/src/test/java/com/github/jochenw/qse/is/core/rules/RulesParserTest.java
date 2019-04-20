package com.github.jochenw.qse.is.core.rules;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.rules.RulesParser.Rule;


public class RulesParserTest {
	@Test
	public void testParseBuiltinRules() {
		final List<Rule> rules = new ArrayList<>();
		RulesParser.parseBuiltinRules((r) -> rules.add(r));
		assertEquals(5, rules.size());
		assertRule(rules.get(0), true, PipelineDebugRule.class, Severity.ERROR, "permittedValues", "0,1");
		assertRule(rules.get(1), true, ForbiddenServicesRule.class, Severity.ERROR, "serviceNames", new String[]{"pub.flow:debugLog"});
		assertRule(rules.get(2), true, ForbiddenServicesRule.class, Severity.WARN, "serviceNames", new String[] {"pub.list:appendToDocumentList", "pub.list:appendToStringList"});
		assertRule(rules.get(3), true, LogMessageCatalogRule.class, Severity.ERROR, "sourceService", "wx.log.pub:logMessageFromCatalogDev", "targetService", "wx.log.pub:logMessageFromCatalog", "severitySpecificationSeverity", "WARN");
		assertRule(rules.get(4), true, AuditSettingsRule.class, Severity.ERROR, "expectedEnableAuditingValue", "1", "expectedLogOnValue", "0", "expectedIncludePipelineValue", "1",
				"includedServices", new String[] {"^.*\\.pub(\\..*\\:|\\:).*$","^.*\\.ws\\.provider(\\..*\\:|\\:).*$","^.*\\:(_get|_post|_put|_delete)$"},
				"excludedServices", new String[0]);
	}

	private void assertRule(Rule pRule, boolean pEnabled, Class<?> pClass, Severity pSeverity, Object... pProperties) {
		assertEquals(pEnabled, pRule.isEnabled());
		assertEquals(pClass.getName(), pRule.getClassName());
		assertEquals(pSeverity, pRule.getSeverity());
		final Map<String, Object> properties = pRule.getProperties();
		if (pProperties == null  ||  pProperties.length == 0) {
			assertTrue(properties.isEmpty());
		} else {
			assertEquals(pProperties.length/2, properties.size());
			for (int i = 0;  i < pProperties.length;  i += 2) {
				final String name = (String) pProperties[i];
				final Object value = pProperties[i+1];
				if (value == null) {
					assertNull(properties.get(name));
				} else if (value instanceof String) {
					assertEquals(value, properties.get(name));
				} else if (value instanceof String[]) {
					assertArrayEquals((String[]) value, (String[]) properties.get(name));
				} else {
					fail("Invalid value type: " + value.getClass().getName());
				}
			}
		}
	}
}
