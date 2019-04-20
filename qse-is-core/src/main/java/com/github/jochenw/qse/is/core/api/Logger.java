package com.github.jochenw.qse.is.core.api;

public interface Logger extends AutoCloseable {
	public void trace(String pMsg, Object... pArgs);
	public void debug(String pMsg, Object... pArgs);
	public void info(String pMsg, Object... pArgs);
	public void warn(String pMsg, Object... pArgs);
	public void error(String pMsg, Object... pArgs);
	public void fatal(String pMsg, Object... pArgs);
}
