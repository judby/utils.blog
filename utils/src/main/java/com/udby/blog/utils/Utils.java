package com.udby.blog.utils;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

public class Utils {
	private Utils() {
	}
	
	/**
     * Returns true if a==b or a.equals(b) handling nulls. 
     * Note this is identical to the java 7 Objects.equals()
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     * @see java.util.Objects#equals(Object, Object)
     */
    public static <T> boolean equals(T a, T b) {
        return (a == b) || (a != null && a.equals(b));
    }

    /**
     * Returns first not-null parameter, or null if all are nulls.
     * Inspired by Oracle database nvl function
     *
     * @param <T>
     * @param ts
     * @return
     */
    public static <T> T nvl(T... ts) {
        if (ts == null) {
            return null; // nino
        }
        for (int i = 0; i < ts.length; i++) {
            if (ts[i] != null) {
                return ts[i];
            }
        }
        return null;
    }

    /**
     * Check for null or empty: Strings, Collections, Arrays...
     *
     * @param o
     * @return
     */
    public static boolean nullOrEmpty(Object o) {
        if (o == null) {
            return true;
        }
        if (o instanceof String) {
            return ((String)o).isEmpty();
        }
        if (o instanceof Collection<?>) {
            return ((Collection<?>)o).isEmpty();
        }
        if (o instanceof Map<?,?>) {
            return ((Map<?,?>)o).isEmpty();
        }
        if (o.getClass().isArray()) {
            return Array.getLength(o) == 0;
        }
        return false;
    }
    
    /**
     * Compares 2 Comparable, nulls-first
     *
     * @param <T>
     * @param a
     * @param b
     * @return
     */
    public static <T extends Comparable<T>> int compare(T a, T b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
       
        return a.compareTo(b);
    }

    
    /**
     * Returns true if needle (not null) is in the haystack (not null)
     *
     * @param <T>
     * @param needle
     * @param haystack
     * @return
     */
    public static <T> boolean in(final T needle, T... haystack) {
        if (needle == null || haystack == null) {
            return false;
        }
        for (T t : haystack) {
            if (equals(needle, t)) {
                return true;
            }
        }
        return false;
    }
}
