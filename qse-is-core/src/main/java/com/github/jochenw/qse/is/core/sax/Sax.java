package com.github.jochenw.qse.is.core.sax;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.github.jochenw.qse.is.core.sax.AbstractContentHandler.TerminationRequest;


public class Sax {
	public static void parse(Path pPath, ContentHandler pHandler) {
		try (InputStream is = Files.newInputStream(pPath)) {
			final InputSource isource = new InputSource(is);
			isource.setSystemId(pPath.toString());
			parse(isource, pHandler);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UndeclaredThrowableException(e);
		} catch (ParserConfigurationException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public static void parse(URL pUrl, ContentHandler pHandler) {
		try (InputStream is = pUrl.openStream()) {
			final InputSource isource = new InputSource(is);
			isource.setSystemId(pUrl.toExternalForm());
			parse(isource, pHandler);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UndeclaredThrowableException(e);
		} catch (ParserConfigurationException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public static void parse(InputStream pIn, String pSystemId, ContentHandler pHandler) {
		try {
			final InputSource isource = new InputSource(pIn);
			isource.setSystemId(pSystemId);
			parse(isource, pHandler);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UndeclaredThrowableException(e);
		} catch (ParserConfigurationException e) {
			throw new UndeclaredThrowableException(e);
		}
	}
	
	public static void parse(InputSource pSource, ContentHandler pHandler) throws IOException, SAXException, ParserConfigurationException {
		final SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setValidating(false);
		spf.setNamespaceAware(true);
		final XMLReader xr = spf.newSAXParser().getXMLReader();
		xr.setContentHandler(pHandler);
		xr.parse(pSource);
	}

	public static boolean parseTerminable(URL pUrl, ContentHandler pHandler) {
		try {
			parse(pUrl, pHandler);
			return false;
		} catch (TerminationRequest e) {
			return true;
		}
	}

	public static boolean parseTerminable(Path pPath, ContentHandler pHandler) {
		try {
			parse(pPath, pHandler);
			return false;
		} catch (TerminationRequest e) {
			return true;
		}
	}

	public static boolean parseTerminable(InputSource pSource, ContentHandler pHandler) throws IOException, SAXException, ParserConfigurationException {
		try {
			parse(pSource, pHandler);
			return false;
		} catch (TerminationRequest e) {
			return true;
		}
	}

	public static boolean parseTerminable(Path pPath, List<ContentHandler> pHandlers) {
		try (InputStream is = Files.newInputStream(pPath)) {
			final InputSource isource = new InputSource(is);
			isource.setSystemId(pPath.toString());
			return parseTerminable(isource, pHandlers);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UndeclaredThrowableException(e);
		} catch (ParserConfigurationException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public static boolean parseTerminable(URL pUrl, List<ContentHandler> pHandlers) {
		try (InputStream is = pUrl.openStream()) {
			final InputSource isource = new InputSource(is);
			isource.setSystemId(pUrl.toExternalForm());
			return parseTerminable(isource, pHandlers);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UndeclaredThrowableException(e);
		} catch (ParserConfigurationException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public static boolean parseTerminable(InputStream pIn, String pSystemId, List<ContentHandler> pHandlers) {
		try {
			final InputSource isource = new InputSource(pIn);
			isource.setSystemId(pSystemId);
			return parseTerminable(isource, pHandlers);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		} catch (SAXException e) {
			throw new UndeclaredThrowableException(e);
		} catch (ParserConfigurationException e) {
			throw new UndeclaredThrowableException(e);
		}
	}

	public static boolean parseTerminable(InputSource pSource, List<ContentHandler> pHandlers) throws IOException, SAXException, ParserConfigurationException {
		final List<ContentHandler> handlerList = new ArrayList<ContentHandler>(pHandlers);
		final boolean result;
		if (!pHandlers.isEmpty()) {
			result = parseTerminable(pSource, new MultiplexingContentHandler(pHandlers));
		} else {
			result = true;
		}
		for (ContentHandler ch : handlerList) {
			if (ch instanceof FinalizableContentHandler) {
				((FinalizableContentHandler) ch).finished();
			}
		}
		return result;
	}
}
