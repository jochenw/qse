package com.github.jochenw.qse.is.core.stax.flow;

import java.util.function.Consumer;

public class LoggingFlowXmlVisitor implements FlowXmlVisitor {
	private final Consumer<String> logLineConsumer;

	public LoggingFlowXmlVisitor(Consumer<String> logLineConsumer) {
		super();
		this.logLineConsumer = logLineConsumer;
	}

	protected void logLine(String pMsg) {
		logLineConsumer.accept(pMsg);
	}

	protected void logLine(String pPrefix, StepInfo pStepInfo, String pMsg) {
		final StringBuilder sb = new StringBuilder();
		sb.append(pPrefix);
		sb.append("label=");
		sb.append(pStepInfo.getLabel());
		sb.append(", comment=");
		sb.append(pStepInfo.getComment());
		sb.append(", enabled=");
		sb.append(pStepInfo.isEnabled());
		if (pMsg != null  &&  pMsg.length() > 0) {
			sb.append(", ");
			sb.append(pMsg);
		}
		logLine(sb.toString());
	}

	@Override
	public void startExit(StepInfo pStepInfo, String pFrom, String pSignal, String pFailureMessage) throws VisitorException {
		logLine("exit: ", pStepInfo, "from=" + pFrom + ", signal=" + pSignal + ", failureMessage=" + pFailureMessage);
	}

	@Override
	public MapActionListener startInvoke(StepInfo pStepInfo, String pServiceName) throws VisitorException {
		logLine("invoke: -> ", pStepInfo, "service=" + pServiceName);
		return null;
	}

	@Override
	public void startBranch(StepInfo pStepInfo, String pSwitch, boolean pEvaluateLabels) throws VisitorException {
		logLine("branch: -> ", pStepInfo, "switch=" + pSwitch + ", evaluateLabels=" + pEvaluateLabels);
	}

	@Override
	public void endBranch() throws VisitorException {
		logLine("branch: <-");
	}

	@Override
	public MapActionListener startMap(StepInfo pStepInfo) throws VisitorException {
		logLine("map: -> ", pStepInfo, null);
		return null;
	}

	@Override
	public void endMap() throws VisitorException {
		logLine("map: <-");
	}

	@Override
	public void startSequence(StepInfo pStepInfo) throws VisitorException {
		logLine("sequence: -> ", pStepInfo, null);
	}

	@Override
	public void endSequence() throws VisitorException {
		logLine("sequence: <-");
	}

	@Override
	public void startFlow(StepInfo pStepInfo, String pVersion, boolean pCleanup) throws VisitorException {
		logLine("flow: -> ", pStepInfo, "version=" + pVersion + ", cleanUp=" + pCleanup);
	}

	@Override
	public void endFlow() throws VisitorException {
		logLine("flow: <-");
	}

	@Override
	public void startRepeat(StepInfo pStepInfo, String pCount, String pRetryInterval, String pLoopOn)
			throws VisitorException {
		logLine("repeat: -> ", pStepInfo, "count=" + pCount + ", retryInterval=" + pRetryInterval + ", loopOn=" + pLoopOn);
	}

	@Override
	public void endRepeat() throws VisitorException {
		logLine("repeat: <-");
	}

	@Override
	public void endInvoke() throws VisitorException {
		logLine("invoke: <-");
	}

	@Override
	public void endExit() {
		logLine("exit: <-");
	}
}
