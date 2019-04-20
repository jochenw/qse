package com.github.jochenw.qse.is.core.rules;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

import com.github.jochenw.qse.is.core.api.IssueConsumer.Severity;
import com.github.jochenw.qse.is.core.model.MessageCatalog;
import com.github.jochenw.qse.is.core.model.MessageCatalog.Component;
import com.github.jochenw.qse.is.core.model.MessageCatalog.Facility;
import com.github.jochenw.qse.is.core.model.MessageCatalog.Message;
import com.github.jochenw.qse.is.core.sax.Sax;


public class LogMessageCatalogParserTest {
	@Test
	public void testParseLogMessagesExample() {
		final URL url = getClass().getResource("log-messages.xml");
		assertNotNull(url);
		final LogMessageCatalogParser lmcp = new LogMessageCatalogParser();
		Sax.parse(url, lmcp);
		final MessageCatalog mc = lmcp.getMessageCatalog();
		assertNotNull(mc);
		assertEquals(Locale.ENGLISH, mc.getDefaultLocale());
		final List<Component> components = mc.getComponents();
		assertNotNull(components);
		assertEquals(1, components.size());
		final Component component = components.get(0);
		assertEquals("Cbx", component.getComponentKey());
		assertEquals("Stiftung CBX Layer", component.getName());
		final List<Facility> facilities = component.getFacilities();
		assertNotNull(facilities);
		assertEquals(4, facilities.size());
		final Facility mainFacility = facilities.get(0);
		final List<Message> mainMessages = assertFacility(mainFacility, "100", "Main");
		assertEquals(7, mainMessages.size());
		assertMessage(mainFacility, mainMessages.get(0), "0001", Severity.TRACE, "Envelope processing. Queue {0} XML: {1} ", mc);
		assertMessage(mainFacility, mainMessages.get(1), "0002", Severity.INFO, "EnvelopeDoc created. RefNo: {0}", mc);
		assertMessage(mainFacility, mainMessages.get(2), "0003", Severity.INFO, "Type processing. Type {0} Refno: {1} ", mc);
		assertMessage(mainFacility, mainMessages.get(3), "0004", Severity.ERROR, "Error in ReceiveService: Type {0} XML: {1} LastError {2}", mc);
		assertMessage(mainFacility, mainMessages.get(4), "0005", Severity.INFO, "Acknowledge created. RefNo: {0} Status: {1}", mc);
		assertMessage(mainFacility, mainMessages.get(5), "0006", Severity.INFO, "Acknowledge created. RefNo: {0} Error {1}", mc);
		assertMessage(mainFacility, mainMessages.get(6), "0007", Severity.ERROR, "Error in ReceiveService: Type {0} LastError {1}", mc);
		final Facility initFacility = facilities.get(1);
		final List<Message> initMessages = assertFacility(initFacility, "200", "Initialisation");
		assertEquals(2, initMessages.size());
		assertMessage(initFacility, initMessages.get(0), "0001", Severity.TRACE, "Started JMS-Autosetup for Package {0}.", mc);
		assertMessage(initFacility, initMessages.get(1), "0002", Severity.TRACE, "Ended JMS-Autosetup for Package {0}.", mc);
		final Facility validateFacility = facilities.get(2);
		final List<Message> validateMessages = assertFacility(validateFacility, "300", "Validate");
		assertEquals(3, validateMessages.size());
		assertMessage(validateFacility, validateMessages.get(0), "0001", Severity.ERROR, "Schema Validation Error! Schema {0} Errors {1} xmlString: {2}", mc);
		assertMessage(validateFacility, validateMessages.get(1), "0002", Severity.INFO, "... {0} subscribed.", mc);
		assertMessage(validateFacility, validateMessages.get(2), "0003", Severity.INFO, "Message not sent, because of validation errors! Type: {0} RefNo: {1}.", mc);

		final Facility publishFacility = facilities.get(3);
		final List<Message> publishMessages = assertFacility(publishFacility, "400", "Publish");
		assertEquals(5, publishMessages.size());
		assertMessage(publishFacility, publishMessages.get(0), "0001", Severity.INFO, "Message Type: {0} Status: {1} Refno: {3} published to: {2}.", mc);
		assertMessage(publishFacility, publishMessages.get(1), "0002", Severity.INFO, "Message Type {0} Refno {2} published to {1}.", mc);
		assertMessage(publishFacility, publishMessages.get(2), "0003", Severity.INFO, "Message {0} not published, no send criteria", mc);
		assertMessage(publishFacility, publishMessages.get(3), "0004", Severity.INFO, "Message Type {0} with uuid  {1} was send to Dispatcher.", mc);
		assertMessage(publishFacility, publishMessages.get(4), "0005", Severity.INFO, "Message Type {0} with uuid  {1} was not send to Dispatcher caused of wrong country and countryBlock.", mc);

		assertFalse(mc.hasMessage("FOO", "100", "0001"));
	}

	private void assertMessage(Facility pFacility, Message pMessage, String pKey, Severity pLevel, String pText, MessageCatalog pMc) {
		assertNotNull(pFacility);
		assertNotNull(pMessage);
		assertEquals(pFacility.getComponentKey(), pMessage.getComponentKey());
		assertEquals(pFacility.getFacilityKey(), pMessage.getFacilityKey());
		assertEquals(pKey, pMessage.getMessageKey());
		assertSame(pLevel, pMessage.getLevel());
		assertEquals(pText, pMessage.getText());
		assertTrue(pMc.hasMessage(pMessage.getComponentKey(), pMessage.getFacilityKey(), pMessage.getMessageKey()));
	}

	private final List<Message> assertFacility(Facility pFacility, String pKey, String pName) {
		assertNotNull(pFacility);
		assertEquals(pKey, pFacility.getFacilityKey());
		assertEquals(pName, pFacility.getName());
		assertNotNull(pFacility.getMessages());
		assertFalse(pFacility.getMessages().isEmpty());
		return pFacility.getMessages();
	}
}
