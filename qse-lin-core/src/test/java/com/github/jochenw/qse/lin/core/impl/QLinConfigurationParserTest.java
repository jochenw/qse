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
package com.github.jochenw.qse.lin.core.impl;

import static org.junit.Assert.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.github.jochenw.qse.lin.core.api.QLinConfiguration;

public class QLinConfigurationParserTest {
	@Test
	public void testLocalProjectConfiguration() throws Exception {
		final String uri = "src/main/qlin/qse-lin-configuration.xml";
		final Path p = Paths.get(uri);
		assertTrue(Files.isRegularFile(p));
		final QLinConfiguration config = new QLinConfigurationParser().parse(p);
		assertNotNull(config);
		final String[] defaultExcludes = config.getDefaultExcludes();
		assertNotNull(defaultExcludes);
		assertEquals(1, defaultExcludes.length);
		assertEquals("all", defaultExcludes[0]);
	}
}
