package com.github.jochenw.qse.is.core.model;

import javax.annotation.Nonnull;

public class IsPackage {
	private final String name, uri;
	private MessageCatalog messageCatalog;

	public IsPackage(@Nonnull String pName, @Nonnull String pUri) {
		name = pName;
		uri = pUri;
	}

	public String getName() {
		return name;
	}

	public String getUri() {
		return uri;
	}

	public MessageCatalog getMessageCatalog() {
		return messageCatalog;
	}

	public void setMessageCatalog(@Nonnull MessageCatalog messageCatalog) {
		this.messageCatalog = messageCatalog;
	}
}
