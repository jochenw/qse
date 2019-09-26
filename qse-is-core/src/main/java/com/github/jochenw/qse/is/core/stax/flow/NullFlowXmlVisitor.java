
package com.github.jochenw.qse.is.core.stax.flow;

import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.MapActionListener;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.StepInfo;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.VisitorException;

public class NullFlowXmlVisitor implements FlowXmlVisitor {
	@Override
	public MapActionListener startInvoke(StepInfo pStepInfo, String pServiceName) {
		// Does nothing.
		return null;
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
	public MapActionListener startMap(StepInfo pStepInfo) {
		// Does nothing.
		return null;
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
	public void startFlow(StepInfo pStepInfo, String pVersion, boolean pCleanup) {
		// Does nothing.
	}

	@Override
	public void endFlow() {
		// Does nothing.
	}

	@Override
	public void startExit(StepInfo pStepInfo, String pFrom, String pSignal, String pFailureMessage) {
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

	@Override
	public void endInvoke() throws VisitorException {
		// Does nothing.
	}

	@Override
	public void endExit() {
		// Does nothing.
	}
}
