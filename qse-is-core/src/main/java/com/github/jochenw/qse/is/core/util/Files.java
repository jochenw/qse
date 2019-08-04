package com.github.jochenw.qse.is.core.util;

import java.io.File;

public class Files {

	public static String getRelativePath(File pFile, File pDir) {
		final File dir = pFile.getParentFile();
		if (dir == null) {
			return null;
		} else {
			if (dir.equals(pDir)) {
				return pFile.getName();
			} else {
				final String path = getRelativePath(dir, pDir);
				if (path == null) {
					return null;
				} else {
					return path + '/' + pFile.getName();
				}
			}
		}
	}

	public static String getPath(File pFile) {
		return pFile.getPath().replace('\\', '/');
	}

}
