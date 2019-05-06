package com.github.jochenw.qse.is.ant;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;

import com.github.jochenw.qse.is.core.api.AbstractLogger;

public class AntLogger extends  AbstractLogger {
	private final Project project;

	public AntLogger(Project pProject) {
		project = pProject;
	}

	public void close() throws Exception {
		// Does nothing
	}

	public void trace(String pMsg, Object... pArgs) {
		if (isTraceEnabled()) {
			final StringBuilder sb = new StringBuilder();
			write(sb, pMsg, pArgs);
			project.log(sb.toString(), Project.MSG_DEBUG);
		}
	}

	public void debug(String pMsg, Object... pArgs) {
		if (isDebugEnabled()) {
			final StringBuilder sb = new StringBuilder();
			write(sb, pMsg, pArgs);
			project.log(sb.toString(), Project.MSG_VERBOSE);
		}
	}

	public void info(String pMsg, Object... pArgs) {
		if (isInfoEnabled()) {
			final StringBuilder sb = new StringBuilder();
			write(sb, pMsg, pArgs);
			project.log(sb.toString(), Project.MSG_INFO);
		}
	}

	public void warn(String pMsg, Object... pArgs) {
		if (isWarnEnabled()) {
			final StringBuilder sb = new StringBuilder();
			write(sb, pMsg, pArgs);
			project.log(sb.toString(), Project.MSG_WARN);
		}
	}

	public void error(String pMsg, Object... pArgs) {
		if (isErrorEnabled()) {
			final StringBuilder sb = new StringBuilder();
			write(sb, pMsg, pArgs);
			project.log(sb.toString(), Project.MSG_ERR);
		}
	}

	public void fatal(String pMsg, Object... pArgs) {
		if (isFatalEnabled()) {
			final StringBuilder sb = new StringBuilder();
			write(sb, pMsg, pArgs);
			project.log(sb.toString(), Project.MSG_ERR);
		}
	}

	@Override
	protected Appendable getAppendable() {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	protected void newLine() {
		throw new IllegalStateException("Not implemented");
	}

	@Override
	protected void level(String pLevel) {
		throw new IllegalStateException("Not implemented");
	}
}
