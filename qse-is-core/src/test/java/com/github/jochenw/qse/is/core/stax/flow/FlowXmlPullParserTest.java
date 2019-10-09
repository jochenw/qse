package com.github.jochenw.qse.is.core.stax.flow;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.xml.sax.InputSource;

import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor;
import com.github.jochenw.qse.is.core.stax.flow.LoggingFlowXmlVisitor;
import com.github.jochenw.qse.is.core.stax.flow.NullFlowXmlVisitor;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.MapLocation;
import com.github.jochenw.qse.is.core.stax.flow.FlowXmlVisitor.MapMode;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Branch;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Exit;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Flow;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Invoke;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Map;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.MapAction;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.MapActionList;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.MapCopy;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.MapSetValue;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Repeat;
import com.github.jochenw.qse.is.core.stax.flow.TreeBuildingFlowVisitor.Step;

public class FlowXmlPullParserTest {
	@Test
	public void testNullVisitor() throws Exception {
		parseService1(null);
	}

	@Test
	public void testNullFlowXmlVisitor() throws Exception {
		final FlowXmlVisitor visitor = new NullFlowXmlVisitor();
		parseService1(visitor);
	}

	@Test
	public void testLoggingVisitor() throws Exception {
		final List<String> lines = new ArrayList<>();
		final LoggingFlowXmlVisitor visitor = new LoggingFlowXmlVisitor((s) -> { lines.add(s); });
		parseService1(visitor);

		final URL url = getClass().getResource("Service1Logging.txt");
		assertNotNull(url);
		try (InputStream in = url.openStream();
			 InputStreamReader isr = new InputStreamReader(in, StandardCharsets.UTF_8);
			 BufferedReader br = new BufferedReader(isr)) {
			for (int i = 0;  i < lines.size();  i++) {
				final String got = lines.get(i);
				final String expect = readLine(br);
				assertNotNull("Line " + i, got);
				assertEquals("Line " + i, expect, got);
			}
		}
	}

	protected String readLine(BufferedReader pReader) {
		final StringWriter sw = new StringWriter();
		for (;;) {
			int c;
			try {
				c = pReader.read();
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
			if (c == -1) {
				break;
			} else if (c == 13) {
				continue;
			} else if (c == 10) {
				break;
			} else {
				sw.write(c);
			}
		}
		return sw.toString();
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
		final MapActionList invokeActions = invoke.getActions();
		final List<MapAction> invokeActionList = invokeActions.getMapActions();
		assertEquals(2, invokeActionList.size());
		final MapSetValue msv = (MapSetValue) invokeActionList.get(0);
		assertNotNull(msv);
		assertEquals("/num1;1;0", msv.getField());
		assertEquals("5", msv.getValue());
		final MapCopy mc1 = (MapCopy) invokeActionList.get(1);
		assertSame(MapMode.INPUT, mc1.getMapMode());
		assertNull(mc1.getMapLocation());
		assertEquals("/num2;1;0", mc1.getFrom());
		assertEquals("/num2;1;0", mc1.getTo());
		final Branch branch = (Branch) flowSteps.get(1);
		assertNotNull(branch);
		assertNull(branch.getLabel());
		assertEquals("", branch.getComment());
		assertTrue(branch.isEnabled());
		assertNull(branch.getSwitchStr());
		assertTrue(branch.isEvaluatingLabels());
		final List<Step> branchSteps = branch.getSteps();
		assertEquals(4, branchSteps.size());
		final Map mapStep0 = (Map) branchSteps.get(0);
		assertEquals("%value% < 5", mapStep0.getLabel());
		assertEquals("Lower than five:", mapStep0.getComment());
		assertTrue(mapStep0.isEnabled());
		final Map mapStep1 = (Map) branchSteps.get(1);
		assertEquals("%value% > 5", mapStep1.getLabel());
		assertEquals("Greater than five", mapStep1.getComment());
		assertTrue(mapStep1.isEnabled());
		final Map mapStep2 = (Map) branchSteps.get(2);
		assertEquals("%value% == 5", mapStep2.getLabel());
		assertEquals("Equal to five", mapStep2.getComment());
		assertTrue(mapStep2.isEnabled());
		final Exit exitStep = (Exit) branchSteps.get(3);
		assertEquals("$flow", exitStep.getFrom());
		assertEquals("FAILURE", exitStep.getSignal());
		assertEquals("Impossible value: %value%", exitStep.getFailureMessage());
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
		final List<Step> repeatSteps = repeat.getSteps();
		assertEquals(2, repeatSteps.size());
		final Invoke disabledInvoke = (Invoke) repeatSteps.get(0);
		assertNull(disabledInvoke.getLabel());
		assertEquals("", disabledInvoke.getComment());
		assertFalse(disabledInvoke.isEnabled());
		final Map transformingMapStep = (Map) repeatSteps.get(1);
		assertNull(transformingMapStep.getLabel());
		assertEquals("", transformingMapStep.getComment());
		assertTrue(transformingMapStep.isEnabled());
	}

	private void parseService1(final FlowXmlVisitor visitor) throws IOException {
		final String uri = "packages/JwiScratch/ns/jwi/scratch/flowParserExample/service1/flow.xml";
		final URL url = Thread.currentThread().getContextClassLoader().getResource(uri);
		assertNotNull(url);
		try (InputStream in = url.openStream()) {
			final InputSource isource = new InputSource(in);
			isource.setSystemId(url.toExternalForm());
			FlowXmlParser.parse(isource, visitor);
		}
	}
}
