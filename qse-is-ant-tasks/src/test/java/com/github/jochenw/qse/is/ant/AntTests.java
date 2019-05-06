package com.github.jochenw.qse.is.ant;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

import com.github.jochenw.afw.core.util.Exceptions;


public class AntTests 	 {
	public static void copyDir(Path pSourceDir, Path pTargetDir) {
		if (!java.nio.file.Files.isDirectory(pSourceDir)) {
			throw new IllegalStateException("Invalid source directory: " + pSourceDir);
		}
		final FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path pQualifiedPath, BasicFileAttributes pAttrs) throws IOException {
				final Path relativePath = pSourceDir.relativize(pQualifiedPath);
				final Path targetFile = pTargetDir.resolve(relativePath);
				final Path targetDir = targetFile.getParent();
				if (targetDir != null) {
					Files.createDirectories(targetDir);
				}
				Files.copy(pQualifiedPath, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
				return super.visitFile(pQualifiedPath, pAttrs);
			}
		};
		try {
			java.nio.file.Files.walkFileTree(pSourceDir, visitor);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static void run(String pTestId) {
		final Path sourceDir = Paths.get("src/test/resources/it-tests").resolve(pTestId);
		final Path sourceScript = sourceDir.resolve("build.xml");
		final Path targetDir = Paths.get("target/it-tests").resolve(pTestId);
		assertTrue("Invalid source directory: " + sourceDir, Files.isDirectory(sourceDir));
		assertTrue("Invalid build script: " + sourceScript, Files.isRegularFile(sourceScript));
		copyDir(sourceDir, targetDir);
		final Path targetScript = targetDir.resolve("build.xml");
		assertTrue("Build script is not available: " + targetScript, Files.isRegularFile(targetScript));
		final AntRunner runner = new AntRunner() {
		};
		final String oldUserDir = System.getProperty("user.dir");
		System.err.println("Changing current directory from " + oldUserDir + " to " + targetDir);
		try {
			final String userDir = targetDir.toAbsolutePath().toString();
			System.setProperty("user.dir", userDir);
			final Properties props = new Properties();
			props.put("baseDir", userDir);
			runner.startAnt(new String[] {"-v", "-f",targetScript.toAbsolutePath().toString()}, props, Thread.currentThread().getContextClassLoader());
		} catch (Throwable t) {
			throw Exceptions.show(t);
		} finally {
			System.setProperty("user.dir", oldUserDir);
		}
	}
}
