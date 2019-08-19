package com.github.jochenw.qse.is.sonar.api;

import static com.github.jochenw.qse.is.sonar.api.SonarQseIsConstants.*;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.config.Configuration;

import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.ScannerBuilder;
import com.github.jochenw.qse.is.core.api.FileLogger;
import com.github.jochenw.qse.is.core.api.Rule;
import com.github.jochenw.qse.is.core.scan.IWorkspaceScanner;
import com.github.jochenw.qse.is.core.scan.NullWorkspaceScanner;
import com.github.jochenw.qse.is.core.scan.SonarWorkspaceScanner;


public class SonarQseIsScannerProvider {
	private static final Logger log = LoggerFactory.getLogger(SonarQseIsScannerProvider.class);
	private final Configuration configuration;

	public SonarQseIsScannerProvider(Configuration pConfiguration) {
		configuration = pConfiguration;
	}

	public List<Rule> getRules() {
		final Scanner scanner = new ScannerBuilder(getRulesFile()).logger(com.github.jochenw.qse.is.core.api.Logger.getNullLogger()).workspaceScanner(new NullWorkspaceScanner()).build();
		return scanner.getRules();
	}
	
	protected Scanner createScanner(Configuration pConfiguration, List<File> pFiles) {
		log.debug("createScanner: ->");
		final com.github.jochenw.qse.is.core.api.Logger logger = getLogger(configuration);
		log.debug("createScanner: Created Logger {}", logger.getClass().getName());
		final Path rulesFile = getRulesFile();
		final IWorkspaceScanner wsScanner = new SonarWorkspaceScanner(() -> pFiles);
		final Scanner scan = new ScannerBuilder(rulesFile).logger(logger).workspaceScanner(wsScanner).build();
		log.debug("createScanner: <- " + scan);
		return scan;
	}

	protected com.github.jochenw.qse.is.core.api.Logger getLogger(final Configuration pConfiguration) {
		final com.github.jochenw.qse.is.core.api.Logger logger;
		final Optional<String> logFileOptional = pConfiguration.get(PROPERTY_SONAR_QSE_IS_LOG_FILE);
		if (logFileOptional.isPresent()) {
			final String logFileStr = logFileOptional.get();
			if (logFileStr.length() == 0) {
				logger = new Slf4jLogger();
			} else {
				final Path logFilePath = Paths.get(logFileStr);
				final Path logFileDir = logFilePath.getParent();
				if (logFileDir != null) {
					if (!Files.isDirectory(logFileDir)) {
						log.info("createScanner: Creating log file directory: " + logFileDir);
						try {
							Files.createDirectories(logFileDir);
						} catch (IOException e) {
							throw new UncheckedIOException(e);
						}
					} else {
						log.debug("createScanner: Using existing log file directory: " + logFileDir);
					}
				}
				logger = new FileLogger(logFilePath);
			}
		} else {
			logger = new Slf4jLogger();
		}
		return logger;
	}

	protected Path getRulesFile() {
		final Optional<String> rulesFileOptional = configuration.get(PROPERTY_SONAR_QSE_IS_RULES_FILE);
		if (rulesFileOptional.isPresent()) {
			final Path rulesFile = Paths.get(rulesFileOptional.get());
			if (!Files.isReadable(rulesFile)) {
				throw new IllegalArgumentException("The specified rules file doesn't exist, or is unreadable: " + rulesFile);
			}
			return rulesFile;
		} else {
			return null;
		}
	}
}
