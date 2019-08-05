package com.github.jochenw.qse.is.core.util;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Files {

	public static String getRelativePath(File pFile, File pDir) {
		final File dir = pFile.getParentFile();
		if (dir == null) {
			return null;
		} else {
			if (dir.equals(pDir)) {
				return pFile.getName();
			} else {
				final String path = getRelativePath(dir, pDir);
				if (path == null) {
					return null;
				} else {
					return path + '/' + pFile.getName();
				}
			}
		}
	}

	public static String getPath(File pFile) {
		return pFile.getPath().replace('\\', '/');
	}

	public static List<File> findFiles(Path pPath) {
		return findPaths(pPath).stream().map((p) -> p.toFile()).collect(Collectors.toList());
	}

	public static List<Path> findPaths(Path pPath) {
		final List<Path> paths = new ArrayList<>();
		final FileVisitor<Path> sfv = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path pFile, BasicFileAttributes pAttrs) throws IOException {
				paths.add(pFile);
				return super.visitFile(pFile, pAttrs);
			}
		};
		try {
			java.nio.file.Files.walkFileTree(pPath, sfv);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		return paths;
	}

}
