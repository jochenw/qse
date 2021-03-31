package com.github.jochenw.qse.is.core.api;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.github.jochenw.afw.core.util.LocalizableDocument;
import com.github.jochenw.afw.core.util.MutableBoolean;


public class IssueWriterTest {
	@Test
	public void testEmptyIssueListCloseable() throws Exception {
		final MutableBoolean closed = new MutableBoolean();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				super.close();
				closed.set();
			}
		};
		// Make the issue writer create an empty issue list.
		try (final IssueWriter iw = new IssueWriter(baos, true, false)) {
			assertNotNull(iw);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		assertNotEquals(0, baos.size());
		assertTrue(closed.isSet());
		baos.writeTo(System.out);
		System.out.println();
		// Verify the empty issue list by parsing it.
		LocalizableDocument.parse(new ByteArrayInputStream(baos.toByteArray()));
	}

	@Test
	public void testEmptyIssueListNonCloseable() throws Exception {
		final MutableBoolean closed = new MutableBoolean();
		final ByteArrayOutputStream baos = new ByteArrayOutputStream() {
			@Override
			public void close() throws IOException {
				super.close();
				closed.set();
			}
		};
		// Make the issue writer create an empty issue list.
		try (final IssueWriter iw = new IssueWriter(baos, false, false)) {
			assertNotNull(iw);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		assertNotEquals(0, baos.size());
		assertFalse(closed.isSet());
		baos.writeTo(System.out);
		System.out.println();
		// Verify the empty issue list by parsing it.
		final LocalizableDocument lDoc = LocalizableDocument.parse(new ByteArrayInputStream(baos.toByteArray()));
		validateEmptyIssueList(lDoc);
	}

	protected void validateEmptyIssueList(final LocalizableDocument lDoc) {
		final Document doc = lDoc.getDocument();
		final Element issuesElement = doc.getDocumentElement();
		assertEquals("issues", issuesElement.getTagName());
		final Element countElement = (Element) issuesElement.getFirstChild();
		assertEquals("count", countElement.getTagName());
		assertEquals("0", countElement.getAttribute("errors"));
		assertEquals("0", countElement.getAttribute("warnings"));
		assertEquals("0", countElement.getAttribute("otherIssues"));
	}
}
