package com.github.jochenw.qse.is.core.stax.flow;

import java.io.InputStream;
import java.util.function.Consumer;

import javax.xml.XMLConstants;
import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import com.github.jochenw.afw.core.util.Holder;
import com.github.jochenw.afw.core.util.Stax;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.MapActionListener;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.MapMode;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.StepInfo;


public class FlowXmlStaxParser {
	private final FlowXmlVisitor visitor;
	private final XMLStreamReader xsr;

	private FlowXmlStaxParser(XMLStreamReader pReader, FlowXmlVisitor pVisitor) {
		visitor = pVisitor;
		xsr = pReader;
	}

	protected boolean asBoolean(String pValue) {
		return "TRUE".equalsIgnoreCase(pValue);
	}

	protected void parseStepList(Consumer<StepInfo> pCommentConsumer) throws XMLStreamException {
		final String tagName = xsr.getLocalName();
		final String label = getAttribute("NAME");
		final String disabledStr = getAttribute("DISABLED");
		final boolean enabled = disabledStr == null   ||  !"TRUE".equalsIgnoreCase(disabledStr);
		boolean commentSeen = false;
		Location commentLocation = null;
		boolean stepSeen = false;
		while (xsr.hasNext()) {
			final int state = xsr.next();
			switch (state) {
			  case XMLStreamReader.CDATA:
			  case XMLStreamReader.CHARACTERS:
			  case XMLStreamReader.SPACE:
			  case XMLStreamReader.COMMENT:
				  break;
			  case XMLStreamReader.START_ELEMENT: {
				  Stax.assertDefaultNamespace(xsr);
				  final String eName = xsr.getLocalName();
				  if ("COMMENT".equals(eName)) {
					  if (!commentSeen) {
						  commentLocation = xsr.getLocation();
						  if (stepSeen) {
							  throw error("COMMENT element found after step has already been detected.");
						  }
						  final String comment = Stax.getElementText(xsr);
						  final StepInfo stepInfo = newStepInfo(label, comment, enabled);
						  pCommentConsumer.accept(stepInfo);
						  commentSeen = true;
					  } else {
						  throw error("COMMENT element repeated, first element at " + Stax.asLocation(commentLocation));
					  }
				  } else if ("MAP".equals(eName)) {
					  parseMapStep();
					  stepSeen = true;
				  } else if ("SEQUENCE".equals(eName)) {
					  parseSequenceStep();
					  stepSeen = true;
				  } else if ("BRANCH".equals(eName)) {
					  parseBranchStep();
					  stepSeen = true;
				  } else if ("INVOKE".equals(eName)) {
					  parseInvokeStep();
					  stepSeen = true;
				  } else if ("EXIT".equals(eName)) {
					  parseExitStep();
					  stepSeen = true;
				  } else if ("RETRY".equals(eName)) {
					  parseRepeatStep();
					  stepSeen = true;
				  } else {
					  // Do nothing, ignore this element.
				  }
				  break;
			  }
			  case XMLStreamReader.END_ELEMENT: {
				  Stax.assertDefaultNamespace(xsr);
				  final String eName = xsr.getLocalName();
				  if (tagName.equals(eName)) {
					  return;
				  } else {
					  throw error("Expected /" + tagName + ", got /" + eName); 
				  }
			  }
			  default:
				  throw error("Unexpected state: " + state);
			}
		}
		throw error("Unexpected end of document");
	}

	protected StepInfo getStepInfo() {
		final String label = getAttribute("NAME");
		final boolean enabled = !asBoolean(getAttribute("DISABLED"));
		return newStepInfo(label, null, enabled);
	}

	private StepInfo newStepInfo(final String pLabel, final String pComment, final boolean pEnabled) {
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

	protected void parseExitStep() throws XMLStreamException {
		final String label = getAttribute("NAME");
		final String from = getAttribute("FROM");
		final String signal = getAttribute("SIGNAL");
		final String failureMessage = getAttribute("FAILURE-MESSAGE");
		final boolean enabled = !asBoolean(getAttribute("DISABLED"));
		String comment = null;
		Location commentLocation = null;
		while (xsr.hasNext()) {
			final int state = xsr.next();
			switch (state) {
			case XMLStreamReader.CHARACTERS:
			case XMLStreamReader.CDATA:
			case XMLStreamReader.SPACE:
			case XMLStreamReader.COMMENT:
				break;
			case XMLStreamReader.START_ELEMENT: {
				Stax.assertDefaultNamespace(xsr);
				final String eName = xsr.getLocalName();
				if ("COMMENT".equals(eName)) {
					if (comment == null) {
						commentLocation = xsr.getLocation();
						comment = Stax.getElementText(xsr);
						if (visitor != null) {
							final StepInfo stepInfo = newStepInfo(label, comment, enabled);
							visitor.startExit(stepInfo, from, signal, failureMessage);
						}
					} else {
						throw error("Repeated COMMENT element found, first element at"
								+ Stax.asLocation(commentLocation));
					}
				} else {
					Stax.skipElementRecursively(xsr);
				}
				break;
			}
			case XMLStreamReader.END_ELEMENT: {
				Stax.assertDefaultNamespace(xsr);
				final String eName = xsr.getLocalName();
				if ("EXIT".equals(eName)) {
					if (comment == null) {
						if (visitor != null) {
							final StepInfo stepInfo = newStepInfo(label, comment, enabled);
							visitor.startExit(stepInfo, from, signal, failureMessage);
						}
					}
					if (visitor != null) {
						visitor.endExit();
					}
					return;
				} else {
					throw error("Expected /EXIT, got /" + eName);
				}
			}
			default:
				throw error("Unexpected state: " + state);
			}
		}
	}

	protected void parseRepeatStep() throws XMLStreamException {
		final String count = getAttribute("COUNT");
		final String retryInterval = getAttribute("BACK-OFF");
		final String loopOn = getAttribute("LOOP-ON");
		final Consumer<StepInfo> consumer = (stepInfo) -> {
			if (visitor != null) {
				visitor.startRepeat(stepInfo, count, retryInterval, loopOn);
			}
		};
		parseStepList(consumer);
		if (visitor != null) {
			visitor.endRepeat();
		}
		
	}

	protected void parseMapStep() throws XMLStreamException {
		final String label = getAttribute("NAME");
		final boolean enabled = !asBoolean(getAttribute("DISABLED"));
		MapActionListener listener = null;
		boolean commentSeen = false;
		Location commentLocation = null;
		while (xsr.hasNext()) {
			final int state = xsr.next();
			switch (state) {
			case XMLStreamReader.CDATA:
			case XMLStreamReader.CHARACTERS:
			case XMLStreamReader.SPACE:
			case XMLStreamReader.COMMENT:
				// Ignore this
				break;
			case XMLStreamReader.START_ELEMENT:
				Stax.assertDefaultNamespace(xsr);
				final String eName = xsr.getLocalName();
				if ("MAPTARGET".equals(eName)) {
					Stax.skipElementRecursively(xsr);
				} else if ("MAPSOURCE".equals(eName)) {
					Stax.skipElementRecursively(xsr);
				} else if ("MAPCOPY".equals(eName)) {
					parseMapCopy(listener, MapMode.STANDALONE);
					break;
				} else if ("MAPINVOKE".equals(eName)) {
					parseMapInvoke(listener, MapMode.STANDALONE);
				} else if ("MAPDELETE".equals(eName)) {
					parseMapDelete(listener, MapMode.STANDALONE);
				} else if ("MAPSET".equals(eName)) {
					parseMapSet(listener, MapMode.STANDALONE);
				} else if ("COMMENT".equals(eName)) {
					if (commentSeen) {
						throw error("Unexpected repeated COMMENT element, first instance: " + Stax.asLocation(commentLocation));
					} else {
						commentLocation = xsr.getLocation();
						final String comment = Stax.getElementText(xsr);
						if (visitor != null) {
							final StepInfo stepInfo = newStepInfo(label, comment, enabled);
							listener = visitor.startMap(stepInfo);
						}
						commentSeen = true;
					}
				} else {
					throw error("Unexpected start element: " + eName);
				}
				break;
			case XMLStreamReader.END_ELEMENT:
				if ("MAP".equals(xsr.getLocalName())) {
					if (!commentSeen) {
						final StepInfo stepInfo = newStepInfo(label, null, enabled);
						if (visitor != null) {
							listener = visitor.startMap(stepInfo);
						}
					}
					if (visitor != null) {
						visitor.endMap();
					}
					return;
				} else {
					throw error("Unexpected end element: " + xsr.getLocalName());
				}
			default:
				throw error("Unexpected state: " + state);
			}
		}
	}

	protected XMLStreamException error(String pMsg) {
		return Stax.error(xsr, pMsg);
	}

	protected void parseMapData(MapActionListener pListener, MapMode pMode) throws XMLStreamException {
		final String tagName = xsr.getLocalName();
		while (xsr.hasNext()) {
			final int state = xsr.next();
			switch (state) {
			case XMLStreamReader.CDATA:
			case XMLStreamReader.CHARACTERS:
			case XMLStreamReader.SPACE:
			case XMLStreamReader.COMMENT:
				// Ignore this
				break;
			case XMLStreamReader.START_ELEMENT:
				Stax.assertDefaultNamespace(xsr);
				final String eName = xsr.getLocalName();
				if ("MAPTARGET".equals(eName)) {
					Stax.skipElementRecursively(xsr);
				} else if ("MAPSOURCE".equals(eName)) {
					Stax.skipElementRecursively(xsr);
				} else if ("MAPCOPY".equals(eName)) {
					parseMapCopy(pListener, pMode);
					break;
				} else if ("MAPINVOKE".equals(eName)) {
					parseMapInvoke(pListener, pMode);
				} else if ("MAPDELETE".equals(eName)) {
					parseMapDelete(pListener, pMode);
				} else if ("MAPSET".equals(eName)) {
					parseMapSet(pListener, pMode);
				} else {
					throw error("Unexpected start element: " + eName);
				}
				break;
			case XMLStreamReader.END_ELEMENT:
				if (tagName.equals(xsr.getLocalName())) {
					return;
				} else {
					throw error("Unexpected end element: " + xsr.getLocalName());
				}
			default:
				throw error("Unexpected state: " + state);
			}
		}
	}

	private void parseMapSet(MapActionListener pListener, MapMode pMode) throws XMLStreamException {
		final String field = getAttribute("FIELD");
		if (field == null  ||  field.trim().length() == 0) {
			throw new IllegalStateException("Missing, or empty attribute: MAPSET/@FIELD");
		}
		OUTER2: while (xsr.hasNext()) {
			final int state2 = xsr.next();
			switch (state2) {
			case XMLStreamReader.CDATA:
			case XMLStreamReader.CHARACTERS:
			case XMLStreamReader.SPACE:
			case XMLStreamReader.COMMENT:
				// Ignore this
				break;
			case XMLStreamReader.START_ELEMENT:
				Stax.assertDefaultNamespace(xsr);
				if ("DATA".equals(xsr.getLocalName())  &&  "XMLValues".equals(getAttribute("ENCODING"))) {
					OUTER3: while (xsr.hasNext()) {
						final int state3 = xsr.next();
						switch (state3) {
						case XMLStreamReader.CDATA:
						case XMLStreamReader.CHARACTERS:
						case XMLStreamReader.SPACE:
						case XMLStreamReader.COMMENT:
							// Ignore this
							break;
						case XMLStreamReader.START_ELEMENT:
							if ("Values".equals(xsr.getLocalName())) {
								String fieldType = null;
								String fieldName = null;
								String fieldValue = null;
								OUTER4: while (xsr.hasNext()) {
									final int state4 = xsr.next();
									switch (state4) {
									case XMLStreamReader.CDATA:
									case XMLStreamReader.CHARACTERS:
									case XMLStreamReader.SPACE:
									case XMLStreamReader.COMMENT:
										// Ignore this
										break;
									case XMLStreamReader.START_ELEMENT:
										if ("value".equals(xsr.getLocalName())  &&  "xml".equals(getAttribute("name"))) {
											fieldValue = xsr.getElementText();
											break;
										} else if ("record".equals(xsr.getLocalName())  &&  "type".equals(getAttribute("name"))  &&  "com.wm.util.Values".equals(getAttribute("javaclass"))) {
											OUTER5: while (xsr.hasNext()) {
												final int state5 = xsr.next();
												switch (state5) {
												case XMLStreamReader.CDATA:
												case XMLStreamReader.CHARACTERS:
												case XMLStreamReader.SPACE:
												case XMLStreamReader.COMMENT:
													// Ignore this
													break;
												case XMLStreamReader.START_ELEMENT:
													if ("value".equals(xsr.getLocalName())) {
														final String name = getAttribute("name");
														if ("field_type".equals(name)) {
															fieldType = xsr.getElementText();
															if (!"string".equals(fieldType)) {
																throw error("Unexpected field type for MAPSET: " + fieldType);
															}
														} else if ("field_name".equals(name)) {
															fieldName = xsr.getElementText();
														}
													}
												case XMLStreamReader.END_ELEMENT:
													if ("record".equals(xsr.getLocalName())) {
														break OUTER5;
													}
													break;
												default:
													throw error("Unexpexted event: " + state3);
												}
											}
											if (fieldType == null) {
												throw error("Field type not found");
											} else if (fieldName == null) {
												throw error("Field name not found");
											} else if (fieldValue == null) {
												throw error("Field value not found");
											} else {
												if (pListener != null) {
													pListener.setValue(pMode, null, field, fieldValue);
												}
												break;
											}
										}
									case XMLStreamReader.END_ELEMENT:
										if ("Values".equals(xsr.getLocalName())) {
											break OUTER4;
										}
										break;
									default:
										throw error("Unexpexted event: " + state3);
									}
								}
							}
							break;
						case XMLStreamReader.END_ELEMENT:
							if ("DATA".equals(xsr.getLocalName())) {
								break OUTER3;
							}
							break;
						default:
							throw error("Unexpexted event: " + state3);
						}
					}
				}
				// Ignore this
				break;
			case XMLStreamReader.END_ELEMENT: {
				if ("MAPSET".equals(xsr.getLocalName())) {
					break OUTER2;
				}
				// Ignore this
				break;
			}
			default:
				throw error("Unexpexted event: " + state2);
			}
		}
	}

	private void parseMapDelete(MapActionListener pListener, MapMode pMode) throws XMLStreamException {
		final String field = getAttribute("FIELD");
		if (field == null  ||  field.trim().length() == 0) {
			throw new IllegalStateException("Missing, or empty attribute: MAPDELETE/@FIELD");
		}
		if (pListener != null) {
			pListener.drop(pMode, null, field);
		}
		Stax.skipElementRecursively(xsr);
	}

	private void parseMapInvoke(MapActionListener pListener, MapMode pMode)
			throws XMLStreamException {
		final String service = getAttribute("SERVICE");
		if (service == null  ||  service.trim().length() == 0) {
			throw new IllegalStateException("Missing, or empty, attribute: MAPINVOKE/@SERVICE");
		}
		boolean haveInputMap = false;
		boolean haveOutputMap = false;
		while (xsr.hasNext()) {
			final int state2 = xsr.next();
			switch (state2) {
			case XMLStreamReader.CDATA:
			case XMLStreamReader.CHARACTERS:
			case XMLStreamReader.SPACE:
			case XMLStreamReader.COMMENT:
				// Ignore this
				break;
			case XMLStreamReader.START_ELEMENT: {
				Stax.assertDefaultNamespace(xsr);
				final String eName2 = xsr.getLocalName();
				if ("MAP".equals(eName2)) {
					final String mode = getAttribute("MODE");
					MapMode mapMode;
					if ("INVOKEINPUT".equals(mode)) {
						mapMode = MapMode.INPUT;
						haveInputMap = true;
					} else if ("INVOKEOUTPUT".equals(mode)) {
						mapMode = MapMode.OUTPUT;
						haveOutputMap = true;
					} else {
						throw error("Invalid value for MAPINVOKE/MAP/@MODE: " + mode);
					}
					final MapActionListener mapActionListener;
					if (pListener == null) {
						mapActionListener = null;
					} else {
						mapActionListener = pListener.invoke(pMode, null, service);
					}
					parseMapData(mapActionListener, mapMode);
				}
				break;
			}
			case XMLStreamReader.END_ELEMENT: {
				Stax.assertDefaultNamespace(xsr);
				final String eName2 = xsr.getLocalName();
				if ("MAPINVOKE".equals(eName2)) {
					return;
				} else {
					throw error("Expected /MAPINVOKE, got /" + eName2);
				}
			}
			default:
				throw error("Unexpected state: " + state2);
			}
		}
		if (!haveInputMap) {
			throw new IllegalStateException("Element not found: MAPINVOKE/MAP[@MODE='INVOKEINPUT']");
		}
		if (!haveOutputMap) {
			throw new IllegalStateException("Element not found: MAPINVOKE/MAP[@MODE='INVOKEOUTPUT']");
		}
	}

	private void parseMapCopy(MapActionListener pListener, MapMode pMode) throws XMLStreamException {
		final String fromString = getAttribute("FROM");
		if (fromString == null) {
			throw new IllegalStateException("Missing attribute: MAPCOPY/@FROM");
		}
		final String toString = getAttribute("TO");
		if (toString == null) {
			throw new IllegalStateException("Missing attribute: MAPCOPY/@TO");
		}
		if (pListener != null) {
			pListener.copy(pMode, null, fromString, toString);
		}
		Stax.skipElementRecursively(xsr);
	}

	protected void parseSequenceStep() throws XMLStreamException {
		final Consumer<StepInfo> consumer = (stepInfo) -> {
			if (visitor != null) {
				visitor.startSequence(stepInfo);
			}
		};
		parseStepList(consumer);
		if (visitor != null) {
			visitor.endSequence();
		}
	}

	protected void parseBranchStep() throws XMLStreamException {
		final String switchExpr = getAttribute("SWITCH");
		final boolean evaluateLabels = asBoolean(getAttribute("LABELEXPRESSIONS"));
		final Consumer<StepInfo> consumer = (stepInfo) -> {
			if (visitor != null) {
				visitor.startBranch(stepInfo, switchExpr, evaluateLabels);
			}
		};
		parseStepList(consumer);
		if (visitor != null) {
			visitor.endBranch();
		}
	}

	protected void parseInvokeStep() throws XMLStreamException {
		final String service = getAttribute("SERVICE");
		final String label = getAttribute("NAME");
		final String disabledStr = getAttribute("DISABLED");
		final boolean enabled = disabledStr == null  ||  !"true".equalsIgnoreCase(disabledStr);
		if (service == null  ||  service.trim().length() == 0) {
			throw error("Missing, or empty, attribute: INVOKE/@SERVICE");
		}
		final Holder<MapActionListener> mapActionListenerHolder = new Holder<MapActionListener>();
		Consumer<StepInfo> consumer = (stepInfo) -> {
			if (visitor == null) {
				mapActionListenerHolder.set(null);
			} else {
				mapActionListenerHolder.set(visitor.startInvoke(stepInfo, service));
			}
		};
		OUTER: while (xsr.hasNext()) {
			final int state = xsr.next();
			switch (state) {
				case XMLStreamReader.COMMENT:
				case XMLStreamReader.CDATA:
				case XMLStreamReader.CHARACTERS:
				case XMLStreamReader.SPACE:
					break;
				case XMLStreamReader.START_ELEMENT: {
					Stax.assertDefaultNamespace(xsr);
					final String eName = xsr.getLocalName();
					if ("MAP".equals(eName)) {
						final String mode = getAttribute("MODE");
						if ("INPUT".equals(mode)) {
							parseMapData(mapActionListenerHolder.get(), MapMode.INPUT);
						} else if ("OUTPUT".equals(mode)) {
							parseMapData(mapActionListenerHolder.get(), MapMode.OUTPUT);
						} else {
							throw error("Invalid map mode: " + mode);
						}
					} else if ("COMMENT".equals(eName)) {
						if (consumer == null) {
							throw error("Multiple COMMENT elements found.");
						} else {
							final String comment = xsr.getElementText();
							final StepInfo stepInfo = newStepInfo(label, comment, enabled);
							consumer.accept(stepInfo);
							consumer = null;
						}
					}
					break;
				}
				case XMLStreamReader.END_ELEMENT: {
					Stax.assertDefaultNamespace(xsr);
					final String eName = xsr.getLocalName();
					if ("INVOKE".equals(eName)) {
						break OUTER;
					}
					break;
				}
				default:
					throw error("Unexpected state: " + state);
			}
		}
		if (visitor != null) {
			visitor.endInvoke();
		}
	}

	protected void assertElement(String pTagName) throws XMLStreamException {
		Stax.assertElement(xsr, pTagName);
	}

	protected String getAttribute(String pName) {
		return xsr.getAttributeValue(XMLConstants.NULL_NS_URI, pName);
	}

	protected void parseFlow() throws XMLStreamException  {
		while (xsr.hasNext()) {
			final int state = xsr.next();
			switch (state) {
			case XMLStreamReader.START_DOCUMENT:
				break;
			case XMLStreamReader.START_ELEMENT:
				assertElement("FLOW");
				final String version = getAttribute("VERSION");
				final boolean cleanUp = asBoolean(getAttribute("CLEANUP"));
				final Consumer<StepInfo> consumer = (stepInfo) -> {
					if (visitor != null) {
						visitor.startFlow(stepInfo, version, cleanUp);
					}
				};
				parseStepList(consumer);
				if (visitor != null) {
					visitor.endFlow();
				}
				break;
			case XMLStreamReader.END_DOCUMENT:
				return;
			default:
				throw error("Expected START_ELEMENT, got " + state);
			}
		}
	}

	public static void parse(InputStream pIn, String pUri, FlowXmlVisitor visitor) throws XMLStreamException {
		final XMLInputFactory xif = XMLInputFactory.newInstance();
		final XMLStreamReader xsr = xif.createXMLStreamReader(pUri, pIn);
		new FlowXmlStaxParser(xsr, visitor).parseFlow();
	}
}
