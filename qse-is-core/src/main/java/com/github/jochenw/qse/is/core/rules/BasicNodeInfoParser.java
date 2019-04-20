package com.github.jochenw.qse.is.core.rules;

import javax.annotation.Nonnull;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.jochenw.qse.is.core.sax.AbstractContentHandler;


public class BasicNodeInfoParser extends AbstractContentHandler {
	private String type, serviceType, subType, sigType, comment;
	private Boolean stateless;
	private boolean inRecord;

	public String getType() {
		return type;
	}

	public String getServiceType() {
		return serviceType;
	}

	public String getSubType() {
		return subType;
	}

	public String getSigType() {
		return sigType;
	}

	public String getComment() {
		return comment;
	}

	public boolean isStateless() {
		return stateless == null ? false : stateless.booleanValue();
	}
	
	@Override
	public void startElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName, @Nonnull Attributes pAttrs)
			throws SAXException {
		super.startElement(pUri, pLocalName, pQName, pAttrs);
		switch (getLevel()) {
		  case 1:
			  assertElement("Values", pUri, pLocalName);
			  break;
		  case 2:
			  if (isElement("value", pUri, pLocalName)) {
				  final String name = pAttrs.getValue("name");
				  if ("node_type".equals(name)) {
					  startCollecting(1, (s) -> { type = s; checkReady(); });
				  } else if ("svc_type".equals(name)) {
					  startCollecting(1, (s) -> { type = "service"; serviceType = s; checkReady(); });
				  } else if ("svc_subtype".equals(name)) {
					  startCollecting(1, (s) -> { subType = s; checkReady(); });
				  } else if ("svc_sigtype".equals(name)) {
					  startCollecting(1, (s) -> { sigType = s; checkReady(); });
				  } else if ("node_comment".equals(name)) {
					  startCollecting(1, (s) -> { comment = s; checkReady(); });
				  } else if ("stateless".equals(name)) {
					  startCollecting(1, (s) -> {
						  if ("yes".equals(s)) {
							  stateless = Boolean.TRUE;
						  } else if ("no".equals(s)) {
							  stateless = Boolean.FALSE;
						  } else {
							  stateless = Boolean.valueOf(s);
						  }
						  checkReady();
					  });
				  }
			  } else if (isElement("record", pUri, pLocalName, pAttrs, "javaclass", "com.wm.util.Values")) {
				  final String name = pAttrs.getValue("name");
				  if (name != null) {
					  inRecord = true;
				  }
			  }
			  break;
		  case 3:
			  if (inRecord  &&  isElement("value", pUri, pLocalName)) {
				  final String name = pAttrs.getValue("name");
				  if ("node_type".equals(name)) {
					  startCollecting(2, (s) -> { type = s; checkReady(); });
				  }
			  }
			  break;
		}
	}

	@Override
	public void endElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName) throws SAXException {
		super.endElement(pUri, pLocalName, pQName);
		switch(getLevel()) {
		case 1:
			if (inRecord) {
				inRecord = false;
			}
			break;
		}
	}

	private void checkReady() {
		if (type != null) {
			if ("service".equals(type)) {
				if (serviceType != null  &&  sigType != null  &&  subType != null  &&  stateless != null) {
					terminate();
				}
			} else {
				terminate();
			}
		}
	}
}