package com.github.jochenw.qse.is.core.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;

public class MessageCatalog {
	public static class Message {
		private final String componentKey;
		private final String facilityKey;
		private final String messageKey;
		private final String text;
		private final Severity  level;

		Message(String pComponentKey, String pFacilityKey, String pMessageKey, Severity pLevel, String pText) {
			Objects.requireNonNull(pComponentKey);
			Objects.requireNonNull(pFacilityKey);
			Objects.requireNonNull(pMessageKey);
			Objects.requireNonNull(pText);
			componentKey = pComponentKey;
			facilityKey = pFacilityKey;
			messageKey = pMessageKey;
			level = pLevel;
			text = pText;
		}

		@Override
		public int hashCode() {
			return 31 * (31 * (31 * 1 + componentKey.hashCode()) + facilityKey.hashCode()) + messageKey.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Message other = (Message) obj;
			return componentKey.equals(other.componentKey)
				&& facilityKey.equals(other.facilityKey)
				&&  messageKey.equals(other.messageKey);
		}

		public String getComponentKey() { return componentKey; }
		public String getFacilityKey() { return facilityKey; }
		public String getMessageKey() { return messageKey; }
		public Severity getLevel() { return level; }
		public String getText() { return text; }
	}
	public static class Facility {
		private final String componentKey;
		private final String facilityKey;
		private String name;
		private final List<Message> messages = new ArrayList<>();

		public Facility(String pComponentKey, String pFacilityKey) {
			componentKey = pComponentKey;
			facilityKey = pFacilityKey;
		}

		public String getComponentKey() { return componentKey; }
		public String getFacilityKey() { return facilityKey; }
		public List<Message> getMessages() {
			return messages;
		}
		public void add(Message pMessage) {
			for (Message m : messages) {
				if (m.messageKey.equals(pMessage.messageKey)) {
					throw new IllegalStateException("Duplicate message key " + pMessage.messageKey
	                        + " for component=" + componentKey
	                        + ", facility=" + facilityKey);
				}
			}
			messages.add(pMessage);
		}
		public String getName() {
			return name;
		}
		public void setName(String pName) {
			name = pName;
		}
	}
	public static class Component {
		private final String componentKey;
		private String name;
		private final List<Facility> facilities = new ArrayList<>();

		public Component(String pComponentKey) {
			componentKey = pComponentKey;
		}
		public String getComponentKey() { return componentKey; }
		public List<Facility> getFacilities() {
			return facilities;
		}
		public void add(Facility pFacility) {
			for (Facility f : facilities) {
				if (f.facilityKey.equals(pFacility.facilityKey)) {
					throw new IllegalStateException("Duplicate facility key " + pFacility.facilityKey
	                        + " for component=" + componentKey);
				}
			}
			facilities.add(pFacility);
		}
		public String getName() {
			return name;
		}
		public void setName(String pName) {
			name = pName;
		}
	}
	

	private final Set<Message> messages = new HashSet<Message>();
	private final List<Component> components = new ArrayList<>();

	private Component currentComponent;
	private Facility currentFacility;
	private Locale defaultLocale;
	
	public MessageCatalog() {
	}

	public void addMessage(@Nonnull String pMessageKey, @Nonnull Severity pSeverity, @Nonnull String pText) {
		if (currentFacility == null) {
			throw new IllegalStateException("Current facility is null.");
		}
		final Message message = new Message(currentFacility.componentKey, currentFacility.facilityKey, pMessageKey, pSeverity, pText);
		currentFacility.add(message);
		messages.add(message);
	}

	public void startFacility(@Nonnull String pFacilityKey) {
		if (currentComponent == null) {
			throw new IllegalStateException("Current component is null.");
		}
		if (currentFacility != null) {
			throw new IllegalStateException("Current facility is not null.");
		}
		final Facility facility = new Facility(currentComponent.componentKey, pFacilityKey);
		currentComponent.add(facility);
		currentFacility = facility;
	}
	public void endFacility() {
		if (currentComponent == null) {
			throw new IllegalStateException("Current component is null.");
		}
		if (currentFacility == null) {
			throw new IllegalStateException("Current facility is not null.");
		}
		currentFacility = null;
	}

	public void startComponent(@Nonnull String pComponentKey) {
		if (currentComponent != null) {
			throw new IllegalStateException("Current component is not null");
		}
		currentComponent = new Component(pComponentKey);
	}
	public void endComponent() {
		if (currentComponent == null) {
			throw new IllegalStateException("Current component is null");
		} else {
			components.add(currentComponent);
		}
		currentComponent = null;
	}

	public Locale getDefaultLocale() {
		return defaultLocale;
	}

	public void setDefaultLocale(@Nonnull Locale pLocale) {
		defaultLocale = pLocale;
	}
	public Component getCurrentComponent() {
		return currentComponent;
	}
	public Facility getCurrentFacility() {
		return currentFacility;
	}
	public List<Component> getComponents() {
		return components;
	}

	public boolean hasMessage(@Nonnull String pComponentKey, @Nonnull String pFacilityKey, @Nonnull String pMessageKey) {
		final Message msg = new Message(pComponentKey, pFacilityKey, pMessageKey, Severity.TRACE, "");
		return messages.contains(msg);
	}
}
