package com.github.jochenw.qse.is.core.rules;

import javax.annotation.Nonnull;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.jochenw.qse.is.core.sax.AbstractContentHandler;


public class AuditSettingsParser extends AbstractContentHandler {
	boolean inAuditSettings, auditSettingsDone;
	private String auditOption;
	private String documentData, startExecution, stopExecution, onError;

	@Override
	public void startElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName, @Nonnull Attributes pAttrs) throws SAXException {
		super.startElement(pUri, pLocalName, pQName, pAttrs);
		final int lvl = getLevel();
		if ("Values".equals(pLocalName)) {
			if (lvl != 1) {
				error("Unexpected element at level " + lvl + ": " + pQName);
			}
		} else if ("record".equals(pLocalName)) {
			if (lvl != 2) {
				error("Unexpected element at level " + lvl + ": " + pQName);
			}
			if ("auditsettings".equals(pAttrs.getValue("name"))  &&  "com.wm.util.Values".equals(pAttrs.getValue("javaclass"))) {
				inAuditSettings = true;
			}
		} else if ("value".equals(pLocalName)) {
			if (inAuditSettings  &&  lvl == 3) {
				final String name = pAttrs.getValue("name");
				startCollecting(lvl-1, s -> {
					switch(name) {
					    case "document_data": documentData = s; break;
					    case "startExecution": startExecution = s; break;
					    case "stopExecution": stopExecution = s; break;
					    case "onError": onError = s; break;
					    default: /* Do nothing */ break;
					}
				});
			} else if (!inAuditSettings  &&  lvl == 2) {
				final String name = pAttrs.getValue("name");
				if ("auditoption".equals(name)) {
					startCollecting(lvl-1, s -> {auditOption = s; if (auditSettingsDone) { terminate(); }
					});
				}
			}
		}
	}

	@Override
	public void endElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName) throws SAXException {
		final int lvl = getLevel();
		if (inAuditSettings) {
			if (lvl == 2  &&  "record".equals(pLocalName)) {
				inAuditSettings = false;
				if (auditOption != null) {
					terminate();
				}
			}
		}
		super.endElement(pUri, pLocalName, pQName);
	}

	public String getDocumentData() {
		return documentData;
	}

	public String getStartExecution() {
		return startExecution;
	}

	public String getStopExecution() {
		return stopExecution;
	}

	public String getOnError() {
		return onError;
	}

	public String getAuditOption() {
		return auditOption;
	}
}
