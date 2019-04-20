package com.github.jochenw.qse.is.core.api;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class FileLogger extends AbstractLogger {
	private final BufferedWriter bw;
	private final long startTime;

	public FileLogger(Path pOutputFile) {
		startTime = System.currentTimeMillis();
		try {
			bw = Files.newBufferedWriter(pOutputFile);
			info("Logging is starting at {}", DateTimeFormatter.ISO_DATE_TIME.format(ZonedDateTime.now()));
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	public void close() throws Exception {
		bw.close();
	}

	@Override
	protected Appendable getAppendable() {
		return bw;
	}

	@Override
	protected void level(String pLevel) {
		try {
			bw.write(String.valueOf(System.currentTimeMillis()-startTime));
			bw.write(' ');
			bw.write(pLevel);
			bw.write(' ');
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	@Override
	protected void newLine() {
		try {
			bw.newLine();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
