package com.github.jochenw.qse.is.core.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import javax.annotation.Nonnull;

import com.github.jochenw.afw.core.io.IReadable;

public class Resources {
	/** Creates a new {@link IReadable}, which reads the URI, that is given by the string
	 * {@code pUri}. More precisely:
	 * <ol>
	 *   <li>If the {@code pUri} has the format "resource:suburi", then the Uri is resolved
	 *     by invoking {@link ClassLoader#getResource(String)} on the String "suburi".</li>
	 *   <li>If the {@code pUri} has the format "default:suburi", then the Uri is resolved
	 *     by invoking {@link ClassLoader#getResource(String)} on the current threads
	 *     context class loader, passing the string {@code pDefaultUri} + "/suburi". If
	 *     the parameter {@code pDefaultUri} is null, throws an exception.</li>
	 *   <li>If the Uri con be converted into an URL, then the URL is being resolved.</li>
	 *   <li>Otherwise, the Uri is interpreted as the path of a file, that must be read.</li>
	 * </ol>
	 * @param pUri The Uri, which supplies the data stream, and the name.
	 * @param pDefaultUri The default Uri, for resolving "default:" URI's.
	 * @return A new instance of {@link IReadable}, with the given file' name,
	 *   and the file's contents as a data stream.
	 */
	public static @Nonnull IReadable of(@Nonnull String pUri, @Nonnull String pDefaultUri) {
		final String uri = Objects.requireNonNull(pUri, "Uri");
		final String defaultUri = Objects.requireNonNull(pDefaultUri, "DefaultUri");
		if (uri.startsWith("resource:")) {
			final String subUri = uri.substring("resource:".length());
			final URL url = Thread.currentThread().getContextClassLoader().getResource(subUri);
			if (url == null) {
				throw new IllegalStateException("Unable to resolve resource URI: " + subUri);
			}
			return IReadable.of(url);
		} else if (uri.startsWith("default:")) {
			final String subUri = uri.substring("default:".length());
			final String u = defaultUri + "/" + subUri;
			final URL url = Thread.currentThread().getContextClassLoader().getResource(u);
			if (url == null) {
				throw new IllegalStateException("Unable to resolve default URI: " + subUri);
			}
			return IReadable.of(url);
		} else {
			final URL url;
			try {
				url = new URL(uri);
			} catch (MalformedURLException e) {
				final Path p = Paths.get(uri);
				if (Files.isRegularFile(p)) {
					return IReadable.of(p);
				} else {
					throw new IllegalArgumentException("Unable to resolve URI, which is"
							+ " neither a valid URL, nor the path to an existing file: " + uri);
				}
			}
			return IReadable.of(url);
		}
	}
}
