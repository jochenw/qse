package com.github.jochenw.qse.is.core.sax;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TreeBuildingVisitor implements FlowXmlVisitor {
	public abstract static class Step {
		private final boolean enabled;
		private final String label, comment;
		protected Step(StepInfo pStepInfo) {
			if (pStepInfo == null) {
				enabled = true;
				label = null;
				comment = null;
			} else {
				enabled = pStepInfo.isEnabled();
				label = pStepInfo.getLabel();
				comment = pStepInfo.getComment();
			}
		}
		public boolean isEnabled() {
			return enabled;
		}
		public String getLabel() {
			return label;
		}
		public String getComment() {
			return comment;
		}
	}
	public static class Invocation extends Step {
		private final String serviceName;
		public Invocation(StepInfo pStepInfo, String pServiceName) {
			super(pStepInfo);
			serviceName = pServiceName;
		}
		public String getServiceName() {
			return serviceName;
		}
	}
	public static class Map extends Step {
		public Map(StepInfo pStepInfo) {
			super(pStepInfo);
		}
	}
	public static class Exit extends Step {
		private final String from;
		private final String signal;
		private final String failureMessage;
		public Exit(StepInfo pStepInfo, String pFrom, String pSignal, String pFailureMessage) {
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
	public abstract static class StepList extends Step {
		private final List<Step> steps = new ArrayList<>();
		public StepList(StepInfo pStepInfo) {
			super(pStepInfo);
		}
		public void add(Step pStep) {
			steps.add(pStep);
		}
		public List<Step> getSteps() {
			return steps;
		}
	}
	public static class Branch extends StepList {
		private final String switchStr;
		private final boolean evaluateLabels;

		public Branch(StepInfo pStepInfo, String pSwitchStr, boolean pEvaluateLabels) {
			super(pStepInfo);
			switchStr = pSwitchStr;
			evaluateLabels = pEvaluateLabels;
		}
		public String getSwitchStr() {
			return switchStr;
		}
		public boolean isEvaluateLabels() {
			return evaluateLabels;
		}
	}
	public static class Sequence extends StepList {
		public Sequence(StepInfo pStepInfo) {
			super(pStepInfo);
		}
	}
	public static class Flow extends Sequence {
		private final String version;
		private final boolean cleanup;
		public Flow(String pVersion, boolean pCleanup) {
			super(null);
			version = pVersion;
			cleanup = pCleanup;
		}
		public String getVersion() {
			return version;
		}
		public boolean isCleanup() {
			return cleanup;
		}
	}
	public static class Repeat extends StepList {
		final String count;
		final String retryInterval;
		final String loopOn;
		public Repeat(StepInfo pStepInfo, String pCount, String pRetryInterval, String pLoopOn) {
			super(pStepInfo);
			count = pCount;
			retryInterval = pRetryInterval;
			loopOn = pLoopOn;
		}
		public String getCount() {
			return count;
		}
		public String getRetryInterval() {
			return retryInterval;
		}
		public String getLoopOn() {
			return loopOn;
		}
	}

	private final List<Step> stepStack = new ArrayList<>();
	private Step currentStep = null;
	private Flow flow = null;

	protected void pushStep(Step pStep) {
		Objects.requireNonNull(pStep);
		if (currentStep != null) {
			if (currentStep instanceof StepList) {
				final StepList stepList = (StepList) currentStep;
				stepList.add(pStep);
			}
			stepStack.add(currentStep);
		}
		currentStep = pStep;
	}

	protected <S extends Step> S pullStep(Class<S> pType) {
		final Step step = currentStep;
		if (stepStack.isEmpty()) {
			currentStep = null;
		} else {
			currentStep = stepStack.remove(stepStack.size()-1);
		}
		Objects.requireNonNull(step);
		if (!pType.isAssignableFrom(step.getClass())) {
			throw new IllegalStateException("Expected " + pType.getSimpleName() + ", got " + step.getClass().getSimpleName());
		}
		@SuppressWarnings("unchecked")
		final S s = (S) step;
		return s;
	}
	
	@Override
	public void invocation(StepInfo pStepInfo, String pServiceName) {
		pushStep(new Invocation(pStepInfo, pServiceName));
		pullStep(Invocation.class);
	}

	@Override
	public void startBranch(StepInfo pStepInfo, String pSwitch, boolean pEvaluateLabels) {
		pushStep(new Branch(pStepInfo, pSwitch, pEvaluateLabels));
	}

	@Override
	public void endBranch() {
		pullStep(Branch.class);
	}

	@Override
	public void startMap(StepInfo pStepInfo) {
		pushStep(new Map(pStepInfo));
	}

	@Override
	public void endMap() {
		pullStep(Map.class);
	}

	@Override
	public void startSequence(StepInfo pStepInfo) {
		pushStep(new Sequence(pStepInfo));
	}

	@Override
	public void endSequence() {
		pullStep(Sequence.class);
	}

	@Override
	public void startFlow(String pVersion, boolean pCleanup) {
		stepStack.clear();
		currentStep = null;
		flow = null;
		pushStep(new Flow(pVersion, pCleanup));
	}

	@Override
	public void endFlow() {
		flow = Objects.requireNonNull(pullStep(Flow.class));
		if (currentStep != null  ||  !stepStack.isEmpty()) {
			throw new IllegalStateException("Expected no more remaining steps");
		}
	}

	public Flow getFlow() {
		if (flow == null) {
			throw new IllegalStateException("Flow not yet available.");
		}
		return flow;
	}

	@Override
	public void exit(StepInfo pStepInfo, String pFrom, String pSignal, String pFailureMessage) {
		pushStep(new Exit(pStepInfo, pFrom, pSignal, pFailureMessage));
		pullStep(Exit.class);
	}

	@Override
	public void startRepeat(StepInfo pStepInfo, String pCount, String pRetryInterval, String pLoopOn) {
		pushStep(new Repeat(pStepInfo, pCount, pRetryInterval, pLoopOn));
	}

	@Override
	public void endRepeat() {
		pullStep(Repeat.class);
	}
}