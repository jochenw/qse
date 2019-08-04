package com.github.jochenw.qse.is.core.util;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

public class FilesTest {
	@Test
	public void testGetPath() {
		assertEquals("foo/bar.txt", Files.getPath(new File("foo/bar.txt")));
		assertEquals("foo/bar.txt", Files.getPath(new File("foo\\bar.txt")));
	}

	@Test
	public void testGetRelativePath() {
		assertEquals("bar.txt", Files.getRelativePath(new File("foo/bar.txt"), new File("foo")));
		assertEquals("bar/baz.txt", Files.getRelativePath(new File("foo/bar/baz.txt"), new File("foo")));
	}
}
