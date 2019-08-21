package com.github.jochenw.qse.is.core.sax;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.net.URL;
import java.util.List;

import org.junit.Test;

import com.github.jochenw.qse.is.core.sax.TreeBuildingVisitor.Branch;
import com.github.jochenw.qse.is.core.sax.TreeBuildingVisitor.Exit;
import com.github.jochenw.qse.is.core.sax.TreeBuildingVisitor.Flow;
import com.github.jochenw.qse.is.core.sax.TreeBuildingVisitor.Invocation;
import com.github.jochenw.qse.is.core.sax.TreeBuildingVisitor.Map;
import com.github.jochenw.qse.is.core.sax.TreeBuildingVisitor.Repeat;
import com.github.jochenw.qse.is.core.sax.TreeBuildingVisitor.Step;

public class FlowServiceParserTest {
	@Test
	public void testParseService1WithNullVisitor() throws Exception {
		final String uri = "packages/JwiScratch/ns/jwi/scratch/flowParserExample/service1/flow.xml";
		final URL url = Thread.currentThread().getContextClassLoader().getResource(uri);
		assertNotNull(url);
		try (InputStream in = url.openStream()) {
			final FlowXmlVisitor visitor = new NullFlowXmlVisitor();
			Sax.parse(in, url.toExternalForm(), new FlowServiceParser(visitor));
		} 
	}

	@Test
	public void testParseService1WithTreeBuilder() throws Exception {
		final String uri = "packages/JwiScratch/ns/jwi/scratch/flowParserExample/service1/flow.xml";
		final URL url = Thread.currentThread().getContextClassLoader().getResource(uri);
		assertNotNull(url);
		try (InputStream in = url.openStream()) {
			final TreeBuildingVisitor tbv = new TreeBuildingVisitor();
			Sax.parse(in, url.toExternalForm(), new FlowServiceParser(tbv));
			final Flow flow = tbv.getFlow();
			validateService1(flow);
		} 
	}

	protected void validateService1(Flow pFlow) {
		assertNotNull(pFlow);
		final List<Step> flowSteps = pFlow.getSteps();
		assertNotNull(flowSteps);
		assertEquals(3, flowSteps.size());
		final Invocation invocation = (Invocation) flowSteps.get(0);
		assertNotNull(invocation);
		assertTrue(invocation.isEnabled());
		assertNull(invocation.getLabel());
		assertNull(invocation.getComment());
		assertEquals("pub.math:addInts", invocation.getServiceName());
		final Branch branch = (Branch) flowSteps.get(1);
		assertNotNull(branch);
		assertNull(branch.getLabel());
		assertNull(branch.getComment());
		final List<Step> branchSteps = branch.getSteps();
		assertNotNull(branchSteps);
		assertEquals(4, branchSteps.size());
		final Map mapStep0 = (Map) branchSteps.get(0);
		assertNotNull(mapStep0);
		assertEquals("%value% < 5", mapStep0.getLabel());
		assertNull(mapStep0.getComment());
		final Map mapStep1 = (Map) branchSteps.get(1);
		assertNotNull(mapStep1);
		assertEquals("%value% > 5", mapStep1.getLabel());
		assertNull(mapStep1.getComment());
		final Map mapStep2 = (Map) branchSteps.get(2);
		assertNotNull(mapStep2);
		assertEquals("%value% == 5", mapStep2.getLabel());
		assertNull(mapStep2.getComment());
		final Exit exit = (Exit) branchSteps.get(3);
		assertNotNull(exit);
		assertEquals("$default", exit.getLabel());
		assertNull(exit.getComment());
		assertEquals("FAILURE", exit.getSignal());
		assertEquals("Impossible value: %value%", exit.getFailureMessage());
		final Repeat repeat = (Repeat) flowSteps.get(2);
		assertNotNull(repeat);
		assertNull(repeat.getLabel());
		assertNull(repeat.getComment());
		assertTrue(repeat.getSteps().isEmpty());
	}
}
