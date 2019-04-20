package com.github.jochenw.qse.is.core.api;

import java.io.PrintStream;

public class PrintStreamLogger extends AbstractLogger {
	private final PrintStream ps;

	public PrintStreamLogger(PrintStream pPs) {
		ps = pPs;
	}

	@Override
	protected Appendable getAppendable() {
		return ps;
	}

	@Override
	protected void level(String pLevel) {
		ps.print(pLevel);
		ps.print(' ');
	}

	@Override
	public void close() throws Exception {
		// Do nothing, assume that the PrintStream is being closed automatically.
	}

	@Override
	protected void newLine() {
		ps.println();
	}
}
