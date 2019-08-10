package com.github.jochenw.qse.is.core;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.xml.sax.InputSource;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.simple.SimpleComponentFactoryBuilder;
import com.github.jochenw.afw.core.plugins.DefaultPluginRegistry;
import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.afw.core.util.AbstractBuilder;
import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.afw.core.util.Functions.FailableSupplier;
import com.github.jochenw.qse.is.core.api.Finalizer;
import com.github.jochenw.qse.is.core.api.Logger;
import com.github.jochenw.qse.is.core.api.Rule;
import com.github.jochenw.qse.is.core.model.IsWorkspace;
import com.github.jochenw.qse.is.core.rules.AbstractRule;
import com.github.jochenw.qse.is.core.rules.PackageScannerRule;
import com.github.jochenw.qse.is.core.rules.RulesParser;
import com.github.jochenw.qse.is.core.sax.Sax;
import com.github.jochenw.qse.is.core.scan.IWorkspaceScanner;
import com.github.jochenw.qse.is.core.scan.PackageFileConsumer;


public class ScannerBuilder extends AbstractBuilder<Scanner,ScannerBuilder> {
	private FailableSupplier<InputSource,IOException> rulesFileSupplier;
	private IWorkspaceScanner workspaceScanner;
	private Logger logger;
	private List<Module> modules;

	public ScannerBuilder(Path pRulesFile) {
		if (pRulesFile == null) {
			rulesFileSupplier = getDefaultRulesSupplier();
		} else {
			rulesFileSupplier = () -> {
				final InputStream in = Files.newInputStream(pRulesFile);
				final InputSource isource = new InputSource(in);
				isource.setSystemId(pRulesFile.toString());
				return isource;
			};
		}
	}

	protected FailableSupplier<InputSource,IOException> getDefaultRulesSupplier() {
		final URL url = Scanner.class.getResource("rules.xml");
		if (url == null) {
			throw new IllegalStateException("Unable to locate rules.xml");
		}
		return () -> {
			final InputStream in = url.openStream();
			final InputSource isource = new InputSource(in);
			isource.setSystemId(url.toExternalForm());
			return isource;
		};
	}

	public IWorkspaceScanner getWorkspaceScanner() {
		if (workspaceScanner == null) {
			throw new IllegalStateException("No WorkspaceScanner configured");
		}
		return workspaceScanner;
	}

	public List<Module> getModules() {
		return modules;
	}

	public Logger getLogger() {
		if (logger == null) {
			throw new IllegalStateException("No Logger configured");
		}
		return logger;
	}

	public ScannerBuilder workspaceScanner(IWorkspaceScanner pWorkspaceScanner) {
		assertMutable();
		workspaceScanner = pWorkspaceScanner;
		return self();
	}

	public ScannerBuilder logger(Logger pLogger) {
		assertMutable();
		logger = pLogger;
		return self();
	}

	public ScannerBuilder module(Module pModule) {
		assertMutable();
		if (pModule != null) {
			if (modules == null) {
				modules = new ArrayList<>();
			}
			modules.add(pModule);
		}
		return self();
	}

	@Override
	protected Scanner newInstance() {
		final Logger logger = getLogger();
		final IWorkspaceScanner workspaceScanner = getWorkspaceScanner();
		final SimpleComponentFactoryBuilder cfb = new SimpleComponentFactoryBuilder()
				.module((b) -> {
					b.bind(IsWorkspace.class).toInstance(new IsWorkspace());
					b.bind(IWorkspaceScanner.class).toInstance(workspaceScanner);
					b.bind(Scanner.class);
					DefaultPluginRegistry pluginRegistry = new DefaultPluginRegistry();
					pluginRegistry.addExtensionPoint(PackageFileConsumer.class);
					pluginRegistry.addExtensionPoint(Finalizer.class);
					b.bind(IPluginRegistry.class).toInstance(pluginRegistry);
					b.bind(Logger.class).toInstance(logger);
				});
		final List<Module> otherModules = getModules();
		if (otherModules != null) {
			cfb.modules(otherModules);
		}
		final IComponentFactory cf = cfb.build();
		final Scanner scanner = cf.getInstance(Scanner.class);
		scanner.add(new PackageScannerRule());
		final Consumer<RulesParser.Rule> rulesConsumer = (r) -> configure(scanner, r);
		final RulesParser rulesParser = new RulesParser(rulesConsumer);
		try {
			Sax.parse(rulesFileSupplier.get(), rulesParser);
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
		return scanner;
	}

	protected void configure(@Nonnull Scanner pScanner, @Nonnull RulesParser.Rule pRule) {
		if (pRule.isEnabled()) {
			final String className = pRule.getClassName();
			final Class<?> clazz;
			try {
				clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException("Class not found:" + className);
			}
			final Rule rule;
			try {
				rule = (Rule) clazz.newInstance();
			} catch (Throwable t) {
				throw new UndeclaredThrowableException(t, "Unable to instantiate rule class "
			        + className + ": " + t.getMessage());
			}
			if (rule instanceof AbstractRule) {
				final AbstractRule abstractRule = (AbstractRule) rule;
				abstractRule.init(pRule);
			} else {
				throw new IllegalStateException("Don't know how to configure severity for rule: " + rule.getClass().getName());
			}
			pScanner.add(rule);
		}
	}
}
