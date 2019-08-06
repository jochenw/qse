package com.github.jochenw.qse.is.sonar.api;

import javax.security.auth.login.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.config.PropertyDefinition;

public class SonarQseIsPlugin implements Plugin {
	private static final Logger log = LoggerFactory.getLogger(SonarQseIsPlugin.class);

	private final Configuration configuration;

	public SonarQseIsPlugin(Configuration pConfiguration) {
		configuration = pConfiguration;
	}

	public void define(Context pContext) {
		log.debug("define: ->");
		final PropertyDefinition rulesFileProperty = PropertyDefinition
			.builder("sonar.qseIs.rulesFile").name("QSE IS rules file")
			.description("Location of the QSE IS rules file (optional, defaults to builtin rules")
			.category("QseIs")
			.build();
		final PropertyDefinition logFileProperty = PropertyDefinition
			.builder("sonar.qseIs.logFile").name("QSE IS log file")
			.description("Location of the QSE IS log file (optional, defaults to use of SLF4J")
			.category("QseIs")
			.build();
		pContext.addExtensions(rulesFileProperty, logFileProperty);
		if (pContext != null) {
			final Sensor sensor = new SonarQseIsSensor();
			pContext.addExtension(sensor);
		}
		log.debug("define: <-");
	}
}
