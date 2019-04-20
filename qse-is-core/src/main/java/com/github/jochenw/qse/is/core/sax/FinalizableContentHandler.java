package com.github.jochenw.qse.is.core.sax;

import org.xml.sax.ContentHandler;

public interface FinalizableContentHandler extends ContentHandler {
	public void finished();
}
