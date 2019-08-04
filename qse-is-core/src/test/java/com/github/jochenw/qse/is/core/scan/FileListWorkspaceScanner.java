package com.github.jochenw.qse.is.core.scan;

import java.io.File;
import java.util.List;

import com.github.jochenw.afw.core.plugins.IPluginRegistry;
import com.github.jochenw.qse.is.core.model.IsWorkspace;

public class FileListWorkspaceScanner extends DefaultWorkspaceScanner {
	private final List<File> files;

	public FileListWorkspaceScanner(List<File> pFiles) {
		files = pFiles;
	}
}
