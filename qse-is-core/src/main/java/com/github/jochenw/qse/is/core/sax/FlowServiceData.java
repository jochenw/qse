package com.github.jochenw.qse.is.core.sax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class FlowServiceData {
	public static class Step {
		private String comment;
		private String label;

		public String getComment() {
			return comment;
		}

		public void setComment(String pComment) {
			comment = pComment;
		}

		public String getLabel() {
			return label;
		}

		public void setLabel(String pLabel) {
			label = pLabel;
		}
	}
	public static class SequenceStep extends Step {
		private final List<Step> steps = new ArrayList<>();

		public List<Step> getSteps() {
			return Collections.unmodifiableList(steps);
		}
		public void addStep(Step pStep) {
			Objects.requireNonNull(pStep, "Step");
			steps.add(pStep);
		}
	}
	public static class BranchStep extends Step {
		private String switchExpr;
		private final List<Step> steps = new ArrayList<Step>();
		private boolean evaluateLabels;

		public void setSwitch(String pSwitch) {
			switchExpr = pSwitch;
		}
		public String getSwitch() {
			return switchExpr;
		}
		public void addStep(Step pStep) {
			final String label = pStep.getLabel();
			if (label == null  ||  label.isEmpty()) {
				throw new IllegalStateException("A branch steps label must not be null, or empty.");
			}
			steps.add(pStep);
		}
		public List<Step> getSteps() {
			return steps;
		}
		public void setEvaluatingLabels(boolean pEvaluateLabels) {
			evaluateLabels = pEvaluateLabels;
		}
		public boolean isEvaluatingLabels() {
			return evaluateLabels;
		}
	}
	public static class FieldInfo {
		private final String name, type, refType;
		private final boolean nullable;
		private FieldInfo(String pName, String pType, String pRefType, boolean pNullable) {
			name = pName;
			type = pType;
			refType = pRefType;
			nullable = pNullable;
		}
		
		public String getName() { return name; }
		public String getType() { return type; }
		public String getRefType() { return refType; }
		public boolean isNullable() { return nullable; }

		public static FieldInfo stringField(String pName, boolean pNullable) {
			return new FieldInfo(pName, "string", null, pNullable);
		}
		public static FieldInfo recordField(String pName, boolean pNullable) {
			return new FieldInfo(pName, "record", null, pNullable);
		}
		public static FieldInfo objectField(String pName, boolean pNullable) {
			return new FieldInfo(pName, "object", null, pNullable);
		}
		public static FieldInfo recRefField(String pName, String pRefType, boolean pNullable) {
			return new FieldInfo(pName, "recRef", pRefType, pNullable);
		}
	}
	public static class MapAction {
	}
	public static class MapInvoke extends MapAction {
		private String service;
		private MapData inputMap, outputMap;
		public String getService() {
			return service;
		}
		public void setService(String service) {
			this.service = service;
		}
		public MapData getInputMap() {
			return inputMap;
		}
		public void setInputMap(MapData inputMap) {
			this.inputMap = inputMap;
		}
		public MapData getOutputMap() {
			return outputMap;
		}
		public void setOutputMap(MapData outputMap) {
			this.outputMap = outputMap;
		}
	}
	public static class MapSet extends MapAction {
		private final String field;
		private String fieldType, fieldName, value;

		public MapSet(String pField) {
			field = pField;
		}
		public String getFieldType() {
			return fieldType;
		}
		public void setFieldType(String pFieldType) {
			fieldType = pFieldType;
		}
		public String getValue() {
			return value;
		}
		public void setValue(String pValue) {
			value = pValue;
		}
		public String getFieldName() {
			return fieldName;
		}
		public void setFieldName(String pFieldName) {
			fieldName = pFieldName;
		}
		public String getField() {
			return field;
		}
		
	}
	public static class MapDelete extends MapAction {
		private final String field;
		public MapDelete(String pField) {
			field = pField;
		}
		public String getField() {
			return field;
		}
	}
	public static class MapCopy extends MapAction {
		private final String from, to;
		public MapCopy(String pFrom, String pTo) {
			from = pFrom;
			to = pTo;
		}

		public String getFrom() {
			return from;
		}
		public String getTo() {
			return to;
		}
	}
	public static class MapData {
		private FieldInfo[] mapTarget;
		private FieldInfo[] mapSource;
		private final List<MapAction> actions = new ArrayList<MapAction>();

		public FieldInfo[] getMapTarget() { return mapTarget; }
		public FieldInfo[] getMapSource() { return mapSource; }
		public List<MapCopy> getMapCopies() { return actions.stream().map(a -> (a instanceof MapCopy) ? (MapCopy) a : null).filter(a -> a != null).collect(Collectors.toList()); }
		public List<MapAction> getMapActions() { return actions; }
		public void setMapSource(FieldInfo[] pFieldInfo) {
			mapSource = pFieldInfo;
		}
		public void setMapTarget(FieldInfo[] pFieldInfo) {
			mapTarget = pFieldInfo;
		}

		public void addAction(MapAction pAction) {
			actions.add(pAction);
		}
	}
	public static class MapStep extends Step {
		private final MapData mapData = new MapData();

		public FieldInfo[] getMapTarget() { return mapData.getMapTarget(); }
		public FieldInfo[] getMapSource() { return mapData.getMapSource(); }
		public List<MapCopy> getMapCopies() { return mapData.getMapCopies(); }

		public MapData getMapData() {
			return mapData;
		}
		public MapAction getMapAction(int pIndex) {
			return mapData.getMapActions().get(pIndex);
		}
		public List<MapAction> getMapActions() {
			return mapData.getMapActions();
		}
	}
	public static class InvokeStep extends Step {
		private String service;
		private MapData inputMap, outputMap;
		public String getService() {
			return service;
		}
		public void setService(String service) {
			this.service = service;
		}
		public MapData getInputMap() {
			return inputMap;
		}
		public void setInputMap(MapData inputMap) {
			this.inputMap = inputMap;
		}
		public MapData getOutputMap() {
			return outputMap;
		}
		public void setOutputMap(MapData outputMap) {
			this.outputMap = outputMap;
		}
	}
	private String version;
	private boolean cleanup;
	private final List<Step> steps = new ArrayList<Step>();
	private FieldInfo[] inputFields, outputFields;

	
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public boolean isCleanup() {
		return cleanup;
	}
	public void setCleanup(boolean cleanup) {
		this.cleanup = cleanup;
	}

	public void addStep(Step pStep) {
		Objects.requireNonNull(pStep, "Step");
		steps.add(pStep);
	}

	public List<Step> getSteps() {
		return Collections.unmodifiableList(steps);
	}

	public FieldInfo[] getInputFields() {
		return inputFields;
	}

	public void setInputFields(FieldInfo[] pInputFields) {
		inputFields = pInputFields;
	}

	public FieldInfo[] getOutputFields() {
		return outputFields;
	}

	public void setOutputFields(FieldInfo[] pOutputFields) {
		outputFields = pOutputFields;
	}

	public void visit(Consumer<Step> pStepConsumer, Consumer<MapAction> pActionConsumer) {
		for (Step step : getSteps()) {
			visit(step, pStepConsumer, pActionConsumer);
		}
	}

	protected void visit(Step pStep, Consumer<Step> pStepConsumer, Consumer<MapAction> pActionConsumer) {
		if (pStepConsumer != null) {
			pStepConsumer.accept(pStep);
		}
		if (pStep instanceof SequenceStep) {
			final SequenceStep step = (SequenceStep) pStep;
			for (Step s : step.getSteps()) {
				visit(s, pStepConsumer, pActionConsumer);
			}
		} else if (pStep instanceof BranchStep) {
			final BranchStep step = (BranchStep) pStep;
			for (Step s : step.getSteps()) {
				visit(s, pStepConsumer, pActionConsumer);
			}
		} else if (pStep instanceof MapStep) {
			final MapStep step = (MapStep) pStep;
			for (MapAction a : step.getMapActions()) {
				if (pActionConsumer != null) {
					pActionConsumer.accept(a);
				}
			}
		}
	}
}
