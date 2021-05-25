package com.github.jochenw.qse.is.core.rules;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.FlowConsumer.Context;
import com.github.jochenw.qse.is.core.rules.PackageScannerRule.FlowXmlVisitorSupplier;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor;

public class DisabledStepsRule extends AbstractRule {
	@Override
	protected void accept(IPluginRegistry pRegistry) {
		super.accept(pRegistry);
		
		pRegistry.addPlugin(FlowXmlVisitorSupplier.class, new FlowXmlVisitorSupplier() {
			@Override
			public FlowXmlVisitor apply(Scanner pScanner, Context pCtx) {
				return DisabledStepsRule.this.newVisitor(pScanner, pCtx);
			}
		});
	}

	protected FlowXmlVisitor newVisitor(Scanner pScanner, Context pCtx) {
		return new FlowXmlVisitor() {
			private int numberOfDisabledSteps;
			
			@Override
			public void startSequence(StepInfo pStepInfo) throws VisitorException {
				checkDisabled(pStepInfo);
			}
			
			@Override
			public void startRepeat(StepInfo pStepInfo, String pCount, String pRetryInterval, String pLoopOn)
					throws VisitorException {
				checkDisabled(pStepInfo);
			}
			
			@Override
			public MapActionListener startMap(StepInfo pStepInfo) throws VisitorException {
				checkDisabled(pStepInfo);
				return null;
			}
			
			@Override
			public MapActionListener startInvoke(StepInfo pStepInfo, String pServiceName) throws VisitorException {
				checkDisabled(pStepInfo);
				return null;
			}
			
			@Override
			public void startFlow(StepInfo pStepInfo, String pVersion, boolean pCleanup) throws VisitorException {
			}
			
			@Override
			public void startExit(StepInfo pStepInfo, String pFrom, String pSignal, String pFailureMessage)
					throws VisitorException {
				checkDisabled(pStepInfo);
			}
			
			@Override
			public void startBranch(StepInfo pStepInfo, String pSwitch, boolean pEvaluateLabels) throws VisitorException {
				checkDisabled(pStepInfo);
			}
			
			@Override
			public void endSequence() throws VisitorException {
			}
			
			@Override
			public void endRepeat() throws VisitorException {
			}
			
			@Override
			public void endMap() throws VisitorException {
			}
			
			@Override
			public void endInvoke() throws VisitorException {
			}
			
			@Override
			public void endFlow() throws VisitorException {
				if (numberOfDisabledSteps > 0) {
					issue(pCtx.getPackage(), pCtx.getFlowLocalPath(), ErrorCodes.DISABLED_STEP,
						  "The flow service " + pCtx.getNode().getName().getQName()
						  + " contains " + numberOfDisabledSteps + " step(s).");
				}
			}
			
			@Override
			public void endExit() {
			}
			
			@Override
			public void endBranch() throws VisitorException {
			}

			protected void checkDisabled(StepInfo pStepInfo) {
				if (!pStepInfo.isEnabled()) {
					++numberOfDisabledSteps;
				}
			}
		};
	}
}
