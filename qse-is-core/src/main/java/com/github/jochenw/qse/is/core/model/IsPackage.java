package com.github.jochenw.qse.is.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class IsPackage {
	private final String name, uri;
	private String version;
	private MessageCatalog messageCatalog;
	private List<NSName> startupServices, shutdownServices;
	private Map<String,String> dependencies;

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

	public void setVersion(String pVersion) {
		version = pVersion;
	}

	public String getVersion() {
		return version;
	}

	public void addStartupService(NSName pName) {
		if (startupServices == null) {
			startupServices = new ArrayList<NSName>();
		}
		startupServices.add(pName);
	}

	public void addShutdownService(NSName pName) {
		if (shutdownServices == null) {
			shutdownServices = new ArrayList<NSName>();
		}
		shutdownServices.add(pName);
	}

	public void addDependency(String pPackageName, String pVersion) {
		if (dependencies == null) {
			dependencies = new HashMap<>();
		}
		dependencies.put(pPackageName, pVersion);
	}

	public Iterable<NSName> getStartupServices() {
		if (startupServices == null) {
			return Collections.emptySet();
		} else {
			return startupServices;
		}
	}

	public Iterable<NSName> getShutdownServices() {
		if (shutdownServices == null) {
			return Collections.emptySet();
		} else {
			return shutdownServices;
		}
	}

	public boolean hasDependency(String pPackageName) {
		return dependencies != null  &&  dependencies.containsKey(pPackageName);
	}
}
