package com.github.jochenw.qse.is.core.stax.flow;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.jochenw.qse.is.core.stax.flow.FlowXmlPullParser;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor;
import com.github.jochenw.qse.is.core.stax.flow.LoggingFlowXmlVisitor;
import com.github.jochenw.qse.is.core.stax.flow.NullFlowXmlVisitor;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Branch;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Flow;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Invoke;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Repeat;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Step;

public class FlowXmlPullParserTest {
	@Test
	public void testNullVisitor() throws Exception {
		final FlowXmlVisitor visitor = new NullFlowXmlVisitor();
		parseService1(visitor);
	}

	@Test
	public void testLoggingVisitor() throws Exception {
		final List<String> lines = new ArrayList<>();
		final LoggingFlowXmlVisitor visitor = new LoggingFlowXmlVisitor((s) -> { System.out.println(s); lines.add(s); });
		parseService1(visitor);

		final URL url = getClass().getResource("Service1Logging.txt");
		assertNotNull(url);
		try (InputStream in = url.openStream();
			 InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
			 BufferedReader br = new BufferedReader(isr)) {
			for (int i = 0;  i < lines.size();  i++) {
				final String expect = lines.get(i);
				final String got = br.readLine();
				assertNotNull("Line " + i, got);
				assertEquals("Line " + i, expect, got);
			}
		}
	}

	@Test
	public void testTreeBuildingVisitor() throws Exception {
		final TreeBuildingFlowVisitor visitor = new TreeBuildingFlowVisitor();
		parseService1(visitor);
		final Flow flow = visitor.getFlow();
		assertNotNull(flow);
		final List<Step> flowSteps = flow.getSteps();
		assertNotNull(flowSteps);
		assertEquals(3, flowSteps.size());
		final Invoke invoke = (Invoke) flowSteps.get(0);
		assertNotNull(invoke);
		assertNull(invoke.getLabel());
		assertEquals("", invoke.getComment());
		assertTrue(invoke.isEnabled());
		assertEquals("pub.math:addInts", invoke.getServiceName());
		final Branch branch = (Branch) flowSteps.get(1);
		assertNotNull(branch);
		assertNull(branch.getLabel());
		assertEquals("", branch.getComment());
		assertTrue(branch.isEnabled());
		assertNull(branch.getSwitchStr());
		assertTrue(branch.isEvaluatingLabels());
		final Repeat repeat = (Repeat) flowSteps.get(2);
		assertNotNull(repeat);
		assertNull(repeat.getLabel());
		assertEquals("", repeat.getComment());
		assertTrue(repeat.isEnabled());
		assertEquals("whatever", repeat.getCount());
		assertEquals(-1, repeat.getCountAsInt(-1));
		try {
			repeat.getCountAsInt();
			fail("Expected exception");
		} catch (IllegalArgumentException iae) {
			// Okay
		}
	}

	private void parseService1(final FlowXmlVisitor visitor) throws IOException {
		final String uri = "packages/JwiScratch/ns/jwi/scratch/flowParserExample/service1/flow.xml";
		final URL url = Thread.currentThread().getContextClassLoader().getResource(uri);
		assertNotNull(url);
		try (InputStream in = url.openStream()) {
			new FlowXmlPullParser(visitor).parse(in, url.toExternalForm());
		}
	}
}
