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

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.github.jochenw.afw.core.ILifecycleController;
import com.github.jochenw.afw.core.impl.DefaultLifecycleController;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Binder;
import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.afw.core.inject.IComponentFactory;
import com.github.jochenw.afw.core.inject.Names;
import com.github.jochenw.afw.core.inject.Scopes;
import com.github.jochenw.afw.core.inject.Types;
import com.github.jochenw.afw.core.inject.simple.SimpleComponentFactoryBuilder;
import com.github.jochenw.afw.core.util.AbstractBuilder;
import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.afw.core.util.Objects;
import com.github.jochenw.qse.lin.core.api.IArchiveHandler;
import com.github.jochenw.qse.lin.core.api.IExclusionList;
import com.github.jochenw.qse.lin.core.api.ILicense;
import com.github.jochenw.qse.lin.core.api.ILicenseFamily;
import com.github.jochenw.qse.lin.core.api.IMatcher;
import com.github.jochenw.qse.lin.core.api.IPluginRegistry;
import com.github.jochenw.qse.lin.core.api.IPluginRegistry.PluginInfo;
import com.github.jochenw.qse.lin.core.plugins.DefaultExclusionList;
import com.github.jochenw.qse.lin.core.plugins.DefaultLicenseFamily;
import com.github.jochenw.qse.lin.core.plugins.Initializable;
import com.github.jochenw.qse.lin.core.plugins.XmlPluginRegistry;
import com.github.jochenw.qse.lin.core.api.IQLinEngine;
import com.github.jochenw.qse.lin.core.api.IQLinEngineBuilder;
import com.github.jochenw.qse.lin.core.api.IQLinReportWriter;
import com.github.jochenw.qse.lin.core.api.IQLinResourceSet;
import com.github.jochenw.qse.lin.core.api.ITemplateEngine;
import com.github.jochenw.qse.lin.core.api.QLinConfiguration;
import com.github.jochenw.qse.lin.core.api.QLinConfiguration.FileSet;
import com.github.jochenw.qse.lin.core.api.QLinReport;


public class QLinEngineBuilder extends AbstractBuilder<IQLinEngine,QLinEngineBuilder> implements IQLinEngineBuilder {
	private final List<Module> modules = new ArrayList<>();
	private final List<IQLinResourceSet> resourceSets = new ArrayList<>();
	private ComponentFactoryBuilder<?> componentFactoryBuilder;
	private IComponentFactory componentFactory;
	private QLinConfiguration configuration;
	private IPluginRegistry pluginRegistry;
	private Path outputFile;
	private ClassLoader classLoader;

	@Override
	public QLinReport scan() {
		final long startTime = System.currentTimeMillis();
		final QLinReport report = new QLinReport();
		build();
		final IQLinEngine engine = componentFactory.requireInstance(IQLinEngine.class);
		engine.scan(report, getResourceSets());
		final long runTime = System.currentTimeMillis()-startTime;
		final Path outputFile = getOutputFile();
		if (outputFile != null) {
			try {
				final Path dir = outputFile.getParent();
				if (dir != null) {
					Files.createDirectories(dir);
				}
				final IQLinReportWriter qrw = getComponentFactory().requireInstance(IQLinReportWriter.class);
				try (OutputStream os = Files.newOutputStream(outputFile);
					 BufferedOutputStream bos = new BufferedOutputStream(os);
					 Writer w = new OutputStreamWriter(bos, configuration.getReportCharset());
					 BufferedWriter bw = new BufferedWriter(w)) {
					qrw.write(report, runTime, bw);
				}
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
		
		return report;
	}

	@Override
	public IComponentFactory getComponentFactory() {
		return componentFactory;
	}

	@Override
	public List<IQLinResourceSet> getResourceSets() {
		return resourceSets;
	}

	@Override
	public IQLinEngineBuilder configuration(Path pPath) {
		QLinConfiguration config = new QLinConfigurationParser().parse(pPath);
		return configuration(config);
	}

	@Override
	public IQLinEngineBuilder outputFile(Path pPath) {
		Objects.requireNonNull(pPath, "Path");
		assertMutable();
		outputFile = pPath;
		return self();
	}

	@Override
	public Path getOutputFile() {
		return outputFile;
	}

	protected ComponentFactoryBuilder<?> newComponentFactoryBuilder() {
		return new SimpleComponentFactoryBuilder();
	}

	@Nonnull protected Module newDefaultModule() {
		final List<IArchiveHandler> archiveHandlers = asPluginList(getPluginRegistry().getArchiveHandlers());
		final List<IMatcher> allMatchers = asPluginList(getPluginRegistry().getMatchers());
		final List<ILicense> licenses = asPluginList(getPluginRegistry().getLicenses());
		final List<ILicenseFamily> licenseFamilies = asPluginList(getPluginRegistry().getFamilies());
		final List<IExclusionList> exclusionLists = asPluginList(getPluginRegistry().getExclusionLists());
		final QLinConfiguration configuration = getConfiguration();
		final Set<String> noticeLicenseIds = asSet(configuration.getNoticeLicenseIds());
		final Set<String> binaryLicenseIds = asSet(configuration.getBinaryLicenseIds());
		final Set<String> generatedLicenseIds = asSet(configuration.getGeneratedLicenseIds());
		final Set<String> approvedLicenseIds = asSet(configuration.getApprovedLicenseIds());
		final Set<String> approvedLicenseFamilyIds = asSet(configuration.getApprovedLicenseFamilyIds());
		if (approvedLicenseIds.isEmpty()  &&  approvedLicenseFamilyIds.isEmpty()) {
			throw new IllegalStateException("No approved license ids in the configuration");
		}
		return new Module() {
			@Override
			public void configure(Binder pBinder) {
				pBinder.bind(ILifecycleController.class).to(DefaultLifecycleController.class).in(Scopes.SINGLETON);
				pBinder.bind(IQLinEngine.class).to(DefaultQLinEngine.class);
				pBinder.bind(QLinConfiguration.class).toProvider(() -> getConfiguration());
				pBinder.bind(IQLinReportWriter.class).to(DefaultQLinReportWriter.class);
				final Types.Type<ITemplateEngine<Map<String,Object>>> templateEngineType = new Types.Type<ITemplateEngine<Map<String,Object>>>() {
				};
				pBinder.bind(templateEngineType).to(DefaultTemplateEngine.class);
				pBinder.bind(new Types.Type<List<IArchiveHandler>>() {}).toInstance(archiveHandlers);
				pBinder.bind(new Types.Type<List<IMatcher>>() {}).toInstance(allMatchers);
				pBinder.bind(ClassLoader.class).toInstance(getClassLoader());
				for (ILicense l : licenses) {
					pBinder.bind(ILicense.class, l.getId()).toInstance(l);
				}
				for (ILicenseFamily f : licenseFamilies) {
					pBinder.bind(ILicenseFamily.class, f.getId()).toInstance(f);
				}
				final Types.Type<Set<String>> setStringType = new Types.Type<Set<String>>() {};
				pBinder.bind(setStringType, "notice").toInstance(noticeLicenseIds);
				pBinder.bind(setStringType, "binary").toInstance(binaryLicenseIds);
				pBinder.bind(setStringType, "generated").toInstance(generatedLicenseIds);
				pBinder.bind(setStringType, "license").toInstance(approvedLicenseIds);
				pBinder.bind(setStringType, "family").toInstance(approvedLicenseFamilyIds);
				final Types.Type<List<IExclusionList>> exclusionListType = new Types.Type<List<IExclusionList>>() {};
				pBinder.bind(exclusionListType).toInstance(exclusionLists);
			}
		};
	}

	protected Set<String> asSet(String[] pArray) {
		if (pArray == null) {
			return Collections.emptySet();
		} else {
			return new HashSet<String>(Arrays.asList(pArray));
		}
	}

	protected <O> List<O> asPluginList(List<PluginInfo> pList) {
		return pList.stream().map((pi -> {
			final O o = asPlugin(pi);
			return o;
		})).collect(Collectors.toList());
	}

	protected <O> O asPlugin(PluginInfo pPluginInfo) {
		final Class<?> cl;
		String className = pPluginInfo.getClassName();
		if (className == null  ||  className.length() == 0) {
			switch (pPluginInfo.getType()) {
			case family:
				cl = DefaultLicenseFamily.class;
				break;
			case license:
				cl = DefaultLicense.class;
				break;
			case exclusion:
				cl = DefaultExclusionList.class;
				break;
			case matcher:
				throw new IllegalStateException("No default implementation available for matchers, id=" + pPluginInfo.getId());
			case archive:
				throw new IllegalStateException("No default implementation available for archive handlers, id=" + pPluginInfo.getId());
			default:
				throw new IllegalStateException("Invalid plugin type: " + pPluginInfo.getType());
			}
		} else {
			final ClassLoader cLoader = getClassLoader();
			try {
				cl = cLoader.loadClass(className);
			} catch (Throwable t) {
				throw Exceptions.show(t);
			}
		}
		final O instance;
		try {
			@SuppressWarnings("unchecked")
			final O o = (O) cl.newInstance();
			instance = o;
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
		setProperty(instance, "id", pPluginInfo.getId());
		setProperty(instance, "name", pPluginInfo.getName());
		setProperty(instance, "familyId", pPluginInfo.getFamilyId());
		setProperty(instance, "description", pPluginInfo.getDescription());
		for (Map.Entry<String,String> en : pPluginInfo.getProperties().entrySet()) {
			setProperty(instance, en.getKey(), en.getValue());
		}
		if (instance instanceof Initializable) {
			((Initializable) instance).initialized();
		}
		return instance;
	}

	protected void setProperty(Object pObject, String pName, String pValue) {
		if (pValue != null) {
			final String setterName = Names.upperCased("set", pName);
			for (Method m : pObject.getClass().getMethods()) {
				if (setterName.equals(m.getName())) {
					if (m.getParameterCount() == 1) {
						final Class<?> returnType = m.getReturnType();
						if (returnType == null  ||  Void.TYPE.equals(returnType)) {
							if (!Modifier.isStatic(m.getModifiers())) {
								if (!Modifier.isAbstract(m.getModifiers())) {
									if (Modifier.isPublic(m.getModifiers())) {
										final Class<?> argType = m.getParameterTypes()[0];
										final Function<String,String> errorMessage = (v) -> {
											return "Invalid property value for method " + m.getName()
											    + " in " + m.getDeclaringClass().getName() + ": " + v;
										};
										Object value = asValue(pValue, errorMessage, argType);
										try {
											m.invoke(pObject, value);
										} catch (Throwable t) {
											throw Exceptions.show(t);
										}
										return;
									}
								}
							}
						}
					}
				}
			}
			throw new IllegalStateException("No setter found for property " + pName + " in class " + pObject.getClass());
		}
	}

	private Object asValue(String pValue, Function<String,String> pErrorMessage, final Class<?> pType) {
		if (pType.isArray()) {
			final Class<?> componentType = pType.getComponentType();
			final List<Object> list = new ArrayList<>();
			String value = pValue.trim();
			int offset = value.indexOf('\n');
			while (offset != -1) {
				final String v = value.substring(0, offset);
				value = value.substring(offset+1);
				list.add(asAtomicValue(v, pErrorMessage, componentType));
				offset = value.indexOf('\n');
			}
			list.add(asAtomicValue(value, pErrorMessage, componentType));
			final Object[] array = (Object[]) Array.newInstance(componentType, list.size());
			for (int i = 0;  i < array.length;  i++) {
				array[i] = list.get(i);
			}
			return array;
		} else {
			return asAtomicValue(pValue, pErrorMessage, pType);
		}
	}
	private Object asAtomicValue(String pValue, Function<String,String> pErrorMessage, final Class<?> pType) {
		String pValueStr = pValue.trim();
		Object value = null;
		if (String.class.equals(pType)) {
			value = pValueStr;
		} else if (Long.class.equals(pType)  ||  Long.TYPE.equals(pType)) {
			try {
				value = Long.valueOf(pValueStr);
			} catch (Throwable t) {
				value = null;
			}
		} else if (Integer.class.equals(pType)  ||  Integer.TYPE.equals(pType)) {
			try {
				value = Integer.valueOf(pValueStr);
			} catch (Throwable t) {
				value = null;
			}
		} else if (Short.class.equals(pType)  ||  Short.TYPE.equals(pType)) {
			try {
				value = Short.valueOf(pValueStr);
			} catch (Throwable t) {
				value = null;
			}
		} else if (Byte.class.equals(pType)  ||  Byte.TYPE.equals(pType)) {
			try {
				value = Byte.valueOf(pValueStr);
			} catch (Throwable t) {
				value = null;
			}
		} else if (Double.class.equals(pType)  ||  Double.TYPE.equals(pType)) {
			try {
				value = Double.valueOf(pValueStr);
			} catch (Throwable t) {
				value = null;
			}
		} else if (Float.class.equals(pType)  ||  Float.TYPE.equals(pType)) {
			try {
				value = Float.valueOf(pValueStr);
			} catch (Throwable t) {
				value = null;
			}
		} else if (Boolean.class.equals(pType)  ||  Boolean.TYPE.equals(pType)) {
			try {
				value = Boolean.valueOf(pValueStr);
			} catch (Throwable t) {
				value = null;
			}
		} else {
			throw new IllegalStateException("Invalid property type for plugin configuration: " + pType.getName());
		}
		if (value == null) {
			throw new IllegalStateException(pErrorMessage.apply(pValueStr));
		}
		return value;
	}

	@Override
	public ClassLoader getClassLoader() {
		return classLoader;
	}

	@Override
	public QLinEngineBuilder classLoader(ClassLoader pClassLoader) {
		assertMutable();
		classLoader = pClassLoader;
		return self();
	}

	@Override
	public List<Module> getModules() {
		return modules;
	}

	@Override
	protected IQLinEngine newInstance() {
		if (componentFactory == null) {
			if (componentFactoryBuilder == null) {
				componentFactoryBuilder = newComponentFactoryBuilder();
			}
			if (classLoader == null) {
				classLoader = newClassLoader();
			}
			if (pluginRegistry == null) {
				pluginRegistry = newPluginRegistry();
			}
			componentFactory = componentFactoryBuilder.module(newDefaultModule()).modules(getModules()).build();
			componentFactory.requireInstance(ILifecycleController.class).start();
		}
		final Map<String,IExclusionList> exclusionListMap = getExclusionListMap();
		final List<String> excList = new ArrayList<>();
		for (IExclusionList exc : exclusionListMap.values()) {
			excList.addAll(Arrays.asList(exc.getExclusions()));
		}
		for (FileSet fs : configuration.getFileSets()) {
			final String[] excludes = fs.getExcludes();
			final List<String> excludeList;
			if (excludes == null  ||  excludes.length == 0) {
				excludeList = excList;
			} else {
				excludeList = new ArrayList<>();
				excludeList.addAll(Arrays.asList(excludes));
				excludeList.addAll(excList);
			}
			final String[] fsExcludes = excludeList.toArray(new String[excludeList.size()]);
			fileSet(Objects.requireNonNull(Paths.get(fs.getDir())), fs.getIncludes(), fsExcludes, fs.isCaseSensitive(), fs.isScanningArchives(), fs.getCharSet());
		}
		return componentFactory.requireInstance(IQLinEngine.class);
	}

	protected Map<String,IExclusionList> getExclusionListMap() {
		final Map<String,IExclusionList> map = new HashMap<>();
		final Types.Type<List<IExclusionList>> type = new Types.Type<List<IExclusionList>>() {};
		final List<IExclusionList> list = getComponentFactory().requireInstance(type);
		for (IExclusionList exc : list) {
			map.put(exc.getId(), exc);
		}
		final String[] ids = getConfiguration().getDefaultExcludes();
		Map<String,IExclusionList> exclusions = new HashMap<>();
		for (String id : ids) {
			if ("all".equals(id)) {
				exclusions.putAll(map);
			} else if (!"none".equals(id)) {
				final IExclusionList exc = map.get(id);
				if (exc == null) {
					throw new IllegalStateException("Unknown exclusion list requested in configuration/defaultExcludes: " + id);
				}
			}
		}
		return exclusions;
	}
	
	@Override
	public IQLinEngineBuilder resourceSet(IQLinResourceSet pResourceSet) {
		Objects.requireNonNull(pResourceSet, "Resource set");
		assertMutable();
		resourceSets.add(pResourceSet);
		return this;
	}

	@Override
	public IQLinEngineBuilder fileSet(@Nonnull Path pDir, String[] pIncludes, String[] pExcludes, boolean pCaseSensitive,
			                          boolean pScanningArchives, Charset pCharset) {
		Objects.requireNonNull(pDir, "Base directory");
		if (!Files.isDirectory(pDir)) {
			throw new IllegalArgumentException("The base directory " + pDir + " does not exist, or is no directory.");
		}
		return resourceSet(new com.github.jochenw.qse.lin.core.impl.FileSet(pDir, pIncludes, pExcludes, pCaseSensitive, pScanningArchives, pCharset));
	}
	
	@Override
	public IQLinEngineBuilder module(Module pModule) {
		assertMutable();
		modules.add(pModule);
		return self();
	}

	@Override
	public IQLinEngineBuilder configuration(QLinConfiguration pConfiguration) {
		Objects.requireNonNull(pConfiguration, "Configuration");
		assertMutable();
		configuration = pConfiguration;
		return self();
	}

	@Override
	public QLinConfiguration getConfiguration() {
		return configuration;
	}

	@Override
	public IPluginRegistry getPluginRegistry() {
		return pluginRegistry;
	}

	@Override
	public IQLinEngineBuilder pluginRegistry(IPluginRegistry pPluginRegistry) {
		Objects.requireNonNull(pPluginRegistry, "PluginRegistry");
		assertMutable();
		pluginRegistry = pPluginRegistry;
		return self();
	}

	protected IPluginRegistry newPluginRegistry() {
		final XmlPluginRegistry xpr = new XmlPluginRegistry(getClassLoader(), "META-INF/qse/lin/qse-lin-plugins.xml");
		xpr.init();
		return xpr;
	}

	protected ClassLoader newClassLoader() {
		return Thread.currentThread().getContextClassLoader();
	}
}
