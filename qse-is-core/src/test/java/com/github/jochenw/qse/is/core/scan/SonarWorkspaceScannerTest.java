package com.github.jochenw.qse.is.core.scan;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Test;

import com.github.jochenw.qse.is.core.scan.SonarWorkspaceScanner.SonarResource;

public class SonarWorkspaceScannerTest {
	private final long seed = 1564926475992l; // System.currentTimeMillis() at the time, this test was written.
	private final Random random = new Random(seed);

	/** Create a list with files from one package.
	 * Use a good degree of randomness, but with a predictable result.
	 */
	private List<File> getAllOnePackageFiles() {
		final File file0 = new File("manifest.v3");
		final File file1 = new File("config/log-messages.xml");
		final File file2 = new File("code/sources/com/foo/barapp/baz.java");
		final File file3 = new File("code/classes/com/foo/barapp/baz.class");
		final File file4 = new File("ns/com/foo/barapp/pub/SomeService/flow.xml");
		final List<File> list = new ArrayList<>(Arrays.asList(file1, file2, file3, file4));
		list.add(random.nextInt(4), file0);
		return list;
	}

	/** Create a list with files from two packages.
	 * Use a good degree of randomness, but with a predictable result.
	 */
	private List<File> getSomePackageFiles() {
		final File file0 = new File("FooBarApp/manifest.v3");
		final File file1 = new File("FooBarApp/config/log-messages.xml");
		final File file2 = new File("FooBarApp/code/sources/com/foo/barapp/baz.java");
		final File file3 = new File("FooBarApp/code/classes/com/foo/barapp/baz.class");
		final File file4 = new File("FooBarApp/ns/com/foo/barapp/pub/SomeService/flow.xml");
		final List<File> list0 = new ArrayList<>(Arrays.asList(file0, file1, file2, file3, file4));
		final File file5 = new File("FooBarApp_Test/manifest.v3");
		final File file6 = new File("FooBarApp_Test/config/wxconfig.cnf");
		final File file7 = new File("FooBarApp_Test/test/WmTestSuite.xml");
		final File file8 = new File("FooBarApp_Test/code/sources/com/foo/barapp/bazTest.java");
		final File file9 = new File("FooBarApp_Test/code/classes/com/foo/barapp/bazTest.class");
		final File file10 = new File("FooBarApp_Test/.gitignore");
		final List<File> list1 = new ArrayList<>(Arrays.asList(file5, file6, file7, file8, file9, file10));
		final List<File> list = new ArrayList<>();
		while (!list0.isEmpty()  &&  !list1.isEmpty()) {
			final boolean useList0 = random.nextBoolean();
			if (useList0) {
				list.add(list0.remove(0));
			} else {
				list.add(list1.remove(0));
			}
		}
		if (list0.isEmpty()) {
			list.addAll(list1);
		} else {
			list.addAll(list0);
		}
		return list;
	}
	
	
	@Test
	public void testAllFiles() {
		for (int i = 0;  i < 100;  i++) {
			final List<File> files = new ArrayList<File>(getAllOnePackageFiles());
			final SonarWorkspaceScanner sws = new SonarWorkspaceScanner(() -> files);
			final Map<String,SonarResource> packages = sws.getPackages(files);
			assertNotNull(packages);
			assertEquals(1, packages.size());
			final List<SonarResource> fileList = sws.getFilesForPackage("", packages, files);
			assertNotNull(fileList);
			assertEquals(5, fileList.size());
			assertEquals("manifest.v3", fileList.get(0).getUri());
			assertEquals("config/log-messages.xml", fileList.get(1).getUri());
			assertEquals("code/sources/com/foo/barapp/baz.java", fileList.get(2).getUri());
			assertEquals("code/classes/com/foo/barapp/baz.class", fileList.get(3).getUri());
			assertEquals("ns/com/foo/barapp/pub/SomeService/flow.xml", fileList.get(4).getUri());
		}
	}

	@Test
	public void testSomePackages() {
		for (int i = 0;  i < 100;  i++) {
			final List<File> files = getSomePackageFiles();
			final SonarWorkspaceScanner sws = new SonarWorkspaceScanner(() -> files);
			final Map<String,SonarResource> packages = sws.getPackages(files);
			assertNotNull(packages);
			assertEquals(2, packages.size());
			final List<SonarResource> fooBarAppList = sws.getFilesForPackage("FooBarApp", packages, files);
			assertNotNull(fooBarAppList);
			assertEquals(5, fooBarAppList.size());
			assertEquals("FooBarApp/manifest.v3", fooBarAppList.get(0).getUri());
			assertEquals("FooBarApp/config/log-messages.xml", fooBarAppList.get(1).getUri());
			assertEquals("FooBarApp/code/sources/com/foo/barapp/baz.java", fooBarAppList.get(2).getUri());
			assertEquals("FooBarApp/code/classes/com/foo/barapp/baz.class", fooBarAppList.get(3).getUri());
			assertEquals("FooBarApp/ns/com/foo/barapp/pub/SomeService/flow.xml", fooBarAppList.get(4).getUri());
			for (int j = 0;  j < fooBarAppList.size();  j++) {
				assertEquals("FooBarApp", fooBarAppList.get(j).getPackage());
			}
			final List<SonarResource> fooBarAppTestList = sws.getFilesForPackage("FooBarApp_Test", packages, files);
			assertNotNull(fooBarAppTestList);
			assertEquals(6, fooBarAppTestList.size());
			assertEquals("FooBarApp_Test/manifest.v3", fooBarAppTestList.get(0).getUri());
			assertEquals("FooBarApp_Test/config/wxconfig.cnf", fooBarAppTestList.get(1).getUri());
			assertEquals("FooBarApp_Test/test/WmTestSuite.xml", fooBarAppTestList.get(2).getUri());
			assertEquals("FooBarApp_Test/code/sources/com/foo/barapp/bazTest.java", fooBarAppTestList.get(3).getUri());
			assertEquals("FooBarApp_Test/code/classes/com/foo/barapp/bazTest.class", fooBarAppTestList.get(4).getUri());
			assertEquals("FooBarApp_Test/.gitignore", fooBarAppTestList.get(5).getUri());
			for (int j = 0;  j < fooBarAppTestList.size();  j++) {
				assertEquals("FooBarApp_Test", fooBarAppTestList.get(j).getPackage());
			}
		}
	}
}
