package com.github.jochenw.qse.is.core.api;

import javax.annotation.Nonnull;

import org.xml.sax.ContentHandler;

import com.github.jochenw.qse.is.core.model.Node;
import com.github.jochenw.qse.is.core.scan.PackageFileConsumer;


public interface NodeConsumer {
	public interface Context extends PackageFileConsumer.Context {
		public Node getNode();
	}

	public default ContentHandler getContentHandler(@Nonnull Context pContext) { return null; }
	public void accept(@Nonnull Context pContext);
}
