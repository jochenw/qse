package com.github.jochenw.qse.is.core.sax;

import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;


public class FlowServiceParser extends AbstractContentHandler {
	public interface ElementTerminator {
		public void run() throws SAXException;
	}

	public static class Step implements FlowXmlVisitor.StepInfo {
		private final int level;
		private final String localName;
		private final ElementTerminator terminator;
		private final boolean enabled;
		private final String label;
		private final String comment;
		private final boolean acceptingChildren;
		public Step(int pLevel, String pLocalName, boolean pAcceptingChildren, boolean pEnabled, String pLabel, String pComment, ElementTerminator pTerminator) {
			level = pLevel;
			localName = pLocalName;
			terminator = pTerminator;
			acceptingChildren = pAcceptingChildren;
			enabled = pEnabled;
			label = pLabel;
			comment = pComment;
		}
		public int getLevel() {
			return level;
		}
		public String getLocalName() {
			return localName;
		}
		public ElementTerminator getTerminator() {
			return terminator;
		}
		@Override
		public boolean isEnabled() {
			return enabled;
		}
		@Override
		public String getLabel() {
			return label;
		}
		@Override
		public String getComment() {
			return comment;
		}
		public boolean isAcceptingChildren() {
			return acceptingChildren;
		}
	}

	private final List<Step> stepStack = new ArrayList<>();
	private Step currentStep;
	private final FlowXmlVisitor visitor;

	public FlowServiceParser(FlowXmlVisitor pVisitor) {
		visitor = pVisitor;
	}

	@Override
	public void startDocument() throws SAXException {
		super.startDocument();
		stepStack.clear();
		currentStep = null;
	}

	@Override
	public void endDocument() throws SAXException {
		super.endDocument();
		if (currentStep != null  ||  !stepStack.isEmpty()) {
			throw error("Expected empty element stack");
		}
	}

	@Override
	public void startElement(String pUri, String pLocalName, String pQName, Attributes pAttrs) throws SAXException {
		Sax.assertDefaultNamespace(pUri, pLocalName);
		super.startElement(pUri, pLocalName, pQName, pAttrs);
		switch (pLocalName) {
		case "FLOW":
			if (getLevel() != 1) {
				throw error("Expected level=1, got " + getLevel());
			}
			addStep(null, true, pLocalName, () -> { if (visitor != null) { visitor.endFlow(); }});
			if (visitor != null) {
				final String version = pAttrs.getValue("VERSION");
				final String cleanupStr = pAttrs.getValue("CLEANUP");
				visitor.startFlow(version, "true".equalsIgnoreCase(cleanupStr));
			}
			break;
		case "EXIT":
		{
			final Step step = addStep(pAttrs, false, pLocalName, () -> {});
			if (visitor != null) {
				final String from = pAttrs.getValue("FROM");
				final String signal = pAttrs.getValue("SIGNAL");
				final String failureMessage = pAttrs.getValue("FAILURE-MESSAGE");
				visitor.exit(step, from, signal, failureMessage);
			}
			break;
		}
		case "INVOKE":
		{
			final String serviceName = pAttrs.getValue("SERVICE");
			if (serviceName == null  ||  serviceName.length() == 0) {
				throw error("Expected non-empty attribute: INVOKE/@SERVICE");
			}
			final Step step = addStep(pAttrs, false, pLocalName, () -> {});
			if (visitor != null) {
				visitor.invocation(step, serviceName);
			}
			break;
		}
		case "SEQUENCE":
		{
			final Step step = addStep(pAttrs, true, pLocalName, () -> { if (visitor != null) { visitor.endSequence(); } });
			if (visitor != null) {
				visitor.startSequence(step);
			}
			break;
		}
		case "BRANCH":
		{
			final Step step = addStep(pAttrs, true, pLocalName, () -> { if (visitor != null) { visitor.endBranch(); } });
			final String switchStr = pAttrs.getValue("SWITCH");
			final String labelExpressionsStr = pAttrs.getValue("LABELEXPRESSIONS");
			if (visitor != null) {
				visitor.startBranch(step, switchStr, "true".equalsIgnoreCase(labelExpressionsStr));
			}
			break;
		}
		case "MAP":
		{
			final String mode = pAttrs.getValue("MODE");
			if ("STANDALONE".equals(mode)) {
				if (currentStep != null  &&  currentStep.isAcceptingChildren()) {
					final Step step = addStep(pAttrs, false, pLocalName, () -> { if (visitor != null) { visitor.endMap(); } });
					if (visitor != null) {
						visitor.startMap(step);
					}
				} else {
					throw error("Unexpected map step (expected parent FLOW|SEQUENCE|BRANCH|REPEAT, got "
							+ ((currentStep == null) ? "null" : currentStep.getLocalName()));
				}
			}
			break;
		}
		case "RETRY":
		{
			final Step step = addStep(pAttrs, true, pLocalName, () -> { if (visitor != null) { visitor.endRepeat(); } });
			if (visitor != null) {
				final String count = pAttrs.getValue("COUNT");
				final String retryInterval = pAttrs.getValue("BACK-OFF");
				final String loopOn = pAttrs.getValue("LOOP-ON");
				visitor.startRepeat(step, count, retryInterval, loopOn);
			}
		}
		default:
			// Do nothing
		}
	}

	protected boolean getBooleanAttribute(Attributes pAttrs, String pAttrName) {
		final String v = pAttrs.getValue(pAttrName);
		return "true".equalsIgnoreCase(v);
	}

	protected Step addStep(Attributes pAttrs, boolean pAcceptingChildren, String pLocalName, ElementTerminator pTerminator) {
		final String disabledStr = (pAttrs == null) ? null : pAttrs.getValue("DISABLED");
		final boolean disabled = "true".equalsIgnoreCase(disabledStr);
		final String label = (pAttrs == null) ? null : pAttrs.getValue("NAME");
		final String comment = (pAttrs == null) ? null : pAttrs.getValue("COMMENT");
		final Step step = new Step(getLevel()-1, pLocalName, pAcceptingChildren, !disabled, label, comment, pTerminator);
		if (currentStep != null) {
			stepStack.add(currentStep);
		}
		currentStep = step;
		return step;
	}
	
	@Override
	public void endElement(String pUri, String pLocalName, String pQName) throws SAXException {
		Sax.assertDefaultNamespace(pUri, pLocalName);
		super.endElement(pUri, pLocalName, pQName);
		final int level = getLevel();
		if (currentStep != null  &&  currentStep.level == level) {
			final ElementTerminator terminator = currentStep.getTerminator();
			if (terminator != null) {
				terminator.run();
			}
			if (stepStack.isEmpty()) {
				currentStep = null;
			} else {
				currentStep = stepStack.remove(stepStack.size()-1);
			}
		}
	}
}
