package com.github.jochenw.qse.is.sonar.api;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputComponent;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.batch.fs.TextRange;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.batch.sensor.SensorContext;
import org.sonar.api.batch.sensor.SensorDescriptor;
import org.sonar.api.batch.sensor.issue.NewIssue;
import org.sonar.api.batch.sensor.issue.NewIssueLocation;
import org.sonar.api.batch.sensor.issue.internal.DefaultIssueLocation;
import org.sonar.api.rule.RuleKey;

import com.github.jochenw.qse.is.core.api.IssueConsumer;
import com.github.jochenw.qse.is.core.scan.IWorkspaceScanner;
import com.github.jochenw.qse.is.sonar.api.Log.ILog;
import com.github.jochenw.qse.is.sonar.api.Log.IMLog;

public class SonarQseIsSensor implements Sensor {
	private static final Logger log = LoggerFactory.getLogger(SonarQseIsSensor.class);

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
	    ArrayList<File> files = new ArrayList<File>();
	    Map<String,InputComponent> inputComponents = new HashMap<>();
	    for (InputFile inputFile : flowFiles) {
	    	final File f;
	    	files.add(f);
	    	inputComponents.put(f.getPath(), inputFile);
	    }
	    final IssueConsumer ic = (i) -> {
	    	final String uri = i.getUri();
	    	final InputComponent iComp = inputComponents.get(uri);
	    	if (iComp == null) {
	    		throw new NullPointerException("No input component for file: " + uri);
	    	}
	    	final NewIssue issue = pContext.newIssue();
	    	final NewIssueLocation loc = issue.newLocation().on(iComp).message(i.getMessage());
	    	issue.at(loc).forRule(RuleKey.of(repository, rule))
	    			
	    			
	    			
	    			
	    	final NewIssueLocation nil = DefaultIssueLocation.on(iComp)
	    			.message(i.getMessage());
				
				@Override
				public NewIssueLocation on(InputComponent component) {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public NewIssueLocation message(String message) {
					// TODO Auto-generated method stub
					return null;
				}
				
				@Override
				public NewIssueLocation at(TextRange location) {
					// TODO Auto-generated method stub
					return null;
				}
			};
	    };
	    log.debug("execute: <-");
	}

}
