// Copyright © 2015-2017 Andrey Mogilev. All rights reserved.

package com.gilecode.xmx.cfg.impl;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestPropertyValueImpl {
	
	@Test
	public void testInt1() {
		PropertyValueImpl pv = PropertyValueImpl.of("1");
		assertEquals(1, pv.asInt());
		assertEquals(true, pv.asBool());
		assertEquals("1", pv.asString());
		
		assertEquals(pv, PropertyValueImpl.of("1"));
	}
	
	@Test
	public void testInt0() {
		PropertyValueImpl pv = PropertyValueImpl.of("0");
		assertEquals(0, pv.asInt());
		assertEquals(false, pv.asBool());
		assertEquals("0", pv.asString());
	}
	
	@Test
	public void testBoolean() {
		PropertyValueImpl pv = PropertyValueImpl.of("true");
		assertEquals(1, pv.asInt());
		assertEquals(true, pv.asBool());
		assertEquals("true", pv.asString());
		
		pv = PropertyValueImpl.of("True");
		assertEquals(1, pv.asInt());
		assertEquals(true, pv.asBool());
		assertEquals("True", pv.asString());
		
		pv = PropertyValueImpl.of("yes");
		assertEquals(1, pv.asInt());
		assertEquals(true, pv.asBool());
		assertEquals("yes", pv.asString());
		
		pv = PropertyValueImpl.of("YES");
		assertEquals(1, pv.asInt());
		assertEquals(true, pv.asBool());
		assertEquals("YES", pv.asString());
		
		pv = PropertyValueImpl.of("false");
		assertEquals(0, pv.asInt());
		assertEquals(false, pv.asBool());
		assertEquals("false", pv.asString());
		
		pv = PropertyValueImpl.of("no");
		assertEquals(0, pv.asInt());
		assertEquals(false, pv.asBool());
		assertEquals("no", pv.asString());
	}
	

}
