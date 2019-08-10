package com.github.jochenw.qse.is.core.scan;

import java.util.List;

import com.github.jochenw.qse.is.core.Scanner;

public interface IWorkspaceScanner {
	public void scan(Scanner pScanner, List<PackageFileConsumer> pListeners);
}
