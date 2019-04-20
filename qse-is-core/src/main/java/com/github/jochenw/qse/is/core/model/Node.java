package com.github.jochenw.qse.is.core.model;

import javax.annotation.Nonnull;

public class Node {
	private final String packageName;
	private final NSName name;
	private final String type, serviceType, subType, comment;

	public Node(@Nonnull String pPackageName, @Nonnull NSName pNodeName, @Nonnull String pType,
			    String pServiceType, String pSubType, String pComment) {
		packageName = pPackageName;
		name = pNodeName;
		type = pType;
		serviceType = pServiceType;
		subType = pSubType;
		comment = pComment;
	}

	public String getPackageName() {
		return packageName;
	}

	public NSName getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public String getServiceType() {
		return serviceType;
	}

	public String getSubType() {
		return subType;
	}

	public String getComment() {
		return comment;
	}
}
