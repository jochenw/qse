package com.github.jochenw.qse.is.core.stax.flow;

public interface FlowXmlVisitor {
	public static class VisitorException extends RuntimeException {
		private static final long serialVersionUID = 2782484822428027620L;

		public VisitorException(String pMessage) {
			super(pMessage);
		}
	}

	public static enum MapMode {
		INPUT, OUTPUT, STANDALONE;
	}
	public static enum MapLocation  {
		SOURCE, TARGET;
	}
	public interface MapActionListener {
		void copy(MapMode pMode, MapLocation pLocation, String pFrom, String pTo);
		void drop(MapMode pMode, MapLocation pLocation, String pField);
		MapActionListener invoke(MapMode pMode, MapLocation pLocation, String pService);
		void setValue(MapMode pMode, MapLocation pLocation, String field, String value);
	}
	public interface StepInfo {
		boolean isEnabled();
		String getComment();
		String getLabel();
	}
	void startExit(StepInfo pStepInfo, String pFrom, String pSignal, String pFailureMessage) throws VisitorException;
	void endExit();
	/** Called for an INVOKE, or MAPINVOKE step. If a listener is being returned, it will be
	 * called with pMapTime=INVOCATION_INPUT|INVOCATION_OUTPUT, and pMapLocation=SOURCE|TARGET.
	 * @param pStepInfo The current step info.
	 * @param pServiceName Name of the service, that is being invoked.
	 * @return A listener, that is being notified about the invocations mapping details. Null, if the
	 *   details are not required.
	 */
	MapActionListener startInvoke(StepInfo pStepInfo, String pServiceName) throws VisitorException;
	void startBranch(StepInfo pStepInfo, String pSwitch, boolean pEvaluateLabels) throws VisitorException;
	void endBranch() throws VisitorException;
	/** Called for a MAP step. If a listener is being returned, it will be called with pMapTime=MAPSTEP, and pMapLocation=SOURCE|TARGET.
	 * @param pStepInfo The current step info.
	 * @return A listener, that is being notified about the mapping details. Null, if the
	 *   details are not required.
	 */
	MapActionListener startMap(StepInfo pStepInfo) throws VisitorException;
	void endMap() throws VisitorException;
	void startSequence(StepInfo pStepInfo) throws VisitorException;
	void endSequence() throws VisitorException;
	void startFlow(StepInfo pStepInfo, String pVersion, boolean pCleanup) throws VisitorException;
	void endFlow() throws VisitorException;
	void startRepeat(StepInfo pStepInfo, String pCount, String pRetryInterval, String pLoopOn) throws VisitorException;
	void endRepeat() throws VisitorException;
	public default VisitorException error(String pMessage) {
		throw new VisitorException(pMessage);
	}
	void endInvoke() throws VisitorException;
}