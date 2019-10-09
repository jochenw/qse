package com.github.jochenw.qse.is.core.stax.flow;

import java.io.IOException;
import java.lang.reflect.UndeclaredThrowableException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.github.jochenw.afw.core.util.LocalizableDocument;
import com.github.jochenw.afw.core.util.Stax;
import com.github.jochenw.afw.core.util.Strings;
import com.github.jochenw.qse.is.core.sax.Sax;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.MapActionListener;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.MapMode;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.StepInfo;


public class FlowXmlParser {
	private final FlowXmlVisitor visitor;
	private LocalizableDocument lDoc;

	private FlowXmlParser(FlowXmlVisitor pVisitor) {
		visitor = pVisitor;
	}

	protected String asQName(Node pNode) {
		return Sax.asQName(pNode.getNamespaceURI(), pNode.getLocalName());
	}

	protected boolean isElement(Node pNode, String pTagName) {
		if (pNode.getNodeType() == Node.ELEMENT_NODE) {
			if (isDefaultNamespace(pNode)) {
				if (pTagName.equals(pNode.getLocalName())) {
					return true;
				}
			}
		}
		return false;
	}

	protected StepInfo newStepInfo(String pLabel, String pComment, boolean pEnabled) {
		return new StepInfo() {
			@Override
			public boolean isEnabled() {
				return pEnabled;
			}

			@Override
			public String getComment() {
				return pComment;
			}

			@Override
			public String getLabel() {
				return pLabel;
			}
		};
	}

	protected String getAttribute(Element pElement, String pAttributeName) {
		final Attr attr = pElement.getAttributeNode(pAttributeName);
		return attr == null ? null : attr.getNodeValue();
	}

	protected boolean asBoolean(String pValue) {
		return "TRUE".equalsIgnoreCase(pValue);
	}

	protected void parseStepList(Element pElement) {
		for (Node node = pElement.getFirstChild();  node != null;  node = node.getNextSibling()) {
			if (node.getNodeType() == Node.ELEMENT_NODE  && isDefaultNamespace(node)) {
				final Element e = (Element) node;
				if (isElement(e, "MAP")) {
					parseMapStep(e);
				} else if (isElement(e, "SEQUENCE")) {
					parseSequenceStep(e);
				} else if (isElement(e, "BRANCH")) {
					parseBranchStep(e);
				} else if (isElement(e, "INVOKE")) {
					parseInvokeStep(e);
				} else if (isElement(e, "EXIT")) {
					parseExitStep(e);
				} else if (isElement(e, "RETRY")) {
					parseRepeatStep(e);
				} else {
					// Do nothing, ignore this element.
				}
			}
		}
	}

	protected StepInfo getStepInfo(Element pElement) {
		final Attr attr = pElement.getAttributeNode("NAME");
		final String label = attr == null ? null : attr.getValue();
		final boolean enabled = !asBoolean(getAttribute(pElement, "DISABLED"));
		for (Node node = pElement.getFirstChild();  node != null;  node = node.getNextSibling()) {
			if (isElement(node, "COMMENT")) {
				return newStepInfo(label, getTextContent((Element) node), enabled);
			}
		}
		return newStepInfo(label, "", enabled);
	}

	protected void parseExitStep(Element pElement) {
		final StepInfo stepInfo = getStepInfo(pElement);
		if (visitor != null) {
			final String from = getAttribute(pElement, "FROM");
			final String signal = getAttribute(pElement, "SIGNAL");
			final String failureMessage = getAttribute(pElement, "FAILURE-MESSAGE");
			visitor.startExit(stepInfo, from, signal, failureMessage);
			visitor.endExit();
		}
		
	}

	protected void parseRepeatStep(Element pElement) {
		final StepInfo stepInfo = getStepInfo(pElement);
		final String count = getAttribute(pElement, "COUNT");
		final String retryInterval = getAttribute(pElement, "BACK-OFF");
		final String loopOn = getAttribute(pElement, "LOOP-ON");
		if (visitor != null) {
			visitor.startRepeat(stepInfo, count, retryInterval, loopOn);
		}
		parseStepList(pElement);
		if (visitor != null) {
			visitor.endRepeat();
		}
		
	}

	protected void parseMapStep(Element pElement) {
		final StepInfo stepInfo = getStepInfo(pElement);
		if (visitor != null) {
			final MapActionListener mapActionListener = visitor.startMap(stepInfo);
			parseMapData(pElement, mapActionListener, MapMode.STANDALONE);
		} else {
			parseMapData(pElement, null, MapMode.STANDALONE);
		}
		if (visitor != null) {
			visitor.endMap();
		}
	}

	protected RuntimeException error(Node pNode, String pMsg) {
		final Locator loc = lDoc.getLocator(pNode);
		if (loc == null) {
			return new IllegalArgumentException(pMsg);
		} else {
			return new IllegalArgumentException(Stax.asLocalizedMessage(pMsg, loc.getSystemId(), loc.getLineNumber(), loc.getColumnNumber()));
		}
	}

	protected String findText(Element pElement, String pTagName, String... pAttributes) {
		final Element e = findFirstChild(pElement, pTagName, pAttributes);
		if (e == null) {
			return null;
		} else {
			return getTextContent(e);
		}
	}

	protected Element findFirstChild(Element pElement, String pTagName, String... pAttributes) {
		for (Node node = pElement.getFirstChild();  node != null;  node = node.getNextSibling()) {
			if (node.getNodeType() == Node.ELEMENT_NODE  &&  isDefaultNamespace(node)) {
				final Element e = (Element) node;
				boolean attributesMatching = true;
				if (pAttributes != null  &&  pAttributes.length > 0) {
					for (int i = 0;  i < pAttributes.length;  i += 2) {
						final String name = pAttributes[i];
						final String value = pAttributes[i+1];
						if (!value.equals(getAttribute(e, name))) {
							attributesMatching = false;
							break;
						}
					}
				}
				if (attributesMatching) {
					return e;
				}
			}
		}
		return null;
	}

	protected void parseMapData(Element pElement, MapActionListener pListener, MapMode pMode) {
		Element mapTargetElement = null;
		Element mapSourceElement = null;
		for (Node node = pElement.getFirstChild();  node != null;  node = node.getNextSibling()) {
			if (node.getNodeType() == Node.ELEMENT_NODE  &&  isDefaultNamespace(node)) {
				final Element e = (Element) node;
				final String localName = node.getLocalName();
				if ("MAPTARGET".equals(localName)) {
					if (mapTargetElement == null) {
						mapTargetElement = (Element) node;
					}
				} else if ("MAPSOURCE".equals(localName)) {
					if (mapSourceElement == null) {
						mapSourceElement = (Element) node;
					}
				} else if ("MAPCOPY".equals(localName)) {
					final String fromString = getAttribute(e, "FROM");
					if (fromString == null) {
						throw new IllegalStateException("Missing attribute: MAPCOPY/@FROM");
					}
					final String toString = getAttribute(e, "TO");
					if (toString == null) {
						throw new IllegalStateException("Missing attribute: MAPCOPY/@TO");
					}
					if (pListener != null) {
						pListener.copy(pMode, null, fromString, toString);
					}
				} else if ("MAPINVOKE".equals(localName)) {
					final String service = getAttribute(e, "SERVICE");
					if (service == null  ||  service.trim().length() == 0) {
						throw new IllegalStateException("Missing, or empty, attribute: MAPINVOKE/@SERVICE");
					}
					boolean haveInputMap = false;
					boolean haveOutputMap = false;
					for (Node node2 = e.getFirstChild();  node2 != null;  node2 = node2.getNextSibling()) {
						if (node2.getNodeType() == Node.ELEMENT_NODE  &&  isDefaultNamespace(node2)) {
							final Element e2 = (Element) node2;
							if ("MAP".equals(e2.getLocalName())) {
								final String mode = getAttribute(e2, "MODE");
								MapMode mapMode;
								if ("INVOKEINPUT".equals(mode)) {
									mapMode = MapMode.INPUT;
									haveInputMap = true;
								} else if ("INVOKEOUTPUT".equals(mode)) {
									mapMode = MapMode.OUTPUT;
									haveOutputMap = true;
								} else {
									throw new IllegalStateException("Invalid value for MAPINVOKE/MAP/@MODE: " + mode);
								}
								final MapActionListener mapActionListener;
								if (pListener == null) {
									mapActionListener = null;
								} else {
									mapActionListener = pListener.invoke(pMode, null, service);
								}
								parseMapData(e2, mapActionListener, mapMode);
							}
						}
					}
					if (!haveInputMap) {
						throw new IllegalStateException("Element not found: MAPINVOKE/MAP[@MODE='INVOKEINPUT']");
					}
					if (!haveOutputMap) {
						throw new IllegalStateException("Element not found: MAPINVOKE/MAP[@MODE='INVOKEOUTPUT']");
					}
				} else if ("MAPDELETE".equals(localName)) {
					final String field = getAttribute(e, "FIELD");
					if (field == null  ||  field.trim().length() == 0) {
						throw new IllegalStateException("Missing, or empty attribute: MAPDELETE/@FIELD");
					}
					if (pListener != null) {
						pListener.drop(pMode, null, field);
					}
				} else if ("MAPSET".equals(localName)) {
					final String field = getAttribute(e, "FIELD");
					if (field == null  ||  field.trim().length() == 0) {
						throw new IllegalStateException("Missing, or empty attribute: MAPSET/@FIELD");
					}
					final Element dataElement = findFirstChild(e, "DATA", "ENCODING", "XMLValues");
					if (dataElement == null) {
						throw new IllegalStateException("Missing child element: MA/MAPSET/DATA[@ENCODING='XMLValues']");
					}
					final Element valuesElement = findFirstChild(dataElement, "Values");
					if (valuesElement == null) {
						throw new IllegalStateException("Missing child element: MAPSET/DATA/Values");
					}
					final Element recordElement = findFirstChild(valuesElement, "record", "name", "type", "javaclass", "com.wm.util.Values");
					if (recordElement == null) {
						throw new IllegalStateException("Missing child element: MAPSET/DATA/Values/record");
					}
					final String fieldType = findText(recordElement, "value", "name", "field_type");
					if (!"string".equals(fieldType)) {
						throw error(recordElement, "Invalid field_type: " + fieldType);
					}
					final String fieldName = findText(recordElement, "value", "name", "field_name");
					final Element valueElement = findFirstChild(valuesElement, "value", "name", "xml");
					if (valueElement == null) {
						throw error(recordElement, "Unable to determine value for MAPSET");
					}
					final String value = getTextContent(valueElement);
					if (pListener != null) {
						pListener.setValue(pMode, null, field, value);
					}
				}
			}
		}
	}

	protected String getTextContent(Element pElement) {
		final StringBuilder sb = new StringBuilder();
		for (Node node = pElement.getFirstChild();  node != null;  node = node.getNextSibling()) {
			switch (node.getNodeType()) {
			case Node.CDATA_SECTION_NODE:
			case Node.TEXT_NODE:
				sb.append(node.getNodeValue());
				break;
			default:
				throw error(pElement, "Invalid node type: " + node.getNodeType());
			}
		}
		return sb.toString();
	}
	protected void parseSequenceStep(Element pElement) {
		final StepInfo stepInfo = getStepInfo(pElement);
		if (visitor != null) {
			visitor.startSequence(stepInfo);
		}
		parseStepList(pElement);
		if (visitor != null) {
			visitor.endSequence();
		}
	}

	protected void parseBranchStep(Element pElement) {
		final StepInfo stepInfo = getStepInfo(pElement);
		final String switchExpr = getAttribute(pElement, "SWITCH");
		final boolean evaluateLabels = asBoolean(getAttribute(pElement, "LABELEXPRESSIONS"));
		if (visitor != null) {
			visitor.startBranch(stepInfo, switchExpr, evaluateLabels);
		}
		parseStepList(pElement);
		if (visitor != null) {
			visitor.endBranch();
		}
	}

	protected void parseInvokeStep(Element pElement) {
		final StepInfo stepInfo = getStepInfo(pElement);
		final String service = getAttribute(pElement, "SERVICE");
		if (service == null  ||  service.trim().length() == 0) {
			throw error(pElement, "Missing, or empty, attribute: INVOKE/@SERVICE");
		}
		MapActionListener mapActionListener;
		if (visitor == null) {
			mapActionListener = null;
		} else {
			mapActionListener = visitor.startInvoke(stepInfo, service);
		}
		final Element inputMapElement = findFirstChild(pElement, "MAP", "MODE", "INPUT");
		if (inputMapElement != null) {
			parseMapData(inputMapElement, mapActionListener, MapMode.INPUT);
		}
		final Element outputMapElement = findFirstChild(pElement, "MAP", "MODE", "OUTPUT");
		if (outputMapElement != null) {
			parseMapData(outputMapElement, mapActionListener, MapMode.OUTPUT);
		}
		if (visitor != null) {
			visitor.endInvoke();
		}
	}

	protected boolean isDefaultNamespace(Node pNode) {
		final String uri = pNode.getNamespaceURI();
		return uri == null  ||  uri.length() == 0;
	}

	protected void assertElement(Node pNode, String pTagName) {
		if (!isElement(pNode, pTagName)) {
			throw new IllegalStateException("Expected FLOW element, got " + asQName(pNode));
		}
	}

	protected void parseFlow(Element pElement)  {
		assertElement(pElement, "FLOW");
		final Element flowElement = (Element) pElement;
		final StepInfo stepInfo = newStepInfo(null, "", true);
		final String version = getAttribute(flowElement, "VERSION");
		final boolean cleanUp = asBoolean(getAttribute(flowElement, "CLEANUP"));
		if (visitor != null) {
			visitor.startFlow(stepInfo, version, cleanUp);
		}
		parseStepList((Element) flowElement);
		if (visitor != null) {
			visitor.endFlow();
		}
	}

	protected void parse(Document pDoc) {
		final Element e = pDoc.getDocumentElement();
		parseFlow(e);
	}

	protected void parse(InputSource pSource) {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setValidating(false);
		lDoc = LocalizableDocument.parse(pSource);
		final Document doc = lDoc.getDocument();
		parse(doc);
	}

	public static void parse(InputSource isource, FlowXmlVisitor visitor) {
		new FlowXmlParser(visitor).parse(isource);
	}
}
