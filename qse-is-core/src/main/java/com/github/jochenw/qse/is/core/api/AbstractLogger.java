package com.github.jochenw.qse.is.core.api;

import java.io.IOException;
import java.io.UncheckedIOException;

public abstract class AbstractLogger implements Logger {
	public void write(Appendable pAppendable, String pMsg, Object... pArgs) {
		try {
			int offset = 0;
			int argOffset = 0;
			while (offset < pMsg.length()) {
				if (offset < pMsg.length()-1  &&  pMsg.charAt(offset) == '{'  &&  pMsg.charAt(offset+1) == '}') {
					if (argOffset < pArgs.length) {
						final Object o = pArgs[argOffset++];
						pAppendable.append(String.valueOf(o));
					} else {
						throw new IllegalArgumentException("Insufficient number of arguments");
					}
					offset += 2;
				} else {
					pAppendable.append(pMsg.charAt(offset++));
				}
			}
			newLine();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	protected abstract Appendable getAppendable();
	protected abstract void newLine();
	
	private boolean traceEnabled = false;
	private boolean debugEnabled = false;
	private boolean infoEnabled = true;
	private boolean warnEnabled = true;
	private boolean errorEnabled = true;
	private boolean fatalEnabled = true;

	public boolean isTraceEnabled() {
		return traceEnabled;
	}

	public void setTraceEnabled(boolean traceEnabled) {
		this.traceEnabled = traceEnabled;
	}

	public boolean isDebugEnabled() {
		return debugEnabled;
	}

	public void setDebugEnabled(boolean debugEnabled) {
		this.debugEnabled = debugEnabled;
	}

	public boolean isInfoEnabled() {
		return infoEnabled;
	}

	public void setInfoEnabled(boolean infoEnabled) {
		this.infoEnabled = infoEnabled;
	}

	public boolean isWarnEnabled() {
		return warnEnabled;
	}

	public void setWarnEnabled(boolean warnEnabled) {
		this.warnEnabled = warnEnabled;
	}

	public boolean isErrorEnabled() {
		return errorEnabled;
	}

	public void setErrorEnabled(boolean errorEnabled) {
		this.errorEnabled = errorEnabled;
	}

	public boolean isFatalEnabled() {
		return fatalEnabled;
	}

	public void setFatalEnabled(boolean fatalEnabled) {
		this.fatalEnabled = fatalEnabled;
	}

	protected abstract void level(String pLevel);

	@Override
	public void trace(String pMsg, Object... pArgs) {
		if (isTraceEnabled()) {
			level("TRACE");
			write(getAppendable(), pMsg, pArgs);
		}
	}

	@Override
	public void debug(String pMsg, Object... pArgs) {
		if (isDebugEnabled()) {
			level("DEBUG");
			write(getAppendable(), pMsg, pArgs);
		}
	}

	@Override
	public void info(String pMsg, Object... pArgs) {
		if (isInfoEnabled()) {
			level("INFO");
			write(getAppendable(), pMsg, pArgs);
		}
	}

	@Override
	public void warn(String pMsg, Object... pArgs) {
		if (isWarnEnabled()) {
			level("WARN");
			write(getAppendable(), pMsg, pArgs);
		}
	}

	@Override
	public void error(String pMsg, Object... pArgs) {
		if (isErrorEnabled()) {
			level("ERROR");
			write(getAppendable(), pMsg, pArgs);
		}
	}

	@Override
	public void fatal(String pMsg, Object... pArgs) {
		if (isFatalEnabled()) {
			level("FATAL");
			write(getAppendable(), pMsg, pArgs);
			final StringBuilder sb = new StringBuilder();
			write(sb, pMsg, pArgs);
			throw new RuntimeException(sb.toString());
		}
	}
}
