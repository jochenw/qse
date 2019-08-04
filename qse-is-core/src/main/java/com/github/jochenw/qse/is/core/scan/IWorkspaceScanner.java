package com.github.jochenw.qse.is.core.scan;

import com.github.jochenw.qse.is.core.Scanner;

public interface IWorkspaceScanner {
	public abstract class Context {
		private boolean immutable;
		private Scanner scanner;

		public void setScanner(Scanner pScanner) {
			if (immutable) {
				throw new IllegalStateException("This object is no longer mutable.");
			}
			scanner = pScanner;
			immutable = true;
		}
		public Scanner getScanner() { return scanner; }
	}
	public void scan(Context pContext);
}
