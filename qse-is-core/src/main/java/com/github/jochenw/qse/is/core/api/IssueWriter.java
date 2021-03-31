package com.github.jochenw.qse.is.core.api;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.UndeclaredThrowableException;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.qse.is.core.Scanner;

public class IssueWriter implements AutoCloseable, IssueConsumer {
	public static class Result implements Scanner.Result {
		private final int numberOfOtherIssues, numberOfWarnings, numberOfErrors;

		public Result(int pNumberOfOtherIssues, int pNumberOfWarnings, int pNumberOfErrors) {
			numberOfOtherIssues = pNumberOfOtherIssues;
			numberOfWarnings = pNumberOfWarnings;
			numberOfErrors = pNumberOfErrors;
		}

		@Override
		public int getNumberOfOtherIssues() {
			return numberOfOtherIssues;
		}

		@Override
		public int getNumberOfErrors() {
			return numberOfErrors;
		}

		@Override
		public int getNumberOfWarnings() {
			return numberOfWarnings;
		}
	}
	private static final AttributesImpl NO_ATTRS = new AttributesImpl();
	private final OutputStream out;
	private final boolean prettyPrint;
	private boolean closeable;
	private boolean closed;
	private TransformerHandler th;
	private int numberOfOtherIssues;
	private int numberOfWarnings;
	private int numberOfErrors;

	public IssueWriter(@Nonnull OutputStream pOut, boolean pCloseable, boolean pPrettyPrint) {
		out = pOut;
		closeable = pCloseable;
		prettyPrint = pPrettyPrint;
	}
	
	@Override
	public void accept(@Nonnull Issue pIssue) {
		try {
			if (th == null) {
				open();
			}
			final AttributesImpl attrs = new AttributesImpl();
			attrs.addAttribute(XMLConstants.NULL_NS_URI, "package", "package", "CDATA", pIssue.getPackage());
			attrs.addAttribute(XMLConstants.NULL_NS_URI, "errorCode", "errorCode", "CDATA", pIssue.getErrorCode());
			attrs.addAttribute(XMLConstants.NULL_NS_URI, "severity", "severity", "CDATA", pIssue.getSeverity().name());
			attrs.addAttribute(XMLConstants.NULL_NS_URI, "rule", "rule", "CDATA", pIssue.getRule());
			attrs.addAttribute(XMLConstants.NULL_NS_URI, "path", "path", "CDATA", pIssue.getUri());
			th.startElement(XMLConstants.NULL_NS_URI, "issue", "issue", attrs);
			th.startElement(XMLConstants.NULL_NS_URI, "message", "message", NO_ATTRS);
			final char[] messageChars = pIssue.getMessage().toCharArray();
			th.characters(messageChars, 0, messageChars.length);
			th.endElement(XMLConstants.NULL_NS_URI, "message", "message");
			th.endElement(XMLConstants.NULL_NS_URI, "issue", "issue");
		} catch (SAXException se) {
			throw new UndeclaredThrowableException(se);
		}
		switch (pIssue.getSeverity()) {
		  case ERROR:
			 ++numberOfErrors;
			 break;
		  case WARN:
			 ++numberOfWarnings;
			 break;
		  default:
			 ++numberOfOtherIssues;
			 break;
		}
	}

	protected void open() {
		try {
			SAXTransformerFactory stf = (SAXTransformerFactory) TransformerFactory.newInstance();
			TransformerHandler trh = stf.newTransformerHandler();
			final Transformer t = trh.getTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, "UTF8");
			t.setOutputProperty(OutputKeys.STANDALONE, "yes");
			if (prettyPrint) {
				t.setOutputProperty(OutputKeys.INDENT, "yes");
			} else {
				t.setOutputProperty(OutputKeys.INDENT, "no");
			}
			trh.setResult(new StreamResult(out));
			trh.startDocument();
			trh.startElement(XMLConstants.NULL_NS_URI, "issues", "issues", NO_ATTRS);
			th = trh;
		} catch (Throwable t) {
			throw Exceptions.show(t);
		}
	}

	@Override
	public void close() throws IOException {
		if (!closed) {
			closed = true;
			if (th == null) {
				open();
			}
			final TransformerHandler trh = th;
			th = null;
			try {
				final AttributesImpl attrs = new AttributesImpl();
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "errors", "errors", "CDATA", String.valueOf(numberOfErrors));
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "warnings", "warnings", "CDATA", String.valueOf(numberOfWarnings));
				attrs.addAttribute(XMLConstants.NULL_NS_URI, "otherIssues", "otherIssues", "CDATA", String.valueOf(numberOfOtherIssues));
				trh.startElement(XMLConstants.NULL_NS_URI, "count", "count", attrs);
				trh.endElement(XMLConstants.NULL_NS_URI, "count", "count");
				trh.endElement(XMLConstants.NULL_NS_URI, "issues", "issues");
				trh.endDocument();
			} catch (SAXException se) {
				throw new UndeclaredThrowableException(se);
			}
			if (closeable) {
				closeable = false;
				out.close();
			}
		}
	}

	public Result getResult() {
		return new Result(numberOfOtherIssues, numberOfWarnings, numberOfErrors);
	}
}
