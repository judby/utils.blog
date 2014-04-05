package com.udby.blog.utils;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public class In {
	public static void main(String[] args) {
		int i = 0;
		
		@SuppressWarnings("serial")
		final Set<Integer> VALID_VALUES = Collections.unmodifiableSet(new TreeSet<Integer>(){
			{
				add(1); 
				add(3);
				add(5);
			}
		});
		
		if (VALID_VALUES.contains(i)) {
			// ...
		}
		
		if (Utils.in(i, 1, 3, 5)) {
			// ...
		}
	}
}
