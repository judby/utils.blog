package com.udby.blog.utils;

import static org.junit.Assert.*;

import java.util.Collections;

import org.junit.Test;

public class UtilsTest {
	final A a1 = new A(1);
	final A a1alt = new A(1);
	final A a2 = new A(2);
	final B b = new B();
	
	@Test
	public void testEquals() {
		assertFalse(Utils.equals(null, a1));
		assertFalse(Utils.equals(b, null));
		assertFalse(Utils.equals(a1, a2));
		assertFalse(Utils.equals(b, a2));
		
		assertTrue(Utils.equals(null, null));
		assertTrue(Utils.equals(a1, a1));
		assertTrue(Utils.equals(b, b));
		assertTrue(Utils.equals(a1, a1alt));
		// below is questionable - a and b are different types
		assertTrue(Utils.equals(a1, b));
	}

	@Test
	public void testNvl() {
		assertNull(Utils.nvl());
		assertNull(Utils.nvl((Object)null));
		assertNull(Utils.nvl((Object[])null));
		assertNull(Utils.nvl(null, null));
		assertEquals(a1, Utils.nvl(a1));
		assertEquals(a1, Utils.nvl(null, a1, null));
		assertEquals(a1, Utils.nvl(null, a1, a2));
	}

	@Test
	public void testNullOrEmpty() {
		assertTrue(Utils.nullOrEmpty(null));
		assertTrue(Utils.nullOrEmpty(""));
		assertTrue(Utils.nullOrEmpty(Collections.emptyList()));
		assertTrue(Utils.nullOrEmpty(Collections.emptySet()));
		assertTrue(Utils.nullOrEmpty(Collections.emptyMap()));
		
		assertFalse(Utils.nullOrEmpty(1));
		assertFalse(Utils.nullOrEmpty(" "));
		assertFalse(Utils.nullOrEmpty(Collections.singletonList(a1)));
		assertFalse(Utils.nullOrEmpty(Collections.singleton(a1)));
		assertFalse(Utils.nullOrEmpty(Collections.singletonMap(b, a1)));

		assertTrue(Utils.nullOrEmpty(new Object[0]));
		assertFalse(Utils.nullOrEmpty(new Object[1]));
	}

	@Test
	public void testCompare() {
		assertEquals(0, Utils.compare(null, null));
		assertEquals(0, Utils.compare(a1, a1));
		assertEquals(0, Utils.compare(a1, a1alt));
		assertEquals(-1, Utils.compare(a1, a2));
		assertEquals(-1, Utils.compare(a1, null));
		assertEquals(1, Utils.compare(a2, a1));
		assertEquals(1, Utils.compare(null, a1));
		// questionable...
		assertEquals(0, Utils.compare((A)b, b));
	}

	@Test
	public void testIn() {
		assertFalse(Utils.in(null));
		assertFalse(Utils.in(1));
		assertFalse(Utils.in(null, 1));
		assertFalse(Utils.in(1, (Object[])null));
		assertFalse(Utils.in(1, (Object)null));
		assertFalse(Utils.in(1, 2));
		assertFalse(Utils.in(1, null, 2));
		assertFalse(Utils.in(1, null, 2, null));

		assertTrue(Utils.in(1, null, 1, null));
		assertTrue(Utils.in(1, 1, 2, 3));
		assertTrue(Utils.in(1, 3, 2, 1));

		assertFalse(Utils.in(null, (Object[])null));
		assertFalse(Utils.in(null, (Object)null));
	}
}
