package com.github.jochenw.qse.is.sonar.api;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.config.Configuration;
import org.sonar.api.rule.RuleKey;

import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.api.IssueConsumer;


public class SonarQseIsSensor implements Sensor {
	private static final Logger log = LoggerFactory.getLogger(SonarQseIsSensor.class);
	private final Configuration configuration;

	public SonarQseIsSensor(Configuration pConfiguration) {
		configuration = pConfiguration;
	}
	
	@Override
	public void describe(SensorDescriptor pDescriptor) {
		log.debug("describe: ->");
		pDescriptor.global();
		pDescriptor.name("QseIsSensor");
		log.debug("describe: <-");
	}

	@SuppressWarnings("deprecation")
	private File getFile(InputFile pInputFile) {
		return pInputFile.file();
	}

	@Override
	public void execute(SensorContext pContext) {
		log.debug("execute: -> sonarQubeVersion={}", pContext.getSonarQubeVersion());
	    FileSystem fs = pContext.fileSystem();
	    Iterable<InputFile> flowFiles = fs.inputFiles(fs.predicates().all());
	    List<File> files = new ArrayList<File>();
	    Map<String,InputComponent> inputComponents = new HashMap<>();
	    for (InputFile inputFile : flowFiles) {
	    	final File f = getFile(inputFile);
	    	files.add(f);
	    	inputComponents.put(f.getPath(), inputFile);
	    }
	    final IssueConsumer ic = newIssueConsumer(pContext, inputComponents);
	    final Scanner scanner = new SonarQseIsScannerProvider(configuration).createScanner(configuration, files);
	    scanner.getWorkspace().addListener(ic);
	    scanner.run();
	    log.debug("execute: <-");
	}

	protected IssueConsumer newIssueConsumer(SensorContext pContext, Map<String,InputComponent> pInputComponents) {
		return (i) -> {
			final String uri = i.getUri();
			final InputComponent iComp = pInputComponents.get(uri);
			if (iComp == null) {
				throw new IllegalStateException("No InputComponent found for uri: " + uri);
			}
			final NewIssue issue = pContext.newIssue();
			final NewIssueLocation loc = issue.newLocation().on(iComp).message(i.getMessage());
			issue.at(loc).forRule(RuleKey.of(SonarQseIsConstants.RULE_REPOSITORY_ID,
					                          i.getRule() + ": " + i.getErrorCode()));
		};
	}
}
