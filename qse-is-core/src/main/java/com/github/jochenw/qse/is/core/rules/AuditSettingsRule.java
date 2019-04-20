package com.github.jochenw.qse.is.core.rules;

import java.util.regex.Pattern;

import org.xml.sax.ContentHandler;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.api.NodeConsumer;
import com.github.jochenw.qse.is.core.api.NodeConsumer.Context;
import com.github.jochenw.qse.is.core.rules.RulesParser.Rule;
import com.github.jochenw.qse.is.core.util.PatternMatchedSelector;


public class AuditSettingsRule extends AbstractRule {
	private static class SeverityValue {
		private final String value;
		private final Severity severity;
		SeverityValue(String pValue, Severity pSeverity) {
			value = pValue;
			severity = pSeverity;
		}
	}
	private SeverityValue svEnableAuditing, svLogOn, svIncludePipeline;
	private PatternMatchedSelector selector;
	
	@Override
	public void init(Rule pParserRule) {
		super.init(pParserRule);
		svEnableAuditing = asSeverityValue(pParserRule.getProperty("expectedEnableAuditingValue"));
		svLogOn = asSeverityValue(pParserRule.getProperty("expectedLogOnValue"));
		svIncludePipeline = asSeverityValue(pParserRule.getProperty("expectedIncludePipelineValue"));
		final String[] includedServiceList = (String[]) pParserRule.requireProperty("includedServices");
		final String[] excludedServiceList = (String[]) pParserRule.requireProperty("excludedServices");
		selector = new PatternMatchedSelector(includedServiceList, excludedServiceList);
	}

	private SeverityValue asSeverityValue(String pValue) {
		if (pValue == null) {
			return null;
		}
		final int offset = pValue.indexOf(':');
		if (offset == -1) {
			return new SeverityValue(pValue, getSeverity());
		} else {
			final String sevString = pValue.substring(0, offset);
			Severity sev;
			try {
				sev = Severity.valueOf(sevString.toUpperCase());
			} catch (Throwable t) {
				throw new IllegalStateException("Invalid severity definition in property value: " + pValue);
			}
			return new SeverityValue(pValue.substring(offset+1), sev);
		}
	}
	
	@Override
	protected void accept(IPluginRegistry pRegistry) {
		super.accept(pRegistry);
		pRegistry.addPlugin(NodeConsumer.class, new NodeConsumer() {
			@Override
			public ContentHandler getContentHandler(Context pContext) {
				final AuditSettingsParser asp = new AuditSettingsParser() {
					@Override
					public void finished() {
						if (isIncluded(pContext)) {
							final String auditOptionValue = super.getAuditOption();
							if (!isValidEnableAuditing(auditOptionValue)) {
								issue(pContext.getPackage(), pContext.getNode().getName().getQName(), ErrorCodes.AUDIT_SETTTING_ENABLE,
										"Invalid value for Audit/Enable auditing: Expected " + svEnableAuditing.value + ", got " + auditOptionValue,
										svEnableAuditing.severity);
							}
							final String startExec = getStartExecution();
							final String stopExec = getStopExecution();
							final String onError = getOnError();
							if (svEnableAuditing != null) {
								if (!isValidLogOn(startExec, stopExec, onError)) {
									issue(pContext.getPackage(), pContext.getNode().getName().getQName(), ErrorCodes.AUDIT_SETTTING_LOG_ON,
											"Invalid value for Audit/Log On: Expected " + svLogOn.value + ", got " +  asLogOn(startExec, stopExec, onError),
											svLogOn.severity);
								}
							}
							final String includePipelineValue = getDocumentData();
							if (!isValidIncludePipelineValue(includePipelineValue)) {
								issue(pContext.getPackage(), pContext.getNode().getName().getQName(), ErrorCodes.AUDIT_SETTTING_INCLUDE_PIPELINE,
									  "Invalid value for Audit/Include pipeline: Expected " + svIncludePipeline.value + ", got " + includePipelineValue,
									  svIncludePipeline.severity);
							}
						}
					}
				};
				return asp;
			}

			@Override
			public void accept(Context pContext) {
				// Nothing to do
			}
		});
	}

	protected boolean isValidIncludePipelineValue(String pValue) {
		if (svIncludePipeline == null) {
			return true;
		}
		switch (svIncludePipeline.value) {
		  case "0":  //  Never
		  case "1":  //  On errors only
		  case "2":  //  Always
			return svIncludePipeline.value.equals(pValue);
	      default:
	        throw new IllegalStateException("Invalid value requested for 'Include pipeline' (Expected 0=Never, 1=On errors only, or 2=Always): "
                        + svEnableAuditing.value);
		}
	}
	
	protected boolean isValidEnableAuditing(String pValue) {
		if (svEnableAuditing == null) {
			return true;
		}
		switch (svEnableAuditing.value) {
		  case "0": // Never
		  case "1": // When top-level service only
		  case "2": // Always
            return svEnableAuditing.value.equals(pValue);
          default:
        	throw new IllegalStateException("Invalid value requested for 'Enable auditing' (Expected 0=Never, 1=When top-level service, or 2=Always): "
                                            + svEnableAuditing.value);
		}
	}

	protected boolean isValidLogOn(String pStartExecution, String pStopExecution, String pOnError) {
		if (svIncludePipeline == null) {
			return true;
		}
		if ("false".equals(pStartExecution)  &&  "false".equals(pStopExecution)  &&  "true".equals(pOnError)) {
			return svLogOn.value.equals("0");  //  Error only
		} else if ("false".equals(pStartExecution)  &&  "true".equals(pStopExecution)  &&  "true".equals(pOnError)) {
			return svLogOn.value.equals("1");  //  Error, and success
		} else if ("true".equals(pStartExecution)  &&  "true".equals(pStopExecution)  &&  "true".equals(pOnError)) {
			return svLogOn.value.equals("2");  //  Error, success and start
		} else {
        	throw new IllegalStateException("Invalid value requested for 'Include pipeline' (Expected 0=Error only, 1=Error, and success, or 2=Error, success, and start): "
                    + svLogOn.value);
		}
	}

	protected String asLogOn(String pStartExecution, String pStopExecution, String pOnError) {
		if ("false".equals(pStartExecution)  &&  "false".equals(pStopExecution)  &&  "true".equals(pOnError)) {
			return "0";  //  Error only
		} else if ("false".equals(pStartExecution)  &&  "true".equals(pStopExecution)  &&  "true".equals(pOnError)) {
			return "1";  //  Error, and success
		} else if ("true".equals(pStartExecution)  &&  "true".equals(pStopExecution)  &&  "true".equals(pOnError)) {
			return "2";  //  Error, success and start
		} else {
			return "-1";
		}
	}
	
	protected boolean isIncluded(Context pContext) {
		final String qName = pContext.getNode().getName().getQName();
		return selector.matches(qName);
	}

	protected boolean matches(String pQName, Pattern[] pPatterns) {
		if (pPatterns == null) {
			return true;
		}
		for (int i = 0;  i < pPatterns.length;  i++) {
			final Pattern pat = pPatterns[i];
			if (pat.matcher(pQName).matches()) {
				return true;
			}
		}
		return false;
	}
}
