package com.github.jochenw.qse.is.core.rules;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
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

	private final Set<String> warnedDependencies = new HashSet<>();
	
	@Override
	protected void accept(IPluginRegistry pRegistry) {
		pRegistry.addPlugin(IsPackageListener.class, new IsPackageListener() {
			@Override
			public void packageStopping() {
				// Does nothing;
			}
			
			@Override
			public void packageStarting(IsPackage pPackage) {
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
								final String packageNamesStr = dependencySpec.getPackageNamesStr();
								if (!hasDependency  &&  !warnedDependencies.contains(packageNamesStr)) {
									warnedDependencies.add(packageNamesStr);
									final String msg = "The flow service "
											+ pContext.getNode().getName().getQName() + " in package " + pkg.getName()
											+ " invokes the service " + pTarget.getQName()
											+ ", but neither of the following packages is declared as a dependency: "
											+ packageNamesStr;
									issue(pkg, pContext.getNode().getName().getQName(), ErrorCodes.DEPENDENCY_MISSING,
										  msg);
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
