package com.github.jochenw.qse.is.core.rules;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.jochenw.qse.is.core.sax.AbstractContentHandler;

public abstract class ServiceInvocationParser extends AbstractContentHandler {
	@Override
	public void startElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName, @Nonnull Attributes pAttrs) throws SAXException {
		super.startElement(pUri, pLocalName, pQName, pAttrs);
		if (isElement("INVOKE", pUri, pLocalName)
				||  isElement("MAPINVOKE", pUri, pLocalName)) {
			final String serviceName = pAttrs.getValue(XMLConstants.NULL_NS_URI, "SERVICE");
			serviceInvocation(serviceName);
		}
	}

	protected abstract void serviceInvocation(@Nonnull String pServiceName);
}
