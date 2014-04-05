package com.udby.blog.utils;

/**
 * Simple class extending A with defunct equals method
 */
public class B extends A {

	public B() {
		super(1);
	}
	
	@Override
	public boolean equals(Object b) {
		return false;
	}
}
