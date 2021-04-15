package com.github.jochenw.qse.is.core.util;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import com.github.jochenw.afw.core.io.IReadable;
import com.github.jochenw.afw.core.io.IReadable.NoLongerReadableException;
import com.github.jochenw.afw.core.util.Holder;
import com.github.jochenw.afw.core.util.Streams;
import com.github.jochenw.afw.core.util.Functions.FailableConsumer;


/** Test for {@link Resources}.
 */
public class ResourcesTest {
	/** Test case for {@link Resources#of(String, String)}.
	 * @throws Exception The test failed.
	 */
	@Test
	public void testString() throws Exception {
		final String htmlFileUri = "com/github/jochenw/qse/is/core/result-table.html";
		final String htmlFilePath = "src/main/resources/" + htmlFileUri;
		final URL htmlFileUrl = Paths.get("target/classes/" + htmlFileUri).toUri().toURL();
		final IReadable r0 = Resources.of("pom.xml", "");
		validateNonRepeatable(r0, "pom.xml", Files.size(Paths.get("pom.xml")));
		final IReadable r1 = Resources.of("resource:com/github/jochenw/qse/is/core/result-table.html", "");
		validateNonRepeatable(r1, htmlFileUrl.toString(), Files.size(Paths.get(htmlFilePath)));
		final IReadable r2 = Resources.of("default:result-table.html", "com/github/jochenw/qse/is/core");
		validateNonRepeatable(r2, htmlFileUrl.toString(), Files.size(Paths.get(htmlFilePath)));
	}

	private void validateNonRepeatable(final IReadable pReadable, String pName, long pNumBytes) throws IOException {
		assertNotNull(pReadable);
		assertEquals(pName, pReadable.getName());
		assertTrue(pReadable.isReadable());
		assertFalse(pReadable.isRepeatable());
		final Holder<String> contentHolder = new Holder<String>();
		pReadable.read((in) -> contentHolder.set(Streams.read(in, StandardCharsets.UTF_8)));
		final String contents = contentHolder.get();
		assertEquals(pNumBytes, contents.getBytes(StandardCharsets.UTF_8).length);
		assertFalse(pReadable.isReadable());
		assertFalse(pReadable.isRepeatable());
		try {
			pReadable.read((FailableConsumer<InputStream,?>) null);
			fail("Expected Exception");
		} catch (NoLongerReadableException e) {
			assertEquals("This IReadable has already been read: " + pName, e.getMessage());
		}
	}
}
