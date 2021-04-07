package com.github.jochenw.qse.is.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.xml.sax.InputSource;

import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.simple.SimpleComponentFactoryBuilder;
import com.github.jochenw.afw.core.io.IReadable;
import com.github.jochenw.afw.core.plugins.DefaultPluginRegistry;
import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.afw.core.template.ITemplateEngine.Template;
import com.github.jochenw.afw.core.template.SimpleTemplateEngine;
import com.github.jochenw.afw.core.util.AbstractBuilder;
import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.afw.core.util.Functions.FailableSupplier;
import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.afw.core.util.Streams;
import com.github.jochenw.qse.is.core.api.Finalizer;
import com.github.jochenw.qse.is.core.api.IssueCollector;
import com.github.jochenw.qse.is.core.api.IssueConsumer;
import com.github.jochenw.qse.is.core.api.IssueWriter;
import com.github.jochenw.qse.is.core.api.Logger;
import com.github.jochenw.qse.is.core.api.PrintStreamLogger;
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
	private List<IssueConsumer> listeners;
	private Path outputFile;
	private IReadable templateFile;
	private String templateTitle;

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

	public ScannerBuilder listener(IssueConsumer pListener) {
		assertMutable();
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		listeners.add(pListener);
		return this;
	}

	public ScannerBuilder outputFile(Path pPath) {
		assertMutable();
		outputFile = pPath;
		return this;
	}

	public ScannerBuilder outputFile(File pFile) {
		return outputFile(pFile.toPath());
	}

	public ScannerBuilder templateFile(Path pPath) {
		assertMutable();
		if (!Files.isRegularFile(pPath)) {
			throw new IllegalStateException("Expected existing template file, got " + pPath);
		}
		templateFile = IReadable.of(pPath);
		return this;
	}

	public ScannerBuilder templateFile(File pFile) {
		return templateFile(pFile.toPath());
	}

	public ScannerBuilder templateFile(String pUri) {
		String uri = Objects.requireNonNull(pUri, "URI");
		if (uri.startsWith("default:")) {
			uri = "resource:com/github/jochenw/qse/is/core/" + uri.substring("default:".length());
		}
		if (uri.startsWith("resource:")) {
			final String resUri = uri.substring("resource:".length());
			final URL url = Thread.currentThread().getContextClassLoader().getResource(resUri);
			if (url == null) {
				throw new IllegalArgumentException("Resource not found: " + resUri);
			}
			templateFile = IReadable.of(url);
		} else {
			final URL url;
			try {
				url = new URL(uri);
				templateFile = IReadable.of(url);
			} catch (MalformedURLException e) {
				final Path path = Paths.get(uri);
				if (Files.isRegularFile(path)) {
					templateFile = IReadable.of(path);
				} else {
					throw new IllegalStateException("Invalid resource specification: Expected "
							+ " existing file, or URL, got " + uri);
				}
			}
		}
		return this;
	}
	public ScannerBuilder templateTitle(String pTitle) {
		assertMutable();
		templateTitle = Objects.requireNonNull(pTitle, "Title");
		return this;
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

	public ScannerBuilder logToSystemOut() {
		return logger(new PrintStreamLogger(System.out));
	}

	public ScannerBuilder logToSystemErr() {
		return logger(new PrintStreamLogger(System.err));
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
		if (listeners != null) {
			listeners.forEach((l) -> scanner.getWorkspace().addListener(l));
		}
		return new Scanner() {
			@Override
			protected void makeImmutable() {
				scanner.makeImmutable();
			}

			@Override
			public List<Rule> getRules() {
				return scanner.getRules();
			}

			@Override
			protected void assertMutable() {
				scanner.assertMutable();
			}

			@Override
			protected void add(Rule pRule) {
				scanner.add(pRule);
			}

			@Override
			public void add(Finalizer pFinalizer) {
				scanner.add(pFinalizer);
			}

			@Override
			public IComponentFactory getComponentFactory() {
				return scanner.getComponentFactory();
			}

			@Override
			public void run() {
				if (outputFile != null) {
					try {
						final Path outputDir = outputFile.getParent();
						if (outputDir != null) {
							Files.createDirectories(outputDir);
						}
						if (templateFile == null) {
							try (OutputStream out = Files.newOutputStream(outputFile);
								 IssueWriter iw = new IssueWriter(out, true, true)) {
								scanner.getWorkspace().addListener(iw);
								scanner.run();
							}
						} else {
							final String templateText = templateFile.apply((r) -> {
								return Streams.read(r);
							}, StandardCharsets.UTF_8);
							final IssueCollector ic = new IssueCollector();
							scanner.getWorkspace().addListener(ic);
							final Template<Map<String,Object>> template = new SimpleTemplateEngine().compile(templateText);
							final Map<String,Object> map = new HashMap<>();
							map.put("issues", ic.getIssues());
							map.put("size", String.valueOf(ic.getIssues().size()));
							map.put("issueList", ic.getIssues());
							map.put("numberOfErrors", String.valueOf(ic.getNumErrors()));
							map.put("numberOfWarnings", String.valueOf(ic.getNumWarnings()));
							map.put("numberOfOtherIssues", String.valueOf(ic.getNumOtherIssues()));
							if (templateTitle != null) {
								map.put("title", templateTitle);
							}
						    try (Writer w = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
								template.write(map, w);
							}
						}
					} catch (IOException e) {
						throw new UncheckedIOException(e);
					}
				}
				if (templateFile != null) {
					final IssueCollector collector = new IssueCollector();
					scanner.getWorkspace().addListener(collector);
					scanner.run();
				} else {
					scanner.run();
				}
			}

			@Override
			public IsWorkspace getWorkspace() {
				return scanner.getWorkspace();
			}

			@Override
			public IPluginRegistry getPluginRegistry() {
				return scanner.getPluginRegistry();
			}

			@Override
			public Logger getLogger() {
				return scanner.getLogger();
			}
		};
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
