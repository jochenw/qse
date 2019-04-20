package com.github.jochenw.qse.is.core.scan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.github.jochenw.qse.is.core.api.FlowConsumer;
import com.github.jochenw.qse.is.core.api.NodeConsumer;
import com.github.jochenw.qse.is.core.model.IsPackage;
import com.github.jochenw.qse.is.core.model.Node;
import com.github.jochenw.qse.is.core.scan.PackageFileConsumer.Editor;
import com.github.jochenw.qse.is.core.scan.PackageFileConsumer.Editor.EditorContext;


public class ContextImpl implements PackageFileConsumer.Context, NodeConsumer.Context, FlowConsumer.Context {
	public static interface EditRequest {
		public String getPath();
		public String getDescription();
	}
	private static class EditorAndContext {
		private final EditorContext context;
		private final String localPath;
		private final Editor editor;
		EditorAndContext(Editor pEditor, String pLocalPath, EditorContext pContext) {
			context = pContext;
			editor = pEditor;
			localPath = pLocalPath;
		}
	}
	private IsPackage pkg;
	private String localPath, localFlowPath;
	private Path file, flowFile;
	private Node node;
	private List<EditorAndContext> editors = new ArrayList<>();

	
	@Override
	public void register(Editor pEditor) {
		final EditorContext context = new EditorContext() {
			@Override
			public OutputStream openForWrite() throws IOException {
				return Files.newOutputStream(file);
			}

			@Override
			public InputStream openForRead() throws IOException {
				return Files.newInputStream(file);
			}
		};
		editors.add(new EditorAndContext(pEditor, localPath, context));
	}

	public List<EditRequest> getEditRequests() {
		final List<EditRequest> requests = new ArrayList<>(editors.size());
		for (EditorAndContext eac : editors) {
			requests.add(new EditRequest() {
				@Override
				public String getPath() {
					return eac.localPath;
				}

				@Override
				public String getDescription() {
					return eac.editor.getDescription();
				}
			});
		}
		return requests;
	}
	public void runEditors() {
		editors.forEach((eac) -> eac.editor.run(eac.context));
	}
	
	@Override
	public IsPackage getPackage() {
		return pkg;
	}

	public void setPackage(IsPackage pPkg) {
		pkg = pPkg;
	}

	@Override
	public String getLocalPath() {
		return localPath;
	}

	public void setLocalPath(String pLocalPath) {
		localPath = pLocalPath;
	}

	@Override
	public String getFlowLocalPath() {
		return localFlowPath;
	}

	public void setFlowLocalPath(String pLocalPath) {
		localFlowPath = pLocalPath;
	}

	public void setFile(Path pFile) {
		file = pFile;
	}

	@Override
	public InputStream open() throws IOException {
		return Files.newInputStream(file);
	}

	@Override
	public InputStream openFlow() throws IOException {
		return Files.newInputStream(flowFile);
	}

	@Override
	public Node getNode() {
		return node;
	}

	public void setNode(Node pNode) {
		node = pNode;
	}

	public Path getFile() {
		return file;
	}

	public Path getFlowFile() {
		return flowFile;
	}

	public void setFlowFile(Path pFile) {
		flowFile = pFile;
	}
}
