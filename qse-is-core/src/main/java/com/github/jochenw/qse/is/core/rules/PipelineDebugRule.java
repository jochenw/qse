package com.github.jochenw.qse.is.core.rules;

import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.function.Consumer;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import com.github.jochenw.qse.is.core.Scanner;
import com.github.jochenw.qse.is.core.api.ErrorCodes;
import com.github.jochenw.qse.is.core.api.NodeConsumer;
import com.github.jochenw.qse.is.core.rules.RulesParser.Rule;
import com.github.jochenw.qse.is.core.sax.AbstractContentHandler;


public class PipelineDebugRule extends AbstractRule {
	public static class PipelineOptionParser extends AbstractContentHandler {
		private final Consumer<String> pipelineOptionConsumer;

		public PipelineOptionParser(Consumer<String> pPipelineOptionConsumer) {
			pipelineOptionConsumer = pPipelineOptionConsumer;
		}

		@Override
		public void startElement(String pUri, String pLocalName, String pQName, Attributes pAttrs) throws SAXException {
			super.startElement(pUri, pLocalName, pQName, pAttrs);
			switch (getLevel()) {
			  case 1:
				  assertElement("Values", pUri, pLocalName);
				  break;
			  case 2:
				  if (isElement("value", pUri, pLocalName)) {
					  final String name = pAttrs.getValue("name");
					  if ("pipeline_option".equals(name)) {
						  startCollecting(1, (s) -> { pipelineOptionConsumer.accept(s); terminate(); });
					  }
				  }
			}
		}
		
	}

	private Set<String> permittedValues = new HashSet<String>();

	@Override
	public void init(Rule pParserRule) {
		super.init(pParserRule);
		String permittedValuesStr = "0,1";
		final String permittedValuesPropertyValue = (String) getProperty("permittedValues");
		if (permittedValuesPropertyValue != null) {
			permittedValuesStr = permittedValuesPropertyValue;
		}
		for (StringTokenizer st = new StringTokenizer(permittedValuesStr, ",");  st.hasMoreTokens();  ) {
			final String s = st.nextToken().trim();
			permittedValues.add(s);
		}
	}

	@Override
	public void accept(Scanner pScanner) {
		super.accept(pScanner);
		pScanner.getPluginRegistry().addPlugin(NodeConsumer.class, new NodeConsumer() {
			@Override
			public ContentHandler getContentHandler(NodeConsumer.Context pNodeInfo) {
				return new PipelineOptionParser((s) -> pipeLineOption(pNodeInfo,s));
			}
			
			@Override
			public void accept(NodeConsumer.Context pNodeInfo) {
				// Do nothing
			}
		});
	}

	private void pipeLineOption(NodeConsumer.Context pNodeInfo, String pValue) {
		if (!permittedValues.contains(pValue)) {
			issue(pNodeInfo.getPackage(), pNodeInfo.getNode().getName().getQName(), ErrorCodes.PIPELINE_DEBUG_USE,
				  "A flow service must have Pipeline debug=None");
		}
	}
}
