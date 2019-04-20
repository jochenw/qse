package com.github.jochenw.qse.is.core;

import java.io.PrintStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Function;

import com.github.jochenw.afw.cli.Options;
import com.github.jochenw.afw.cli.Options.Result;
import com.github.jochenw.qse.is.core.api.FileLogger;
import com.github.jochenw.qse.is.core.api.Logger;
import com.github.jochenw.qse.is.core.api.PrintStreamLogger;

public class Main {
	public static void main(String[] pArgs) throws Exception {
		final Options options = new Options()
				.pathOption().names("scanDir").description("Sets the scan directory (Default: Current Directory)").end()
				.pathOption().names("outFile").description("Sets the output file (Default: STDOUT)").end()
				.pathOption().names("logFile").description("Sets the log file (Default: STDERR").end()
				.pathOption().names("rulesFile").description("Sets the rules file (Required)").required().end()
				.booleanOption(false).names("h", "?", "help").description("Prints this help message, and exits with error status.").end();
		final Function<String,RuntimeException> errorHandler = (s) -> {
			Usage(s);
			System.exit(1);
			return null;
		};
		final Result optionsResult = options.process(pArgs, errorHandler);
		if (optionsResult.getBoolValue("h")) {
			throw errorHandler.apply(null);
		}
		final Path rulesFile = optionsResult.getValue("rulesFile");
		final Path logFile = optionsResult.getValue("logFile");
		final Path outFile = optionsResult.getValue("outFile");
		Path scanDir = optionsResult.getValue("scanDir");
		if (scanDir == null) {
			scanDir = FileSystems.getDefault().getPath(".");
		} else if (!Files.isDirectory(scanDir)) {
			throw errorHandler.apply("Scan directory (as given by option 'scanDir') does not exist: " + scanDir);
		}
		if (!Files.isRegularFile(rulesFile)) {
			throw errorHandler.apply("Rules file (as given by option 'rulesFile') does not exist, or is not a file: " + rulesFile);
		}
		try (final Logger logger = getLogger(logFile)) {
			final Scanner.Result scannerResult = scan(scanDir, outFile, rulesFile, logger);
		}
	}

	private static Scanner.Result scan(Path pScanDir, Path pOutputFile, Path pRulesFile, Logger pLogger) {
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
	
	public static final void Usage(String pMsg) {
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
	}
}
