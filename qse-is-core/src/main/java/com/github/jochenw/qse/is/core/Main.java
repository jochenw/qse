package com.github.jochenw.qse.is.core;

import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

import com.github.jochenw.afw.core.cli.Args;
import com.github.jochenw.afw.core.cli.Args.Context;
import com.github.jochenw.qse.is.core.api.FileLogger;
import com.github.jochenw.qse.is.core.api.Logger;
import com.github.jochenw.qse.is.core.api.PrintStreamLogger;

public class Main {
	public static class OptionsBean {
		Path rulesFile, logFile, outFile, scanDir;
		int maxNumberOfErrors, maxNumberOfWarnings, maxNumberOfOtherIssues;
	}

	public static OptionsBean parse(String[] pArgs) {
		final OptionsBean options = new OptionsBean();
		final Args.Listener listener = new Args.Listener() {
			@Override
			public void option(Context pCtx, String pName) {
				switch (pName) {
				case "scanDir":
					options.scanDir = pCtx.getSinglePathValue((p) -> Files.isDirectory(p),
							"Invalid argument for option " + pName + ": " + options.scanDir
						    + " (Doesn't exist, or is not a directory)");
					break;
				case "outFile":
					options.outFile = pCtx.getSinglePathValue();
					break;
				case "logFile":
					options.logFile = pCtx.getSinglePathValue();
					break;
				case "rulesFile":
					options.rulesFile = pCtx.getSinglePathValue();
					break;
				case "maxNumberOfErrors":
					options.maxNumberOfErrors = pCtx.getSingleIntValue((i) -> i >= 0, "Invalid argument for option "
						+ pName + ": Integer value must be >= 0");
					break;
				case "maxNumberOfWarnings":
					options.maxNumberOfWarnings = pCtx.getSingleIntValue((i) -> i >= 0, "Invalid argument for option "
							+ pName + ": Integer value must be >= 0");
					break;
				case "maxNumberOfOtherIssues":
					options.maxNumberOfOtherIssues = pCtx.getSingleIntValue((i) -> i >= 0, "Invalid argument for option "
							+ pName + ": Integer value must be >= 0");
					break;
				}
			}
		};
		Args.parse(listener, pArgs);
		return options;
	}

	public static void main(String[] pArgs) throws Exception {
		final OptionsBean options = parse(pArgs);
		final Path rulesFile = options.rulesFile;
		final Path logFile = options.logFile;
		final Path outFile = options.outFile;
		Path scanDir = options.scanDir;
		final int maxNumberOfErrors = options.maxNumberOfErrors;
		final int maxNumberOfWarnings = options.maxNumberOfWarnings;
		final int maxNumberOfOtherIssues = options.maxNumberOfOtherIssues;
		if (scanDir == null) {
			scanDir = FileSystems.getDefault().getPath(".");
		} else if (!Files.isDirectory(scanDir)) {
			throw Usage("Scan directory (as given by option 'scanDir') does not exist: " + scanDir);
		}
		if (!Files.isRegularFile(rulesFile)) {
			throw Usage("Rules file (as given by option 'rulesFile') does not exist, or is not a file: " + rulesFile);
		}
		try (final Logger logger = getLogger(logFile)) {
			final Scanner.Result scannerResult = scan(scanDir, outFile, rulesFile, logger);
			if (maxNumberOfErrors != -1  &&  maxNumberOfErrors > scannerResult.getNumberOfErrors()) {
				String msg = "Number of errors (" + scannerResult.getNumberOfErrors()
				                         + ") exceeds permitted maximum number (" + maxNumberOfErrors + ")";
				logger.error(msg);
				System.exit(1);
			}
			if (maxNumberOfWarnings != -1  &&  maxNumberOfWarnings > scannerResult.getNumberOfWarnings()) {
				logger.error("Number of warnings (" + scannerResult.getNumberOfWarnings()
				                         + ") exceeds permitted maximum number (" + maxNumberOfWarnings + ")");
				System.exit(1);
			}
			if (maxNumberOfOtherIssues != -1  &&  maxNumberOfOtherIssues > scannerResult.getNumberOfOtherIssues()) {
				logger.error("Number of other issues (" + scannerResult.getNumberOfOtherIssues()
				                         + ") exceeds permitted maximum number (" + maxNumberOfOtherIssues + ")");
				System.exit(1);
			}
		}
	}

	public static Scanner.Result scan(Path pScanDir, Path pOutputFile, Path pRulesFile, Logger pLogger) {
		final long startTime = System.currentTimeMillis();
		System.out.println("Scanning directory: " + pScanDir);
		final Scanner.Result result = Scanner.scan(pScanDir, pOutputFile, pRulesFile, pLogger);
		System.out.println("Errors: " + result.getNumberOfErrors() + ", warnings: " + result.getNumberOfWarnings() + ", other issues:" + result.getNumberOfOtherIssues());
		System.out.println("Scan time: " + (System.currentTimeMillis()-startTime) + " millis");
		return result;
	}

	private static Logger getLogger(Path pLogFile) {
		if (pLogFile == null) {
			return new PrintStreamLogger(System.err);
		} else {
			return new FileLogger(pLogFile);
		}
	}
	
	public static final RuntimeException Usage(String pMsg) {
		final PrintStream ps = System.err;
		if (pMsg != null) {
			ps.println(pMsg);
			ps.println();
		}
		ps.println("Usage: java " + Main.class.getName() + " <OPTIONS>");
		ps.println();
		ps.println("Required options are:");
		ps.println("  -rulesFile <FILE>  Sets the rules file");
		ps.println("Possible options are:");
		ps.println("  -scanDir <DIR>     Sets the scan directory (Default: Current Directory)");
		ps.println("  -logFile <FILE>    Sets the log file (Default: STDERR)");
		ps.println("  -outFile <FILE>    Sets the output file (Default: STDOUT)");
		ps.println("  -h|-help           Prints this help message, and exits with error status");
		System.exit(1);
		return new RuntimeException(pMsg);
	}
}
