package com.github.jochenw.qse.is.core.scan;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.Function;

import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.IsWorkspace;
import com.github.jochenw.qse.is.core.scan.ContextImpl.EditRequest;


public class WorkspaceScannerOld {
	private final IsWorkspace workspace;
	private final Path baseDir;
	private Function<List<EditRequest>,Boolean> editRequestHandler;

	public WorkspaceScannerOld(IsWorkspace pWorkspace, Path pBaseDir) {
		workspace = pWorkspace;
		baseDir = pBaseDir;
	}

	public Function<List<EditRequest>,Boolean> getEditRequestHandler() {
		return editRequestHandler;
	}

	public void setEditRequestHandler(Function<List<EditRequest>,Boolean> editRequestHandler) {
		this.editRequestHandler = editRequestHandler;
	}
	
	public void scan(List<PackageFileConsumer> pListeners) {
		for (;;) {
			final ContextImpl ctx = new ContextImpl();
			final FileVisitor<Path> fv = new FileVisitor<Path>() {
				private StringBuilder sb = new StringBuilder();
				private IsPackage pkg;
				private int level;
				private int packageLevel;

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					++level;
					final Path manifestFile = dir.resolve("manifest.v3");
					if (Files.isRegularFile(manifestFile)) {
						if (pkg == null) {
							final String packageName = dir.getFileName().toString();
							final String uri = baseDir.relativize(dir).toString();
							pkg = workspace.addPackage(packageName, uri);
							ctx.setPackage(pkg);
							packageLevel = level;
							sb.setLength(0);
						} else {
							throw new IllegalStateException("Package directory " + dir
									+ " is nested within another package directory: " + pkg.getUri());
						}
					} else if (pkg != null) {
						if (sb.length() > 0) {
							sb.append('/');
						}
						sb.append(dir.getFileName().toString());
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path pFile, BasicFileAttributes pAttrs) throws IOException {
					final int len = sb.length();
					if (sb.length() > 0) {
						sb.append('/');
						sb.append(pFile.getFileName().toString());
					}
					ctx.setLocalPath(sb.toString());
					sb.setLength(len);
					ctx.setFile(pFile);
					for (PackageFileConsumer listener : pListeners) {
						listener.accept(ctx);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path pFile, IOException pExc) throws IOException {
					throw pExc;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if (pkg == null) {

					} else {
						if (packageLevel == level) {
							ctx.setPackage(null);
							pkg = null;
						} else {
							final int offset = sb.lastIndexOf("/");
							if (offset != -1) {
								sb.setLength(offset);
							} else {
								sb.setLength(0);
							}
						}
					}
					--level;
					return FileVisitResult.CONTINUE;
				}
			};
			try {
				Files.walkFileTree(baseDir, fv);
				final List<EditRequest> editRequests = ctx.getEditRequests();
				if (editRequests.isEmpty()  ||  getEditRequestHandler() == null  ||  !getEditRequestHandler().apply(editRequests).booleanValue()) {
					break;
				}
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
	}
}
