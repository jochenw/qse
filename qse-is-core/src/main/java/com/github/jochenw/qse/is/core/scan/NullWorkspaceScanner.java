package com.github.jochenw.qse.is.core.scan;

import java.util.List;

import com.github.jochenw.qse.is.core.Scanner;


/** A workspace scanner, which does nothing. This allows to create a scanner
 * for the sole purpose of obtaining the plugin list.
 */
public class NullWorkspaceScanner implements IWorkspaceScanner {
	@Override
	public void scan(Scanner pScanner, List<PackageFileConsumer> pListeners) {
	}
}
