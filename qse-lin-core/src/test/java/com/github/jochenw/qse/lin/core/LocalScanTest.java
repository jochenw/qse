/*
 *    Copyright 2019 Jochen Wiedmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.github.jochenw.qse.lin.core;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.github.jochenw.qse.lin.core.api.IQLinEngine;
import com.github.jochenw.qse.lin.core.api.IQLinEngineBuilder;
import com.github.jochenw.qse.lin.core.api.QLinReport;

public class LocalScanTest {
	@Test
	public void testScanProject() throws Exception {
		final String outputFilePath = "target/qlin/qse-lin-report.txt";
		final Path outputPath = Paths.get(outputFilePath);
		Files.deleteIfExists(outputPath);
		IQLinEngineBuilder qleb =
				IQLinEngine.builder().configuration("src/main/qlin/qse-lin-configuration.xml")
				.outputFile(outputFilePath);
		qleb.getConfiguration().setApprovedLicenseIds(new String[] {"Apache-2.0"});
		qleb.getConfiguration().setApprovedLicenseFamilyIds(new String[] {"ASLV2"});
		qleb.getConfiguration().setNumberOfThreads(1);
		QLinReport report = qleb.scan();
		assertEquals(0, report.getNumberOfUnknownFiles());
		assertTrue(Files.isRegularFile(outputPath));
	}
}
