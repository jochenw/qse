package com.github.jochenw.qse.is.core.rules;

import java.util.function.Consumer;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.jochenw.qse.is.core.sax.AbstractContentHandler;

/*
 * Find either of the following XPath expressions:
 *   /Values/record[@name=\"startup_services\"]/null/@name
 *   /Values/record[@name=\"shutdown_services\"]/null/@name
 */
public class ManifestParser extends AbstractContentHandler {
	public interface Listener {
		public default void version(String pVersion) {}
		public default void startupService(String pService) {}
		public default void shutdownService(String pService) {}
		public default void requires(String pPackageName, String pVersion) {}
	}
	private final Listener listener;
	private boolean inStartupServices, inShutdownServices, inRequires;

	public ManifestParser(Listener pListener) {
		listener = pListener;
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		inStartupServices = false;
		inShutdownServices = false;
	}

	@Override
	public void startElement(String pUri, String pLocalName, String pQName, Attributes pAttrs) throws SAXException {
		super.startElement(pUri, pLocalName, pQName, pAttrs);
		switch (getLevel()) {
		  case 1:
			  assertElement("Values", pUri, pLocalName);
			  break;
		  case 2:
			  if (isElement("record", pUri, pLocalName)) {
				  String name = pAttrs.getValue("name");
				if ("startup_services".equals(name)) {
					  inStartupServices = true;
				  } else if ("shutdown_services".equals(name)) {
					  inShutdownServices = true;
				  } else if ("requires".equals(name)) {
					  inRequires = true;
				  }
			  } else if (isElement("value", pUri, pLocalName)) {
				  final String name = pAttrs.getValue("name");
				  if ("version".equals(name)) {
					  startCollecting(1, (s) -> {
						  if (listener != null) {
							  listener.version(s);
						  }
					  });
				  }
			  }
			  break;
		  case 3:
			  if (isElement("null", pUri, pLocalName)) {
				  final String name = pAttrs.getValue("name");
				  if (name != null  &&  listener != null) {
					  if (inStartupServices) {
						  listener.startupService(name);
					  } else if (inShutdownServices) {
						  listener.shutdownService(name);
					  }
				  }
			  } else if (isElement("value", pUri, pLocalName)) {
				  final String name = pAttrs.getValue("name");
				  if (name != null  &&  listener != null) {
					  if (inRequires) {
						  startCollecting(2, (s) -> {
							  listener.requires(name, s);
						  });
					  }
				  }
			  }
			  break;
		}
	}

	@Override
	public void endElement(String pUri, String pLocalName, String pQName) throws SAXException {
		super.endElement(pUri, pLocalName, pQName);
		switch(getLevel()) {
		case 1:
			if (inStartupServices) {
				inStartupServices = false;
			}
			if (inShutdownServices) {
				inShutdownServices = false;
			}
			break;
		}
	}
}