package com.github.jochenw.qse.is.core.stax.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class TreeBuildingFlowVisitor implements FlowXmlVisitor {
	public abstract static class Step {
		private final String label;
		private final String comment;
		private final boolean enabled;
		public Step(String pLabel, String pComment, boolean pEnabled) {
			label = pLabel;
			comment = pComment;
			enabled = pEnabled;
		}
		public Step(StepInfo pStepInfo) {
			Objects.requireNonNull(pStepInfo);
			label = pStepInfo.getLabel();
			comment = pStepInfo.getComment();
			enabled = pStepInfo.isEnabled();
		}

		public String getLabel() { return label; }
		public String getComment() { return comment; }
		public boolean isEnabled() { return enabled; }
	}
	public abstract static class StepList extends Step {
		private final List<Step> list = new ArrayList<>();
		protected StepList(String pLabel, String pComment, boolean pEnabled) {
			super(pLabel, pComment, pEnabled);
		}
		public StepList(StepInfo pStepInfo) {
			super(pStepInfo);
		}
		public List<Step> getSteps() {
			return list;
		}
		public void add(Step pStep) {
			list.add(pStep);
		}
	}
	public static class Flow extends StepList {
		public Flow() {
			super(null, null, true);
		}
	}
	public static class Sequence extends StepList {
		public Sequence(StepInfo pStepInfo) {
			super(pStepInfo);
		}
	}
	public static class Repeat extends StepList {
		private final String count;
		private final String retryInterval;
		private final String loopOn;
		public Repeat(StepInfo pStepInfo, String pCount, String pRetryInterval, String pLoopOn) {
			super(pStepInfo);
			count = pCount;
			retryInterval = pRetryInterval;
			loopOn = pLoopOn;
		}
		public String getCount() {
			return count;
		}
		public int getCountAsInt() {
			return Integer.parseInt(count);
		}
		public int getCountAsInt(int pDefault) {
			try {
				return Integer.parseInt(count);
			} catch (RuntimeException e) {
				return pDefault;
			}
		}
		public String getRetryInterval() {
			return retryInterval;
		}
		public int getRetryIntervalAsInt() {
			return Integer.parseInt(retryInterval);
		}
		public int getRetryIntervalAsInt(int pDefault) {
			try {
				return Integer.parseInt(retryInterval);
			} catch (RuntimeException e) {
				return pDefault;
			}
		}
		public String getLoopOn() {
			return loopOn;
		}
	}
	public static class Branch extends StepList {
		private final String switchStr;
		private final boolean evaluateLabels;
		public Branch(StepInfo pStepInfo, String pSwitch, boolean pEvaluateLabels) {
			super(pStepInfo);
			switchStr = pSwitch;
			evaluateLabels = pEvaluateLabels;
		}
		public String getSwitchStr() {
			return switchStr;
		}
		public boolean isEvaluatingLabels() {
			return evaluateLabels;
		}
	}
	public static class Exit extends Step {
		private final String from, signal, failureMessage;
		public Exit(StepInfo pStepInfo, String pFrom,
				    String pSignal, String pFailureMessage) {
			super(pStepInfo);
			from = pFrom;
			signal = pSignal;
			failureMessage = pFailureMessage;
		}
		public String getFrom() {
			return from;
		}
		public String getSignal() {
			return signal;
		}
		public String getFailureMessage() {
			return failureMessage;
		}
	}
	public static class Invoke extends Step {
		private final String serviceName;
		private final MapActionList actions = new MapActionList();
		public Invoke(StepInfo pStepInfo, String pServiceName) {
			super(pStepInfo);
			serviceName = pServiceName;
		}
		public String getServiceName() {
			return serviceName;
		}
		public MapActionList getActions() {
			return actions;
		}
	}
	public static class MapAction {
		private final MapMode mapMode;
		private final MapLocation mapLocation;
		public MapAction(MapMode pMode, MapLocation pLocation) {
			mapMode = pMode;
			mapLocation = pLocation;
		}
		public MapMode getMapMode() {
			return mapMode;
		}
		public MapLocation getMapLocation() {
			return mapLocation;
		}
	}
	public static class MapCopy extends MapAction {
		private final String from, to;
		public MapCopy(MapMode pMode, MapLocation pLocation, String pFrom, String pTo) {
			super(pMode, pLocation);
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
	public static class MapDrop extends MapAction {
		private final String field;
		public MapDrop(MapMode pMode, MapLocation pLocation, String pField) {
			super(pMode, pLocation);
			field = pField;
		}
		public String getField() {
			return field;
		}
	}
	public static class MapSetValue extends MapAction {
		private final String field, value;
		public MapSetValue(MapMode pMode, MapLocation pLocation, String pField, String pValue) {
			super(pMode, pLocation);
			field = pField;
			value = pValue;
		}
		public String getField() {
			return field;
		}
		public String getValue() {
			return value;
		}
	}
	public static class MapInvoke extends MapAction {
		private final String service;
		private final MapActionList actionList = new MapActionList();
		public MapInvoke(MapMode pMode, MapLocation pLocation, String pService) {
			super(pMode, pLocation);
			service = pService;
		}
		public String getService() {
			return service;
		}
	}
	public static class MapActionList implements MapActionListener {
		private final List<MapAction> actions = new ArrayList<>();

		@Override
		public void copy(MapMode pMode, MapLocation pLocation, String pFrom, String pTo) {
			actions.add(new MapCopy(pMode, pLocation, pFrom, pTo));
		}

		@Override
		public void setValue(MapMode pMode, MapLocation pLocation, String pField, String pValue) {
			actions.add(new MapSetValue(pMode, pLocation, pField, pValue));
		}

		@Override
		public void drop(MapMode pMode, MapLocation pLocation, String pField) {
			actions.add(new MapDrop(pMode, pLocation, pField));
		}

		@Override
		public MapActionListener invoke(MapMode pMode, MapLocation pLocation, String pService) {
			final MapInvoke transformer = new MapInvoke(pMode, pLocation, pService);
			actions.add(transformer);
			return transformer.actionList;
		}

		public List<MapAction> getMapActions() {
			return actions;
		}
	}
	public static class Map extends Step {
		private final MapActionList actions = new MapActionList();
		public Map(StepInfo pStepInfo) {
			super(pStepInfo);
		}
		public MapActionList getActions() {
			return actions;
		}
	}

	private List<StepList> stepListStack = new ArrayList<>();
	private StepList currentStepList;
	private Flow flow;
	
	@Override
	public void startExit(StepInfo pStepInfo, String pFrom, String pSignal, String pFailureMessage) {
		final Exit exit = new Exit(pStepInfo,
				                   pFrom, pSignal, pFailureMessage);
		add(exit);
	}

	@Override
	public MapActionListener startInvoke(StepInfo pStepInfo, String pServiceName) {
		final Invoke invoke = new Invoke(pStepInfo, pServiceName);
		add(invoke);
		return invoke.getActions();
	}

	@Override
	public void endInvoke() throws VisitorException {
		
	}
	@Override
	public void startBranch(StepInfo pStepInfo, String pSwitch, boolean pEvaluateLabels) {
		final Branch branch = new Branch(pStepInfo, pSwitch, pEvaluateLabels);
		add(branch);
	}

	@Override
	public void endBranch() {
		endStepList();
	}

	@Override
	public MapActionListener startMap(StepInfo pStepInfo) {
		final Map map = new Map(pStepInfo);
		add(map);
		return map.getActions();
	}

	@Override
	public void endMap() {
	}

	@Override
	public void startSequence(StepInfo pStepInfo) {
	}

	@Override
	public void endSequence() {
		endStepList();
	}

	@Override
	public void startFlow(StepInfo pStepInfo, String pVersion, boolean pCleanup) {
		if (currentStepList != null) {
			throw error("Expected currentStepList == null");
		}
		if (!stepListStack.isEmpty()) {
			throw error("Expected stack to be empty.");
		}
		final Flow flow = new Flow();
		currentStepList = flow;
	}

	@Override
	public void endFlow() {
		flow = (Flow) currentStepList;
		endStepList();
	}

	protected void add(Step pStep) {
		if (currentStepList == null) {
			throw error("Expected currentStepList != null");
		}
		currentStepList.add(pStep);
		if (pStep instanceof StepList) {
			final StepList stepList = (StepList) pStep;
			stepListStack.add(currentStepList);
			currentStepList = stepList;
		}
	}
	protected void endStepList() {
		if (currentStepList == null) {
			throw error("Expected currentStepList != null");
		}
		if (stepListStack.isEmpty()) {
			currentStepList = null;
		} else {
			currentStepList = stepListStack.remove(stepListStack.size()-1);
		}
	}
	
	@Override
	public void startRepeat(StepInfo pStepInfo, String pCount, String pRetryInterval, String pLoopOn) {
		final Repeat repeat = new Repeat(pStepInfo, pCount, pRetryInterval, pLoopOn);
		add(repeat);
	}

	@Override
	public void endRepeat() {
		endStepList();
	}

	public Flow getFlow() {
		if (flow == null) {
			throw new IllegalStateException("Flow not yet available.");
		}
		return flow;
	}

	@Override
	public void endExit() {
	}
}