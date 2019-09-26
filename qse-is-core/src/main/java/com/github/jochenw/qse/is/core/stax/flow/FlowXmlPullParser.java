package com.github.jochenw.qse.is.core.stax.flow;

import java.util.function.Consumer;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.github.jochenw.qse.is.core.sax.Sax;
import com.github.jochenw.qse.is.core.stax.AbstractPullParser;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlPullParser.Data.StepData;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.StepInfo;

public class FlowXmlPullParser extends AbstractPullParser {
	public static class Data {
		public static class StepData implements StepInfo {
			private boolean enabled;
			private String label;
			private String comment;
			public boolean isEnabled() {
				return enabled;
			}
			public void setEnabled(boolean enabled) {
				this.enabled = enabled;
			}
			public String getLabel() {
				return label;
			}
			public void setLabel(String label) {
				this.label = label;
			}
			public String getComment() {
				return comment;
			}
			public void setComment(String comment) {
				this.comment = comment;
			}
		}

		private final XMLStreamReader reader;
		private final StepData stepData = new StepData();

		public Data(XMLStreamReader pReader) {
			reader = pReader;
		}
		public StepData getStepData() {
			return stepData;
		}
		public XMLStreamReader getReader() {
			return reader;
		}
	}
	private final FlowXmlVisitor visitor;

	public FlowXmlPullParser(FlowXmlVisitor pVisitor) {
		visitor = pVisitor;
	}

	protected void parseFlow(Data pData) throws XMLStreamException {
		final XMLStreamReader rdr = pData.getReader();
		final String label = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "NAME");
		final String version = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "VERSION");
		final String disabledStr = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "DISABLED");
		final boolean enabled = !"true".equalsIgnoreCase(disabledStr);
		final String cleanupStr = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "CLEANUP");
		final boolean cleanUp = "true".equalsIgnoreCase(cleanupStr);
		final Consumer<String> commentConsumer = (s) -> {
			final StepData stepInfo = pData.getStepData();
			stepInfo.setComment(s);
			if (visitor != null) {
				pData.getStepData().setEnabled(enabled);
				pData.getStepData().setLabel(label);
				visitor.startFlow(stepInfo, version, cleanUp);
			}
		};
		parseStepList(pData, "FLOW", commentConsumer);
		if (visitor != null) {
			visitor.endFlow();
		}
	}

	protected void parseStep(Data pData, String pEndElementName, Consumer<String> pCommentConsumer)
			throws XMLStreamException {
		Consumer<String> commentConsumer = pCommentConsumer;
		final XMLStreamReader rdr = pData.getReader();
		while (rdr.hasNext()) {
			final int event = rdr.next();
			switch (event) {
			case XMLStreamReader.SPACE:
			case XMLStreamReader.CHARACTERS:
				if (!isSpaceEvent(rdr)) {
					throw error(rdr, "Unexpected non-whitespace characters: " + rdr.getText());
				}
				break;
			case XMLStreamReader.START_ELEMENT:
				assertDefaultNamespace(rdr);
				final String localName = rdr.getLocalName();
				switch(localName) {
				case "COMMENT":
					final String elementText = readTextElement(rdr);
					if (commentConsumer != null) {
						commentConsumer.accept(elementText);
						commentConsumer = null;
					}
					break;
				case "SEQUENCE":
				case "BRANCH":
				case "INVOKE":
				case "EXIT":
				case "RETRY":
					throw error(rdr, "Unexpected element in simple step: " + localName);
				default:
					skipElementRecursive(rdr);
				}
				break;
			case XMLStreamReader.END_ELEMENT:
				assertDefaultNamespace(rdr);
				if (!pEndElementName.equals(rdr.getLocalName())) {
					throw error(rdr, "Expected /" + pEndElementName + ", got /" + rdr.getLocalName()); 
				}
				if (commentConsumer != null) {
					commentConsumer.accept(null);
				}
				return;
			}
		}
	}

	protected void parseStepList(Data pData, String pEndElementName, Consumer<String> pCommentConsumer, boolean pLogging)
	throws XMLStreamException {
		parseStepList(pData, pEndElementName, pCommentConsumer);
	}

	protected void parseStepList(Data pData, String pEndElementName, Consumer<String> pCommentConsumer)
	        throws XMLStreamException {
		Consumer<String> commentConsumer = pCommentConsumer;
		final XMLStreamReader rdr = pData.getReader();
		while (rdr.hasNext()) {
			final int event = rdr.next();
			switch (event) {
			case XMLStreamReader.SPACE:
			case XMLStreamReader.CHARACTERS:
				if (!isSpaceEvent(rdr)) {
					throw error(rdr, "Unexpected non-whitespace characters: " + rdr.getText());
				}
				break;
			case XMLStreamReader.START_ELEMENT:
				assertDefaultNamespace(rdr);
				final String localName = rdr.getLocalName();
				switch(localName) {
				case "COMMENT":
					if (commentConsumer != null) {
						commentConsumer.accept(rdr.getElementText());
						commentConsumer = null;
					}
					break;
				case "MAP":
					if (commentConsumer != null) {
						commentConsumer.accept(null);
						commentConsumer = null;
					}
					parseMapStep(pData);
					break;
				case "SEQUENCE":
					if (commentConsumer != null) {
						commentConsumer.accept(null);
						commentConsumer = null;
					}
					parseSequence(pData);
					break;
				case "BRANCH":
					if (commentConsumer != null) {
						commentConsumer.accept(null);
						commentConsumer = null;
					}
					parseBranch(pData);
					break;
				case "INVOKE":
					if (commentConsumer != null) {
						commentConsumer.accept(null);
						commentConsumer = null;
					}
					parseInvoke(pData);
					break;
				case "EXIT":
					if (commentConsumer != null) {
						commentConsumer.accept(null);
						commentConsumer = null;
					}
					parseExit(pData);
					break;
				case "RETRY":
					if (commentConsumer != null) {
						commentConsumer.accept(null);
						commentConsumer = null;
					}
					parseRepeat(pData);
					break;
				default:
					throw error(rdr, "Unexpected element in step sequence");
				}
				break;
			case XMLStreamReader.END_ELEMENT:
				if (commentConsumer != null) {
					commentConsumer.accept(null);
				}
				return;
			}
		}
	}

	protected void parseInvoke(Data pData) throws XMLStreamException {
		final XMLStreamReader rdr = pData.getReader();
		final String service = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "SERVICE");
		if (service == null || service.length() == 0) {
			throw error(rdr, "Missing, or empty attribute: SERVICE");
		}
		final Consumer<String> commentConsumer = (s) -> {
			if (visitor != null) {
				visitor.startInvoke(pData.getStepData(), service);
			}
		};
		parseStep(pData, "INVOKE", commentConsumer);
		if (visitor != null) {
			visitor.endInvoke();
		}
	}

	protected void parseExit(Data pData) throws XMLStreamException {
		final XMLStreamReader rdr = pData.getReader();
		final String from = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "FROM");
		if (from == null  ||  from.length() == 0) {
			throw error(rdr, "Missing, or empty attribute: EXIT/@FROM");
		}
		final String signal = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "SIGNAL");
		if (signal == null  ||  signal.length() == 0) {
			throw error(rdr, "Missing, or empty attribute: EXIT/@SIGNAL");
		}
		final String failureMessage = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "FAILURE-MESSAGE");
		if (failureMessage == null  ||  failureMessage.length() == 0) {
			throw error(rdr, "Missing, or empty attribute: EXIT/@FAILURE-MESSAGE");
		}
		final String label = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "NAME");
		final String disabledStr = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "DISABLED");
		final boolean enabled = !"true".equalsIgnoreCase(disabledStr);
		final Consumer<String> commentConsumer = (s) -> {
			if (visitor != null) {
				pData.getStepData().setEnabled(enabled);
				pData.getStepData().setLabel(label);
				visitor.startExit(pData.getStepData(), from, signal, failureMessage);
			}
		};
		parseStep(pData, "EXIT", commentConsumer);
		if (visitor != null) {
			visitor.endExit();
		}
	}

	protected void skipElement(XMLStreamReader pReader, Consumer<String> pCommentConsumer) throws XMLStreamException {
		Consumer<String> commentConsumer = pCommentConsumer;
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
				if (isElementNS(pReader, XMLConstants.NULL_NS_URI, "COMMENT")) {
					if (commentConsumer != null) {
						final String text = pReader.getElementText();
						commentConsumer.accept(text);
						commentConsumer = null;
					}
				}
				skipElementRecursively(pReader);
				break;
			default:
				throw error(pReader, "Unexpected event: " + event);
			}
		}
		throw error(pReader, "Unexpected end of document, while waiting for end of element: "
					+ Sax.asQName(nsUri, localName));
	}

	protected void parseMapStep(Data pData) throws XMLStreamException {
		final XMLStreamReader rdr = pData.getReader();
		final String label = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "NAME");
		final String disabledStr = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "DISABLED");
		final boolean enabled = !"true".equalsIgnoreCase(disabledStr);
		final Consumer<String> commentConsumer = (s) -> {
			if (visitor != null) {
				pData.getStepData().setEnabled(enabled);
				pData.getStepData().setLabel(label);
				pData.getStepData().setComment(s);
				visitor.startMap(pData.getStepData());
			}
		};
		parseStep(pData, "MAP", commentConsumer);
		if (visitor != null) {
			visitor.endMap();
		}
	}

	protected void parseSequence(Data pData) throws XMLStreamException {
		final XMLStreamReader rdr = pData.getReader();
		final Consumer<String> commentConsumer = (s) -> {
			if (visitor != null) {
				final String label = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "NAME");
				final String disabledStr = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "DISABLED");
				final boolean enabled = !"true".equalsIgnoreCase(disabledStr);
				pData.getStepData().setEnabled(enabled);
				pData.getStepData().setLabel(label);
				visitor.startSequence(pData.getStepData());
			}
		};
		parseStepList(pData, "SEQUENCE", commentConsumer);
		if (visitor != null) {
			visitor.endSequence();
		}
	}

	protected void parseBranch(Data pData) throws XMLStreamException {
		final XMLStreamReader rdr = pData.getReader();
		final String label = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "NAME");
		final String disabledStr = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "DISABLED");
		final boolean enabled = !"true".equalsIgnoreCase(disabledStr);
		pData.getStepData().setEnabled(enabled);
		pData.getStepData().setLabel(label);
		final String switchStr = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "SWITCH");
		final String labelExpressionsStr = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "LABELEXPRESSIONS");
		final boolean evaluateLabels = "true".equalsIgnoreCase(labelExpressionsStr);
		final Consumer<String> commentConsumer = (s) -> {
			if (visitor != null) {
				visitor.startBranch(pData.getStepData(), switchStr, evaluateLabels);
			}
		};
		parseStepList(pData, "BRANCH", commentConsumer);
		if (visitor != null) {
			visitor.endBranch();
		}
	}

	protected void parseRepeat(Data pData) throws XMLStreamException {
		final XMLStreamReader rdr = pData.getReader();
		final String label = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "NAME");
		final String disabledStr = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "DISABLED");
		final boolean enabled = !"true".equalsIgnoreCase(disabledStr);
		final String count = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "COUNT");
		final String retryInterval = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "BACK-OFF");
		final String loopOn = rdr.getAttributeValue(XMLConstants.NULL_NS_URI, "LOOP-ON");
		final Consumer<String> commentConsumer = (s) -> {
			if (visitor != null) {
				pData.getStepData().setEnabled(enabled);
				pData.getStepData().setLabel(label);
				visitor.startRepeat(pData.getStepData(), count, retryInterval, loopOn);
			}
		};
		parseStepList(pData, "RETRY", commentConsumer);
		if (visitor != null) {
			visitor.endRepeat();
		}
	}

	@Override
	protected void parse(XMLStreamReader pReader) throws XMLStreamException {
		final Data data = new Data(pReader);
		skipToDocumentElement(pReader);
		assertElement(pReader, "FLOW");
		parseFlow(data);
		parseEndDocument(pReader);
	}

}
