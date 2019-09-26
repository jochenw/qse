package com.github.jochenw.qse.is.core.stax;

import java.io.InputStream;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.function.Consumer;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.github.jochenw.afw.core.util.Exceptions;
import com.github.jochenw.qse.is.core.sax.Sax;

public abstract class AbstractPullParser {
	protected abstract void parse(XMLStreamReader pReader) throws XMLStreamException;

	public void parse(InputStream pIn, String pUri) {
		final XMLInputFactory xif = XMLInputFactory.newInstance();
		try {
			final XMLStreamReader xsr = xif.createXMLStreamReader(pUri, pIn);
			parse(xsr);
		} catch (XMLStreamException e) {
			final Location loc = e.getLocation();
			if (loc == null) {
				throw new UndeclaredThrowableException(e);
			} else {
				final String msg = Stax.asLocalizedMessage(loc, e.getMessage());
				throw new UndeclaredThrowableException(e, msg);
			}
		}
		
	}

	protected void findStartElement(XMLStreamReader pReader, String pRootElement) throws XMLStreamException {
		while (pReader.hasNext()) {
			final int state = pReader.next();
			switch(state) {
			case XMLStreamReader.START_DOCUMENT:
				// Ignore this event.
				break;
			case XMLStreamReader.SPACE:
			case XMLStreamReader.CHARACTERS:
				if (!isSpaceEvent(pReader)) {
					throw error(pReader, "Unexpected non-whitespace characters");
				}
				break;
			case XMLStreamReader.START_ELEMENT:
				final String nsUri = pReader.getNamespaceURI();
				if ((nsUri == null  ||  nsUri.length() == 0)  &&  pRootElement.equals(pReader.getLocalName())) {
					return;
				} else {
					throw error(pReader, "Expected root element " + pRootElement
							    + ", got " + Sax.asQName(nsUri, pReader.getLocalName()));
				}
			default:
				throw error(pReader, "Unexpected event: " + state);
			}
		}
	}

	protected void parseEndDocument(XMLStreamReader pReader) throws XMLStreamException {
		while (pReader.hasNext()) {
			final int event = pReader.next();
			switch(event) {
			case XMLStreamReader.START_DOCUMENT:
				// Ignore this event.
				break;
			case XMLStreamReader.SPACE:
			case XMLStreamReader.CHARACTERS:
				if (!isSpaceEvent(pReader)) {
					throw error(pReader, "Unexpected non-whitespace characters");
				}
				break;
			case XMLStreamReader.COMMENT:
				// Ignore
				break;
			case XMLStreamReader.START_ELEMENT:
				throw error(pReader, "Unexpected startElement: " + pReader.getName());
			case XMLStreamReader.END_ELEMENT:
				throw error(pReader, "Unexpected endElement: " + pReader.getName());
			case XMLStreamReader.END_DOCUMENT:
				return;
			default:
				throw error(pReader, "Unexpected event: " + event);
			}
		}
	}

	protected boolean isSpaceEvent(XMLStreamReader pReader) throws XMLStreamException {
		final int event = pReader.getEventType();
		switch (event) {
		case XMLStreamReader.SPACE:
			return true;
		case XMLStreamReader.CHARACTERS:
			final int len = pReader.getTextLength();
			final int offset = pReader.getTextStart();
			final char[] chars = pReader.getTextCharacters();
			for (int i = 0;  i < len;  i++) {
				final char c = chars[offset+i];
				if (!Character.isWhitespace(c)) {
					return false;
				}
			}
			return true;
		default:
			throw error(pReader, "Unexpected event: " + event);
		}
	}

	protected void skipElement(XMLStreamReader pReader) throws XMLStreamException {
		final String nsUri = pReader.getNamespaceURI();
		final String localName = pReader.getLocalName();
		while (!pReader.hasNext()) {
			final int event = pReader.next();
			switch (event) {
			case XMLStreamReader.END_ELEMENT:
				if (isElementNS(pReader, nsUri, localName)) {
					return;
				} else {
					throw error(pReader, "Unexpected end of element "
							    + Sax.asQName(pReader.getNamespaceURI(), pReader.getLocalName())
							    + ", while waiting for end of element "
							    + Sax.asQName(nsUri, localName));
				}
			case XMLStreamReader.START_ELEMENT:
				throw error(pReader, "Unexpected start of element "
							    + Sax.asQName(pReader.getNamespaceURI(), pReader.getLocalName())
							    + ", while waitint for end of element "
							    + Sax.asQName(nsUri, localName));
			default:
				throw error(pReader, "Unexpected event: " + event);
			}
		}
		throw error(pReader, "Undexpected end of document, while waiting for end of element: "
					+ Sax.asQName(nsUri, localName));
	}

	protected void skipElementRecursively(XMLStreamReader pReader) throws XMLStreamException {
		final String nsUri = pReader.getNamespaceURI();
		final String localName = pReader.getLocalName();
		while (!pReader.hasNext()) {
			final int event = pReader.next();
			switch (event) {
			case XMLStreamReader.END_ELEMENT:
				if (isElementNS(pReader, nsUri, localName)) {
					return;
				} else {
					throw error(pReader, "Unexpected end of element "
							    + Sax.asQName(pReader.getNamespaceURI(), pReader.getLocalName())
							    + ", while waitint for end of element "
							    + Sax.asQName(nsUri, localName));
				}
			case XMLStreamReader.START_ELEMENT:
				skipElementRecursively(pReader);
				break;
			case XMLStreamReader.CHARACTERS:
			case XMLStreamReader.CDATA:
			case XMLStreamReader.COMMENT:
			case XMLStreamReader.DTD:
			case XMLStreamReader.NAMESPACE:
				break;
			default:
				throw error(pReader, "Unexpected event: " + event);
			}
		}
		throw error(pReader, "Undexpected end of document, while waiting for end of element: "
					+ Sax.asQName(nsUri, localName));
	}


	protected boolean isElementNS(XMLStreamReader pReader, String pUri, String pLocalName) {
		final String uri = pReader.getNamespaceURI();
		final String localName = pReader.getLocalName();
		if (pUri == null  ||  pUri.length() == 0) {
			return (uri == null  ||  uri.length() == 0)  &&  pLocalName.equals(localName);
		} else {
			return pUri.equals(uri)  &&  pLocalName.equals(localName);
		}
	}

	protected void assertDefaultNamespace(XMLStreamReader pReader) throws XMLStreamException {
		final String nsUri = pReader.getNamespaceURI();
		if (nsUri != null  &&  nsUri.length() > 0) {
			throw error(pReader, "Expected default namespace element, got " + Sax.asQName(nsUri, pReader.getLocalName()));
		}
	}

	protected XMLStreamException error(XMLStreamReader pReader, String pMsg) {
		return new XMLStreamException(pMsg, pReader.getLocation()); 
	}

	protected void assertElement(XMLStreamReader pReader, String pLocalName) throws XMLStreamException {
		assertDefaultNamespace(pReader);
		final String localName = pReader.getLocalName();
		if (!pLocalName.equals(localName)) {
			throw error(pReader, "Expected element " + pLocalName + ", got " + localName);
		}
	}

	protected void skipToDocumentElement(XMLStreamReader pReader) throws XMLStreamException {
		while (pReader.hasNext()) {
			final int state = pReader.next();
			switch(state) {
			case XMLStreamReader.START_DOCUMENT:
				break;
			case XMLStreamReader.START_ELEMENT:
				return;
			default:
				throw error(pReader, "Unexpected state: " + state);
			}
		}
	}

	protected String readTextElement(XMLStreamReader pReader) throws XMLStreamException {
		final StringBuilder sb = new StringBuilder();
		while (pReader.hasNext()) {
			final int state = pReader.next();
			switch(state) {
			  case XMLStreamReader.END_ELEMENT:
				return sb.toString();
			  case XMLStreamReader.CDATA:
			  case XMLStreamReader.CHARACTERS:
			    sb.append(pReader.getText());
				break;
			  case XMLStreamReader.COMMENT:
				break;
			  default:
				throw error(pReader, "Unexpected state while reading text element: " + state);
			}
		}
		throw error(pReader, "Unexpected end of document while reading text element");
	}

	protected void skipElementRecursive(XMLStreamReader pReader) throws XMLStreamException {
		while (pReader.hasNext()) {
			final int state = pReader.next();
			switch(state) {
			  case XMLStreamReader.START_ELEMENT:
				skipElementRecursive(pReader);
				break;
			  case XMLStreamReader.END_ELEMENT:
			    return;
			  case XMLStreamReader.CDATA:
			  case XMLStreamReader.CHARACTERS:
			  case XMLStreamReader.COMMENT:
				break;
			  default:
				throw error(pReader, "Invalid state: " + state);
			}
		}
	}
}
