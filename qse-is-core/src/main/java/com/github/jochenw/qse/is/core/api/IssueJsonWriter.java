package com.github.jochenw.qse.is.core.api;

import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Supplier;

import javax.json.spi.JsonProvider;
import javax.json.stream.JsonGenerator;

public class IssueJsonWriter implements IssueConsumer, AutoCloseable {
	private final Supplier<OutputStream> outputStreamSupplier;
	private JsonGenerator jGen;

	public IssueJsonWriter(Supplier<OutputStream> pSupplier) {
		outputStreamSupplier = Objects.requireNonNull(pSupplier, "Supplier");
	}

	protected void open() {
		if (jGen == null) {
			jGen = JsonProvider.provider().createGenerator(outputStreamSupplier.get());
			jGen.writeStartArray();
		}
	}
	@Override
	public void close() throws Exception {
		open();
		jGen.writeEnd();
		jGen.close();
	}

	@Override
	public void accept(Issue pIssue) {
		open();
		jGen.writeStartArray();
		jGen.write(pIssue.getPackage());
		jGen.write(pIssue.getErrorCode());
		jGen.write(pIssue.getRule());
		jGen.write(pIssue.getSeverity().toString());
		jGen.write(pIssue.getUri());
		jGen.write(pIssue.getMessage());
		jGen.writeEnd();
	}
}
