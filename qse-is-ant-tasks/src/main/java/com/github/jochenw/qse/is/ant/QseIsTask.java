package com.github.jochenw.qse.is.ant;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.api.FileLogger;
import com.github.jochenw.qse.is.core.api.Logger;
import com.github.jochenw.qse.is.core.api.PrintStreamLogger;

public class QseIsTask extends Task {
	private File scanDir, outFile, logFile, rulesFile;
	private int maxNumberOfErrors = -1, maxNumberOfWarnings = -1, maxNumberOfOtherIssues = -1;

	public int getMaxNumberOfErrors() {
		return maxNumberOfErrors;
	}

	public void setMaxNumberOfErrors(int maxNumberOfErrors) {
		this.maxNumberOfErrors = maxNumberOfErrors;
	}

	public int getMaxNumberOfWarnings() {
		return maxNumberOfWarnings;
	}

	public void setMaxNumberOfWarnings(int maxNumberOfWarnings) {
		this.maxNumberOfWarnings = maxNumberOfWarnings;
	}

	public int getMaxNumberOfOtherIssues() {
		return maxNumberOfOtherIssues;
	}

	public void setMaxNumberOfOtherIssues(int maxNumberOfOtherIssues) {
		this.maxNumberOfOtherIssues = maxNumberOfOtherIssues;
	}

	public File getScanDir() {
		return scanDir;
	}

	public void setScanDir(File scanDir) {
		this.scanDir = scanDir;
	}

	public File getOutFile() {
		return outFile;
	}

	public void setOutFile(File outFile) {
		this.outFile = outFile;
	}

	public File getLogFile() {
		return logFile;
	}

	public void setLogFile(File logFile) {
		this.logFile = logFile;
	}

	public File getRulesFile() {
		return rulesFile;
	}

	public void setRulesFile(File rulesFile) {
		this.rulesFile = rulesFile;
	}

	@Override
	public void execute() throws BuildException {
		if (rulesFile != null  &&  (!rulesFile.isFile()  ||  !rulesFile.canRead())) {
			throw new BuildException("Rules file does not exist, is unreadable, or not a file: " + rulesFile);
		}
		if (logFile != null) {
			final File logDir = logFile.getParentFile();
			if (logDir != null  &&  (!logDir.isDirectory()  ||  !logDir.canWrite())) {
				throw new BuildException("Directory for log file does not exist, is not a directory, or not writable: " + logDir);
			}
		}
		if (outFile != null) {
			final File outDir = outFile.getParentFile();
			if (outDir != null  &&  (!outDir.isDirectory()  ||  !outDir.canWrite())) {
				throw new BuildException("Directory for output file does not exist, is not a directory, or not writable: " + outDir);
			}
		}
		final File dir;
		if (scanDir == null) {
			dir = FileSystems.getDefault().getPath(".").toAbsolutePath().toFile();
		} else {
			if (!scanDir.isDirectory()  ||  !scanDir.canRead()) {
				throw new BuildException("Scan directory does not exist, or is not a directory: " + scanDir);
			}
			dir = scanDir;
		}
		try (final Logger logger = getLogger()) {
			final Path scanDirPath = dir.toPath();
			final Path outFilePath = outFile == null ? null : outFile.toPath();
			final Path rulesFilePath = rulesFile == null ? null : rulesFile.toPath();
			final Scanner.Result scannerResult = Scanner.scan(scanDirPath, outFilePath, rulesFilePath, logger);
			if (maxNumberOfErrors != -1  &&  maxNumberOfErrors > scannerResult.getNumberOfErrors()) {
				throw new BuildException("Number of errors (" + scannerResult.getNumberOfErrors()
				                         + ") exceeds permitted maximum number (" + maxNumberOfErrors + ")");
			}
			if (maxNumberOfWarnings != -1  &&  maxNumberOfWarnings > scannerResult.getNumberOfWarnings()) {
				throw new BuildException("Number of warnings (" + scannerResult.getNumberOfWarnings()
				                         + ") exceeds permitted maximum number (" + maxNumberOfWarnings + ")");
			}
			if (maxNumberOfOtherIssues != -1  &&  maxNumberOfOtherIssues > scannerResult.getNumberOfOtherIssues()) {
				throw new BuildException("Number of other issues (" + scannerResult.getNumberOfOtherIssues()
				                         + ") exceeds permitted maximum number (" + maxNumberOfOtherIssues + ")");
			}
		} catch (Exception e) {
			throw Exceptions.show(e);
		}
	}

	private Logger getLogger() {
		if (logFile == null) {
			return new PrintStreamLogger(System.err);
		} else {
			return new FileLogger(logFile.toPath());
		}
	}
}
