package com.github.jochenw.qse.lin.maven;

import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.InstantiationStrategy;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import com.github.jochenw.afw.core.inject.ComponentFactoryBuilder.Module;
import com.github.jochenw.qse.lin.core.api.IQLinEngine;
import com.github.jochenw.qse.lin.core.api.IQLinEngineBuilder;
import com.github.jochenw.qse.lin.core.api.QLinConfiguration;
import com.github.jochenw.qse.lin.core.api.QLinReport;


@Mojo(name="check", defaultPhase=LifecyclePhase.VERIFY, instantiationStrategy=InstantiationStrategy.SINGLETON)
public class QseLinMojo extends AbstractMojo {
	@Parameter(name="skip", property="qlin.skip", defaultValue="false")
	private boolean skip;

	@Parameter(name="configurationFile", property="qlin.configFile", defaultValue="src/main/qlin/qse-lin-configuration.xml")
	private Path configurationFile;

	@Parameter(name="reportOutputFile", property="qlin.outputFile", defaultValue="${project.build.directory}/qlin/report.txt")
	private String reportOutputFile;

	@Parameter(name="reportTemplate", property="qlin.template", defaultValue="com/github/jochenw/qse/lin/core/impl/qse-lin-report-template.fm")
	private String reportTemplate;

	@Parameter(name="reportDateTimePattern", property="qlin.dateTimePattern", defaultValue="yyyy-MM-dd HH:mm:ss.SSS")
	private String reportDateTimePattern;

	@Parameter(name="reportCharset", property="qlin.report.charset", defaultValue="UTF-8")
	private String reportCharset;

	@Parameter(name="reportLocale", property="qlin.report.locale", defaultValue="en_US")
	private String reportLocale;

	@Parameter(name="maxUnknownFiles", property="qlin.maxUnknownFiles", defaultValue="0")
	private int maxUnknownFiles;

	@Parameter(name="numberOfThreads", property="qlin.numThreads", defaultValue="5")
	private int numberOfThreads;

	@Parameter(name="componentModules")
	private String[] componentModules;

	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skip) {
			getLog().info("Skip property is true, skipping.");
			return;
		}

		final IQLinEngineBuilder builder = IQLinEngine.builder();
		if (configurationFile != null) {
			if (!Files.isReadable(configurationFile)) {
				throw new MojoExecutionException("Configuration file is unreadable: " + configurationFile);
			}
			builder.configuration(configurationFile);
		}
		if (reportOutputFile != null) {
			builder.outputFile(reportOutputFile);
		}
		QLinConfiguration config = builder.getConfiguration();
		if (reportTemplate != null  &&  reportTemplate.length() > 0) {
			config.setReportTemplate(reportTemplate);
		}
		if (reportDateTimePattern != null  && reportDateTimePattern.length() > 0) {
			config.setReportDateTimePattern(reportDateTimePattern);
		}
		if (reportCharset != null  &&  reportCharset.length() > 0) {
			final Charset cset;
			try {
				cset = Charset.forName(reportCharset);
			} catch (Throwable t) {
				throw new MojoFailureException("Invalid report character set: " + reportCharset, t);
			}
			config.setReportCharset(cset);
		}
		if (reportLocale == null  &&  reportLocale.length() > 0) {
			final Locale locale;
			try {
				locale = Locale.forLanguageTag(reportLocale);
			} catch (Throwable t) {
				throw new MojoFailureException("Invalid report locale: " + reportLocale, t);
			}
			config.setReportLocale(locale);
		}
		config.setNumberOfThreads(numberOfThreads);
		if (componentModules != null) {
			for (String s : componentModules) {
				final Class<?> c;
				try {
					c = Class.forName(s);
				} catch (Throwable t) {
					throw new MojoExecutionException("Unable to load component module class: " + s, t);
				}
				if (!Module.class.isAssignableFrom(c)) {
					throw new MojoFailureException("The component module class " + c.getName() + " doesn't implement " + Module.class.getName());
				}
				final Module module;
				try {
					module = (Module) c.newInstance();
				} catch (Throwable t) {
					throw new MojoExecutionException("Unable to instantiate component module class: " + s + ", " + t.getMessage(), t);
				}
				builder.module(module);
			}
		}
		final QLinReport report = builder.scan();
		if (maxUnknownFiles > 0  &&  maxUnknownFiles < report.getNumberOfUnknownFiles()) {
			throw new MojoExecutionException("The number of files with unknown license (" + report.getNumberOfUnknownFiles()
				+ ") exceeds the permitted maximum number (" + maxUnknownFiles + ")"); 
		}
	}

}
