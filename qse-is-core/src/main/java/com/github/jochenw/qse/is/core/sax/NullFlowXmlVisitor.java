
package com.github.jochenw.qse.is.core.sax;

public class NullFlowXmlVisitor implements FlowXmlVisitor {
	@Override
	public void invocation(StepInfo pStepInfo, String pServiceName) {
		// Does nothing.
	}

	@Override
	public void startBranch(StepInfo pStepInfo, String pSwitch, boolean pEvaluateLabels) {
		// Does nothing.
	}

	@Override
	public void endBranch() {
		// Does nothing.
	}

	@Override
	public void startMap(StepInfo pStepInfo) {
		// Does nothing.
	}

	@Override
	public void endMap() {
		// Does nothing.
	}

	@Override
	public void startSequence(StepInfo pStepInfo) {
		// Does nothing.
	}

	@Override
	public void endSequence() {
		// Does nothing.
	}

	@Override
	public void startFlow(String pVersion, boolean pCleanup) {
		// Does nothing.
	}

	@Override
	public void endFlow() {
		// Does nothing.
	}

	@Override
	public void exit(StepInfo pStepInfo, String pFrom, String pSignal, String pFailureMessage) {
		// Does nothing.
	}

	@Override
	public void startRepeat(StepInfo pStepInfo, String pCount, String pRetryInterval, String pLoopOn) {
		// Does nothing.
	}

	@Override
	public void endRepeat() {
		// Does nothing.
	}
}
