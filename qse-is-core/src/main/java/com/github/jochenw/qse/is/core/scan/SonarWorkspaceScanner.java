package com.github.jochenw.qse.is.core.scan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.jochenw.afw.core.util.Sax;
import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.api.Logger;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.IsWorkspace;
import com.github.jochenw.qse.is.core.model.NSName;
import com.github.jochenw.qse.is.core.rules.ManifestParser;
import com.github.jochenw.qse.is.core.rules.PackageScannerRule.IsPackageListener;
import com.github.jochenw.qse.is.core.util.Files;

public class SonarWorkspaceScanner implements IWorkspaceScanner {
	public interface SonarResource {
		String getPackage();
		String getUri();
		InputStream open() throws IOException;
	}
	public static class PackageFileConsumerImpl extends ContextImpl {
		private SonarResourceImpl sonarResource;

		public SonarResourceImpl getSonarResource() {
			return sonarResource;
		}
		public void setSonarResource(SonarResourceImpl pSonarResource) {
			sonarResource = pSonarResource;
			
		}
	}
	static class SonarResourceImpl implements SonarResource {
		private final String packageName;
		private final String uri;
		private final File file;

		SonarResourceImpl(String pPackageName, String pUri, File pFile) {
			packageName = pPackageName;
			uri = pUri;
			file = pFile;
		}

		@Override
		public String getPackage() {
			return packageName;
		}

		@Override
		public String getUri() {
			return uri;
		}

		@Override
		public InputStream open() throws IOException {
			return new FileInputStream(file);
		}
	}
	public static class SonarWSContext extends Context {
		private final List<File> files;

		public SonarWSContext(List<File> pFiles) {
			files = pFiles;
		}

		public List<File> getFiles() {
			return files;
		}
	}

	/**
	 * Returns the set of package roots in the given file list. A package root is defined as
	 * a directory, which contains a "manifest.v3" file.
	 */
	protected Map<String,SonarResource> getPackages(List<File> pAllFiles) {
		final Map<String,SonarResource> packages = new HashMap<>();
		for (File f : pAllFiles) {
			if ("manifest.v3".equals(f.getName())) {
				File dir = f.getParentFile();
				final String packageName;
				if (dir == null) {
					packageName = "";
					final SonarResource resource = new SonarResourceImpl("", "manifest.v3", f);
					return Collections.singletonMap("", resource);
				} else {
					packageName = dir.getName();
					final SonarResourceImpl existingResource = (SonarResourceImpl) packages.get(packageName);
					if (existingResource != null) {
						throw new IllegalArgumentException("Duplicate package name: " + packageName
								+ " (specified by " + f.getPath() + ", and "
								+ existingResource.file.getPath() + ")");
					}
					final SonarResource resource = new SonarResourceImpl(packageName,
							                                            packageName + "/manifest.v3",
							                                            f);
					packages.put(packageName, resource);
				}
			}
		}
		return packages;
	}

	protected List<SonarResource> getFilesForPackage(String pPackageName, Map<String,SonarResource> pPackages, List<File> pAllFiles) {
		final List<SonarResource> resources = new ArrayList<>();
		final SonarResourceImpl manifestResource = (SonarResourceImpl) pPackages.get(pPackageName);
		if (manifestResource == null) {
			throw new IllegalArgumentException("Invalid package name: " + pPackageName);
		}
		final File manifestFile = manifestResource.file;
		final File manifestDir = manifestFile.getParentFile();
		resources.add(manifestResource);
		if (manifestDir == null) {
			for (File f : pAllFiles) {
				if (!f.equals(manifestFile)) {
					final String uri = Files.getPath(f);
					final SonarResource resource = new SonarResourceImpl(pPackageName, uri, f);
					resources.add(resource);
				}
			}
		} else {
			for (File f : pAllFiles) {
				final String uri = Files.getRelativePath(f, manifestDir);
				if (uri != null) {
					if (!f.equals(manifestFile)) {
						final SonarResource resource = new SonarResourceImpl(pPackageName, pPackageName + "/" + uri, f);
						resources.add(resource);
					}
				}
			}
		}
		return resources;
	}

	public static String getPath(File pFile) {
		return Files.getPath(pFile);
	}

	public static String getRelativePath(File pFile, File pDir) {
		return Files.getRelativePath(pFile, pDir);
	}
	
	@Override
	public void scan(Context pContext) {
		final Scanner scanner = pContext.getScanner();
		final Logger logger = scanner.getLogger();
		logger.info("scan: ->");
		final SonarWSContext context = (SonarWSContext) pContext;
		final List<File> files = context.getFiles();
		final Map<String,SonarResource> packages = getPackages(files);
		logger.info("Found " + packages.size() + " IS Packages");
		if (packages.isEmpty()) {
			throw new IllegalStateException("No IS Package found.");
		}
		final PackageFileConsumerImpl pfci = new PackageFileConsumerImpl();
		final IsWorkspace isWorkspace = scanner.getWorkspace();
		final List<PackageFileConsumer> pfConsumers = scanner.getPluginRegistry().getPlugins(PackageFileConsumer.class);
		final List<IsPackageListener> packageListeners = scanner.getPluginRegistry().getPlugins(IsPackageListener.class);
		for (Map.Entry<String,SonarResource> en : packages.entrySet()) {
			final String packageName = en.getKey();
			final SonarResourceImpl manifestResource = (SonarResourceImpl) en.getValue();
			logger.debug("scan: Scanning IS Package {} at {}", packageName, manifestResource.file.getParentFile());
			final List<SonarResource> packageResources = getFilesForPackage(packageName, packages, files);
			logger.debug("scan: Found " + packageResources.size() + " files for IS Package {}", packageName);
			final IsPackage isPkg = isWorkspace.addPackage(packageName, manifestResource.getUri());
			final ManifestParser.Listener mpl = new ManifestParser.Listener() {
				@Override
				public void version(String pVersion) {
					isPkg.setVersion(pVersion);
				}

				@Override
				public void startupService(String pService) {
					isPkg.addStartupService(NSName.valueOf(pService));
				}

				@Override
				public void shutdownService(String pService) {
					isPkg.addShutdownService(NSName.valueOf(pService));
				}

				@Override
				public void requires(String pPackageName, String pVersion) {
					isPkg.addDependency(pPackageName, pVersion);
				}
			};
			final ManifestParser mp = new ManifestParser(mpl);
			Sax.parse(manifestResource.file, mp);
			packageListeners.forEach((pl) -> pl.packageStarting(isPkg));
			pfci.setPackage(isPkg);
			for (SonarResource sr : packageResources) {
				final SonarResourceImpl sri = (SonarResourceImpl) sr;
				pfci.setFile(sri.file.toPath());
				pfci.setLocalPath(sri.uri);
				pfConsumers.forEach((pfc) -> pfc.accept(pfci));
			}
			packageListeners.forEach((pl) -> pl.packageStopping());
		}
	}

}
