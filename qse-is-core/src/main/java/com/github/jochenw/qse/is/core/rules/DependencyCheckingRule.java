package com.github.jochenw.qse.is.core.rules;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.afw.core.util.Strings;
import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.FlowConsumer;
import com.github.jochenw.qse.is.core.api.IServiceInvocationListener;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.NSName;
import com.github.jochenw.qse.is.core.rules.PackageScannerRule.IsPackageListener;
import com.github.jochenw.qse.is.core.rules.RulesParser.Rule;

public class DependencyCheckingRule extends AbstractRule {
	public static class DependencySpec {
		@SuppressWarnings("unused")
		private final String specificationString, packageNamesStr;
		private final String[] packageNames;
		private final Pattern namespaceRe;

		public DependencySpec(String pSpecificationString) {
			if (pSpecificationString == null) {
				throw new NullPointerException("A dependency specification must not be null.");
			}
			final int offset = pSpecificationString.indexOf(':');
			if (offset <= 0) {
				throw new IllegalArgumentException("Invalid dependency specification: " + pSpecificationString);
			}
			packageNamesStr = pSpecificationString.substring(0, offset);
			packageNames = packageNamesStr.split("[,\\|]");
			for (String packageName : packageNames) {
				if (packageName.length() == 0) {
					throw new IllegalArgumentException("Invalid dependency specification (Empty package name): " + pSpecificationString);
				}
			}
			final String namepaceReStr = pSpecificationString.substring(offset+1);
			try {
				namespaceRe = Pattern.compile(namepaceReStr);
			} catch (PatternSyntaxException pse) {
				throw new IllegalArgumentException("Invalid dependency specification (Namespace syntax): " + pSpecificationString);
			}
			specificationString = pSpecificationString;
		}

		public boolean matches(String pServiceName) {
			return namespaceRe.matcher(pServiceName).matches();
		}

		public String[] getPackageNames() {
			return packageNames;
		}

		public String getPackageNamesStr() {
			return packageNamesStr;
		}
	}

	private DependencySpec[] dependencySpecifications;
	
	@Override
	public void init(Rule pParserRule) {
		super.init(pParserRule);
		final String[] dependencySpecificationStrings = pParserRule.getProperty("dependencySpecifications");
		if (dependencySpecificationStrings == null  ||  dependencySpecificationStrings.length == 0) {
			throw new IllegalStateException("Missing plugin property: dependencySpecifications");
		}
		dependencySpecifications = new DependencySpec[dependencySpecificationStrings.length];
		for (int i = 0;  i < dependencySpecifications.length;  i++) {
			final String dependencySpecificationStr = dependencySpecificationStrings[i];
			dependencySpecifications[i] = new DependencySpec(dependencySpecificationStr);
		}
	}

	private final Map<String,Set<String>> warnedDependencies = new HashMap<>();
	
	@Override
	protected void accept(IPluginRegistry pRegistry) {
		pRegistry.addPlugin(IsPackageListener.class, new IsPackageListener() {
			@Override
			public void packageStopping(IsPackage pPkg) {
				for (Map.Entry<String,Set<String>> en : warnedDependencies.entrySet()) {
					final String packageNamesStr = en.getKey();
					final Set<String> serviceNames = en.getValue();
					final ServiceListDescription sld = getServiceListDescription(serviceNames);
					
					final String msg = "The flow service(s) " + sld.getServiceNameListDescription() + " in package " + pPkg.getName()
							+ " seem to be referencing either of the following packages, none of which is declared as a dependency: "
							+ packageNamesStr;
					issue(pPkg, sld.getFirstService(), ErrorCodes.DEPENDENCY_MISSING,
						  msg);
				}
			}
			
			@Override
			public void packageStarting(IsPackage pPackage) {
				System.out.println("packageStarting: " + pPackage.getName());
				warnedDependencies.clear();
			}
		});
		pRegistry.addPlugin(FlowConsumer.class, new FlowConsumer() {
			@Override
			public IServiceInvocationListener getServiceInvocationListener(Context pContext) {
				return new IServiceInvocationListener() {
					@Override
					public void serviceInvocation(IsPackage pPackage, NSName pSource, NSName pTarget) {
						final String targetServiceName = pTarget.getQName();
						for (DependencySpec dependencySpec : dependencySpecifications) {
							if (dependencySpec.matches(targetServiceName)) {
								boolean hasDependency = false;
								final IsPackage pkg = pContext.getPackage();
								for (String packageName : dependencySpec.getPackageNames()) {
									if (pkg.hasDependency(packageName)) {
										hasDependency = true;
										break;
									}
								}
								System.out.println("serviceInvocation: " + pSource.getQName() + " ->" + pTarget.getQName() + ", " + Strings.toString(warnedDependencies));
								final String packageNamesStr = dependencySpec.getPackageNamesStr();
								if (!hasDependency) {
									Set<String> sourceServices = warnedDependencies.get(packageNamesStr);
									if (sourceServices == null) {
										sourceServices = new HashSet<String>();
										warnedDependencies.put(packageNamesStr, sourceServices);
									}
									sourceServices.add(pSource.getQName());
								}
							}
						}
					}
				};
			}

			@Override
			public void accept(Context pContext) {
			}
		});
	}
}
