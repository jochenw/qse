package com.github.jochenw.qse.is.core.sax;


public interface FlowXmlVisitor {
	public interface StepInfo {
		boolean isEnabled();
		String getLabel();
		String getComment();
	}
	void exit(StepInfo pStepInfo, String pFrom, String pSignal, String pFailureMessage);
	void invocation(FlowXmlVisitor.StepInfo pStepInfo, String pServiceName);
	void startBranch(FlowXmlVisitor.StepInfo pStepInfo, String pSwitch, boolean pEvaluateLabels);
	void endBranch();
	void startMap(FlowXmlVisitor.StepInfo pStepInfo);
	void endMap();
	void startSequence(FlowXmlVisitor.StepInfo pStepInfo);
	void endSequence();
	void startFlow(String pVersion, boolean pCleanup);
	void endFlow();
	void startRepeat(FlowXmlVisitor.StepInfo pStepInfo, String pCount, String pRetryInterval, String pLoopOn);
	void endRepeat();
}