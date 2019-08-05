package com.github.jochenw.qse.is.core.rules;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.xml.sax.ContentHandler;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.qse.is.core.api.FlowConsumer;
import com.github.jochenw.qse.is.core.api.IServiceInvocationListener;
import com.github.jochenw.qse.is.core.api.NodeConsumer;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.NSName;
import com.github.jochenw.qse.is.core.model.Node;
import com.github.jochenw.qse.is.core.sax.Sax;
import com.github.jochenw.qse.is.core.scan.ContextImpl;
import com.github.jochenw.qse.is.core.scan.PackageFileConsumer;


public class PackageScannerRule extends AbstractRule implements PackageFileConsumer {
	public interface IsPackageListener {
		public void packageStarting(IsPackage pPackage);
		public void packageStopping();
	}
	@Override
	protected void accept(@Nonnull IPluginRegistry pRegistry) {
		pRegistry.addExtensionPoint(NodeConsumer.class);
		pRegistry.addExtensionPoint(FlowConsumer.class);
		pRegistry.addExtensionPoint(IsPackageListener.class);
		pRegistry.addPlugin(PackageFileConsumer.class, this);
	}

	private static final String nodeNdfSuffix = "/node.ndf";
	private static final String manifestFileName = "/manifest.v3";

	public void accept(@Nonnull PackageFileConsumer.Context pContext) {
		final ContextImpl ctx = (ContextImpl) pContext;
		final IsPackage pkg = pContext.getPackage();
		if (pkg != null) {
			String uri = pContext.getLocalPath();
			if (uri == null) {
			} else if (uri.endsWith(manifestFileName)) {
				final String expectedUri = pkg.getName() + manifestFileName;
				if (!expectedUri.equals(uri)) {
					throw new IllegalStateException("Unexpected manifest file: " + uri + ", expected " + expectedUri);
				}
			} else if (uri.endsWith(nodeNdfSuffix)) {
				final String nsPrefix = pContext.getPackage().getName() + "/ns/";
				if (uri.startsWith(nsPrefix)) {
					uri = uri.substring(nsPrefix.length());
				} else {
					throw new IllegalStateException("Expected node URI to start with " + nsPrefix + ", got " +uri);
				}
				final BasicNodeInfoParser bnip = new BasicNodeInfoParser();
				final List<ContentHandler> handlers = new ArrayList<ContentHandler>();
				handlers.add(bnip);
				getScanner().getPluginRegistry().forEach(NodeConsumer.class, (nc) -> {
					final ContentHandler ch = nc.getContentHandler(ctx);
					if (ch != null) {
						handlers.add(ch);
					}
				});
				final String serviceUri = uri.substring(0, uri.length()-nodeNdfSuffix.length());
				final NSName nsName = NSName.valueOf(serviceUri.replace('/', '.'));
				final Node node = new Node(pkg.getName(), nsName,
						bnip.getType(), bnip.getServiceType(), bnip.getSubType(),
						bnip.getComment());
				ctx.setNode(node);
				try (InputStream in = pContext.open()) {
					Sax.parseTerminable(in, pkg.getName() + "/ns/" + uri, handlers);
				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
				getPluginRegistry().forEach(NodeConsumer.class, (nc) -> nc.accept(ctx));
				if ("service".equals(bnip.getType())  &&  "flow".equals(bnip.getServiceType())) {
					final Path nodeNdfPath = ctx.getFile();
					final Path nodeNdfDir = nodeNdfPath.getParent();
					final Path flowXmlPath = nodeNdfDir == null ? Paths.get("flow.xml") : nodeNdfDir.resolve("flow.xml");
					ctx.setFlowFile(flowXmlPath);
					final String localNodePath = pContext.getLocalPath();
					final String localFlowPath = localNodePath.substring(0, localNodePath.length()-nodeNdfSuffix.length()) + "/flow.xml";
					ctx.setFlowLocalPath(localFlowPath);
					final List<IServiceInvocationListener> serviceInvocationListeners = new ArrayList<>();
					final List<ContentHandler> flowHandlers = new ArrayList<>();
					getPluginRegistry().forEach(FlowConsumer.class, (fc) -> {
						final IServiceInvocationListener listener = fc.getServiceInvocationListener(ctx);
						if (listener != null) {
							serviceInvocationListeners.add(listener);
						}
						final ContentHandler ch = fc.getContentHandler(ctx);
						if (ch != null) {
							flowHandlers.add(ch);
						}
					});
					if (!serviceInvocationListeners.isEmpty()) {
						final IsPackage sourcePackage = ctx.getPackage();
						final Node sourceNode = ctx.getNode();
						flowHandlers.add(new ServiceInvocationParser() {
							@Override
							protected void serviceInvocation(String pServiceName) {
								final NSName targetServiceName = NSName.valueOf(pServiceName);
								for (IServiceInvocationListener sic : serviceInvocationListeners) {
									sic.serviceInvocation(sourcePackage, sourceNode.getName(), targetServiceName);
								}
							}
						});
					}
					if (!flowHandlers.isEmpty()) {
						Sax.parseTerminable(flowXmlPath, flowHandlers);
					}
					getPluginRegistry().forEach(FlowConsumer.class, (fc) -> fc.accept(ctx));
				}
			}
		}
	}
}
