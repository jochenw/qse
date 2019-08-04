package com.github.jochenw.qse.is.core;

import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Binder;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.inject.Scopes;
import com.github.jochenw.afw.core.inject.simple.SimpleComponentFactoryBuilder;
import com.github.jochenw.afw.core.plugins.DefaultPluginRegistry;
import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.qse.is.core.api.Finalizer;
import com.github.jochenw.qse.is.core.api.IssueWriter;
import com.github.jochenw.qse.is.core.api.Logger;
import com.github.jochenw.qse.is.core.api.Rule;
import com.github.jochenw.qse.is.core.model.IsWorkspace;
import com.github.jochenw.qse.is.core.rules.AbstractRule;
import com.github.jochenw.qse.is.core.rules.PackageScannerRule;
import com.github.jochenw.qse.is.core.rules.RulesParser;
import com.github.jochenw.qse.is.core.sax.Sax;
import com.github.jochenw.qse.is.core.scan.IWorkspaceScanner;
import com.github.jochenw.qse.is.core.scan.PackageFileConsumer;
import com.github.jochenw.qse.is.core.scan.DefaultWorkspaceScanner;


public class Scanner {
	public static interface Result {
		public int getNumberOfErrors();
		public int getNumberOfWarnings();
		public int getNumberOfOtherIssues();
	}

	@Inject IComponentFactory componentFactory;
	@Inject IPluginRegistry pluginRegistry;
	@Inject Logger logger;
	@Inject IsWorkspace workspace;
	private final List<Rule> rules = new ArrayList<>();
	private boolean immutable;

	protected void makeImmutable() {
		immutable = true;
	}

	protected void assertMutable() {
		if (immutable) {
			throw new IllegalStateException("This object is no longer mutable.");
		}
	}

	protected void add(Rule pRule) {
		pRule.accept(this);
		rules.add(pRule);
	}

	public void add(Finalizer pFinalizer) {
		pluginRegistry.addPlugin(Finalizer.class, pFinalizer);
	}

	public IComponentFactory getComponentFactory() {
		return componentFactory;
	}
	
	public void run() {
		makeImmutable();
		IWorkspaceScanner workspaceScanner = getComponentFactory().requireInstance(IWorkspaceScanner.class);
		final IWorkspaceScanner.Context context = getComponentFactory().requireInstance(IWorkspaceScanner.Context.class);
		context.setScanner(this);
		workspaceScanner.scan(context);
		pluginRegistry.forEach(Finalizer.class, (f) -> f.run());
	}

	public IsWorkspace getWorkspace() {
		return workspace;
	}
	
	public IPluginRegistry getPluginRegistry() {
		return pluginRegistry;
	}

	public Logger getLogger() {
		return logger;
	}

	public static Scanner newInstance(Path pRulesFile, Logger pLogger, Module pModule, Module... pOtherModules) {
		Objects.requireNonNull(pLogger, "Logger");
		Objects.requireNonNull(pModule, "Module");
		final SimpleComponentFactoryBuilder cfb = new SimpleComponentFactoryBuilder()
				.module((b) -> {
					b.bind(IsWorkspace.class).toInstance(new IsWorkspace());
					b.bind(IWorkspaceScanner.class).to(DefaultWorkspaceScanner.class);
					b.bind(Scanner.class);
					DefaultPluginRegistry pluginRegistry = new DefaultPluginRegistry();
					pluginRegistry.addExtensionPoint(PackageFileConsumer.class);
					pluginRegistry.addExtensionPoint(Finalizer.class);
					b.bind(IPluginRegistry.class).toInstance(pluginRegistry);
					b.bind(Logger.class).toInstance(pLogger);
				})
				.module(pModule);
		if (pOtherModules != null) {
			cfb.modules(pOtherModules);
		}
		final IComponentFactory cf = cfb.build();
		final Scanner scanner = cf.getInstance(Scanner.class);
		scanner.add(new PackageScannerRule());
		final Consumer<RulesParser.Rule> rulesConsumer = (r) -> configure(scanner, r);
		if (pRulesFile == null) {
			RulesParser.parseBuiltinRules(rulesConsumer);
		} else {
			Sax.parse(pRulesFile, new RulesParser(rulesConsumer));
		}
		return scanner;
	}

	public static void configure(@Nonnull Scanner pScanner, @Nonnull RulesParser.Rule pRule) {
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

	public static Result scan(Path pScanDir, Path pOutputFile, Path pRulesFile, Logger pLogger) {
		if (pOutputFile == null) {
			return scan(pScanDir, System.out, false, true, pRulesFile, pLogger);
		} else {
			try (OutputStream os = Files.newOutputStream(pOutputFile)) {
				return scan(pScanDir, os, true, false, pRulesFile, pLogger);
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
	}

	public static Result scan(Path pScanDir, OutputStream pOut, boolean pCloseOut, boolean pPrettyPrint, Path pRulesFile, Logger pLogger) {
		final Path scanDir;
		if (pScanDir == null) {
			scanDir = Paths.get(".");
		} else {
			scanDir = pScanDir;
		}
		final Module module = new Module() {
			@Override
			public void configure(Binder pBinder) {
				final IWorkspaceScanner.Context context = new DefaultWorkspaceScanner.DefaultWSContext(scanDir);
				pBinder.bind(IWorkspaceScanner.class).to(DefaultWorkspaceScanner.class).in(Scopes.SINGLETON);
				pBinder.bind(IWorkspaceScanner.Context.class).toInstance(context);
			}
		};
		final Scanner scanner = newInstance(pRulesFile, pLogger, module);
		try (IssueWriter writer = new IssueWriter(pOut, pCloseOut, pPrettyPrint)) {
			scanner.getWorkspace().addListener(writer);
			scanner.run();
			final Result result = writer.getResult();
			return result;
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}
}
