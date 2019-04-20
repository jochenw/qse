package com.github.jochenw.qse.is.core.scan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.github.jochenw.qse.is.core.model.IsPackage;

public interface PackageFileConsumer {
	public interface Editor extends Runnable {
		public interface EditorContext {
			public InputStream openForRead() throws IOException;
			public OutputStream openForWrite() throws IOException;
		}
		public String getDescription();
		public void run(EditorContext pContext);
	}
	public interface Context {
		public IsPackage getPackage();
		public String getLocalPath();
		public InputStream open() throws IOException;
		public void register(Editor pEditor);
	}

	public void accept(Context pContext);
}
