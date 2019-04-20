package com.github.jochenw.qse.is.core.api;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Nonnull;

import org.xml.sax.ContentHandler;

import com.github.jochenw.qse.is.core.model.Node;


public interface FlowConsumer {
	public interface Context extends NodeConsumer.Context {
		public String getFlowLocalPath();
		public Node getNode();
		public InputStream openFlow() throws IOException;
	}

	public default ContentHandler getContentHandler(@Nonnull Context pContext) { return null; }
	public void accept(@Nonnull Context pContext);
}
