package com.github.jochenw.qse.is.core.stax;

import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class Stax {
	public static String asLocalizedMessage(Location pLoc, String pMsg) {
		return com.github.jochenw.afw.core.util.Stax.asLocalizedMessage(pLoc, pMsg);
	}

	public static String asLocalizedMessage(String pMsg, String pSystemId, int pLineNumber, int pColumnNumber) {
		return com.github.jochenw.afw.core.util.Stax.asLocalizedMessage(pMsg, pSystemId, pLineNumber, pColumnNumber);
	}

	public static String asString(XMLStreamReader pReader) {
		return com.github.jochenw.afw.core.util.Stax.asString(pReader);
	}

	public static XMLStreamException error(XMLStreamReader pReader, String pMsg) {
		final Location loc = pReader.getLocation();
		return new XMLStreamException(asLocalizedMessage(loc, pMsg), loc);
	}

	public static void assertDefaultNamesace(XMLStreamReader pReader) throws XMLStreamException {
		final String uri = pReader.getNamespaceURI();
		if (uri != null  &&  uri.length() > 0) {
			throw error(pReader, "Expected default namespace URI, got " + uri);
		}
	}
}
