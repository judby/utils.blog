package com.udby.blog.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.Test;

public class UtilsAltTest {
	final A a1 = new A(1);
	final A a1alt = new A(1);
	final A a2 = new A(2);
	final B b = new B();

	@Test
	public void testEquals() {
		assertFalse(UtilsAlt.equals(null, a1));
		assertFalse(UtilsAlt.equals(b, null));
		assertFalse(UtilsAlt.equals(a1, a2));
		assertFalse(UtilsAlt.equals(b, a2));
		
		assertTrue(UtilsAlt.equals(null, null));
		assertTrue(UtilsAlt.equals(a1, a1));
		assertTrue(UtilsAlt.equals(b, b));
		assertTrue(UtilsAlt.equals(a1, a1alt));
		// below is questionable - a and b are different types
		assertTrue(UtilsAlt.equals(a1, b));
	}

	@Test
	public void testNvl() {
		assertNull(UtilsAlt.nvl());
		assertNull(UtilsAlt.nvl((Object)null));
		assertNull(UtilsAlt.nvl((Object[])null));
		assertNull(UtilsAlt.nvl(null, null));
		assertEquals(a1, UtilsAlt.nvl(a1));
		assertEquals(a1, UtilsAlt.nvl(null, a1, null));
		assertEquals(a1, UtilsAlt.nvl(null, a1, a2));
	}

	@Test
	public void testNullOrEmpty() {
		assertTrue(UtilsAlt.nullOrEmpty(null));
		assertTrue(UtilsAlt.nullOrEmpty(""));
		assertTrue(UtilsAlt.nullOrEmpty(Collections.emptyList()));
		assertTrue(UtilsAlt.nullOrEmpty(Collections.emptySet()));
		assertTrue(UtilsAlt.nullOrEmpty(Collections.emptyMap()));
		
		assertFalse(UtilsAlt.nullOrEmpty(1));
		assertFalse(UtilsAlt.nullOrEmpty(" "));
		assertFalse(UtilsAlt.nullOrEmpty(Collections.singletonList(a1)));
		assertFalse(UtilsAlt.nullOrEmpty(Collections.singleton(a1)));
		assertFalse(UtilsAlt.nullOrEmpty(Collections.singletonMap(b, a1)));

		assertTrue(UtilsAlt.nullOrEmpty(new Object[0])); // OOOPS!
		assertFalse(UtilsAlt.nullOrEmpty(new Object[1]));
	}

	@Test
	public void testCompare() {
		assertEquals(0, UtilsAlt.compare(null, null));
		assertEquals(0, UtilsAlt.compare(a1, a1));
		assertEquals(0, UtilsAlt.compare(a1, a1alt));
		assertEquals(-1, UtilsAlt.compare(a1, a2));
		assertEquals(-1, UtilsAlt.compare(a1, null));
		assertEquals(1, UtilsAlt.compare(a2, a1));
		assertEquals(1, UtilsAlt.compare(null, a1));
		// questionable...
		assertEquals(0, UtilsAlt.compare((A)b, b));
	}

	@Test
	public void testIn() {
		assertFalse(UtilsAlt.in(null));
		assertFalse(UtilsAlt.in(1));
		assertFalse(UtilsAlt.in(null, 1));
		assertFalse(UtilsAlt.in(1, (Object[])null));
		assertFalse(UtilsAlt.in(1, (Object)null));
		assertFalse(UtilsAlt.in(1, 2));
		assertFalse(UtilsAlt.in(1, null, 2));
		assertFalse(UtilsAlt.in(1, null, 2, null));

		assertTrue(UtilsAlt.in(1, null, 1, null));
		assertTrue(UtilsAlt.in(1, 1, 2, 3));
		assertTrue(UtilsAlt.in(1, 3, 2, 1));

		assertFalse(UtilsAlt.in(null, (Object[])null));
		assertFalse(UtilsAlt.in(null, (Object)null));
	}

}
