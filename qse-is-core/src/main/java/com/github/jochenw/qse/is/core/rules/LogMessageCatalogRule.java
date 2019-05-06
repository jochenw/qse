package com.github.jochenw.qse.is.core.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.annotation.Nonnull;

import org.xml.sax.ContentHandler;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.Finalizer;
import com.github.jochenw.qse.is.core.api.FlowConsumer;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.MessageCatalog;
import com.github.jochenw.qse.is.core.rules.InvocationValidatingServiceParser.Invocation;
import com.github.jochenw.qse.is.core.rules.RulesParser.Rule;
import com.github.jochenw.qse.is.core.sax.Sax;
import com.github.jochenw.qse.is.core.scan.PackageFileConsumer;


public class LogMessageCatalogRule extends AbstractRule {
	private String sourceService, targetService;
	private Severity severityUseSeverity;

	@Override
	public void init(@Nonnull Rule pParserRule) {
		super.init(pParserRule);
		sourceService = pParserRule.getProperty("sourceService");
		targetService = pParserRule.requireProperty("targetService");
		final String severityStr = pParserRule.getProperty("severitySpecificationSeverity");
		if (severityStr == null  ||  severityStr.length() == 0) {
			severityUseSeverity = null;
		} else {
			try {
				severityUseSeverity = Severity.valueOf(severityStr);
			} catch (Throwable t) {
				throw new IllegalArgumentException("Invalid value for property severitySpecificationSeverity: " + severityStr);
			}
		}
	}

	@Override
	protected void accept(@Nonnull IPluginRegistry pRegistry) {
		super.accept(pRegistry);
		pRegistry.addPlugin(FlowConsumer.class, new FlowConsumer() {
			@Override
			public ContentHandler getContentHandler(Context pContext) {
				return new InvocationValidatingServiceParser() {
					@Override
					protected void note(Invocation pInvocation) {
						LogMessageCatalogRule.this.note(pInvocation, pContext);
					}
				};
			}

			@Override
			public void accept(Context pContext) {
				// Nothing to do
			}
		});
		pRegistry.addPlugin(PackageFileConsumer.class, new PackageFileConsumer() {
			public void accept(PackageFileConsumer.Context pContext) {
				final String localPath = pContext.getLocalPath();
				if (localPath.endsWith("/config/log-messages.xml")) {
					final String expectedPath = pContext.getPackage().getName() + "/config/log-messages.xml";
					if (expectedPath.equals(localPath)) {
						final IsPackage pkg = pContext.getPackage();
						final String systemId = pkg.getName() + "/" + localPath;
						final LogMessageCatalogParser lmcp = new LogMessageCatalogParser();
						try (InputStream pIn = pContext.open()) {
							Sax.parse(pIn, systemId, lmcp);
							pkg.setMessageCatalog(lmcp.getMessageCatalog());
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					} else {
						throw new IllegalStateException("Unexpected log message file: " + localPath
								+ ", expected " + expectedPath);
					}
				}
			}
		});
	}

	protected void note(@Nonnull Invocation pInvocation, @Nonnull FlowConsumer.Context pContext) {
		if (sourceService.equals(pInvocation.getServiceName())  ||  targetService.equals(pInvocation.getServiceName())) {
			final IsPackage mypkg = pContext.getPackage();
			final String mypath = pContext.getNode().getName().getQName();
			if (severityUseSeverity != null  &&  pInvocation.getParameterValue("severity") != null) {
				getWorkspace().issue(this, mypkg, mypath, ErrorCodes.SEVERITY_EXPLICIT,
						severityUseSeverity, "Overriding a message severity is discouraged, because the severity from the message catalog should be used.");
			}
			final String componentKey = pInvocation.getParameterValue("componentKey");
			final String facilityKey = pInvocation.getParameterValue("facilityKey");
			final String messageKey = pInvocation.getParameterValue("messageKey");
			if (componentKey != null  &&  facilityKey != null  &&  messageKey != null) {
				final Finalizer finalizer = () -> {
					boolean found = false;
					for (IsPackage pkg :getWorkspace().getPackages()) {
						final MessageCatalog mc = pkg.getMessageCatalog();
						if (mc != null  &&  mc.hasMessage(componentKey, facilityKey, messageKey)) {
							found = true;
							break;
						}
					};
					if (!found) {
						issue(mypkg, mypath, ErrorCodes.LOG_MESSAGE_CATALOG_MISSING,
								"No such entry in the message catalog: " + componentKey
								+ "|" + facilityKey + "|" + messageKey);
					}
				};
				getScanner().add(finalizer);
			}
		}
	}
}
