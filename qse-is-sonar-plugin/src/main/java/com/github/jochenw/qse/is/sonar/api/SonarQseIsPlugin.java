package com.github.jochenw.qse.is.sonar.api;

import static com.github.jochenw.qse.is.sonar.api.SonarQseIsConstants.*;

import javax.security.auth.login.Configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.Plugin;
import org.sonar.api.batch.sensor.Sensor;
import org.sonar.api.config.PropertyDefinition;

public class SonarQseIsPlugin implements Plugin {
	private static final Logger log = LoggerFactory.getLogger(SonarQseIsPlugin.class);

	public SonarQseIsPlugin() {
	}

	public void define(Context pContext) {
		log.debug("define: ->");
		final PropertyDefinition rulesFileProperty = PropertyDefinition
			.builder(PROPERTY_SONAR_QSE_IS_RULES_FILE).name("QSE IS rules file")
			.description("Location of the QSE IS rules file (optional, defaults to builtin rules")
			.category("QseIs")
			.build();
		final PropertyDefinition logFileProperty = PropertyDefinition
			.builder(PROPERTY_SONAR_QSE_IS_LOG_FILE).name("QSE IS log file")
			.description("Location of the QSE IS log file (optional, defaults to use of SLF4J")
			.category("QseIs")
			.build();
		pContext.addExtensions(rulesFileProperty, logFileProperty, SonarQseIsScannerProvider.class,
				               SonarQseIsSensor.class);
		log.debug("define: <-");
	}
}
