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
package com.github.jochenw.qse.lin.core.api;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.util.Objects;


public interface IQLinEngineBuilder {
	public QLinReport scan();
	public IQLinEngineBuilder configuration(Path pPath);
	public default IQLinEngineBuilder configuration(String pPath) {
		return configuration(Paths.get(pPath));
	}
	public default IQLinEngineBuilder configuration(File pPath) {
		return configuration(pPath.toPath());
	}
	public default IQLinEngineBuilder outputFile(String pOutputFilePath) {
		Objects.requireNonNull(pOutputFilePath, "Path");
		return outputFile(Paths.get(pOutputFilePath));
	}
	public IQLinEngineBuilder module(Module pModule);
	public default IQLinEngineBuilder modules(Module... pModules) {
		Objects.requireAllNonNull(pModules, "Module");
		for (Module m : pModules) {
			module(m);
		}
		return this;
	}
	public default IQLinEngineBuilder modules(Iterable<Module> pModules) {
		Objects.requireAllNonNull(pModules, "Module");
		for (Module m : pModules) {
			module(m);
		}
		return this;
	}
	List<Module> getModules();
	IQLinEngineBuilder configuration(QLinConfiguration pConfiguration);
	IQLinEngineBuilder outputFile(Path pPath);
	List<IQLinResourceSet> getResourceSets();
	QLinConfiguration getConfiguration();
	Path getOutputFile();
	ClassLoader getClassLoader();
	IQLinEngineBuilder classLoader(ClassLoader pClassLoader);
	IPluginRegistry getPluginRegistry();
	IQLinEngineBuilder pluginRegistry(IPluginRegistry pPluginRegistry);
	IComponentFactory getComponentFactory();
	IQLinEngineBuilder resourceSet(IQLinResourceSet pResourceSet);
	IQLinEngineBuilder fileSet(@Nonnull Path pDir, String[] pIncludes, String[] pExcludes, boolean pCaseSensitive,
			boolean pScanningArchives, Charset pCharset);
}
