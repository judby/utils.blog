package com.udby.blog.utils;

public class A implements Comparable<A> {
	final int x;
	
	public A(int x) {
		this.x = x;
	}

	@Override
	public boolean equals(Object b) {
		if (this == b) {
			return true;
		}
		if (!(b instanceof A)) {
			return false;
		}
		return this.x == ((A)b).x;
	}

	public int compareTo(A o) {
		if (equals(o)) {
			return 0;
		} else if (this.x < o.x) {
			return -1;
		} else {
			return 1;
		}
	}
}
