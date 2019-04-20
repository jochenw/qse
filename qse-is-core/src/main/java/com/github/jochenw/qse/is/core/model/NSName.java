package com.github.jochenw.qse.is.core.model;

import java.util.Objects;

import javax.annotation.Nonnull;

public class NSName {
	private final String folderName, nodeName, qName;

	private NSName(@Nonnull String pFolderName, @Nonnull String pNodeName, @Nonnull String pQName) {
		Objects.requireNonNull(pFolderName, "FolderName");
		Objects.requireNonNull(pNodeName, "NodeName");
		Objects.requireNonNull(pQName, "QName");
		folderName = pFolderName;
		nodeName = pNodeName;
		qName = pQName;
	}

	public String getPackageName() {
		return folderName;
	}

	public String getNodeName() {
		return nodeName;
	}

	public String getQName() {
		return qName;
	}

	public static NSName valueOf(String pName) {
		final int offset1 = pName.lastIndexOf(':');
		if (offset1 == -1) {
			final int offset2 = pName.lastIndexOf('.');
			if (offset2 == -1) {
				return new NSName("", pName, pName);
			} else {
				final String svcName = pName.substring(offset2+1);
				final String pkgName = pName.substring(0, offset2);
				return new NSName(pkgName, svcName, pkgName + ":" + svcName);
			}
		} else {
			final String svcName = pName.substring(offset1+1);
			final String pkgName = pName.substring(0, offset1);
			return new NSName(pkgName, svcName, pkgName + ":" + svcName);
		}
	}
}
