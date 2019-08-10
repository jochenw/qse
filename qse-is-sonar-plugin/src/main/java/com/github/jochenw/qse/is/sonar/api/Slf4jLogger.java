package com.github.jochenw.qse.is.sonar.api;

import org.slf4j.LoggerFactory;

import com.github.jochenw.qse.is.core.api.Logger;

public class Slf4jLogger implements Logger {
	private static final org.slf4j.Logger slf4jLogger = LoggerFactory.getLogger("qse.is");

	@Override
	public void close() throws Exception {
		// Does nothing
	}

	@Override
	public void trace(String pMsg, Object... pArgs) {
		slf4jLogger.trace(pMsg, pArgs);
	}

	@Override
	public void debug(String pMsg, Object... pArgs) {
		slf4jLogger.debug(pMsg, pArgs);
	}

	@Override
	public void info(String pMsg, Object... pArgs) {
		slf4jLogger.info(pMsg, pArgs);
	}

	@Override
	public void warn(String pMsg, Object... pArgs) {
		slf4jLogger.warn(pMsg, pArgs);
	}

	@Override
	public void error(String pMsg, Object... pArgs) {
		slf4jLogger.error(pMsg, pArgs);
	}

	@Override
	public void fatal(String pMsg, Object... pArgs) {
		slf4jLogger.error("<FATAL> " + pMsg, pArgs);
	}
}
