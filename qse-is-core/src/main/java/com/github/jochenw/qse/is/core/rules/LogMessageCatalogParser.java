package com.github.jochenw.qse.is.core.rules;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.model.MessageCatalog;
import com.github.jochenw.qse.is.core.sax.AbstractContentHandler;


public class LogMessageCatalogParser extends AbstractContentHandler {
	private final MessageCatalog messageCatalog = new MessageCatalog();
	private String locale;
	private String currentMessageKey;
	private Severity currentLevel;

	public MessageCatalog getMessageCatalog() {
		return messageCatalog;
	}

	@Override
	public void startElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName, @Nonnull Attributes pAttrs) throws SAXException {
		super.startElement(pUri, pLocalName, pQName, pAttrs);
		switch (getLevel()) {
		  case 1:
			  assertElement("logMessageConfig", pUri, pLocalName);
			  break;
		  case 2:
			  if (isElement("locale", pUri, pLocalName)) {
				  if (messageCatalog.getDefaultLocale() != null) {
					  throw error("Multiple logMessageConfig/locale elements found");
				  }
				  startCollecting(1, (s) -> {
					  final Locale locale = Locale.forLanguageTag(s);
					  if (locale == null) {
						  throw new UndeclaredThrowableException(error("Invalid value for logMessageConfig/locale: " + s));
					  }
					  messageCatalog.setDefaultLocale(locale);
					  LogMessageCatalogParser.this.locale = s;
				  });
			  } else if (isElement("componentList", pUri, pLocalName)) {
				  // Do nothing, wait for the component elements.
			  }
			  break;
		  case 3:
			  if (isElement("component", pUri, pLocalName)) {
				  final String key = pAttrs.getValue("key");
				  if (key == null  ||  key.length() == 0) {
					  throw error("Missing, or empty, attribute: logMessageConfig/componentList/locale/@key");
				  }
				  messageCatalog.startComponent(key);
			  }
			  break;
		  case 4:
			  if (isElement("name", pUri, pLocalName)) {
				  if (messageCatalog.getCurrentComponent().getName() != null) {
					  throw error("Multiple logMessageConfig/componentList/component/name elements found");
				  }
				  startCollecting(3, (s) -> {
					  messageCatalog.getCurrentComponent().setName(s);
				  });
			  } else if (isElement("facilityList", pUri, pLocalName)) {
				  // Do nothing, wait for the facility elements.
			  }
			  break;
		  case 5:
			  if (isElement("facility", pUri, pLocalName)) {
				  final String key = pAttrs.getValue("key");
				  if (key == null  ||  key.length() == 0) {
					  throw error("Missing, or empty, attribute: logMessageConfig/componentList/component/facilityList/facility/@key");
				  }
				  messageCatalog.startFacility(key);
			  }
			  break;
		  case 6:
			  if (isElement("name", pUri, pLocalName)) {
				  if (messageCatalog.getCurrentFacility().getName() != null) {
					  throw error("Multiple logMessageConfig/componentList/component/facilityList/facility/name elements found");
				  }
				  startCollecting(5, (s) -> {
					  messageCatalog.getCurrentFacility().setName(s);
				  });
			  } else if (isElement("messageList", pUri, pLocalName)) {
				  // Do nothing, wait for the message elements
			  }
			  break;
		  case 7:
			  if (isElement("message", pUri, pLocalName)) {
				  final String key = pAttrs.getValue("key");
				  if (key == null  ||  key.length() == 0) {
					  throw error("Missing, or empty, attribute: logMessageConfig/componentList/component/facilityList/facility/messageList/message/@key");
				  }
				  final String levelStr = pAttrs.getValue("level");
				  if (levelStr == null  ||  levelStr.length() == 0) {
					  throw error("Missing, or empty, attribute: logMessageConfig/componentList/component/facilityList/facility/messageList/message/@key");
				  }
				  final Severity level;
				  try {
					  level = Severity.valueOf(levelStr);
				  } catch (Throwable t) {
					  throw error("Invalid value " + levelStr + " for attribute: logMessageConfig/componentList/component/facilityList/facility/messageList/message/@level");
				  }
				  currentMessageKey = key;
				  currentLevel = level;
			  }
			  break;
		  case 8:
			  if (isElement("text", pUri, pLocalName)) {
				  final String locale = pAttrs.getValue("locale");
				  if (locale == null  ||  locale.length() == 0) {
					  throw error("Missing, or empty, attribute: logMessageConfig/componentList/component/facilityList/facility/messageList/message/text/@locale");
				  }
				  if (!locale.equals(LogMessageCatalogParser.this.locale)) {
					  throw error("Invalid value (must be same than default locale) for attribute: logMessageConfig/componentList/componentList/component/facilityList/facility/messageList/message/text/@locale");
				  }
				  startCollecting(7, (s) -> {
					  messageCatalog.addMessage(currentMessageKey, currentLevel, s);
				  });
			  }
			  break;
		  default:
			  throw error("Unexpected element at level " + getLevel() + ": " + asQName(pUri, pLocalName));
		}
	}

	@Override
	public void endElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName) throws SAXException {
		super.endElement(pUri, pLocalName, pQName);
		switch (getLevel()) {
		  case 0:
			  //logMessageConfig: Do nothing.
			  break;
		  case 1:
			  // locale, or componentList: Do nothing
			  break;
		  case 2:
			  if (isElement("component", pUri, pLocalName)) {
				  messageCatalog.endComponent();
			  }
			  break;
		  case 3:
			  // name, or facilityList: Do nothing
			  break;
		  case 4:
			  if (isElement("facility", pUri, pLocalName)) {
				  messageCatalog.endFacility();
			  }
			  break;
		  case 5:
			  // name, or messageList: Do nothing
			  break;
		  case 6:
			  // message: Do nothing
			  break;
		  case 7:
			  // text: Do nothing
			  break;
		  default:
			  throw error("Unexpected element at level " + getLevel() + ": " + asQName(pUri, pLocalName));
		}
	}
}
