package com.github.jochenw.qse.is.core.rules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.xml.XMLConstants;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import com.github.jochenw.qse.is.core.sax.AbstractContentHandler;


public class InvocationValidatingServiceParser extends AbstractContentHandler {
	public static class Invocation {
		private final int level;
		private final String serviceName;
		private final Locator locator;
		private boolean inMap, inMapTarget, inValues, inRecord, inArray, inRecord2;
		private boolean inMapSet, inData, inValues2;
		private final Map<String,String> parametersByName = new HashMap<>();
		private String mapSetField;
		Invocation(int pLevel,@Nonnull String pServiceName, @Nonnull Locator pLocator) {
			level = pLevel;
			serviceName = pServiceName;
			locator = pLocator;
		}
		public String getServiceName() {
			return serviceName;
		}
		public Map<String,String> getParameters() {
			return parametersByName;
		}
		public String getParameterValue(@Nonnull String pName) {
			return parametersByName.get(pName);
		}
		public Locator getLocator() {
			return locator;
		}
	}
	private List<Invocation> context = new ArrayList<>();
	private Invocation currentInvocation;

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		setCurrentInvocation(null);
		context.clear();
	}

	@Override
	public void endDocument() throws SAXException {
		if (getCurrentInvocation() != null  ||  !context.isEmpty()) {
			throw error("endDocument within invocation");
		}
	}
	
	@Override
	public void startElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName, Attributes pAttrs) throws SAXException {
		super.startElement(pUri, pLocalName, pQName, pAttrs);
		if (isElement("INVOKE", pUri, pLocalName)
				||  isElement("MAPINVOKE", pUri, pLocalName)) {
			final String serviceName = pAttrs.getValue(XMLConstants.NULL_NS_URI, "SERVICE");
			if (serviceName == null) {
				throw error("Missing attribute: " + pQName + "/@SERVICE");
			}
			final Invocation invocation = new Invocation(getLevel(), serviceName, getLocator());
			if (getCurrentInvocation() != null) {
				context.add(getCurrentInvocation());
			}
			setCurrentInvocation(invocation);
		} else if (getCurrentInvocation() != null) {
			final Invocation invocation = getCurrentInvocation();
			final int invocationLevel = getCurrentInvocation().level;
			final int currentLevel = getLevel();
			switch(currentLevel-invocationLevel) {
			  case 1:
				  if (isElement("MAP", pUri, pLocalName)) {
					  invocation.inMap = true;
				  }
				  break;
			  case 2:
				  if (invocation.inMap) {
					  if (isElement("MAPTARGET", pUri, pLocalName)) {
						  invocation.inMapTarget = true;
					  } else if (isElement("MAPSET", pUri, pLocalName)) {
						  invocation.inMapSet = true;
						  invocation.mapSetField = pAttrs.getValue(XMLConstants.NULL_NS_URI, "FIELD");
					  }
				  }
				  break;
			  case 3:
				  if (invocation.inMapTarget  &&  isElement("Values", pUri, pLocalName)) {
					  invocation.inValues = true;
				  } else if (invocation.inMapSet  &&  invocation.mapSetField != null
                          &&  isElement("DATA", pUri, pLocalName)) {
					  invocation.inData = true;
				  }
				  break;
			  case 4:
				  final String javaclass = pAttrs.getValue(XMLConstants.NULL_NS_URI, "javaclass");
				  if (invocation.inValues  &&  isElement("record", pUri, pLocalName)
						                      &&  "com.wm.util.Values".equals(javaclass)) {
					  invocation.inRecord = true;
				  } else if (invocation.inData  &&  isElement("Values", pUri, pLocalName)) {
					  invocation.inValues2 = true;
				  }
				  break;
			  case 5:
				  final String name = pAttrs.getValue(XMLConstants.NULL_NS_URI, "name");
				  final String type = pAttrs.getValue(XMLConstants.NULL_NS_URI, "type");
				  final String depth = pAttrs.getValue(XMLConstants.NULL_NS_URI, "depth");
				  if (invocation.inRecord  &&  isElement("array", pUri, pLocalName)
						                   &&  "rec_fields".equals(name)
						                   &&  "record".equals(type)
						                   &&  (depth == null  ||  "1".equals(depth))) {
					  invocation.inArray = true;
				  } else if (invocation.inValues2  &&  invocation.mapSetField != null
                          &&  isElement("value", pUri, pLocalName)
                          &&  "xml".equals(name)) {
					  startCollecting(currentLevel-1, (s) -> {
						  final String mapSetField = invocation.mapSetField;
						  if (mapSetField.startsWith("/")  &&  mapSetField.endsWith(";1;0")) {
							  final String parameterName = mapSetField.substring("/".length(), mapSetField.length()-";1;0".length());
							  if (invocation.parametersByName.containsKey(parameterName)) {
								  invocation.parametersByName.put(parameterName, s);
							  }
						  }
					  });
				  }
				  break;
			  case 6:
				  final String javaclass2 = pAttrs.getValue(XMLConstants.NULL_NS_URI, "javaclass");
				  if (invocation.inArray  &&  isElement("record", pUri, pLocalName)
						  	              &&  "com.wm.util.Values".equals(javaclass2)) {
					  invocation.inRecord2 = true;
				  }
				  break;
			  case 7:
				  final String name2 = pAttrs.getValue(XMLConstants.NULL_NS_URI, "name");
				  if (invocation.inRecord2  &&  isElement("value", pUri, pLocalName)
					         &&  "field_name".equals(name2)) {
					  startCollecting(currentLevel-1, (s) ->
				      {
						  final String parameterName = s;
						  invocation.parametersByName.put(parameterName, null);
				      });
				  }
				  break;
			}
		}
	}

	@Override
	public void endElement(@Nonnull String pUri, @Nonnull String pLocalName, @Nonnull String pQName) throws SAXException {
		super.endElement(pUri, pLocalName, pQName);
		if (getCurrentInvocation() != null) {
			if (getLevel() == getCurrentInvocation().level-1) {
				final Invocation invocation = getCurrentInvocation();
				if (context.isEmpty()) {
					setCurrentInvocation(null);
				} else {
					setCurrentInvocation(context.remove(context.size()-1));
				}
				note(invocation);
			}
		}
	}

	protected void note(@Nonnull Invocation pInvocation) {
		// Does nothing. To overwrite by subclasses.
	}

	private Invocation getCurrentInvocation() {
		return currentInvocation;
	}

	private void setCurrentInvocation(Invocation currentInvocation) {
		this.currentInvocation = currentInvocation;
	}
}
