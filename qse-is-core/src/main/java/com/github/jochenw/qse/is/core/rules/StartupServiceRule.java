package com.github.jochenw.qse.is.core.rules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.xml.sax.ContentHandler;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.Finalizer;
import com.github.jochenw.qse.is.core.api.NodeConsumer;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.rules.ManifestParser.Listener;
import com.github.jochenw.qse.is.core.rules.PackageScannerRule.ManifestConsumer;

public class StartupServiceRule extends AbstractRule {
	public static class LifecycleService {
		private final IsPackage isPackage;
		private final String serviceName;
		private final String uri;
		private boolean found;
		LifecycleService(IsPackage pPackage, String pServiceName, String pUri) {
			isPackage = pPackage;
			serviceName = pServiceName;
			uri = pUri;
		}
	}

	private List<LifecycleService> startupServices = new ArrayList<>();
	private List<LifecycleService> shutdownServices = new ArrayList<>();
	
	@Override
	protected void accept(IPluginRegistry pRegistry) {
		pRegistry.addPlugin(ManifestConsumer.class, new ManifestConsumer() {
			@Override
			public ContentHandler getContentHandler(
					com.github.jochenw.qse.is.core.scan.PackageFileConsumer.Context pContext) {
				final Listener listener = new Listener() {
					@Override
					public void startupService(String pService) {
						startupServices.add(new LifecycleService(pContext.getPackage(), pService, pContext.getLocalPath()));
					}

					@Override
					public void shutdownService(String pService) {
						shutdownServices.add(new LifecycleService(pContext.getPackage(), pService, pContext.getLocalPath()));
					}
					
				};
				return new ManifestParser(listener);
			}
			
		});
		pRegistry.addPlugin(NodeConsumer.class, new NodeConsumer() {
			@Override
			public void accept(Context pContext) {
				if (pContext.getNode().isService()) {
					final String packageName = pContext.getPackage().getName();
					final String serviceName = pContext.getNode().getName().getQName();
					final Consumer<LifecycleService> consumer = (lcs) -> {
						if (lcs.isPackage.getName().equals(packageName)  &&  lcs.serviceName.equals(serviceName)) {
							lcs.found = true;
						}
					};
					startupServices.forEach(consumer);
					shutdownServices.forEach(consumer);
				}
			}
		});
		pRegistry.addPlugin(Finalizer.class, new Finalizer() {
			@Override
			public void run() {
				for (LifecycleService lcs : startupServices) {
					if (!lcs.found) {
						getWorkspace().issue(StartupServiceRule.this, lcs.isPackage, lcs.uri, ErrorCodes.STARTUP_SERVICE_UNKNOWN, getSeverity(),
								             "Startup service " + lcs.serviceName + " is not present in package " + lcs.isPackage.getName()); 
					}
				}
				for (LifecycleService lcs : shutdownServices) {
					if (!lcs.found) {
						getWorkspace().issue(StartupServiceRule.this, lcs.isPackage, lcs.uri, ErrorCodes.SHUTDOWN_SERVICE_UNKNOWN, getSeverity(),
								             "Shutdown service " + lcs.serviceName + " is not present in package " + lcs.isPackage.getName()); 
					}
				}
			}
		});
	}
	
}
