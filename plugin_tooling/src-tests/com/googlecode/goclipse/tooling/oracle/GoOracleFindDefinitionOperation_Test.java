/*******************************************************************************
 * Copyright (c) 2014, 2014 Bruno Medeiros and other Contributors.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Bruno Medeiros - initial API and implementation
 *******************************************************************************/
package com.googlecode.goclipse.tooling.oracle;

import static melnorme.utilbox.core.Assert.AssertNamespace.assertFail;
import static melnorme.utilbox.core.Assert.AssertNamespace.assertTrue;

import org.junit.Test;

import com.googlecode.goclipse.tooling.CommonGoToolingTest;
import com.googlecode.goclipse.tooling.StatusException;
import com.googlecode.goclipse.tooling.oracle.GoOracleFindDefinitionOperation.GoOracleFindDefinitionResult;

public class GoOracleFindDefinitionOperation_Test extends CommonGoToolingTest {
	
	@Test
	public void testCreateProcessBuilder() throws Exception { testCreateProcessBuilder$(); }
	public void testCreateProcessBuilder$() throws Exception {
		GoOracleFindDefinitionOperation op = new GoOracleFindDefinitionOperation("gopath");
		
		ProcessBuilder pb;
		pb = op.createProcessBuilder(SAMPLE_GOEnv_1, SAMPLE_GO_PATH.resolve("src/foobar/file.go"), 0);
		assertEquals(pb.command().get(4), "foobar");
		
		try {
			op.createProcessBuilder(SAMPLE_GOEnv_1, SAMPLE_GO_PATH.resolve("not_on_src/foobar/file.go"), 0);
			assertFail();
		} catch (StatusException se) {
			assertTrue(se.getMessage().contains("file not in the Go environment"));
		}
	}
	
	@Test
	public void test() throws Exception { test$(); }
	public void test$() throws Exception {
		
		GoOracleFindDefinitionOperation op = new GoOracleFindDefinitionOperation("gopath");
		
		GoOracleFindDefinitionResult result = op.parseJsonResult(
			getClassResourceAsString(GoOracleFindDefinitionOperation_Test.class, "result1.json"));
		
		assertEquals(result.path, path("D:\\devel\\tools.Go\\go-workspace\\src\\github.com\\user\\newmath\\sqrt.go"));
		assertEquals(result.line, 5);
		assertEquals(result.column, 6);
	}
	
}