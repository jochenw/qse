package com.github.jochenw.qse.is.core.rules;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.sax.AbstractContentHandler;
import com.github.jochenw.qse.is.core.sax.Sax;



public class RulesParser extends AbstractContentHandler {
	public static class Rule {
		private final boolean enabled;
		private final String id;
		private final String className;
		private final Severity severity;
		private Map<String,Object> properties;

		public Rule(boolean pEnabled, @Nonnull String pId, @Nonnull String pClassName, @Nonnull Severity pSeverity) {
			enabled = pEnabled;
			className = pClassName;
			id = pId;
			severity = pSeverity;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public String getId() {
			return id;
		}
		
		public String getClassName() {
			return className;
		}

		public Severity getSeverity() {
			return severity;
		}

		public void addProperty(@Nonnull String pName, @Nonnull Object pValue) {
			if (properties == null) {
				properties = new HashMap<>();
			}
			properties.put(pName, pValue);
		}

		public Map<String,Object> getProperties() {
			if (properties == null) {
				return Collections.emptyMap();
			} else {
				return properties;
			}
		}

		public <O> O getProperty(String pKey) {
			if (properties == null) {
				return null;
			} else {
				@SuppressWarnings("unchecked")
				final O o = (O) properties.get(pKey);
				return o;
			}
		}

		public <O> O requireProperty(String pKey) {
			final O o = getProperty(pKey);
			if (o == null) {
				throw new NoSuchElementException("Property " + pKey
						+ " is not defined for rule class: " + getClassName());
			}
			return o;
		}
	}
	public static final String NS = "http://namespaces.github.com/jochenw/qse/is/rules/1.0.0";

	private final Consumer<Rule> listener;
	private Rule currentRule;
	private String propertyKey;
	private List<String> values;

	public RulesParser(@Nonnull Consumer<Rule> pListener) {
		listener = pListener;
	}

	@Override
	public void startDocument() throws SAXException {
		propertyKey = null;
		values = null;
	}

	@Override
	public void startElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName, @Nonnull Attributes pAttrs) throws SAXException {
		if (propertyKey != null  &&  getLevel() == 3  &&  isElement(NS, "values", pUri, pLocalName)) {
			super.stopCollecting(false);
		}
		super.startElement(pUri, pLocalName, pQName, pAttrs);
		switch (getLevel()) {
		  case 1:
			  assertElement(NS, "rules", pUri, pLocalName);
			  break;
		  case 2:
			  assertElement(NS, "rule", pUri, pLocalName);
			  final String enabledStr = pAttrs.getValue(XMLConstants.NULL_NS_URI, "enabled");
			  final boolean enabled;
			  if (enabledStr == null) {
				  enabled = false;
			  } else {
				  enabled = Boolean.parseBoolean(enabledStr);
			  }
			  final String className = pAttrs.getValue(XMLConstants.NULL_NS_URI, "class");
			  if (className == null  ||  className.length() == 0) {
				  throw error("Missing, or empty attribute: rule/@class");
			  }
			  final String id = pAttrs.getValue("id");
			  final String severityStr = pAttrs.getValue(XMLConstants.NULL_NS_URI, "severity");
			  final Severity severity;
			  if (severityStr == null) {
				  severity = Severity.WARN;
			  } else {
				  try {
					  severity = Severity.valueOf(severityStr);
				  } catch (IllegalArgumentException e) {
					  throw error("Invalid value for rule/@severity: " + severityStr);
				  }
			  }
			  currentRule = new Rule(enabled, id, className, severity);
			  break;
		  case 3:
			  if (isElement(NS, "property", pUri, pLocalName)) {
				  propertyKey = pAttrs.getValue(XMLConstants.NULL_NS_URI, "name");			  
				  startCollecting(2, (s) -> currentRule.addProperty(propertyKey, s));
			  } else if (isElement(NS, "listProperty", pUri, pLocalName)) {
				  propertyKey = pAttrs.getValue(XMLConstants.NULL_NS_URI, "name");			  
				  values = new ArrayList<String>();
			  }
			  if (propertyKey == null  ||  propertyKey.length() == 0) {
				  throw error("Missing, or empty attribute: " + pQName + "/@name");
			  }
			  break;
		  case 4:
			  if (values != null) {
				  assertElement(NS, "value", pUri, pLocalName);
				  startCollecting(3, (s) -> values.add(s));
			  }
			  break;
	      default:
			  throw error("Unexpected startElement at level " + getLevel() + ": " + asQName(pUri, pLocalName));
		}
	}

	@Override
	public void endElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName) throws SAXException {
		super.endElement(pUri, pLocalName, pQName);
		switch(getLevel()) {
	    case 0:
	    	assertElement(NS, "rules", pUri, pLocalName);
	    	break;
	    case 1:
	    	assertElement(NS, "rule", pUri, pLocalName);
			listener.accept(currentRule);
			currentRule = null;
	    	break;
		case 2:
			if (values == null) {
				assertElement(NS, "property", pUri, pLocalName);
			} else {
				assertElement(NS, "listProperty", pUri, pLocalName);
				final String[] valueArray = values.toArray(new String[values.size()]);
				currentRule.addProperty(propertyKey, valueArray);
				values = null;
			}
			break;
		case 3:
			assertElement(NS, "value", pUri, pLocalName);
			break;
		default:
			throw error("Unexpected endElement at level " + getLevel() + ": " + asQName(pUri, pLocalName));
		}
	}

	public static void parseBuiltinRules(@Nonnull Consumer<Rule> pRuleConsumer) {
		final RulesParser rp = new RulesParser(pRuleConsumer);
		final URL url = Scanner.class.getResource("rules.xml");
		if (url == null) {
			throw new IllegalStateException("Unable to locate rules.xml");
		}
		Sax.parse(url,rp);
	}
}
