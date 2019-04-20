package com.github.jochenw.qse.is.core.sax;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.github.jochenw.qse.is.core.sax.AbstractContentHandler.TerminationRequest;


public class MultiplexingContentHandler implements ContentHandler {
	private final List<ContentHandler> handlers;

	public MultiplexingContentHandler(List<ContentHandler> pHandlers) {
		handlers = new ArrayList<>(pHandlers);
	}

	@Override
	public void setDocumentLocator(Locator pLocator) {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().setDocumentLocator(pLocator);
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void startDocument() throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().startDocument();
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void endDocument() throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().endDocument();
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void startPrefixMapping(String pPrefix, String pUri) throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().startPrefixMapping(pPrefix, pUri);
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void endPrefixMapping(String pPrefix) throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().endPrefixMapping(pPrefix);
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void startElement(String pUri, String pLocalName, String pQName, Attributes pAttrs) throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().startElement(pUri, pLocalName, pQName, pAttrs);
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void endElement(String pUri, String pLocalName, String pQName) throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().endElement(pUri, pLocalName, pQName);
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void characters(char[] pChars, int pStart, int pLength) throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().characters(pChars, pStart, pLength);
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void ignorableWhitespace(char[] pChars, int pStart, int pLength) throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().ignorableWhitespace(pChars, pStart, pLength);
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void processingInstruction(String pTarget, String pData) throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().processingInstruction(pTarget, pData);
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

	@Override
	public void skippedEntity(String pName) throws SAXException {
		for (Iterator<ContentHandler> iter = handlers.iterator();  iter.hasNext();  ) {
			try {
				iter.next().skippedEntity(pName);;
			} catch (TerminationRequest tr) {
				iter.remove();
			}
		}
		if (handlers.isEmpty()) {
			throw new TerminationRequest();
		}
	}

}
