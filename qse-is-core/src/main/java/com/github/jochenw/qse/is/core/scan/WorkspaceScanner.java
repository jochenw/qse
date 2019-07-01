package com.github.jochenw.qse.is.core.scan;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.afw.core.util.Sax;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.IsWorkspace;
import com.github.jochenw.qse.is.core.model.NSName;
import com.github.jochenw.qse.is.core.rules.ManifestParser;
import com.github.jochenw.qse.is.core.rules.PackageScannerRule.IsPackageListener;

public class WorkspaceScanner {
	private final IsWorkspace workspace;
	private final IPluginRegistry pluginRegistry;

	public WorkspaceScanner(IsWorkspace pWorkspace, IPluginRegistry pPluginRegistry) {
		workspace = pWorkspace;
		pluginRegistry = pPluginRegistry;
	}

	public void scan(Path baseDir, List<PackageFileConsumer> pPackageFileConsumers) {
		final ContextImpl context = new ContextImpl();
		final FileVisitor<Path> visitor = newVisitor(context, pPackageFileConsumers);
		try {
			Files.walkFileTree(baseDir, visitor);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private FileVisitor<Path> newVisitor(ContextImpl pContext, List<PackageFileConsumer> pPackageFileConsumers) {
		final List<IsPackageListener> packageListeners = pluginRegistry.requirePlugins(IsPackageListener.class);
		return new SimpleFileVisitor<Path>() {
			final StringBuilder fullPath = new StringBuilder();
			final StringBuilder localPath = new StringBuilder();
			IsPackage currentIsPackage;
			int currentIsPackageLevel;
			int level = 0;

			@Override
			public FileVisitResult preVisitDirectory(Path pDir, BasicFileAttributes pAttrs) throws IOException {
				++level;
				final Path manifestFile = pDir.resolve("manifest.v3");
				String dirName = pDir.getFileName().toString();
				if (Files.isRegularFile(manifestFile)) {
					if (currentIsPackage == null) {
						localPath.setLength(0);
						localPath.append(dirName);
					} else {
						throw new IllegalStateException("More than one manifest file detected in package " + currentIsPackage.getName() + ": "
								+ manifestFile.toAbsolutePath());
					}
					currentIsPackage = workspace.addPackage(dirName, dirName + "/manifest.v3");
					final ManifestParser.Listener mpl = new ManifestParser.Listener() {
						@Override
						public void version(String pVersion) {
							currentIsPackage.setVersion(pVersion);
						}

						@Override
						public void startupService(String pService) {
							currentIsPackage.addStartupService(NSName.valueOf(pService));
						}

						@Override
						public void shutdownService(String pService) {
							currentIsPackage.addShutdownService(NSName.valueOf(pService));
						}

						@Override
						public void requires(String pPackageName, String pVersion) {
							currentIsPackage.addDependency(pPackageName, pVersion);
						}
					};
					final ManifestParser mp = new ManifestParser(mpl);
					Sax.parse(manifestFile, mp);
					for (IsPackageListener ipl : packageListeners) {
						ipl.packageStarting(currentIsPackage);
					}
					
					currentIsPackageLevel = level;
					pContext.setPackage(currentIsPackage);
				} else {
					if (currentIsPackage != null) {
						if (localPath.length() > 0) {
							localPath.append('/');
							localPath.append(dirName);
							fullPath.append('/');
							fullPath.append(dirName);
						}
					}
				}
				return super.preVisitDirectory(pDir, pAttrs);
			}

			@Override
			public FileVisitResult visitFile(Path pFile, BasicFileAttributes pAttrs) throws IOException {
				if (currentIsPackage != null) {
					final int localPathLen = localPath.length();
					final int fullPathLen = fullPath.length();
					final String fileName = pFile.getFileName().toString();
					localPath.append('/');
					localPath.append(fileName);
					fullPath.append('/');
					fullPath.append(fileName);
					pContext.setLocalPath(localPath.toString());
					pContext.setFile(pFile);
					for (PackageFileConsumer pfc : pPackageFileConsumers) {
						pfc.accept(pContext);
					}
					localPath.setLength(localPathLen);
					fullPath.setLength(fullPathLen);
				}
				return super.visitFile(pFile, pAttrs);
			}

			@Override
			public FileVisitResult postVisitDirectory(Path pDir, IOException pExc) throws IOException {
				if (currentIsPackage != null) {
					if (level == currentIsPackageLevel) {
						currentIsPackage = null;
						for (IsPackageListener ipl : packageListeners) {
							ipl.packageStopping();
						}
						pContext.setPackage(null);
						pContext.setLocalPath(null);
						pContext.setFile(null);
					} else {
						final int offset = localPath.lastIndexOf("/");
						if (offset == -1) {
							localPath.setLength(0);
						} else {
							localPath.setLength(offset);
						}
					}
				}
				--level;
				return super.postVisitDirectory(pDir, pExc);
			}
		};
	}

}
